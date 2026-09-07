'use strict';

const fs = require('fs');
const path = require('path');
const { VALID_ADR_STATUSES } = require('../config');
const { passed, failed, warned } = require('../lib/findings');
const { toForwardSlash } = require('../lib/paths');
const { parseAdrStatus } = require('../lib/adr-parser');

// Parses DEUDA_TECNICA.md content into DTI records.
// Pure — no filesystem access.
// Returns [{ id, adrLinks, decisionStatus, implStatus }]
function parseDeuaTecnicaFile(content) {
  const dtis = [];
  const blocks = content.split(/(?=\n## DTI-)/);

  for (const block of blocks) {
    const idMatch = block.match(/## (DTI-\d+)/);
    if (!idMatch) continue;

    const adrLinks = [];
    const rowPattern = /\|\s*ADR(?:\s+complementario)?\s*\|\s*(.*?)\s*\|/g;
    let rowMatch;
    while ((rowMatch = rowPattern.exec(block)) !== null) {
      const linkMatch = rowMatch[1].match(/\]\(([^)]+)\)/);
      if (linkMatch) adrLinks.push(linkMatch[1].trim());
    }

    let decisionStatus = null;
    const decisionRow = block.match(/\|\s*Decision status\s*\|\s*`?([^`|\n]+)`?\s*\|/);
    if (decisionRow) decisionStatus = decisionRow[1].trim().replace(/`/g, '');

    let implStatus = null;
    const implRow = block.match(/\|\s*Implementation status\s*\|\s*([^|\n]+)\|/);
    if (implRow) implStatus = implRow[1].trim();

    dtis.push({ id: idMatch[1], adrLinks, decisionStatus, implStatus });
  }

  return dtis;
}

function checkDeuaTecnicaIntegrity(repoRoot) {
  const findings = [];
  const dtiPath = path.join(repoRoot, 'docs', 'adr', 'DEUDA_TECNICA.md');
  const dtiDir = path.join(repoRoot, 'docs', 'adr');

  if (!fs.existsSync(dtiPath)) {
    findings.push(failed('DTI_ADR_MISSING', 'docs/adr/DEUDA_TECNICA.md not found', 'docs/adr/DEUDA_TECNICA.md'));
    return findings;
  }

  const dtis = parseDeuaTecnicaFile(fs.readFileSync(dtiPath, 'utf8'));

  if (dtis.length === 0) {
    findings.push(passed('DEUDA_TECNICA_INTEGRITY', 'DEUDA_TECNICA.md parsed — no DTI entries found'));
    return findings;
  }

  let failCount = 0;
  const seenIds = new Set();
  for (const dti of dtis) {
    if (seenIds.has(dti.id)) {
      findings.push(failed('DTI_DUPLICATE_ID', `duplicate DTI ID — ${dti.id}`, 'docs/adr/DEUDA_TECNICA.md'));
      failCount++;
    }
    seenIds.add(dti.id);
  }

  for (const dti of dtis) {
    for (const linkTarget of dti.adrLinks) {
      const resolved = path.join(dtiDir, linkTarget);
      if (!fs.existsSync(resolved)) {
        findings.push(failed('DTI_ADR_MISSING',
          `${dti.id}: ADR link not found — ${toForwardSlash(path.relative(repoRoot, resolved))}`,
          'docs/adr/DEUDA_TECNICA.md'));
        failCount++;
      } else if (dti.decisionStatus) {
        try {
          const adrStatus = parseAdrStatus(fs.readFileSync(resolved, 'utf8'));
          if (adrStatus && adrStatus !== dti.decisionStatus.toLowerCase()) {
            findings.push(warned('DTI_ADR_STATUS_MISMATCH',
              `${dti.id}: Decision status "${dti.decisionStatus}" differs from ADR Status: "${adrStatus}"`,
              'docs/adr/DEUDA_TECNICA.md'));
          }
        } catch { /* ignore unreadable ADR */ }
      }
    }

    if (dti.decisionStatus !== null && !VALID_ADR_STATUSES.has(dti.decisionStatus.toLowerCase())) {
      findings.push(failed('DTI_DECISION_STATUS_INVALID',
        `${dti.id}: invalid Decision status "${dti.decisionStatus}" — expected: ${[...VALID_ADR_STATUSES].join(' | ')}`,
        'docs/adr/DEUDA_TECNICA.md'));
      failCount++;
    }

    if (dti.implStatus !== null && dti.implStatus.replace(/[`\s\-|]/g, '') === '') {
      findings.push(warned('DTI_IMPLEMENTATION_STATUS_EMPTY',
        `${dti.id}: Implementation status field is empty`, 'docs/adr/DEUDA_TECNICA.md'));
    }
  }

  if (failCount === 0 && !findings.some(f => f.id === 'DTI_DUPLICATE_ID')) {
    findings.push(passed('DEUDA_TECNICA_INTEGRITY',
      `${dtis.length} DTIs validated — IDs unique, ADR links exist, Decision statuses valid`));
  }

  return findings;
}

module.exports = { checkDeuaTecnicaIntegrity, parseDeuaTecnicaFile };
