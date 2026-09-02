'use strict';

const fs = require('fs');
const path = require('path');
const { AGENTS_ALLOWLIST } = require('../config');
const { passed, failed } = require('../lib/findings');
const { walkFiles, isUnderHistory } = require('../lib/paths');

function checkAgentsCanonicity(repoRoot) {
  const findings = [];

  if (fs.existsSync(path.join(repoRoot, 'AGENTS.md'))) {
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
        'unexpected active AGENTS.md — only allowlisted paths are authorized; add to AGENTS_ALLOWLIST to authorize (Wave 8+)',
        rel
      ));
    }
  }

  // Wave 8: each allowlisted nested AGENTS.md must exist — absence is a governance violation.
  const nestedAllowlist = [...AGENTS_ALLOWLIST].filter(p => p !== 'AGENTS.md');
  for (const nestedRel of nestedAllowlist) {
    const nestedFull = path.join(repoRoot, ...nestedRel.split('/'));
    if (fs.existsSync(nestedFull)) {
      findings.push(passed('AGENTS_ALLOWLISTED_MISSING', `allowlisted nested ${nestedRel} present`));
    } else {
      findings.push(failed(
        'AGENTS_ALLOWLISTED_MISSING',
        `allowlisted nested ${nestedRel} declared in governance policy but not found — create the file or remove from allowlist`,
        nestedRel
      ));
    }
  }

  return findings;
}

module.exports = { checkAgentsCanonicity };
