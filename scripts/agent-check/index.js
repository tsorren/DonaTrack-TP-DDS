'use strict';

// Agent Governance Check — aggregator, renderer, and public API
// Consumed by: scripts/agent-check.js (CLI) and scripts/tests/run-tests.js (tests)

const { checkAgentsCanonicity } = require('./checks/agents');
const { checkEvaluatorPolicy } = require('./checks/evaluator');
const { checkStaleTerms } = require('./checks/stale-terms');
const { checkInternalLinks } = require('./checks/links');
const { checkContextIndexReferences } = require('./checks/context-index');
const { checkDeuaTecnicaIntegrity, parseDeuaTecnicaFile } = require('./checks/deuda-tecnica');
const { checkAdrStatus } = require('./checks/adr');
const { checkModuleRouting, parseContextIndexServices } = require('./checks/modules');
const { checkTemporalDrift } = require('./checks/temporal-drift');

// lib helpers re-exported for unit testing
const { extractMarkdownLinks, extractCodespanPaths } = require('./lib/markdown');
const { resolveInternalLink } = require('./lib/paths');
const { parseAdrEntry, parseAdrStatus } = require('./lib/adr-parser');
const { parsePomModules } = require('./lib/pom');

// ─── Aggregator ───────────────────────────────────────────────────────────────

function runAllChecks(repoRoot) {
  return [
    ...checkAgentsCanonicity(repoRoot),
    ...checkEvaluatorPolicy(repoRoot),
    ...checkStaleTerms(repoRoot),
    ...checkInternalLinks(repoRoot),
    ...checkContextIndexReferences(repoRoot),
    ...checkDeuaTecnicaIntegrity(repoRoot),
    ...checkAdrStatus(repoRoot),
    ...checkModuleRouting(repoRoot),
    ...checkTemporalDrift(repoRoot),
  ];
}

// ─── Renderer ─────────────────────────────────────────────────────────────────

function renderFindings(findings) {
  const lines = [];
  for (const f of findings) {
    const fileInfo = f.file ? ` — ${f.file}` : '';
    lines.push(`[${f.severity}] ${f.id}: ${f.message}${fileInfo}`);
  }
  const passCount = findings.filter(f => f.severity === 'PASS').length;
  const warnCount = findings.filter(f => f.severity === 'WARN').length;
  const failCount = findings.filter(f => f.severity === 'FAIL').length;
  lines.push('');
  lines.push('─'.repeat(60));
  lines.push(`PASS: ${passCount}  │  WARN: ${warnCount}  │  FAIL: ${failCount}`);
  lines.push(`Exit: ${failCount > 0 ? 1 : 0}${failCount > 0 ? ' (FAIL count > 0)' : ''}`);
  lines.push('─'.repeat(60));
  return lines.join('\n');
}

// ─── Public API ───────────────────────────────────────────────────────────────

module.exports = {
  // Checks
  checkAgentsCanonicity,
  checkEvaluatorPolicy,
  checkStaleTerms,
  checkInternalLinks,
  checkContextIndexReferences,
  checkDeuaTecnicaIntegrity,
  checkAdrStatus,
  checkModuleRouting,
  checkTemporalDrift,
  // Aggregator + renderer
  runAllChecks,
  renderFindings,
  // Pure helpers (unit-testable without filesystem)
  extractMarkdownLinks,
  resolveInternalLink,
  extractCodespanPaths,
  parseDeuaTecnicaFile,
  parseAdrEntry,
  parseAdrStatus,
  parsePomModules,
  parseContextIndexServices,
};
