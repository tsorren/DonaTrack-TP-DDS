const { DiscordNotifierChannel, UserResolver, PRReviewNotificationFormatter } = require('./shared_notifier.js');

module.exports = async ({ github, context }) => {
  const pr = context.payload.pull_request;
  
  // Wait, if the event is ready_for_review, we want to notify all requested reviewers currently on the PR.
  // If the event is review_requested, the payload has pr.requested_reviewers, or context.payload.requested_reviewer which is the specific reviewer requested.
  // To make it robust and unified, we can fetch the latest state of the PR from GitHub API.
  console.log(`Checking requested reviewers for PR #${pr.number}...`);
  
  let currentPr;
  try {
    const res = await github.rest.pulls.get({
      owner: context.repo.owner,
      repo: context.repo.repo,
      pull_number: pr.number
    });
    currentPr = res.data;
  } catch (err) {
    console.log(`Failed to fetch PR details: ${err.message}`);
    currentPr = pr; // Fallback to context payload
  }

  const reviewers = currentPr.requested_reviewers.map(r => r.login);
  console.log("Currently requested reviewers:", reviewers);

  if (reviewers.length === 0) {
    console.log("No requested reviewers to notify.");
    return;
  }

  const resolver = new UserResolver(process.env.DISCORD_USER_MAP);
  const formatter = new PRReviewNotificationFormatter(resolver);
  const message = formatter.format({
    prNumber: currentPr.number,
    prTitle: currentPr.title,
    prUrl: currentPr.html_url,
    reviewers
  });

  const channel = new DiscordNotifierChannel(process.env.DISCORD_WEBHOOK_URL);
  try {
    await channel.send(message);
    console.log("Discord notification dispatched successfully.");
  } catch (err) {
    console.log(`Failed to send Discord notification: ${err.message}`);
  }
};
