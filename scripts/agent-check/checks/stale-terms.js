'use strict';

const fs = require('fs');
const { STALE_TERMS, STALE_TERMS_EXCLUSIONS } = require('../config');
const { passed, failed } = require('../lib/findings');
const { walkFiles, isUnderHistory } = require('../lib/paths');

function checkStaleTerms(repoRoot) {
  const findings = [];
  const allMd = walkFiles(repoRoot, repoRoot, name => name.endsWith('.md'));
  const activeFiles = allMd.filter(f => !isUnderHistory(f.rel) && !STALE_TERMS_EXCLUSIONS.has(f.rel));

  let found = false;
  for (const { rel, full } of activeFiles) {
    let content;
    try { content = fs.readFileSync(full, 'utf8'); } catch { continue; }
    const lower = content.toLowerCase();
    for (const term of STALE_TERMS) {
      if (lower.includes(term.toLowerCase())) {
        findings.push(failed('STALE_TERM', `obsolete term "${term}" found — regression from Wave 6 removal`, rel));
        found = true;
      }
    }
  }

  if (!found) {
    findings.push(passed('STALE_TERMS_CHECK', `no stale terms found in ${activeFiles.length} active documents`));
  }

  return findings;
}

module.exports = { checkStaleTerms };
