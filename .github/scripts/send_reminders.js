const { DiscordNotifierChannel, UserResolver, PRInactivityNotificationFormatter } = require('./shared_notifier.js');

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

class PRRemindersOrchestrator {
  constructor(githubRepo, filter, resolver, formatter, channel, prState, limitHours) {
    this.githubRepo = githubRepo;
    this.filter = filter;
    this.resolver = resolver;
    this.formatter = formatter;
    this.channel = channel;
    this.prState = prState;
    this.limitHours = limitHours;
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
        const reviewerLogins = requestedReviewers.map(r => r.login);

        const message = this.formatter.format({
          prNumber: pr.number,
          prTitle: pr.title,
          prUrl: pr.html_url,
          reviewers: reviewerLogins,
          limitHours: this.limitHours
        });

        try {
          await this.channel.send(message);
          console.log(`Notification sent for PR #${pr.number}`);
        } catch (err) {
          console.log(`Failed to post to Discord: ${err.message}`);
        }
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

  const resolver = new UserResolver(userMapStr);
  const formatter = new PRInactivityNotificationFormatter(resolver);
  const channel = new DiscordNotifierChannel(webhookUrl);

  const orchestrator = new PRRemindersOrchestrator(
    githubRepo,
    filter,
    resolver,
    formatter,
    channel,
    prState,
    limitHours
  );
  await orchestrator.run();
};
