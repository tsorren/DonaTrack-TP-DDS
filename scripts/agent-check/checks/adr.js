'use strict';

const fs = require('fs');
const path = require('path');
const { VALID_ADR_STATUSES } = require('../config');
const { passed, failed, warned } = require('../lib/findings');
const { walkFiles, toForwardSlash } = require('../lib/paths');
const { parseAdrEntry } = require('../lib/adr-parser');

// Historical distinction policy:
// - ADR files are identified by the date-prefix naming convention (YYYYMMDD-slug.md).
// - All such files are expected to have a - Status: field (MADR format in this corpus).
// - If Status is absent: WARN (conservative — not FAIL) to avoid penalizing pre-MADR docs.
// - If Status is present but invalid: FAIL.
// - If superseded with a resolvable ref that's missing: FAIL.
// - If superseded with no ref: WARN.

function checkAdrStatus(repoRoot) {
  const findings = [];
  const adrDir = path.join(repoRoot, 'docs', 'adr');

  if (!fs.existsSync(adrDir)) {
    return [warned('ADR_STATUS_VALID', 'docs/adr/ not found — ADR status check skipped')];
  }

  // Only files with YYYYMMDD- prefix are ADRs; README.md, DEUDA_TECNICA.md, index.md are governance docs.
  const adrFiles = walkFiles(adrDir, repoRoot, name => /^\d{8}-/.test(name) && name.endsWith('.md'));

  let invalidCount = 0;
  let refInvalidCount = 0;

  for (const { rel, full } of adrFiles) {
    let content;
    try { content = fs.readFileSync(full, 'utf8'); } catch { continue; }

    const { status, supersededRef } = parseAdrEntry(content);

    if (status === null) {
      // No Status line — may be a pre-MADR historical ADR; warn, don't fail.
      findings.push(warned('ADR_STATUS_MISSING', `no Status: field found`, rel));
      continue;
    }

    if (!VALID_ADR_STATUSES.has(status)) {
      findings.push(failed('ADR_STATUS_INVALID', `Status: "${status}" is not a valid value (expected: ${[...VALID_ADR_STATUSES].join(' | ')})`, rel));
      invalidCount++;
      continue;
    }

    if (status === 'superseded') {
      if (supersededRef === null) {
        // Superseded but no "by [link]" reference — warn only
        findings.push(warned('ADR_SUPERSEDED_NO_REF', `Status: superseded but no "by [link]" reference found`, rel));
      } else {
        const resolved = path.join(path.dirname(full), supersededRef);
        if (!fs.existsSync(resolved)) {
          const refRel = toForwardSlash(path.relative(repoRoot, resolved));
          findings.push(failed('ADR_SUPERSEDED_REF_INVALID', `superseded-by reference not found — ${refRel}`, rel));
          refInvalidCount++;
        }
      }
    }
  }

  if (invalidCount === 0 && refInvalidCount === 0) {
    const warnCount = findings.filter(f => f.severity === 'WARN').length;
    const msg = warnCount > 0
      ? `${adrFiles.length} ADRs — statuses valid (${warnCount} with advisory warnings)`
      : `${adrFiles.length} ADRs — all statuses valid`;
    findings.push(passed('ADR_STATUS_VALID', msg));
  }

  return findings;
}

module.exports = { checkAdrStatus };
