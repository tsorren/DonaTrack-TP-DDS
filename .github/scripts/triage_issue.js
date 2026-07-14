async function ensureLabelExists(github, repo, labelName, color) {
  try {
    await github.rest.issues.getLabel({
      owner: repo.owner,
      repo: repo.repo,
      name: labelName
    });
    console.log(`Label ${labelName} already exists.`);
  } catch (e) {
    if (e.status === 404) {
      console.log(`Label ${labelName} does not exist. Creating it...`);
      await github.rest.issues.createLabel({
        owner: repo.owner,
        repo: repo.repo,
        name: labelName,
        color: color
      });
      console.log(`Label ${labelName} created successfully.`);
    } else {
      throw e;
    }
  }
}

async function addIssueToProjectAndSetPriority(github, repo, issueNodeId, priorityValue, projectNumber) {
  // Query to find ProjectV2 ID and its Priority field & options
  const queryProject = `
    query($owner: String!, $number: Int!) {
      user(login: $owner) {
        projectV2(number: $number) {
          id
          fields(first: 100) {
            nodes {
              ... on ProjectV2Field {
                id
                name
              }
              ... on ProjectV2SingleSelectField {
                id
                name
                options {
                  id
                  name
                }
              }
            }
          }
        }
      }
      organization(login: $owner) {
        projectV2(number: $number) {
          id
          fields(first: 100) {
            nodes {
              ... on ProjectV2Field {
                id
                name
              }
              ... on ProjectV2SingleSelectField {
                id
                name
                options {
                  id
                  name
                }
              }
            }
          }
        }
      }
    }
  `;

  let projectData;
  try {
    const res = await github.graphql(queryProject, {
      owner: repo.owner,
      number: projectNumber
    });
    projectData = (res.user && res.user.projectV2) ? res.user.projectV2 : (res.organization && res.organization.projectV2 ? res.organization.projectV2 : null);
  } catch (err) {
    console.log(`Failed to query project V2 via GraphQL (Check token permissions): ${err.message}`);
    return;
  }

  if (!projectData) {
    console.log(`Project V2 with number ${projectNumber} not found under owner ${repo.owner}.`);
    return;
  }

  const projectId = projectData.id;
  console.log(`Found Project V2 ID: ${projectId}`);

  // Find the 'Priority' (or 'Prioridad') single-select field
  const priorityField = projectData.fields.nodes.find(f => 
    f.name && (f.name.toLowerCase() === 'priority' || f.name.toLowerCase() === 'prioridad')
  );

  if (!priorityField) {
    console.log("Priority/Prioridad single-select field not found in project.");
    return;
  }

  // Find the option corresponding to ALTA, MEDIA, or BAJA
  const priorityOption = priorityField.options.find(opt => 
    opt.name && opt.name.toUpperCase() === priorityValue.toUpperCase()
  );

  if (!priorityOption) {
    console.log(`Priority option matching ${priorityValue} not found in field options:`, priorityField.options);
    return;
  }

  console.log(`Adding issue to project ${projectId}...`);
  const addProjectItemMutation = `
    mutation($projectId: ID!, $contentId: ID!) {
      addProjectV2ItemById(input: {projectId: $projectId, contentId: $contentId}) {
        item {
          id
        }
      }
    }
  `;

  let itemId;
  try {
    const addItemRes = await github.graphql(addProjectItemMutation, {
      projectId,
      contentId: issueNodeId
    });
    itemId = addItemRes.addProjectV2ItemById.item.id;
    console.log(`Successfully added issue as project item: ${itemId}`);
  } catch (err) {
    console.log(`Failed to add issue to project: ${err.message}`);
    return;
  }

  console.log(`Updating Priority field to ${priorityValue} (Option ID: ${priorityOption.id})...`);
  const updateProjectItemMutation = `
    mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
      updateProjectV2ItemFieldValue(input: {
        projectId: $projectId,
        itemId: $itemId,
        fieldId: $fieldId,
        value: {
          singleSelectOptionId: $optionId
        }
      }) {
        projectV2Item {
          id
        }
      }
    }
  `;

  try {
    await github.graphql(updateProjectItemMutation, {
      projectId,
      itemId,
      fieldId: priorityField.id,
      optionId: priorityOption.id
    });
    console.log("Successfully updated Priority field in GitHub Projects V2.");
  } catch (err) {
    console.log(`Failed to update project item field value: ${err.message}`);
  }
}

module.exports = async ({ github, context }) => {
  const repo = context.repo;
  const issue = context.payload.issue;
  const issueNumber = issue.number;
  const issueTitle = issue.title;
  const issueNodeId = issue.node_id;

  console.log(`Triage started for Issue #${issueNumber}: "${issueTitle}"`);

  // Detect priority tag from title
  const match = issueTitle.match(/^\[(ALTA|MEDIA|BAJA)\]/i);
  if (!match) {
    console.log("No priority tag [ALTA], [MEDIA], or [BAJA] found in issue title. Skipping triage.");
    return;
  }

  const priority = match[1].toUpperCase(); // ALTA, MEDIA, or BAJA
  console.log(`Detected priority: ${priority}`);

  // Define label names and colors
  const labelMap = {
    'ALTA': { name: 'prioridad:alta', color: 'd73a4a' },
    'MEDIA': { name: 'prioridad:media', color: 'e9c46a' },
    'BAJA': { name: 'prioridad:baja', color: '2a9d8f' }
  };

  const targetLabel = labelMap[priority];

  // 1. Ensure target label exists in repo and add it to the issue
  try {
    await ensureLabelExists(github, repo, targetLabel.name, targetLabel.color);
    await github.rest.issues.addLabels({
      owner: repo.owner,
      repo: repo.repo,
      issue_number: issueNumber,
      labels: [targetLabel.name]
    });
    console.log(`Added label "${targetLabel.name}" to Issue #${issueNumber}`);
  } catch (err) {
    console.log(`Failed to apply label: ${err.message}`);
  }

  // 2. Add issue to Projects V2 and set its Priority field
  const projectNumberStr = process.env.PROJECT_NUMBER || '1';
  const projectNumber = parseInt(projectNumberStr);
  await addIssueToProjectAndSetPriority(github, repo, issueNodeId, priority, projectNumber);

  console.log("Triage completed.");
};
