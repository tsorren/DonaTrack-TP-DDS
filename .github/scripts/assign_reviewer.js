class GitHubRepository {
  constructor(github, context) {
    this.github = github;
    this.context = context;
    this.repo = context.repo;
  }

  async listPullRequests(state = "all", perPage = 50) {
    const res = await this.github.rest.pulls.list({
      owner: this.repo.owner,
      repo: this.repo.repo,
      state,
      per_page: perPage
    });
    return res.data;
  }

  async createPRComment(prNumber, body) {
    await this.github.rest.issues.createComment({
      owner: this.repo.owner,
      repo: this.repo.repo,
      issue_number: prNumber,
      body
    });
  }

  async listCollaborators() {
    const res = await this.github.rest.repos.listCollaborators({
      owner: this.repo.owner,
      repo: this.repo.repo
    });
    return res.data.map(c => c.login);
  }

  async listCommits(prNumber) {
    const res = await this.github.rest.pulls.listCommits({
      owner: this.repo.owner,
      repo: this.repo.repo,
      pull_number: prNumber
    });
    return res.data;
  }

  async requestReviewers(prNumber, reviewers) {
    await this.github.rest.pulls.requestReviewers({
      owner: this.repo.owner,
      repo: this.repo.repo,
      pull_number: prNumber,
      reviewers
    });
  }
}

class PullRequestValidator {
  constructor(githubRepo) {
    this.githubRepo = githubRepo;
  }

  async validateSequentiality(currentBranch, prNumber) {
    const patternStr = process.env.BRANCH_PATTERN || '^E(\\d+)_req-(\\d+)-task(\\d+)$';
    const branchMatch = currentBranch.match(new RegExp(patternStr));
    if (!branchMatch) return true; // Not a stacked PR branch

    const deliveryNum = branchMatch[1];
    const reqId = branchMatch[2];
    const taskNum = parseInt(branchMatch[3]);

    if (taskNum <= 1) return true; // First task doesn't have precedents

    console.log(`Validating sequentiality for Req #${reqId}, Task ${taskNum}`);
    const prs = await this.githubRepo.listPullRequests("all", 100);

    for (let k = 1; k < taskNum; k++) {
      const precedingBranch = `E${deliveryNum}_req-${reqId}-task${k}`;
      const precedingPr = prs.find(pr => pr.head.ref === precedingBranch);

      if (precedingPr && precedingPr.draft === true) {
        const comment = `⚠️ **Validación de Secuencialidad:** No se puede solicitar revisión para la **Task ${taskNum}** porque la **Task ${k}** precedente ([PR #${precedingPr.number}](${precedingPr.html_url})) aún se encuentra en estado Borrador (Draft).\n\nLas revisiones deben realizarse en orden secuencial.`;
        await this.githubRepo.createPRComment(prNumber, comment);
        throw new Error(`Sequentiality Block: preceding Task ${k} is still a draft.`);
      }
    }

    return true;
  }
}

class CollaboratorPool {
  constructor(githubRepo) {
    this.githubRepo = githubRepo;
  }

  async getEligibleReviewers(prNumber, author) {
    const collaborators = await this.githubRepo.listCollaborators();
    console.log("Repo collaborators:", collaborators);

    const commits = await this.githubRepo.listCommits(prNumber);
    const committers = new Set(commits.map(c => c.author ? c.author.login : null).filter(Boolean));
    console.log("Exclude committers:", Array.from(committers));

    return collaborators.filter(login => login !== author && !committers.has(login));
  }
}

class RoundRobinSelector {
  static selectReviewer(eligibleReviewers, recentPrs) {
    if (eligibleReviewers.length === 0) return null;

    // Count review assignments
    const assignCount = {};
    eligibleReviewers.forEach(r => { assignCount[r] = 0; });

    let lastReviewerAssigned = null;

    for (const pr of recentPrs) {
      if (pr.requested_reviewers && pr.requested_reviewers.length > 0) {
        pr.requested_reviewers.forEach(reviewer => {
          if (eligibleReviewers.includes(reviewer.login)) {
            assignCount[reviewer.login]++;
          }
        });

        if (!lastReviewerAssigned) {
          const eligibleReq = pr.requested_reviewers.find(r => eligibleReviewers.includes(r.login));
          if (eligibleReq) {
            lastReviewerAssigned = eligibleReq.login;
          }
        }
      }
    }

    console.log("Round-Robin assignments counts:", assignCount);
    console.log("Last reviewer assigned:", lastReviewerAssigned);

    // Apply consecutive avoidance pool
    let pool = eligibleReviewers.filter(r => r !== lastReviewerAssigned);
    if (pool.length === 0) {
      pool = eligibleReviewers;
    }

    // Determine oldest assignment (tie-breaker)
    const lastAssignIndex = {};
    eligibleReviewers.forEach(r => { lastAssignIndex[r] = -1; });

    recentPrs.forEach((pr, index) => {
      if (pr.requested_reviewers) {
        pr.requested_reviewers.forEach(reviewer => {
          if (eligibleReviewers.includes(reviewer.login) && lastAssignIndex[reviewer.login] === -1) {
            lastAssignIndex[reviewer.login] = index;
          }
        });
      }
    });

    let selectedReviewer = null;
    let minCount = Infinity;

    pool.forEach(r => {
      const count = assignCount[r] || 0;
      if (count < minCount) {
        minCount = count;
        selectedReviewer = r;
      } else if (count === minCount) {
        const currentIdx = lastAssignIndex[r];
        const selectedIdx = lastAssignIndex[selectedReviewer];

        if (currentIdx === -1 && selectedIdx !== -1) {
          selectedReviewer = r;
        } else if (currentIdx !== -1 && selectedIdx !== -1 && currentIdx > selectedIdx) {
          selectedReviewer = r;
        }
      }
    });

    return selectedReviewer;
  }
}

class DiscordNotifier {
  constructor(webhookUrl, userMapStr) {
    this.webhookUrl = webhookUrl;
    this.userMap = this.parseUserMap(userMapStr);
  }

  parseUserMap(str) {
    if (!str) return {};
    try {
      return JSON.parse(str);
    } catch (e) {
      console.log(`Failed to parse DISCORD_USER_MAP secret: ${e.message}`);
      return {};
    }
  }

  async notifyAssignment(reviewer, prNumber, prTitle, prUrl) {
    if (!this.webhookUrl) return;

    const discordId = this.userMap[reviewer];
    const mention = discordId ? `<@${discordId}>` : `@${reviewer}`;
    const message = `👀 **Nueva PR asignada para revisión:**\nPR #${prNumber}: [${prTitle}](${prUrl})\nRevisor asignado: ${mention}`;

    try {
      await fetch(this.webhookUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content: message })
      });
      console.log("Discord notification sent successfully.");
    } catch (error) {
      console.log(`Failed to send Discord webhook: ${error.message}`);
    }
  }
}

class AssignReviewerOrchestrator {
  constructor(githubRepo, validator, pool, notifier, historyLimit) {
    this.githubRepo = githubRepo;
    this.validator = validator;
    this.pool = pool;
    this.notifier = notifier;
    this.historyLimit = historyLimit;
  }

  async run() {
    const context = this.githubRepo.context;
    const prNumber = context.payload.pull_request.number;
    const prTitle = context.payload.pull_request.title;
    const prUrl = context.payload.pull_request.html_url;
    const currentBranch = context.payload.pull_request.head.ref;
    const author = context.payload.pull_request.user.login;

    // 1. Validate Sequentiality
    await this.validator.validateSequentiality(currentBranch, prNumber);

    // 2. Get Eligible Reviewers
    const eligibleReviewers = await this.pool.getEligibleReviewers(prNumber, author);
    if (eligibleReviewers.length === 0) {
      console.log("No eligible reviewers found.");
      return;
    }

    // 3. List recent PRs
    const recentPrs = await this.githubRepo.listPullRequests("all", this.historyLimit);

    // 4. Select Reviewer
    const selectedReviewer = RoundRobinSelector.selectReviewer(eligibleReviewers, recentPrs);
    if (!selectedReviewer) {
      console.log("Reviewer selection returned null.");
      return;
    }

    // 5. Request Review
    await this.githubRepo.requestReviewers(prNumber, [selectedReviewer]);
    console.log(`Assigned review to ${selectedReviewer}`);

    // 6. Notify Discord
    await this.notifier.notifyAssignment(selectedReviewer, prNumber, prTitle, prUrl);
  }
}

module.exports = async ({ github, context }) => {
  const githubRepo = new GitHubRepository(github, context);
  const validator = new PullRequestValidator(githubRepo);
  const pool = new CollaboratorPool(githubRepo);
  const notifier = new DiscordNotifier(
    process.env.DISCORD_WEBHOOK_URL,
    process.env.DISCORD_USER_MAP
  );

  const historyLimitStr = process.env.RECENT_PR_LIMIT || '50';
  const historyLimit = parseInt(historyLimitStr);

  const orchestrator = new AssignReviewerOrchestrator(githubRepo, validator, pool, notifier, historyLimit);
  await orchestrator.run();
};
