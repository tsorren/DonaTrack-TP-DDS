const fs = require('fs');
const path = require('path');

class IssueBodyParser {
  static parseMilestone(body) {
    const milestoneMatch = body.match(/Entrega\s+(\d+)/i);
    return milestoneMatch ? milestoneMatch[1] : "1";
  }

  static parseAssignments(body) {
    const assignments = {};
    const lines = body.split("\n");
    for (const line of lines) {
      const match = line.match(/(?:Task\s*(\d+)|\[?([^\]:]+)\]?)\s*:\s*@?([a-zA-Z0-9-]+)/i);
      if (match) {
        const taskNum = match[1];
        const taskName = match[2];
        const username = match[3].trim();
        if (taskNum) {
          assignments[parseInt(taskNum)] = username;
        } else if (taskName) {
          assignments[taskName.toLowerCase().trim()] = username;
        }
      }
    }
    return assignments;
  }
}

class GitHubRepository {
  constructor(github, context) {
    this.github = github;
    this.context = context;
    this.repo = context.repo;
  }

  async getBranchSha(branchName) {
    try {
      const refData = await this.github.rest.git.getRef({
        owner: this.repo.owner,
        repo: this.repo.repo,
        ref: `heads/${branchName}`
      });
      return refData.data.object.sha;
    } catch (error) {
      throw new Error(`Branch heads/${branchName} not found: ${error.message}`);
    }
  }

  async createBranch(branchName, sha) {
    await this.github.rest.git.createRef({
      owner: this.repo.owner,
      repo: this.repo.repo,
      ref: `refs/heads/${branchName}`,
      sha: sha
    });
  }

  async createIssue(title, body, assignees) {
    const response = await this.github.rest.issues.create({
      owner: this.repo.owner,
      repo: this.repo.repo,
      title: title,
      body: body,
      assignees: assignees
    });
    return response.data;
  }

  async createPullRequest(title, head, base, body, draft = true) {
    const response = await this.github.rest.pulls.create({
      owner: this.repo.owner,
      repo: this.repo.repo,
      title: title,
      head: head,
      base: base,
      body: body,
      draft: draft
    });
    return response.data;
  }

  async updateIssueBody(issueNumber, body) {
    await this.github.rest.issues.update({
      owner: this.repo.owner,
      repo: this.repo.repo,
      issue_number: issueNumber,
      body: body
    });
  }
}

class IssueTemplateRenderer {
  constructor(templateFilePath) {
    this.templateContent = this.loadTemplate(templateFilePath);
  }

  loadTemplate(filePath) {
    try {
      if (fs.existsSync(filePath)) {
        return fs.readFileSync(filePath, 'utf8');
      }
    } catch (e) {
      console.log(`Failed to read template file at ${filePath}: ${e.message}`);
    }
    // Fallback template
    return `# [TASK-{task_id}] - {task_title}\n\nSub-tarea correspondiente al requerimiento principal #{parent_id}.\n\nEtapa: **{task_name}**`;
  }

  render(params) {
    let content = this.templateContent;
    Object.keys(params).forEach(key => {
      content = content.replace(new RegExp(`{${key}}`, 'g'), params[key]);
    });
    return content;
  }
}

class CascadeOrchestrator {
  constructor(githubRepository, templateRenderer, standardTasks) {
    this.gitHubRepo = githubRepository;
    this.templateRenderer = templateRenderer;
    this.standardTasks = standardTasks;
  }

  async run() {
    const context = this.gitHubRepo.context;
    const body = context.payload.issue.body || "";
    const issueNumber = context.payload.issue.number;

    console.log(`Starting cascade flow orchestration for issue #${issueNumber}`);

    const deliveryNum = IssueBodyParser.parseMilestone(body);
    const assignments = IssueBodyParser.parseAssignments(body);

    // Get Base branch SHA
    let baseSha;
    try {
      baseSha = await this.gitHubRepo.getBranchSha(`ENTREGA_${deliveryNum}`);
    } catch (e) {
      console.log(`ENTREGA_${deliveryNum} branch not found, falling back to main.`);
      try {
        baseSha = await this.gitHubRepo.getBranchSha("main");
      } catch (err) {
        console.log(`Failed to get base branch SHA: ${err.message}`);
        return;
      }
    }

    // Create parent branch
    const reqId = `req-${issueNumber}`;
    const parentBranchName = `E${deliveryNum}_${reqId}`;
    try {
      await this.gitHubRepo.createBranch(parentBranchName, baseSha);
      console.log(`Created parent branch: ${parentBranchName}`);
    } catch (e) {
      console.log(`Parent branch ${parentBranchName} already exists or failed: ${e.message}`);
    }

    // Create sub-issues
    const createdSubIssues = [];
    for (const task of this.standardTasks) {
      const assignee = assignments[task.id] || assignments[task.name.toLowerCase()] || null;
      const assignees = assignee ? [assignee] : [];

      const renderedBody = this.templateRenderer.render({
        task_id: task.id,
        task_title: task.title,
        parent_id: issueNumber,
        task_name: task.name
      });

      try {
        const title = `[REQ-${issueNumber}] [TASK-${task.id}] - [${task.name}] ${task.title}`;
        const issue = await this.gitHubRepo.createIssue(title, renderedBody, assignees);
        console.log(`Created Issue #${issue.number} for Task ${task.id}`);
        createdSubIssues.push({
          taskNum: task.id,
          number: issue.number,
          title: `[${task.name}] ${task.title}`,
          assignee
        });
      } catch (err) {
        console.log(`Failed to create Issue for Task ${task.id}: ${err.message}`);
      }
    }

    // Create stacked branches and PRs (excluding Task 8)
    let lastBranchName = parentBranchName;
    let lastSha = baseSha;

    try {
      lastSha = await this.gitHubRepo.getBranchSha(parentBranchName);
    } catch (e) {}

    for (const si of createdSubIssues) {
      if (si.taskNum === 8) {
        console.log(`Skipping branch/PR creation for Task 8. Managed manually.`);
        continue;
      }

      const branchName = `E${deliveryNum}_req-${issueNumber}-task${si.taskNum}`;

      // Create Branch
      try {
        await this.gitHubRepo.createBranch(branchName, lastSha);
        console.log(`Created branch ${branchName}`);
      } catch (e) {
        console.log(`Branch ${branchName} already exists or failed: ${e.message}`);
      }

      // Create Draft PR
      try {
        const title = `[REQ-${issueNumber}] PR Task ${si.taskNum} - ${si.title}`;
        const prBody = `PR correspondiente a la sub-issue #${si.number}.\n\nEsta PR forma parte de los Stacked PRs para el requerimiento #${issueNumber}.\n\nPor favor, realiza las reviews en orden secuencial.`;
        const pr = await this.gitHubRepo.createPullRequest(title, branchName, lastBranchName, prBody, true);
        console.log(`Created Draft PR #${pr.number} (${branchName} -> ${lastBranchName})`);
      } catch (e) {
        console.log(`Failed to create PR for ${branchName}: ${e.message}`);
      }

      lastBranchName = branchName;
      try {
        lastSha = await this.gitHubRepo.getBranchSha(branchName);
      } catch (e) {}
    }

    // Create Parent PR (parentBranchName -> ENTREGA_N)
    try {
      const parentPrTitle = `[REQ-${issueNumber}] Requerimiento Principal - ${context.payload.issue.title}`;
      const parentPrBody = `PR principal para el requerimiento #${issueNumber}.\n\nSub-issues asociadas:\n` +
                            createdSubIssues.map(si => `- #${si.number} (Task ${si.taskNum}): ${si.title}`).join("\n");
      const parentPr = await this.gitHubRepo.createPullRequest(parentPrTitle, parentBranchName, `ENTREGA_${deliveryNum}`, parentPrBody, true);
      console.log(`Created Parent Draft PR #${parentPr.number}`);
    } catch (e) {
      console.log(`Failed to create parent PR: ${e.message}`);
    }

    // Update parent issue body
    let updatedBody = body;
    updatedBody += `\n\n### Sub-Issues Generadas:\n`;
    for (const si of createdSubIssues) {
      updatedBody += `- [ ] #${si.number} Task ${si.taskNum}: ${si.title}\n`;
    }

    try {
      await this.gitHubRepo.updateIssueBody(issueNumber, updatedBody);
      console.log("Updated parent issue body with sub-issues.");
    } catch (e) {
      console.log(`Failed to update parent issue body: ${e.message}`);
    }
  }
}

module.exports = async ({ github, context }) => {
  const workspace = process.env.GITHUB_WORKSPACE || '.';
  
  const templatePathEnv = process.env.SUB_ISSUE_TEMPLATE_PATH || '.github/scripts/sub_issue_body_template.md';
  const templatePath = path.join(workspace, templatePathEnv);

  const standardTasksPathEnv = process.env.STANDARD_TASKS_PATH || '.github/scripts/standard_tasks.json';
  const standardTasksPath = path.join(workspace, standardTasksPathEnv);

  let standardTasks;
  try {
    if (fs.existsSync(standardTasksPath)) {
      standardTasks = JSON.parse(fs.readFileSync(standardTasksPath, 'utf8'));
    }
  } catch (e) {
    console.log(`Failed to read standard tasks from ${standardTasksPath}: ${e.message}`);
  }

  if (!standardTasks) {
    standardTasks = [
      { id: 1, name: "Estructura", title: "Creación de la estructura de la implementación (paquetes, entidades, interfaces)" },
      { id: 2, name: "Dominio", title: "Implementación de la lógica de la capa de dominio" },
      { id: 3, name: "Tests Unidad", title: "Desarrollo de pruebas unitarias correspondientes" },
      { id: 4, name: "Persistencia", title: "Implementación de gestores, servicios y repositorios de datos" },
      { id: 5, name: "Tests Orquestación", title: "Desarrollo de pruebas de integración y orquestación de servicios" },
      { id: 6, name: "Controladores/Endpoints", title: "Implementación de controladores, endpoints y planificadores" },
      { id: 7, name: "Tests UI/Endpoints", title: "Desarrollo de pruebas del controlador, endpoint y planificador" },
      { id: 8, name: "Calidad y Diseño", title: "Documentación de decisiones (ADR) y verificación de consistencia con diagramas UML" }
    ];
  }

  const githubRepo = new GitHubRepository(github, context);
  const templateRenderer = new IssueTemplateRenderer(templatePath);
  const orchestrator = new CascadeOrchestrator(githubRepo, templateRenderer, standardTasks);

  await orchestrator.run();
};
