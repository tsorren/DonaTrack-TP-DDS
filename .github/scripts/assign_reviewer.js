const fs = require('fs');
const path = require('path');

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

  async listReviews(prNumber) {
    const res = await this.github.rest.pulls.listReviews({
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

  async getEligibleReviewers(prNumber, author, allowedReviewers) {
    const collaborators = await this.githubRepo.listCollaborators();
    console.log("Repo collaborators:", collaborators);

    // Filter collaborators by allowed pool in config
    const eligibleCollaborators = collaborators.filter(login => allowedReviewers.includes(login));
    console.log("Eligible pool based on configuration groups:", eligibleCollaborators);

    const commits = await this.githubRepo.listCommits(prNumber);
    const committers = new Set(commits.map(c => c.author ? c.author.login : null).filter(Boolean));
    console.log("Exclude committers:", Array.from(committers));

    return eligibleCollaborators.filter(login => login !== author && !committers.has(login));
  }
}

class RoundRobinSelector {
  static selectReviewer(eligibleReviewers, recentPrsWithReviews) {
    if (eligibleReviewers.length === 0) return null;

    // Count review assignments
    const assignCount = {};
    eligibleReviewers.forEach(r => { assignCount[r] = 0; });

    let lastReviewerAssigned = null;

    for (const pr of recentPrsWithReviews) {
      const reviewersInPr = new Set();
      
      if (pr.requested_reviewers) {
        pr.requested_reviewers.forEach(reviewer => {
          if (eligibleReviewers.includes(reviewer.login)) {
            reviewersInPr.add(reviewer.login);
          }
        });
      }

      if (pr.reviews) {
        pr.reviews.forEach(review => {
          if (review.user && eligibleReviewers.includes(review.user.login)) {
            reviewersInPr.add(review.user.login);
          }
        });
      }

      reviewersInPr.forEach(reviewer => {
        assignCount[reviewer]++;
      });

      if (!lastReviewerAssigned && reviewersInPr.size > 0) {
        lastReviewerAssigned = Array.from(reviewersInPr)[0];
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

    recentPrsWithReviews.forEach((pr, index) => {
      const reviewersInPr = new Set();
      if (pr.requested_reviewers) {
        pr.requested_reviewers.forEach(reviewer => {
          if (eligibleReviewers.includes(reviewer.login)) {
            reviewersInPr.add(reviewer.login);
          }
        });
      }
      if (pr.reviews) {
        pr.reviews.forEach(review => {
          if (review.user && eligibleReviewers.includes(review.user.login)) {
            reviewersInPr.add(review.user.login);
          }
        });
      }

      reviewersInPr.forEach(reviewer => {
        if (lastAssignIndex[reviewer] === -1) {
          lastAssignIndex[reviewer] = index;
        }
      });
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

class AssignReviewerOrchestrator {
  constructor(githubRepo, validator, pool, historyLimit) {
    this.githubRepo = githubRepo;
    this.validator = validator;
    this.pool = pool;
    this.historyLimit = historyLimit;
  }

  async run() {
    const context = this.githubRepo.context;
    const pr = context.payload.pull_request;
    const prNumber = pr.number;
    const prTitle = pr.title;
    const currentBranch = pr.head.ref;
    const author = pr.user.login;

    // 0. Skip if PR already has reviewers requested or assigned
    const requestedReviewers = pr.requested_reviewers || [];
    const requestedTeams = pr.requested_teams || [];
    
    // Fetch latest details to verify if a manual assignment happened during the run
    let latestPr = pr;
    try {
      const res = await this.githubRepo.github.rest.pulls.get({
        owner: context.repo.owner,
        repo: context.repo.repo,
        pull_number: prNumber
      });
      latestPr = res.data;
    } catch (e) {
      console.log(`Failed to fetch latest PR details: ${e.message}`);
    }

    const currentReviewers = latestPr.requested_reviewers || [];
    const currentTeams = latestPr.requested_teams || [];

    if (currentReviewers.length > 0 || currentTeams.length > 0) {
      console.log("PR already has reviewers requested. Skipping auto-assignment.");
      return;
    }

    // 1. Validate Sequentiality
    await this.validator.validateSequentiality(currentBranch, prNumber);

    // 2. Load groups configuration
    const configPath = path.join(process.env.GITHUB_WORKSPACE || '.', '.github/scripts/reviewer_groups.json');
    let config;
    try {
      config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    } catch (err) {
      console.log(`Failed to read reviewer_groups.json: ${err.message}`);
      return;
    }

    // 3. Resolve priority/impact level of the PR
    let priority = null;

    // Look in PR labels
    if (latestPr.labels && latestPr.labels.length > 0) {
      const priorityLabel = latestPr.labels.find(l => l.name && l.name.startsWith('prioridad:'));
      if (priorityLabel) {
        const match = priorityLabel.name.match(/^prioridad:(alta|media|baja)$/i);
        if (match) {
          priority = match[1].toUpperCase();
        }
      }
    }

    // Fallback to PR title prefix
    if (!priority) {
      const match = prTitle.match(/^\[(ALTA|MEDIA|BAJA)\]/i);
      if (match) {
        priority = match[1].toUpperCase();
      }
    }

    // Fallback to default
    if (!priority) {
      priority = config.default_group || "MEDIA";
      console.log(`No priority detected. Falling back to default: ${priority}`);
      
      const comment = `⚠️ **Advertencia:** Esta PR no contiene una clasificación de impacto en su título (\`[ALTA]\`, \`[MEDIA]\`, \`[BAJA]\`) ni etiquetas asociadas. Se ha asumido la prioridad por defecto **${priority}**.`;
      try {
        await this.githubRepo.createPRComment(prNumber, comment);
      } catch (err) {
        console.log(`Failed to create fallback warning comment: ${err.message}`);
      }
    } else {
      console.log(`Resolved priority: ${priority}`);
    }

    const levels = config.levels || {};
    const altaDevs = levels.ALTA || [];
    const mediaDevs = levels.MEDIA || [];
    const bajaDevs = levels.BAJA || [];

    let allowedReviewers = [];
    if (priority === 'BAJA') {
      // BAJA tasks are reviewed strictly by BAJA (Juniors) and MEDIA (Mids) levels, excluding ALTA (Seniors)
      allowedReviewers = [...bajaDevs, ...mediaDevs];
    } else if (priority === 'MEDIA' || priority === 'ALTA') {
      // MEDIA and ALTA tasks are reviewed strictly by ALTA level (Seniors)
      allowedReviewers = [...altaDevs];
    } else {
      allowedReviewers = [...altaDevs, ...mediaDevs];
    }

    console.log(`PR Priority: ${priority} -> Routing to Reviewer Pool (Size: ${allowedReviewers.length})`);
    console.log("Allowed reviewers:", allowedReviewers);

    if (allowedReviewers.length === 0) {
      console.log("No allowed reviewers configured for this priority routing.");
      return;
    }

    // 4. Get Eligible Reviewers from the configured pool
    const eligibleReviewers = await this.pool.getEligibleReviewers(prNumber, author, allowedReviewers);
    if (eligibleReviewers.length === 0) {
      console.log("No eligible reviewers found.");
      return;
    }

    // 5. List recent PRs (reducing window to 15 to avoid rate limits)
    const recentPrs = await this.githubRepo.listPullRequests("all", this.historyLimit);

    // Fetch actual reviews for the recent PRs to resolve the bug
    const recentPrsWithReviews = [];
    for (const recentPr of recentPrs) {
      if (recentPr.draft === true) continue;
      try {
        const reviews = await this.githubRepo.listReviews(recentPr.number);
        recentPrsWithReviews.push({
          number: recentPr.number,
          requested_reviewers: recentPr.requested_reviewers || [],
          reviews: reviews
        });
      } catch (e) {
        console.log(`Failed to list reviews for PR #${recentPr.number}: ${e.message}`);
        recentPrsWithReviews.push({
          number: recentPr.number,
          requested_reviewers: recentPr.requested_reviewers || [],
          reviews: []
        });
      }
    }

    // 6. Select Reviewer
    const selectedReviewer = RoundRobinSelector.selectReviewer(eligibleReviewers, recentPrsWithReviews);
    if (!selectedReviewer) {
      console.log("Reviewer selection returned null.");
      return;
    }

    // 7. Request Review
    await this.githubRepo.requestReviewers(prNumber, [selectedReviewer]);
    console.log(`Assigned review to ${selectedReviewer}`);
  }
}

module.exports = async ({ github, context }) => {
  const githubRepo = new GitHubRepository(github, context);
  const validator = new PullRequestValidator(githubRepo);
  const pool = new CollaboratorPool(githubRepo);

  const historyLimitStr = process.env.RECENT_PR_LIMIT || '15';
  const historyLimit = parseInt(historyLimitStr);

  const orchestrator = new AssignReviewerOrchestrator(githubRepo, validator, pool, historyLimit);
  await orchestrator.run();
};
