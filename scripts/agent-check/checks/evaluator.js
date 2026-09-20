'use strict';

const fs = require('fs');
const path = require('path');
const { passed, failed } = require('../lib/findings');

function checkEvaluatorPolicy(repoRoot) {
  const findings = [];
  const evaluatorRel = 'docs/IA/review/evaluator.md';
  const evaluatorFull = path.join(repoRoot, 'docs', 'IA', 'review', 'evaluator.md');
  const agentsFull = path.join(repoRoot, 'AGENTS.md');

  if (fs.existsSync(evaluatorFull)) {
    findings.push(passed('EVALUATOR_EXISTS', `${evaluatorRel} present`));
  } else {
    findings.push(failed('EVALUATOR_EXISTS', `${evaluatorRel} not found — review policy is broken`, evaluatorRel));
  }

  if (fs.existsSync(agentsFull)) {
    const content = fs.readFileSync(agentsFull, 'utf8');
    if (content.includes('evaluator.md')) {
      findings.push(passed('EVALUATOR_LINK', 'AGENTS.md references evaluator.md'));
    } else {
      findings.push(failed('EVALUATOR_LINK', 'AGENTS.md does not reference evaluator.md — review policy link is broken', 'AGENTS.md'));
    }
  }

  return findings;
}

module.exports = { checkEvaluatorPolicy };
