'use strict';

const fs = require('fs');
const { AGENTS_ALLOWLIST } = require('../config');
const { passed, failed } = require('../lib/findings');
const { walkFiles, isUnderHistory } = require('../lib/paths');

function checkAgentsCanonicity(repoRoot) {
  const findings = [];

  if (fs.existsSync(require('path').join(repoRoot, 'AGENTS.md'))) {
    findings.push(passed('AGENTS_CANONICAL', '/AGENTS.md present at repository root'));
  } else {
    findings.push(failed('AGENTS_CANONICAL', '/AGENTS.md not found at repository root'));
  }

  const unexpected = walkFiles(repoRoot, repoRoot, name => name === 'AGENTS.md')
    .map(f => f.rel)
    .filter(rel => !AGENTS_ALLOWLIST.has(rel) && !isUnderHistory(rel));

  if (unexpected.length === 0) {
    findings.push(passed('AGENTS_UNEXPECTED', 'no unexpected AGENTS.md files found'));
  } else {
    for (const rel of unexpected) {
      findings.push(failed(
        'AGENTS_UNEXPECTED',
        'unexpected active AGENTS.md — only /AGENTS.md is canonical; nested AGENTS require allowlist entry (Wave 8+)',
        rel
      ));
    }
  }

  return findings;
}

module.exports = { checkAgentsCanonicity };
