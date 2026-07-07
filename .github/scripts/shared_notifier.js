class NotifierChannel {
  async send(content) {
    throw new Error("Method 'send()' must be implemented.");
  }
}

class DiscordNotifierChannel extends NotifierChannel {
  constructor(webhookUrl) {
    super();
    this.webhookUrl = webhookUrl;
  }

  async send(content) {
    if (!this.webhookUrl) return;
    const payload = typeof content === 'string' ? { content } : content;
    const response = await fetch(this.webhookUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!response.ok) {
      throw new Error(`Discord Webhook failed with status ${response.status}`);
    }
  }
}

class UserResolver {
  constructor(userMapStr) {
    this.userMap = this.parseUserMap(userMapStr);
  }

  parseUserMap(str) {
    if (!str) return {};
    try {
      return JSON.parse(str);
    } catch (e) {
      console.log(`Failed to parse User Map: ${e.message}`);
      return {};
    }
  }

  resolveMention(githubUsername) {
    const id = this.userMap[githubUsername];
    return id ? `<@${id}>` : `@${githubUsername}`;
  }
}

class NotificationFormatter {
  format(data) {
    throw new Error("Method 'format()' must be implemented.");
  }
}

class PRReviewNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ prNumber, prTitle, prUrl, reviewers }) {
    const mentions = reviewers.map(r => this.userResolver.resolveMention(r)).join(", ");
    return `⚠️ ${mentions}, la PR #${prNumber} ("${prTitle}") [ver aquí](${prUrl}) está esperando tu revisión.`;
  }
}

class PRInactivityNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ prNumber, prTitle, prUrl, reviewers, limitHours }) {
    if (reviewers.length > 0) {
      const mentions = reviewers.map(r => this.userResolver.resolveMention(r)).join(", ");
      return `⚠️ ${mentions}, la PR #${prNumber} ("${prTitle}") [ver aquí](${prUrl}) sigue esperando revisión hace más de ${limitHours} horas.`;
    }
    return `🚨 **Alerta:** La PR #${prNumber} ("${prTitle}") [ver aquí](${prUrl}) lleva más de ${limitHours} horas abierta y no tiene revisores asignados.`;
  }
}

class IssueAutoAssignedNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ issueNumber, issueTitle, issueUrl, assignee }) {
    const mention = this.userResolver.resolveMention(assignee);
    return `⚠️ Como ningún integrante cumplió con su responsabilidad de asignarse la issue #${issueNumber} ("${issueTitle}") manualmente, se ha autoasignado por Round-Robin a ${mention}.`;
  }
}

class IssueCreatedNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ issueNumber, issueTitle, issueUrl, priority, assignee }) {
    const mention = assignee ? this.userResolver.resolveMention(assignee) : "Sin asignar";
    return `🆕 **Nueva Issue Creada [#${issueNumber}]:** [${issueTitle}](${issueUrl})\nPrioridad: **${priority}** | Asignado: ${mention}`;
  }
}

class IssueInactiveNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ issueNumber, issueTitle, issueUrl, assignee, daysInactive }) {
    const mention = assignee ? this.userResolver.resolveMention(assignee) : "Sin asignar";
    return `💤 **Issue Inactiva [#${issueNumber}]:** [${issueTitle}](${issueUrl}) sin actividad hace ${daysInactive} días. Atención: ${mention}`;
  }
}

class IssueDueSoonNotificationFormatter extends NotificationFormatter {
  constructor(userResolver) {
    super();
    this.userResolver = userResolver;
  }

  format({ issueNumber, issueTitle, issueUrl, assignee, dueDate }) {
    const mention = assignee ? this.userResolver.resolveMention(assignee) : "Sin asignar";
    return `⏰ **Próximo Vencimiento [#${issueNumber}]:** [${issueTitle}](${issueUrl}) vence el ${dueDate}. Asignado: ${mention}`;
  }
}

module.exports = {
  DiscordNotifierChannel,
  UserResolver,
  PRReviewNotificationFormatter,
  PRInactivityNotificationFormatter,
  IssueAutoAssignedNotificationFormatter,
  IssueCreatedNotificationFormatter,
  IssueInactiveNotificationFormatter,
  IssueDueSoonNotificationFormatter
};
