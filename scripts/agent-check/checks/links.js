'use strict';

const fs = require('fs');
const path = require('path');
const { INTERNAL_LINK_SCOPE } = require('../config');
const { passed, failed, warned } = require('../lib/findings');
const { toForwardSlash, resolveInternalLink } = require('../lib/paths');
const { extractMarkdownLinks } = require('../lib/markdown');

// scopeOverride: optional array for testing; production always uses INTERNAL_LINK_SCOPE.
// Limitation (documented): anchor targets not validated — only file/directory existence.
function checkInternalLinks(repoRoot, scopeOverride) {
  const findings = [];
  const scope = scopeOverride || INTERNAL_LINK_SCOPE;
  let brokenCount = 0;
  let validCount = 0;
  let skippedCount = 0;

  for (const relPath of scope) {
    const full = path.join(repoRoot, relPath);
    if (!fs.existsSync(full)) {
      findings.push(warned('LINK_SCOPE_DOC_MISSING', `scope document not found — link check skipped`, relPath));
      continue;
    }
    let content;
    try { content = fs.readFileSync(full, 'utf8'); } catch { continue; }

    for (const { raw } of extractMarkdownLinks(content)) {
      const result = resolveInternalLink(relPath, raw, repoRoot);
      if (result.skip) { skippedCount++; continue; }
      if (fs.existsSync(result.resolved)) {
        validCount++;
      } else {
        brokenCount++;
        const targetRel = toForwardSlash(path.relative(repoRoot, result.resolved));
        findings.push(failed('BROKEN_LINK', `broken internal reference — ${relPath} -> ${targetRel}`));
      }
    }
  }

  if (brokenCount === 0) {
    findings.push(passed('INTERNAL_LINKS', `all internal links valid (${validCount} checked, ${skippedCount} skipped)`));
  }

  return findings;
}

module.exports = { checkInternalLinks };
