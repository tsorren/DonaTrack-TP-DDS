class GitHubRepository {
  constructor(github, context) {
    this.github = github;
    this.context = context;
    this.repo = context.repo;
  }

  async listPullRequests(state = "open") {
    const res = await this.github.rest.pulls.list({
      owner: this.repo.owner,
      repo: this.repo.repo,
      state: state
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
}

class PRInactivityFilter {
  constructor(githubRepo, inactivityLimitMs) {
    this.githubRepo = githubRepo;
    this.inactivityLimitMs = inactivityLimitMs;
  }

  isDraft(pr) {
    return pr.draft === true;
  }

  isInactiveLimitExceeded(pr, now) {
    const updatedAt = new Date(pr.updated_at);
    return (now - updatedAt) > this.inactivityLimitMs;
  }

  async hasApproval(prNumber) {
    try {
      const reviews = await this.githubRepo.listReviews(prNumber);
      return reviews.some(r => r.state === "APPROVED");
    } catch (e) {
      console.log(`Failed to list reviews for PR #${prNumber}: ${e.message}`);
      return false;
    }
  }

  async shouldRemind(pr, now) {
    if (this.isDraft(pr)) return false;
    if (!this.isInactiveLimitExceeded(pr, now)) return false;
    
    const approved = await this.hasApproval(pr.number);
    return !approved;
  }
}

class DiscordNotifier {
  constructor(webhookUrl, userMapStr, limitHours) {
    this.webhookUrl = webhookUrl;
    this.userMap = this.parseUserMap(userMapStr);
    this.limitHours = limitHours;
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

  async sendDiscordNotification(content) {
    try {
      await fetch(this.webhookUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content })
      });
      console.log(`Notification sent: "${content}"`);
    } catch (error) {
      console.log(`Failed to post to Discord: ${error.message}`);
    }
  }

  async sendReminder(pr, reviewers) {
    if (reviewers.length > 0) {
      const mentions = reviewers.map(reviewer => {
        const discordId = this.userMap[reviewer.login];
        return discordId ? `<@${discordId}>` : `@${reviewer.login}`;
      }).join(", ");

      const message = `⚠️ ${mentions}, la PR #${pr.number} ("${pr.title}") [ver aquí](${pr.html_url}) está esperando tu revisión hace más de ${this.limitHours} horas.`;
      await this.sendDiscordNotification(message);
    } else {
      const message = `🚨 **Alerta:** La PR #${pr.number} ("${pr.title}") [ver aquí](${pr.html_url}) lleva más de ${this.limitHours} horas abierta y no tiene revisores asignados.`;
      await this.sendDiscordNotification(message);
    }
  }
}

class PRRemindersOrchestrator {
  constructor(githubRepo, filter, notifier, prState) {
    this.githubRepo = githubRepo;
    this.filter = filter;
    this.notifier = notifier;
    this.prState = prState;
  }

  async run() {
    console.log(`Starting PR inactivity reminder scan (Target State: ${this.prState})...`);

    const openPrs = await this.githubRepo.listPullRequests(this.prState);
    const now = new Date();

    for (const pr of openPrs) {
      const shouldTrigger = await this.filter.shouldRemind(pr, now);
      if (shouldTrigger) {
        console.log(`PR #${pr.number} qualifies for reminder.`);
        const requestedReviewers = pr.requested_reviewers || [];
        await this.notifier.sendReminder(pr, requestedReviewers);
      }
    }
    console.log("Reminder scan completed.");
  }
}

module.exports = async ({ github, context }) => {
  const webhookUrl = process.env.DISCORD_WEBHOOK_URL;
  const userMapStr = process.env.DISCORD_USER_MAP;

  if (!webhookUrl) {
    console.log("Error: DISCORD_WEBHOOK_URL environment variable is missing.");
    return;
  }

  const prState = process.env.PR_STATE || 'open';
  const limitHoursStr = process.env.INACTIVITY_LIMIT_HOURS || '48';
  const limitHours = parseInt(limitHoursStr);
  const inactivityLimitMs = limitHours * 60 * 60 * 1000;

  const githubRepo = new GitHubRepository(github, context);
  const filter = new PRInactivityFilter(githubRepo, inactivityLimitMs);
  const notifier = new DiscordNotifier(webhookUrl, userMapStr, limitHours);

  const orchestrator = new PRRemindersOrchestrator(githubRepo, filter, notifier, prState);
  await orchestrator.run();
};
