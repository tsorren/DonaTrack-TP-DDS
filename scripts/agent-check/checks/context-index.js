'use strict';

const fs = require('fs');
const path = require('path');
const { passed, failed } = require('../lib/findings');
const { extractCodespanPaths } = require('../lib/markdown');

function checkContextIndexReferences(repoRoot) {
  const findings = [];
  const indexPath = path.join(repoRoot, 'docs', 'context-index.md');

  if (!fs.existsSync(indexPath)) {
    findings.push(failed('CONTEXT_REFERENCE_MISSING', 'docs/context-index.md not found', 'docs/context-index.md'));
    return findings;
  }

  const content = fs.readFileSync(indexPath, 'utf8');
  const unique = [...new Set(extractCodespanPaths(content))];

  let missingCount = 0;
  let validCount = 0;
  for (const span of unique) {
    if (fs.existsSync(path.join(repoRoot, span))) {
      validCount++;
    } else {
      missingCount++;
      findings.push(failed('CONTEXT_REFERENCE_MISSING', `path referenced in context-index.md not found — ${span}`));
    }
  }

  if (missingCount === 0) {
    findings.push(passed('CONTEXT_INDEX_REFERENCES', `all context-index code-span paths valid (${validCount} checked)`));
  }

  return findings;
}

module.exports = { checkContextIndexReferences };
