const fs = require('fs');
const path = require('path');
const { DiscordNotifierChannel, UserResolver, IssueAutoAssignedNotificationFormatter } = require('./shared_notifier.js');

class GitHubIssueRepository {
  constructor(github, context) {
    this.github = github;
    this.context = context;
    this.repo = context.repo;
  }

  async listUnassignedIssues() {
    // Fetches open issues with no assignee. Note: PRs are technically issues in GitHub API.
    const res = await this.github.rest.issues.listForRepo({
      owner: this.repo.owner,
      repo: this.repo.repo,
      state: "open",
      assignee: "none",
      per_page: 100
    });
    // Filter out Pull Requests
    return res.data.filter(issue => !issue.pull_request);
  }

  async listRecentIssues(limit = 30) {
    const res = await this.github.rest.issues.listForRepo({
      owner: this.repo.owner,
      repo: this.repo.repo,
      state: "all",
      per_page: limit
    });
    // Filter out Pull Requests
    return res.data.filter(issue => !issue.pull_request);
  }

  async assignIssue(issueNumber, assignees) {
    await this.github.rest.issues.addAssignees({
      owner: this.repo.owner,
      repo: this.repo.repo,
      issue_number: issueNumber,
      assignees
    });
  }
}

class IssueRoundRobinSelector {
  static selectAssignee(eligiblePool, recentIssues) {
    if (eligiblePool.length === 0) return null;

    // Initialize assignment counts
    const assignCount = {};
    eligiblePool.forEach(u => { assignCount[u] = 0; });

    // Last assignee trace (for consecutive avoidance)
    let lastAssignee = null;

    for (const issue of recentIssues) {
      if (issue.assignees && issue.assignees.length > 0) {
        issue.assignees.forEach(assignee => {
          if (eligiblePool.includes(assignee.login)) {
            assignCount[assignee.login]++;
          }
        });

        if (!lastAssignee) {
          const eligibleAssignee = issue.assignees.find(a => eligiblePool.includes(a.login));
          if (eligibleAssignee) {
            lastAssignee = eligibleAssignee.login;
          }
        }
      }
    }

    console.log("Issue Round-Robin counts:", assignCount);
    console.log("Last assignee found:", lastAssignee);

    // Apply consecutive avoidance
    let pool = eligiblePool.filter(u => u !== lastAssignee);
    if (pool.length === 0) {
      pool = eligiblePool;
    }

    // Determine oldest assignment (tie-breaker index)
    const lastAssignIndex = {};
    eligiblePool.forEach(u => { lastAssignIndex[u] = -1; });

    recentIssues.forEach((issue, index) => {
      if (issue.assignees) {
        issue.assignees.forEach(assignee => {
          if (eligiblePool.includes(assignee.login) && lastAssignIndex[assignee.login] === -1) {
            lastAssignIndex[assignee.login] = index;
          }
        });
      }
    });

    let selectedAssignee = null;
    let minCount = Infinity;

    pool.forEach(u => {
      const count = assignCount[u] || 0;
      if (count < minCount) {
        minCount = count;
        selectedAssignee = u;
      } else if (count === minCount) {
        const currentIdx = lastAssignIndex[u];
        const selectedIdx = lastAssignIndex[selectedAssignee];

        // Preference to developer who has no recent assignments (-1)
        // or whose last assignment is older (higher index in reverse-chronological list)
        if (currentIdx === -1 && selectedIdx !== -1) {
          selectedAssignee = u;
        } else if (currentIdx !== -1 && selectedIdx !== -1 && currentIdx > selectedIdx) {
          selectedAssignee = u;
        }
      }
    });

    return selectedAssignee;
  }
}

module.exports = async ({ github, context }) => {
  const repo = context.repo;
  const issueRepo = new GitHubIssueRepository(github, context);

  // 1. Fetch unassigned issues
  const unassignedIssues = await issueRepo.listUnassignedIssues();
  console.log(`Found ${unassignedIssues.length} unassigned open issues (excl. PRs).`);

  if (unassignedIssues.length === 0) {
    console.log("No unassigned issues to process.");
    return;
  }

  // 2. Load configurations
  const configPath = path.join(process.env.GITHUB_WORKSPACE || '.', '.github/scripts/reviewer_groups.json');
  let config;
  try {
    config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  } catch (err) {
    console.log(`Failed to read reviewer_groups.json config: ${err.message}`);
    return;
  }

  const resolver = new UserResolver(process.env.DISCORD_USER_MAP);
  const formatter = new IssueAutoAssignedNotificationFormatter(resolver);
  const channel = new DiscordNotifierChannel(process.env.DISCORD_WEBHOOK_URL);

  // 3. Fetch recent issues for Round-Robin state
  const recentIssues = await issueRepo.listRecentIssues(50);

  // 4. Process each issue
  for (const issue of unassignedIssues) {
    console.log(`\nProcessing Issue #${issue.number}: "${issue.title}"`);

    // Resolve priority
    let priority = null;
    
    // Check labels first
    if (issue.labels && issue.labels.length > 0) {
      const priorityLabel = issue.labels.find(l => l.name && l.name.startsWith('prioridad:'));
      if (priorityLabel) {
        const match = priorityLabel.name.match(/^prioridad:(alta|media|baja)$/i);
        if (match) {
          priority = match[1].toUpperCase();
        }
      }
    }

    // Fallback to title prefix
    if (!priority) {
      const match = issue.title.match(/^\[(ALTA|MEDIA|BAJA)\]/i);
      if (match) {
        priority = match[1].toUpperCase();
      }
    }

    // Fallback to default
    if (!priority) {
      priority = config.default_group || "MEDIA";
      console.log(`No priority detected. Falling back to default: ${priority}`);
    } else {
      console.log(`Resolved priority: ${priority}`);
    }

    // Get developer pool based on priority levels
    const levels = config.levels || {};
    const pool = levels[priority] || levels[config.default_group] || [];
    console.log(`Developer pool for priority ${priority}:`, pool);

    if (pool.length === 0) {
      console.log(`Empty developer pool for priority ${priority}. Skipping.`);
      continue;
    }

    // Select assignee
    const selectedAssignee = IssueRoundRobinSelector.selectAssignee(pool, recentIssues);
    console.log(`Selected assignee: ${selectedAssignee}`);

    if (!selectedAssignee) {
      console.log(`Could not select an assignee. Skipping.`);
      continue;
    }

    // Assign in GitHub
    try {
      await issueRepo.assignIssue(issue.number, [selectedAssignee]);
      console.log(`Successfully assigned ${selectedAssignee} to Issue #${issue.number}`);
    } catch (err) {
      console.log(`Failed to assign developer in GitHub: ${err.message}`);
      continue;
    }

    // Notify Discord
    const message = formatter.format({
      issueNumber: issue.number,
      issueTitle: issue.title,
      issueUrl: issue.html_url,
      assignee: selectedAssignee
    });

    try {
      await channel.send(message);
      console.log("Discord notification sent.");
    } catch (err) {
      console.log(`Failed to send notification: ${err.message}`);
    }
  }
};
