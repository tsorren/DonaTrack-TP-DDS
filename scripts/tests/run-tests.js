'use strict';

// Agent Governance Check — Test Suite (Wave 7A + 7B + 7C + 8)
// Run: node scripts/tests/run-tests.js
// Uses only Node.js built-ins. No npm dependencies.

const fs = require('fs');
const os = require('os');
const path = require('path');

const {
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
  runAllChecks,
  // Pure helpers
  extractMarkdownLinks,
  resolveInternalLink,
  extractCodespanPaths,
  parseDeuaTecnicaFile,
  parseAdrEntry,
  parsePomModules,
  parseContextIndexServices,
} = require('../agent-check/index');

// ─── Harness ──────────────────────────────────────────────────────────────────

let passed = 0;
let failed = 0;

function assert(name, condition) {
  if (condition) { console.log(`  [PASS] ${name}`); passed++; }
  else { console.error(`  [FAIL] ${name}`); failed++; }
}

function makeTemp() { return fs.mkdtempSync(path.join(os.tmpdir(), 'agent-check-test-')); }

function write(base, relPath, content) {
  const full = path.join(base, relPath);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content || '', 'utf8');
}

function cleanup(dir) { try { fs.rmSync(dir, { recursive: true, force: true }); } catch { /* ignore */ } }

function hasFail(findings, id) { return findings.some(f => f.severity === 'FAIL' && f.id === id); }
function hasWarn(findings, id) { return findings.some(f => f.severity === 'WARN' && f.id === id); }
function noFail(findings) { return findings.every(f => f.severity !== 'FAIL'); }

// ─── [1] checkAgentsCanonicity ─────────────────────────────────────────────────

console.log('\nAgent Governance — Test Suite (Wave 7A + 7B + 7C + 8)');
console.log('═'.repeat(56));
console.log('\n[1] checkAgentsCanonicity');

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS');
  write(tmp, 'common-lib/AGENTS.md', '# Shared Kernel nested');
  write(tmp, 'docs/IA/history/AGENTS-v3.5.md', '# historical');
  assert('1.1  root + common-lib nested + history allowed → no FAIL', noFail(checkAgentsCanonicity(tmp)));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  assert('1.2  root AGENTS.md missing → AGENTS_CANONICAL FAIL', hasFail(checkAgentsCanonicity(tmp), 'AGENTS_CANONICAL'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS'); write(tmp, 'some-module/AGENTS.md', '# extra');
  assert('1.3  extra active AGENTS.md → AGENTS_UNEXPECTED FAIL', hasFail(checkAgentsCanonicity(tmp), 'AGENTS_UNEXPECTED'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS'); write(tmp, 'docs/IA/history/AGENTS.md', '# in history');
  assert('1.4  AGENTS.md under history → excluded, no AGENTS_UNEXPECTED FAIL', !hasFail(checkAgentsCanonicity(tmp), 'AGENTS_UNEXPECTED'));
} finally { cleanup(tmp); } }

// Wave 8 — nested AGENTS allowlist
{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS');
  write(tmp, 'common-lib/AGENTS.md', '# Shared Kernel nested');
  assert('1.5  root + common-lib/AGENTS.md (allowlisted) → no AGENTS_UNEXPECTED FAIL', !hasFail(checkAgentsCanonicity(tmp), 'AGENTS_UNEXPECTED'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS');
  write(tmp, 'common-lib/AGENTS.md', '# Shared Kernel nested');
  write(tmp, 'logistica-service/AGENTS.md', '# unauthorized nested');
  assert('1.6  unauthorized nested AGENTS.md → AGENTS_UNEXPECTED FAIL', hasFail(checkAgentsCanonicity(tmp), 'AGENTS_UNEXPECTED'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS');
  write(tmp, 'common-lib/AGENTS.md', '# Shared Kernel nested');
  assert('1.7  allowlisted nested present → AGENTS_ALLOWLISTED_MISSING PASS', !hasFail(checkAgentsCanonicity(tmp), 'AGENTS_ALLOWLISTED_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '# AGENTS');
  // common-lib/AGENTS.md intentionally absent
  assert('1.8  allowlisted nested missing → AGENTS_ALLOWLISTED_MISSING FAIL', hasFail(checkAgentsCanonicity(tmp), 'AGENTS_ALLOWLISTED_MISSING'));
} finally { cleanup(tmp); } }

// ─── [2] checkEvaluatorPolicy ─────────────────────────────────────────────────

console.log('\n[2] checkEvaluatorPolicy');

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/IA/review/evaluator.md', '# Evaluator'); write(tmp, 'AGENTS.md', 'evaluator.md for policy');
  assert('2.1  evaluator exists + AGENTS references → no FAIL', noFail(checkEvaluatorPolicy(tmp)));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', 'evaluator.md ref');
  assert('2.2  evaluator missing → EVALUATOR_EXISTS FAIL', hasFail(checkEvaluatorPolicy(tmp), 'EVALUATOR_EXISTS'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/IA/review/evaluator.md', '# Evaluator'); write(tmp, 'AGENTS.md', '# no ref here');
  assert('2.3  evaluator exists but AGENTS no reference → EVALUATOR_LINK FAIL', hasFail(checkEvaluatorPolicy(tmp), 'EVALUATOR_LINK'));
} finally { cleanup(tmp); } }

// ─── [3] checkStaleTerms ──────────────────────────────────────────────────────

console.log('\n[3] checkStaleTerms');

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/clean.md', '# Clean');
  assert('3.1  clean repo → no STALE_TERM FAIL', !hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/active.md', 'invoke_subagent used');
  assert('3.2  invoke_subagent in active doc → STALE_TERM FAIL', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/IA/history/old.md', 'invoke_subagent');
  assert('3.3  invoke_subagent only in history → excluded, no FAIL', !hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/spec.md', '[DEFERRED_WAVE_5] here');
  assert('3.4  [DEFERRED_WAVE_5] in active doc → STALE_TERM FAIL', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/spec.md', '[DEFERRED_WAVE_6] pending');
  assert('3.5  [DEFERRED_WAVE_6] in active doc → STALE_TERM FAIL', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/doc.md', 'Mode: Fallback Monoproceso');
  assert('3.6  "Fallback Monoproceso" → STALE_TERM FAIL', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/doc.md', 'fallback monoproceso lower');
  assert('3.7  lowercase variant → STALE_TERM FAIL (case-insensitive)', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/ESTADO_DOCUMENTACION.md', '| invoke_subagent removed in Oleada 6 |');
  assert('3.8  ESTADO_DOCUMENTACION.md excluded (audit log) → no FAIL', !hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'common-lib/AGENTS.md', 'invoke_subagent used in nested'); // stale term in active nested
  assert('3.9  stale term in common-lib/AGENTS.md (active nested) → STALE_TERM FAIL', hasFail(checkStaleTerms(tmp), 'STALE_TERM'));
} finally { cleanup(tmp); } }

// ─── [4] runAllChecks exit code ────────────────────────────────────────────────

console.log('\n[4] runAllChecks — exit code aggregation');

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', 'evaluator.md ref');
  write(tmp, 'common-lib/AGENTS.md', '# Shared Kernel nested'); // Wave 8: allowlisted nested required
  write(tmp, 'docs/IA/review/evaluator.md', '# Evaluator');
  write(tmp, 'docs/clean.md', '# Clean');
  write(tmp, 'docs/context-index.md', '# No code spans');
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', '# No DTIs');
  const r = runAllChecks(tmp);
  assert('4.1  minimal all-pass scenario → failCount === 0', r.filter(f => f.severity === 'FAIL').length === 0);
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  const r = runAllChecks(tmp);
  assert('4.2  empty repo → failCount > 0', r.filter(f => f.severity === 'FAIL').length > 0);
} finally { cleanup(tmp); } }

// ─── [5] extractMarkdownLinks (pure) ──────────────────────────────────────────

console.log('\n[5] extractMarkdownLinks (pure)');

assert('5.1  relative link', extractMarkdownLinks('[t](./rel.md)')[0]?.raw === './rel.md');
assert('5.2  ../ link', extractMarkdownLinks('[t](../up.md)')[0]?.raw === '../up.md');
assert('5.3  root-relative', extractMarkdownLinks('[t](/AGENTS.md)')[0]?.raw === '/AGENTS.md');
assert('5.4  PDF link', extractMarkdownLinks('[t](file.pdf)')[0]?.raw === 'file.pdf');
assert('5.5  local anchor raw preserved', extractMarkdownLinks('[t](#sec)')[0]?.raw === '#sec');
assert('5.6  file+anchor raw preserved', extractMarkdownLinks('[t](f.md#sec)')[0]?.raw === 'f.md#sec');
assert('5.7  external URL', extractMarkdownLinks('[t](https://x.com)')[0]?.raw === 'https://x.com');

// ─── [6] resolveInternalLink (pure) ───────────────────────────────────────────

console.log('\n[6] resolveInternalLink (pure)');

assert('6.1  external URL → skip', resolveInternalLink('d.md', 'https://x.com', '/r').skip === true);
assert('6.2  local anchor → skip', resolveInternalLink('d.md', '#sec', '/r').skip === true);
assert('6.3  file+anchor → not skipped, anchor stripped', (() => { const r = resolveInternalLink('d.md', 'f.md#s', '/r'); return !r.skip && r.resolved.endsWith('f.md'); })());
assert('6.4  root-relative /AGENTS.md resolves correctly', (() => { const r = resolveInternalLink('AGENTS.md', '/AGENTS.md', '/r'); return !r.skip && r.resolved === path.join('/r', 'AGENTS.md'); })());

// ─── [7] checkInternalLinks (fs tests) ────────────────────────────────────────

console.log('\n[7] checkInternalLinks');

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[link](docs/t.md) [dir](docs/sub/)');
  write(tmp, 'docs/t.md', ''); fs.mkdirSync(path.join(tmp, 'docs/sub'), { recursive: true });
  assert('7.1  relative link + directory → PASS', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/src.md', '[up](../root.md)'); write(tmp, 'root.md', '');
  assert('7.2  ../ link → PASS', !hasFail(checkInternalLinks(tmp, ['docs/src.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[self](/AGENTS.md)');
  assert('7.3  root-relative self-link → PASS', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[pdf](docs/s.pdf)'); write(tmp, 'docs/s.pdf', '%PDF');
  assert('7.4  PDF link → PASS', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[broken](docs/nope.md)');
  assert('7.5  broken link → BROKEN_LINK FAIL', hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[ext](https://x.com) [mail](mailto:a@b)');
  assert('7.6  external URL + mailto → ignored', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[anchor](#local)');
  assert('7.7  local anchor → ignored', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[fa](docs/real.md#sec)'); write(tmp, 'docs/real.md', '');
  assert('7.8  file+anchor, file exists → PASS (anchor not validated)', !hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'AGENTS.md', '[fa](docs/miss.md#sec)');
  assert('7.9  file+anchor, file missing → BROKEN_LINK FAIL', hasFail(checkInternalLinks(tmp, ['AGENTS.md']), 'BROKEN_LINK'));
} finally { cleanup(tmp); } }

// ─── [8] extractCodespanPaths (pure) ──────────────────────────────────────────

console.log('\n[8] extractCodespanPaths (pure)');

assert('8.1  docs/ span extracted', extractCodespanPaths('See `docs/x/`')[0] === 'docs/x/');
assert('8.2  <N> placeholder excluded', extractCodespanPaths('`docs/e/<N>/f.pdf`').length === 0);
assert('8.3  <servicio> placeholder excluded', extractCodespanPaths('`docs/adr/<servicio>/`').length === 0);
assert('8.4  no-slash span excluded', extractCodespanPaths('`donaciones-service`').length === 0);
assert('8.5  non-docs/ excluded', extractCodespanPaths('`pom.xml` and `models/entities/`').length === 0);

// ─── [9] checkContextIndexReferences ─────────────────────────────────────────

console.log('\n[9] checkContextIndexReferences');

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/context-index.md', 'See `docs/valid/`');
  fs.mkdirSync(path.join(tmp, 'docs/valid'), { recursive: true });
  assert('9.1  valid docs/ span → PASS', !hasFail(checkContextIndexReferences(tmp), 'CONTEXT_REFERENCE_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/context-index.md', 'See `docs/gone/`');
  assert('9.2  nonexistent docs/ span → FAIL', hasFail(checkContextIndexReferences(tmp), 'CONTEXT_REFERENCE_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/context-index.md', 'Use `docs/e/<N>/f.pdf` for compliance');
  assert('9.3  <N> placeholder → not checked, no FAIL', !hasFail(checkContextIndexReferences(tmp), 'CONTEXT_REFERENCE_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/context-index.md', 'Ref `docs/adr/<servicio>/`');
  assert('9.4  <servicio> placeholder → not checked, no FAIL', !hasFail(checkContextIndexReferences(tmp), 'CONTEXT_REFERENCE_MISSING'));
} finally { cleanup(tmp); } }

// ─── [10] parseDeuaTecnicaFile (pure) ────────────────────────────────────────

console.log('\n[10] parseDeuaTecnicaFile (pure)');

const DTI_FIXTURE = `
## DTI-01 — First

| Campo | Valor |
|---|---|
| ADR | [slug](./svc/adr-01.md) |
| Decision status | \`proposed\` |
| Implementation status | \`unknown\` |

## DTI-02 — Second

| Campo | Valor |
|---|---|
| ADR | [slug](./svc/adr-02.md) |
| ADR complementario | [extra](./svc/adr-extra.md) |
| Decision status | \`accepted\` |
| Implementation status | \`in-progress\` |
`;

{ const dtis = parseDeuaTecnicaFile(DTI_FIXTURE);
  assert('10.1  two DTI blocks parsed', dtis.length === 2);
  assert('10.2  DTI-01 id correct', dtis[0].id === 'DTI-01');
  assert('10.3  DTI-01 one ADR link', dtis[0].adrLinks.length === 1);
  assert('10.4  DTI-02 two ADR links', dtis[1].adrLinks.length === 2);
  assert('10.5  DTI-01 Decision status = proposed', dtis[0].decisionStatus === 'proposed');
  assert('10.6  DTI-02 Decision status = accepted', dtis[1].decisionStatus === 'accepted'); }

// ─── [11] checkDeuaTecnicaIntegrity ──────────────────────────────────────────

console.log('\n[11] checkDeuaTecnicaIntegrity');

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', `\n## DTI-01 — Item\n\n| Campo | Valor |\n|---|---|\n| ADR | [s](./adr-01.md) |\n| Decision status | \`proposed\` |\n| Implementation status | \`unknown\` |\n`);
  write(tmp, 'docs/adr/adr-01.md', '- Status: proposed\n');
  assert('11.1  valid DTI → PASS', noFail(checkDeuaTecnicaIntegrity(tmp)));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  const body = `\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./a.md)|\n|Decision status|\`proposed\`|\n\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./a.md)|\n|Decision status|\`proposed\`|\n`;
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', body); write(tmp, 'docs/adr/a.md', '- Status: proposed\n');
  assert('11.2  duplicate ID → DTI_DUPLICATE_ID FAIL', hasFail(checkDeuaTecnicaIntegrity(tmp), 'DTI_DUPLICATE_ID'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', `\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./gone.md)|\n|Decision status|\`proposed\`|\n|Implementation status|\`unknown\`|\n`);
  assert('11.3  ADR link missing → DTI_ADR_MISSING FAIL', hasFail(checkDeuaTecnicaIntegrity(tmp), 'DTI_ADR_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', `\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./a.md)|\n|Decision status|\`invalid_val\`|\n|Implementation status|\`unknown\`|\n`);
  write(tmp, 'docs/adr/a.md', '- Status: proposed\n');
  assert('11.4  invalid Decision status → DTI_DECISION_STATUS_INVALID FAIL', hasFail(checkDeuaTecnicaIntegrity(tmp), 'DTI_DECISION_STATUS_INVALID'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', `\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./a.md)|\n|Decision status|\`proposed\`|\n|Implementation status|\`unknown\`|\n`);
  write(tmp, 'docs/adr/a.md', '- Status: accepted\n');
  assert('11.5  DTI proposed ≠ ADR accepted → DTI_ADR_STATUS_MISMATCH WARN', hasWarn(checkDeuaTecnicaIntegrity(tmp), 'DTI_ADR_STATUS_MISMATCH'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', `\n## DTI-01\n\n|Campo|Valor|\n|---|---|\n|ADR|[s](./a.md)|\n|Decision status|\`proposed\`|\n|Implementation status| |\n`);
  write(tmp, 'docs/adr/a.md', '- Status: proposed\n');
  assert('11.6  empty impl status → DTI_IMPLEMENTATION_STATUS_EMPTY WARN', hasWarn(checkDeuaTecnicaIntegrity(tmp), 'DTI_IMPLEMENTATION_STATUS_EMPTY'));
} finally { cleanup(tmp); } }

// ─── [12] parseAdrEntry + checkAdrStatus ──────────────────────────────────────

console.log('\n[12] parseAdrEntry (pure) + checkAdrStatus');

assert('12.1  proposed → status=proposed, no ref', (() => { const r = parseAdrEntry('- Status: proposed\n'); return r.status === 'proposed' && r.supersededRef === null; })());
assert('12.2  accepted → status=accepted', parseAdrEntry('- Status: accepted\n').status === 'accepted');
assert('12.3  no Status line → status=null', parseAdrEntry('# Just a title\n').status === null);
assert('12.4  superseded by → status=superseded, ref extracted', (() => {
  const r = parseAdrEntry('- Status: superseded by [slug](./20260901-slug.md)\n');
  return r.status === 'superseded' && r.supersededRef === './20260901-slug.md';
})());

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/20260101-valid.md', '- Status: proposed\n# ADR content');
  assert('12.5  valid proposed ADR → ADR_STATUS_VALID PASS', !hasFail(checkAdrStatus(tmp), 'ADR_STATUS_INVALID'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/20260101-bad.md', '- Status: invalid_value\n# ADR content');
  assert('12.6  invalid status → ADR_STATUS_INVALID FAIL', hasFail(checkAdrStatus(tmp), 'ADR_STATUS_INVALID'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // File without Status field — should WARN, not FAIL
  write(tmp, 'docs/adr/20260101-no-status.md', '# ADR without status field\nSome content');
  const r = checkAdrStatus(tmp);
  assert('12.7  no Status field → ADR_STATUS_MISSING WARN (not FAIL)', hasWarn(r, 'ADR_STATUS_MISSING') && !hasFail(r, 'ADR_STATUS_MISSING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // Non-ADR file (no date prefix) must be excluded
  write(tmp, 'docs/adr/README.md', '- Status: invalid_because_im_not_an_adr\n');
  assert('12.8  governance doc (no date prefix) → excluded from ADR check', !hasFail(checkAdrStatus(tmp), 'ADR_STATUS_INVALID'));
} finally { cleanup(tmp); } }

// ─── [13] ADR_SUPERSEDED_REF_VALID ────────────────────────────────────────────

console.log('\n[13] ADR superseded reference');

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/notif/20260520-old.md', '- Status: superseded by [new](./20260521-new.md)\n');
  write(tmp, 'docs/adr/notif/20260521-new.md', '- Status: accepted\n');
  assert('13.1  superseded with existing ref → no FAIL', !hasFail(checkAdrStatus(tmp), 'ADR_SUPERSEDED_REF_INVALID'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/adr/20260520-old.md', '- Status: superseded by [missing](./20260521-gone.md)\n');
  assert('13.2  superseded with missing ref → ADR_SUPERSEDED_REF_INVALID FAIL', hasFail(checkAdrStatus(tmp), 'ADR_SUPERSEDED_REF_INVALID'));
} finally { cleanup(tmp); } }

// ─── [14] parsePomModules + checkModuleRouting ─────────────────────────────────

console.log('\n[14] parsePomModules (pure) + checkModuleRouting');

assert('14.1  parsePomModules extracts modules', (() => {
  const xml = '<project><modules><module>svc-a</module><module>svc-b</module></modules></project>';
  const mods = parsePomModules(xml);
  return mods.length === 2 && mods.includes('svc-a') && mods.includes('svc-b');
})());

assert('14.2  parsePomModules empty when no modules block', parsePomModules('<project></project>').length === 0);

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'docs/context-index.md', `## Service Context (Level 2)\n\n| Servicio | Doc |\n| --- | --- |\n| \`svc-a\` | [doc](./doc.md) |\n`);
  assert('14.3  module + routing → MODULE_ROUTING_COMPLETENESS PASS', !hasFail(checkModuleRouting(tmp), 'ROUTING_DEAD_ENTRY') && !hasWarn(checkModuleRouting(tmp), 'MODULE_WITHOUT_ROUTING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module><module>svc-b</module></modules></project>');
  write(tmp, 'docs/context-index.md', `## Service Context (Level 2)\n\n| Servicio | Doc |\n| --- | --- |\n| \`svc-a\` | [doc](./doc.md) |\n`);
  assert('14.4  Maven module without routing → MODULE_WITHOUT_ROUTING WARN', hasWarn(checkModuleRouting(tmp), 'MODULE_WITHOUT_ROUTING'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // integration-tests is excluded → no WARN expected for it
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module><module>integration-tests</module></modules></project>');
  write(tmp, 'docs/context-index.md', `## Service Context (Level 2)\n\n| Servicio | Doc |\n| --- | --- |\n| \`svc-a\` | [doc](./doc.md) |\n`);
  const r = checkModuleRouting(tmp);
  const warns = r.filter(f => f.severity === 'WARN' && f.message.includes('integration-tests'));
  assert('14.5  integration-tests excluded → no MODULE_WITHOUT_ROUTING WARN for it', warns.length === 0);
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'docs/context-index.md', `## Service Context (Level 2)\n\n| Servicio | Doc |\n| --- | --- |\n| \`svc-a\` | [doc](./d.md) |\n| \`svc-ghost\` | [doc](./d.md) |\n`);
  assert('14.6  context-index entry without Maven module → ROUTING_DEAD_ENTRY FAIL', hasFail(checkModuleRouting(tmp), 'ROUTING_DEAD_ENTRY'));
} finally { cleanup(tmp); } }

// ─── [15] checkTemporalDrift ──────────────────────────────────────────────────

console.log('\n[15] checkTemporalDrift');

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project><dependencies></dependencies></project>');
  assert('15.1  service without JPA → no TEMPORAL_DRIFT WARN', !hasWarn(checkTemporalDrift(tmp), 'TEMPORAL_DRIFT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project><dependencies><dependency><artifactId>spring-boot-starter-data-jpa</artifactId></dependency></dependencies></project>');
  assert('15.2  JPA dependency detected → TEMPORAL_DRIFT WARN', hasWarn(checkTemporalDrift(tmp), 'TEMPORAL_DRIFT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project></project>');
  write(tmp, 'svc-a/src/main/java/com/app/models/entities/MyEntity.java', '@Entity\npublic class MyEntity {}');
  assert('15.3  @Entity in models/entities/ → TEMPORAL_DRIFT WARN', hasWarn(checkTemporalDrift(tmp), 'TEMPORAL_DRIFT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // @Entity outside models/entities/ should NOT trigger drift
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project></project>');
  write(tmp, 'svc-a/src/main/java/com/app/SomeClass.java', '@Entity\npublic class SomeClass {}');
  assert('15.4  @Entity outside models/entities/ → no TEMPORAL_DRIFT WARN', !hasWarn(checkTemporalDrift(tmp), 'TEMPORAL_DRIFT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // integration-tests excluded from drift scan
  write(tmp, 'pom.xml', '<project><modules><module>integration-tests</module></modules></project>');
  write(tmp, 'integration-tests/pom.xml', '<project><dependencies><dependency><artifactId>spring-boot-starter-data-jpa</artifactId></dependency></dependencies></project>');
  assert('15.5  integration-tests excluded → no TEMPORAL_DRIFT WARN', !hasWarn(checkTemporalDrift(tmp), 'TEMPORAL_DRIFT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  // Drift in svc-a must not affect svc-b
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module><module>svc-b</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project><dependencies><dependency><artifactId>spring-boot-starter-data-jpa</artifactId></dependency></dependencies></project>');
  write(tmp, 'svc-b/pom.xml', '<project></project>');
  const r = checkTemporalDrift(tmp);
  const aWarn = r.some(f => f.severity === 'WARN' && f.message.includes('svc-a'));
  const bWarn = r.some(f => f.severity === 'WARN' && f.message.includes('svc-b'));
  assert('15.6  drift in svc-a does not contaminate svc-b report', aWarn && !bWarn);
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'pom.xml', '<project><modules><module>svc-a</module></modules></project>');
  write(tmp, 'svc-a/pom.xml', '<project><dependencies><dependency><artifactId>spring-boot-starter-data-jpa</artifactId></dependency></dependencies></project>');
  const r = checkTemporalDrift(tmp);
  assert('15.7  TEMPORAL_DRIFT is always WARN, never FAIL', r.every(f => f.id !== 'TEMPORAL_DRIFT' || f.severity === 'WARN'));
} finally { cleanup(tmp); } }

// ─── Summary ──────────────────────────────────────────────────────────────────

console.log('\n' + '═'.repeat(56));
console.log(`PASS: ${passed}  │  FAIL: ${failed}`);
console.log('═'.repeat(56));
if (failed > 0) { console.error('\nTest suite FAILED'); process.exit(1); }
else { console.log('\nTest suite PASSED'); process.exit(0); }
