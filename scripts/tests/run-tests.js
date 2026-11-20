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
  checkSkillsIntegrity,
  checkSpecsIntegrity,
  runAllChecks,
  // Pure helpers
  extractMarkdownLinks,
  resolveInternalLink,
  extractCodespanPaths,
  parseDeuaTecnicaFile,
  parseAdrEntry,
  parsePomModules,
  parseContextIndexServices,
  parseSpecFile,
} = require('../agent-check/index');
const { checkSkillAdapters } = require('../agent-check/checks/skills');
const { checkInterfazHarness, TOPES } = require('../agent-check/checks/interfaz');
const { checkManifiestosUi } = require('../agent-check/checks/manifiestos-ui');

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
  write(tmp, 'cliente-liviano/AGENTS.md', '# Cliente liviano nested'); // E6: allowlisted nested required
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
  write(tmp, 'cliente-liviano/AGENTS.md', '# Cliente liviano nested');
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
  write(tmp, 'cliente-liviano/AGENTS.md', '# Cliente liviano nested'); // E6: allowlisted nested required
  write(tmp, 'docs/IA/review/evaluator.md', '# Evaluator');
  write(tmp, 'docs/clean.md', '# Clean');
  write(tmp, 'docs/context-index.md', '# No code spans');
  write(tmp, 'docs/adr/DEUDA_TECNICA.md', '# No DTIs');
  write(tmp, '.gitignore', '!.agents/skills/\n');
  const skills = ['define-spec', 'design-spec', 'design-review', 'implement-task', 'implementation-review', 'engineering-loop', 'explain-concept', 'review-pr'];
  for (const s of skills) {
    let extra = 'AGENTS.md anchor\n';
    if (s === 'design-review') extra += 'D1 D2 D3 D4 D5 D6\n';
    if (s === 'implementation-review') extra += 'V1 V2 V3 V4 V5 V6 V7 V8 V9\n';
    if (s === 'explain-concept') extra += 'Paso 1: Paso 2: Paso 3: Paso 4:\n';
    if (s === 'review-pr') extra += 'Arquitectura Contratos Concurrencia Tests Seguridad Rendimiento Scope Simplicidad\ngrepai_search grepai_trace_callers grepai_trace_callees\nREVISIÓN CRÍTICA DE PR Veredicto Matriz de Evaluación Rápida Hallazgos Bloqueantes\n';
    write(tmp, `.agents/skills/${s}/SKILL.md`, `---\nname: ${s}\ndescription: desc\n---\n${extra}`);
  }
  for (const s of ['construir-ui', 'revisar-ui', 'explorar-ui']) {
    write(tmp, `.agents/skills/${s}/SKILL.md`, `---\nname: ${s}\ndescription: desc\n---\nAGENTS.md\n`);
  }
  write(tmp, '.claude/rules/interfaz.md', '# Interfaz\n');
  write(tmp, 'docs/specs/active/.gitkeep', '');
  write(tmp, 'docs/specs/completed/.gitkeep', '');
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

// ─── [16] checkSpecsIntegrity & parseSpecFile ───────────────────────────────

console.log('\n[16] checkSpecsIntegrity & parseSpecFile');

{
  const canonicalContent = `
# Spec: SPEC-99 — Test Specification

> **Estado:** APPROVED_BY_USER
> **Nivel:** ARCHITECTURAL
> **Fecha:** 2026-09-11

## 1. Objetivo Funcional (Goal)
Goal description.

## 2. Alcance Delimitado (Scope Boundaries)
### In-Scope
- item 1
### Out-of-Scope
- item 2

## 3. Decisiones Acordadas y Trade-offs
Trade-off evaluation under 5D criteria.

## 4. Invariantes
- invariant 1

## 5. Criterios de Aceptación (Gherkin / Given-When-Then)
Dado precondition
Cuando action
Entonces result

## 7. Especificación Técnica de Diseño (Technical Design)
Two-Gate ADR evaluation and TDD plan.

=== DESIGN REVIEW CONTRACT ===
Verdict: DESIGN_APPROVED
`;

  const parsed = parseSpecFile(canonicalContent);
  assert('16.1  parseSpecFile all fields true on canonical spec',
    parsed.hasCanonicalTitle &&
    parsed.hasStatus &&
    parsed.hasLevel &&
    parsed.isArchitectural &&
    parsed.hasGoal &&
    parsed.hasScope &&
    parsed.hasTradeoffs &&
    parsed.hasInvariants &&
    parsed.hasGherkin &&
    parsed.hasTechnicalDesign &&
    parsed.hasReviewContract
  );

  const brokenScope = `# Spec: S\n> **Estado:** OK\n> **Nivel:** STANDARD\n## 1. Goal\n## 2. Alcance Delimitado\n### In-Scope\n`;
  assert('16.2  parseSpecFile detects missing Out-of-Scope', !parseSpecFile(brokenScope).hasScope);

  const brokenGherkin = `# Spec: S\n> **Estado:** OK\n> **Nivel:** STANDARD\n## 5. Criterios de Aceptación\nJust plain text`;
  assert('16.3  parseSpecFile detects missing Given-When-Then keywords', !parseSpecFile(brokenGherkin).hasGherkin);
}

{ const tmp = makeTemp(); try {
  assert('16.4  missing docs/specs directory → SPECS_DIR_EXISTS FAIL', hasFail(checkSpecsIntegrity(tmp), 'SPECS_DIR_EXISTS'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/specs/active/.gitkeep', '');
  write(tmp, 'docs/specs/completed/.gitkeep', '');
  assert('16.5  empty docs/specs with .gitkeep → no FAIL and SPECS_INTEGRITY PASS', noFail(checkSpecsIntegrity(tmp)));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/specs/active/SPEC-01-valid.md', `
# Spec: SPEC-01 — Valid Spec
> **Estado:** APPROVED_BY_USER
> **Nivel:** STANDARD
## 1. Objetivo Funcional (Goal)
Goal.
## 2. Alcance Delimitado
### In-Scope
### Out-of-Scope
## 3. Decisiones Acordadas y Trade-offs
Trade-off with 5D.
## 4. Invariantes
Invariants.
## 5. Criterios de Aceptación
Dado x
Cuando y
Entonces z
`);
  assert('16.6  valid STANDARD spec in active → no FAIL', noFail(checkSpecsIntegrity(tmp)));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/specs/active/SPEC-02-no-tradeoffs.md', `
# Spec: SPEC-02 — No Tradeoffs
> **Estado:** APPROVED_BY_USER
> **Nivel:** STANDARD
## 1. Objetivo Funcional (Goal)
Goal.
## 2. Alcance Delimitado
### In-Scope
### Out-of-Scope
## 4. Invariantes
Invariants.
## 5. Criterios de Aceptación
Dado x
Cuando y
Entonces z
`);
  assert('16.7  missing Trade-offs section → SPEC_SECTION_TRADEOFFS FAIL', hasFail(checkSpecsIntegrity(tmp), 'SPEC_SECTION_TRADEOFFS'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/specs/completed/SPEC-03-no-review.md', `
# Spec: SPEC-03 — Completed without Review Contract
> **Estado:** COMPLETED
> **Nivel:** STANDARD
## 1. Objetivo Funcional (Goal)
Goal.
## 2. Alcance Delimitado
### In-Scope
### Out-of-Scope
## 3. Decisiones Acordadas y Trade-offs
Trade-off 5D.
## 4. Invariantes
Invariants.
## 5. Criterios de Aceptación
Dado x
Cuando y
Entonces z
`);
  assert('16.8  completed spec without review contract → SPEC_COMPLETED_REVIEW_CONTRACT FAIL', hasFail(checkSpecsIntegrity(tmp), 'SPEC_COMPLETED_REVIEW_CONTRACT'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, 'docs/specs/completed/SPEC-04-with-review.md', `
# Spec: SPEC-04 — Completed with Review Contract
> **Estado:** COMPLETED
> **Nivel:** STANDARD
## 1. Objetivo Funcional (Goal)
Goal.
## 2. Alcance Delimitado
### In-Scope
### Out-of-Scope
## 3. Decisiones Acordadas y Trade-offs
Trade-off 5D.
## 4. Invariantes
Invariants.
## 5. Criterios de Aceptación
Dado x
Cuando y
Entonces z

=== REVIEW CONTRACT ===
Result: PASS
`);
  assert('16.9  completed spec with review contract → no FAIL', noFail(checkSpecsIntegrity(tmp)));
} finally { cleanup(tmp); } }

// ─── [17] checkSkillsIntegrity & review-pr ────────────────────────────────────

console.log('\n[17] checkSkillsIntegrity & review-pr');

{ const tmp = makeTemp(); try {
  // Empty repo missing skills dir
  assert('17.1  missing skills dir → SKILLS_DIR_EXISTS FAIL', hasFail(checkSkillsIntegrity(tmp), 'SKILLS_DIR_EXISTS'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, '.agents/skills/dummy.txt', 'not tracked');
  // Missing gitignore un-ignore
  write(tmp, '.gitignore', '# empty');
  assert('17.2  missing !.agents/skills/ in gitignore → SKILLS_GITIGNORE_TRACKED FAIL', hasFail(checkSkillsIntegrity(tmp), 'SKILLS_GITIGNORE_TRACKED'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, '.gitignore', '!.agents/skills/\n');
  write(tmp, '.agents/skills/review-pr/SKILL.md', `---
name: review-pr
description: Review skill
---
# Skill review-pr
References AGENTS.md.
## Protocol
grepai_search grepai_trace_callers grepai_trace_callees
## Vectors
Arquitectura Contratos Concurrencia Tests Seguridad Rendimiento Scope Simplicidad
## Report
REVISIÓN CRÍTICA DE PR
Veredicto
Matriz de Evaluación Rápida
Hallazgos Bloqueantes
`);
  const findings = checkSkillsIntegrity(tmp);
  assert('17.3  review-pr with frontmatter, AGENTS.md anchor, vectors, grepai, report → PASS on review-pr checks',
    findings.some(f => f.severity === 'PASS' && f.id === 'REVIEW_PR_VECTORS') &&
    findings.some(f => f.severity === 'PASS' && f.id === 'REVIEW_PR_GREPAI_PROTOCOL') &&
    findings.some(f => f.severity === 'PASS' && f.id === 'REVIEW_PR_REPORT_FORMAT')
  );
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  write(tmp, '.gitignore', '!.agents/skills/\n');
  write(tmp, '.agents/skills/review-pr/SKILL.md', `---
name: review-pr
description: Incomplete review skill
---
# Incomplete
References AGENTS.md.
Missing vectors and protocol.
`);
  const findings = checkSkillsIntegrity(tmp);
  assert('17.4  review-pr missing vectors → REVIEW_PR_VECTORS FAIL', hasFail(findings, 'REVIEW_PR_VECTORS'));
  assert('17.5  review-pr missing grepai tools → REVIEW_PR_GREPAI_PROTOCOL FAIL', hasFail(findings, 'REVIEW_PR_GREPAI_PROTOCOL'));
} finally { cleanup(tmp); } }

// ─── [18] checkSkillAdapters & checkInterfazHarness ──────────────────────────

console.log('\n[18] checkSkillAdapters & checkInterfazHarness');

{ const tmp = makeTemp(); try {
  const skill = '---\nname: construir-ui\ndescription: desc\n---\n';
  write(tmp, '.agents/skills/construir-ui/SKILL.md', skill + 'Canónica.\n');
  write(tmp, '.claude/skills/construir-ui/SKILL.md', skill + 'Leé la canónica.\n');
  assert('18.1  adapter with the same frontmatter → SKILL_ADAPTER_SYNC PASS', noFail(checkSkillAdapters(tmp)));
  write(tmp, '.claude/skills/construir-ui/SKILL.md', '---\nname: construir-ui\ndescription: otra\n---\n');
  assert('18.2  adapter with a different frontmatter → SKILL_ADAPTER_SYNC FAIL', hasFail(checkSkillAdapters(tmp), 'SKILL_ADAPTER_SYNC'));
  write(tmp, '.claude/skills/sin-canonica/SKILL.md', skill);
  assert('18.3  adapter without canonical skill → SKILL_ADAPTER_SYNC FAIL', hasFail(checkSkillAdapters(tmp), 'SKILL_ADAPTER_SYNC'));
  write(tmp, '.agents/skills/sin-canonica/SKILL.md', 'Sin frontmatter.\n');
  write(tmp, '.claude/skills/sin-canonica/SKILL.md', 'Sin frontmatter.\n');
  assert('18.7  adapter and canonical without frontmatter → SKILL_ADAPTER_SYNC FAIL', hasFail(checkSkillAdapters(tmp), 'SKILL_ADAPTER_SYNC'));
} finally { cleanup(tmp); } }

{ const tmp = makeTemp(); try {
  for (const [rel] of TOPES) write(tmp, rel, 'una línea\n');
  assert('18.4  UI harness files within their caps → UI_HARNESS_SIZE PASS', noFail(checkInterfazHarness(tmp)));
  write(tmp, '.claude/rules/interfaz.md', 'x\n'.repeat(11));
  assert('18.5  rule over 10 lines → UI_HARNESS_SIZE FAIL', hasFail(checkInterfazHarness(tmp), 'UI_HARNESS_SIZE'));
  fs.rmSync(path.join(tmp, '.agents/skills/revisar-ui/SKILL.md'));
  assert('18.6  missing UI skill → UI_HARNESS_SIZE FAIL', hasFail(checkInterfazHarness(tmp), 'UI_HARNESS_SIZE'));
} finally { cleanup(tmp); } }

// ─── [19] checkManifiestosUi & workflow oleada-ui ────────────────────────────

console.log('\n[19] checkManifiestosUi & workflow oleada-ui');

const REPO = path.resolve(__dirname, '../..');
const PLANTILLA = JSON.parse(fs.readFileSync(path.join(REPO, 'docs/interfaz/plantillas/manifiesto-oleada.ejemplo.json'), 'utf8'));
const WORKFLOW = fs.readFileSync(path.join(REPO, '.claude/workflows/oleada-ui.js'), 'utf8');
const conManifiesto = (mutar) => {
  const m = JSON.parse(JSON.stringify(PLANTILLA));
  mutar(m);
  return m;
};
const chequear = (m, rel = 'docs/interfaz/m.json') => {
  const tmp = makeTemp();
  try {
    write(tmp, rel, typeof m === 'string' ? m : JSON.stringify(m));
    return checkManifiestosUi(tmp);
  } finally { cleanup(tmp); }
};
const manifiestoFalla = (m) => hasFail(chequear(m), 'UI_WAVE_MANIFEST');

assert('19.1  template manifest → UI_WAVE_MANIFEST PASS', noFail(chequear(PLANTILLA)));
assert('19.2  decision without lote or diferida → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { delete m.decisiones[0].lote; delete m.decisiones[0].criterio; })));
assert('19.3  decision assigned to a missing lote → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { m.decisiones[0].lote = 'no-existe'; })));
assert('19.4  decision in a lote without criterio → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { delete m.decisiones[0].criterio; })));
assert('19.5  diferida without ok → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { delete m.decisiones[2].diferida.ok; })));
assert('19.6  duplicated decision id → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { m.decisiones[1].id = m.decisiones[0].id; })));
assert('19.7  decision with lote and diferida → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla(conManifiesto((m) => { m.decisiones[0].diferida = { quien: 'usuario', ok: 'ronda' }; })));
assert('19.8  JSON without formato (R11 manifest) → ignored',
  noFail(chequear({ version: '1.0.0', lotes: [], decisiones: [{ id: 'D1' }] })));
assert('19.9  invalid JSON that names oleada-ui/1 → UI_WAVE_MANIFEST FAIL',
  manifiestoFalla('{ "formato": "oleada-ui/1", '));
assert('19.10 manifest under docs/IA/history → ignored',
  noFail(chequear(conManifiesto((m) => { delete m.decisiones[0].lote; }), 'docs/IA/history/x/m.json')));

const bloqueDeRegla = (codigo) => {
  const m = codigo.match(/\/\/ ── Regla oleada-ui\/1[\s\S]*?\/\/ ── fin de la regla oleada-ui\/1 ──/);
  return m ? m[0].replace(/\r\n/g, '\n') : null;
};
const reglaDelWorkflow = bloqueDeRegla(WORKFLOW);
assert('19.11 the rule is identical in the workflow and in the check',
  reglaDelWorkflow !== null &&
  reglaDelWorkflow === bloqueDeRegla(fs.readFileSync(path.join(REPO, 'scripts/agent-check/checks/manifiestos-ui.js'), 'utf8')));

// El workflow, cargado como en el sandbox (sin el export) y con agentes que no responden: prueba la
// sintaxis y el arranque sin lanzar agentes. No reemplaza una corrida real.
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const correrWorkflow = async (manifiesto, responder = async () => null) => {
  const llamadas = [];
  const agentes = [];
  const fn = new AsyncFunction('args', 'agent', 'parallel', 'phase', 'log', WORKFLOW.replace(/^export const meta/m, 'const meta'));
  const resultado = await fn(
    { manifiesto, ola: 'P0', fase: 'construir', carpeta: '/tmp/oleada' },
    async (prompt, opts) => { llamadas.push(opts.label); agentes.push({ prompt, ...opts }); return responder(opts); },
    async (thunks) => Promise.all(thunks.map((t) => t())),
    () => {}, () => {});
  return { resultado, llamadas, agentes };
};
// Agentes falsos que completan su esquema: todo responde bien, salvo la revisión de pantallas si
// pantallasPasan es false, los agentes cuya etiqueta está en mudos, y las revisiones que rinden una
// decisión como NO CUMPLE sin listarla como bloqueante o que omiten una.
const agenteFalso = (pantallasPasan, { mudos = [], noCumple = '', omite = '', extra = [] } = {}) => async (opts) => {
  if (mudos.includes(opts.label)) return null;
  const props = opts.schema.properties;
  const r = {};
  for (const k of opts.schema.required) r[k] = props[k].type === 'array' ? [] : props[k].type === 'boolean' ? true : 'x';
  if ('base' in props) r.base = 'sha0';
  if ('integrados' in props) r.integrados = PLANTILLA.lotes.map((l) => l.id);
  if ('veredicto' in props) {
    const falla = !pantallasPasan && opts.label.startsWith('pantallas');
    r.veredicto = falla ? 'CHANGES_REQUIRED' : 'PASS';
    if (falla) r.bloqueantes = ['inconsistencia entre pantallas'];
    r.decisiones = [...['D1', 'D2'].filter((id) => id !== omite).map((id) => `${id}: ${id === noCumple ? 'NO CUMPLE' : 'CUMPLE'} — captura`), ...extra];
  }
  if ('url' in props) r.url = 'https://pr';
  return r;
};

(async () => {
  let rechazo = null;
  let llamadasAlRechazar = -1;
  try {
    const r = await correrWorkflow(conManifiesto((m) => { delete m.decisiones[0].lote; delete m.decisiones[0].criterio; }));
    llamadasAlRechazar = r.llamadas.length;
  } catch (e) { rechazo = e; llamadasAlRechazar = 0; }
  assert('19.12 workflow with an unassigned decision → throws before any agent',
    rechazo !== null && /sin asignar/.test(rechazo.message) && llamadasAlRechazar === 0);

  const { resultado, llamadas } = await correrWorkflow(PLANTILLA);
  assert('19.13 workflow with the template → one agent, stops at Preparar',
    llamadas.length === 1 && /Detenida en Preparar/.test(resultado.detenida));
  assert('19.14 workflow reports every decision by ID (assigned and diferida)',
    resultado.decisionesDelUsuario.length === 3 &&
    resultado.decisionesDelUsuario.some((d) => d.startsWith('D3: diferida')));

  const completa = await correrWorkflow(PLANTILLA, agenteFalso(true));
  assert('19.15 full run with passing reviews → screen review, contact sheet and PR, decisions met',
    completa.resultado.pr === 'https://pr' &&
    ['pantallas r1', 'hoja P0', 'PR P0'].every((l) => completa.llamadas.includes(l)) &&
    completa.resultado.decisionesDelUsuario.filter((d) => d.includes('cumplida (PASS')).length === 2);

  const sinPass = await correrWorkflow(PLANTILLA, agenteFalso(false));
  assert('19.16 screen review without PASS → no contact sheet, no PR, decisions not met',
    sinPass.resultado.pr === '' && /No se abre el PR/.test(sinPass.resultado.detenida) &&
    !sinPass.llamadas.includes('hoja P0') && !sinPass.llamadas.includes('PR P0') &&
    sinPass.resultado.decisionesDelUsuario.filter((d) => d.includes('no cumplida')).length === 2);

  const noCumple = await correrWorkflow(PLANTILLA, agenteFalso(true, { noCumple: 'D1' }));
  assert('19.17 lote review with PASS but a decision NO CUMPLE → blocking, no PR',
    noCumple.resultado.pr === '' && /p0-mis-donaciones/.test(noCumple.resultado.detenida) &&
    noCumple.llamadas.includes('arreglos p0-mis-donaciones r1'));

  const sinConstruir = await correrWorkflow(PLANTILLA, agenteFalso(true, { mudos: ['p0-mis-donaciones'] }));
  assert('19.18 visual lote with decisions never built → no PR, decisions not met',
    sinConstruir.resultado.pr === '' && /p0-mis-donaciones/.test(sinConstruir.resultado.detenida) &&
    sinConstruir.resultado.decisionesDelUsuario.filter((d) => d.includes('no cumplida: el constructor no respondió')).length === 2);

  const omitida = await correrWorkflow(PLANTILLA, agenteFalso(true, { omite: 'D2' }));
  assert('19.19 review with PASS that omits a decision → not met, no PR',
    omitida.resultado.pr === '' && /p0-mis-donaciones/.test(omitida.resultado.detenida) &&
    !omitida.resultado.decisionesDelUsuario.some((d) => d.startsWith('D2') && d.includes(': cumplida')));

  const porEtiqueta = (l) => completa.agentes.find((a) => a.label === l);
  const vista = porEtiqueta('p0-mis-donaciones');
  const servicio = porEtiqueta('p0-api-donaciones');
  const pr = porEtiqueta('PR P0');
  assert('19.20 views: Opus high in builder, review and screen review; no "Lo más simple" in views only',
    vista.model === 'opus' && vista.effort === 'high' && !vista.prompt.includes('Lo más simple que funcione') &&
    servicio.effort !== 'high' && servicio.prompt.includes('Lo más simple que funcione') &&
    ['revisión p0-mis-donaciones r1', 'pantallas r1'].every((l) => porEtiqueta(l).effort === 'high'));
  assert('19.21 PR as draft with the unchecked contact-sheet box',
    pr.prompt.includes('gh pr create --draft') && pr.prompt.includes('- [ ] Hoja de contacto aprobada por el usuario'));

  const contradice = await correrWorkflow(PLANTILLA, agenteFalso(true, { extra: ['D2: NO CUMPLE — captura'] }));
  const parcial = await correrWorkflow(PLANTILLA, agenteFalso(true, { omite: 'D2', extra: ['D2: CUMPLE parcialmente'] }));
  assert('19.22 contradictory lines (NO CUMPLE wins) or "CUMPLE parcialmente" → no PR',
    contradice.resultado.pr === '' && parcial.resultado.pr === '' &&
    !parcial.resultado.decisionesDelUsuario.some((d) => d.startsWith('D2') && d.includes(': cumplida')));

  // ─── [20] workflow frente-backend ──────────────────────────────────────────
  // Igual que arriba: el script cargado como en el sandbox, con agentes falsos. Prueba la regla del
  // manifiesto, el orden de las tandas, cada fila de "Cuando algo falla" de la guía, el estado
  // entre corridas y los controles de la fase pr. No reemplaza una corrida real.
  console.log('\n[20] workflow frente-backend');

  const RUTA_FRENTE = path.join(REPO, '.claude/workflows/frente-backend.js');
  assert('20.0  .claude/workflows/frente-backend.js is in the repo (it is force-added: .claude/ may be locally excluded)',
    fs.existsSync(RUTA_FRENTE));
  if (!fs.existsSync(RUTA_FRENTE)) { resumen(); return; }
  const FRENTE = fs.readFileSync(RUTA_FRENTE, 'utf8');
  const RUTA_GQL = ['active', 'completed']
    .map((d) => path.join(REPO, 'docs/specs', d, 'SPEC-11-graphql.manifiesto.json'))
    .find((r) => fs.existsSync(r));
  const GQL = JSON.parse(fs.readFileSync(RUTA_GQL, 'utf8'));
  const TODOS = GQL.lotes.map((l) => l.id);
  const SHA_A = 'a'.repeat(40);
  const SHA_CIERRE = '9'.repeat(40);
  const conFrente = (mutar) => { const m = JSON.parse(JSON.stringify(GQL)); mutar(m); return m; };
  const correrFrente = async (args, responder = async () => null) => {
    const agentes = [];
    const fn = new AsyncFunction('args', 'agent', 'parallel', 'phase', 'log', FRENTE.replace(/^export const meta/m, 'const meta'));
    const resultado = await fn(
      { manifiesto: GQL, carpeta: '/tmp/frente', ...args },
      async (prompt, opts) => { agentes.push({ prompt, ...opts }); return responder(opts, prompt); },
      async (thunks) => Promise.all(thunks.map((t) => t())),
      () => {}, () => {});
    return { resultado, agentes, llamadas: agentes.map((a) => a.label) };
  };
  // Agentes falsos que completan su esquema y responden bien. "cambios" pisa la respuesta de una
  // etiqueta: recibe la respuesta por defecto y cuántas veces se llamó. "guardado.estado" es el
  // último estado.json escrito, tal como lo devolvería el preparador de la corrida siguiente.
  const frenteFalso = ({ mudos = [], estado = null, head = 'c0ffee0'.repeat(5) + 'c0ffe', noCumple = '', tareasVigentes = [], lotesEnRama = null, sinTipos = false, cambios = {} } = {}) => {
    const guardado = { estado };
    const veces = {};
    const responder = async (opts, prompt) => {
      if (sinTipos && opts.agentType) throw new Error('agent type not found');
      if (mudos.includes(opts.label)) return null;
      const props = opts.schema.properties;
      const r = {};
      for (const k of opts.schema.required) r[k] = props[k].type === 'array' ? [] : props[k].type === 'boolean' ? true : 'x';
      if (opts.label === 'preparar') {
        // Por defecto, la rama tiene los lotes que el estado da por integrados.
        const enRama = lotesEnRama || (estado && typeof estado === 'object' ? estado.lotes : []);
        Object.assign(r, { base: head, baseline: 'GREEN', tareasVigentes, lotesEnRama: enRama, hayEstado: Boolean(estado),
          estado: !estado ? '' : typeof estado === 'string' ? estado : JSON.stringify(estado) });
      }
      if ('integrados' in props) Object.assign(r, { integrados: TODOS, sha: 'b'.repeat(40) });
      if ('veredicto' in props) {
        Object.assign(r, { veredicto: 'PASS', shaAuditado: SHA_A });
        r.decisiones = GQL.decisiones.map((d) => `${d.id}: ${d.id === noCumple ? 'NO CUMPLE' : 'CUMPLE'} — archivo:1`);
      }
      if (opts.label.startsWith('estado')) {
        const m = prompt.match(/\n(\{"formato".*\})\n/);
        if (m) { r.contenido = m[1]; guardado.estado = JSON.parse(m[1]); }
      }
      if (opts.label.startsWith('revertir')) r.detalle = 'e'.repeat(40);
      if (opts.label === 'cierre') r.sha = SHA_CIERRE;
      if ('url' in props) r.url = 'https://pr';
      veces[opts.label] = (veces[opts.label] || 0) + 1;
      return cambios[opts.label] ? cambios[opts.label](r, veces[opts.label]) : r;
    };
    return { responder, guardado };
  };
  const lanza = async (args, responder) => {
    try { const r = await correrFrente(args, responder); return { error: null, llamadas: r.llamadas.length }; }
    catch (e) { return { error: e, llamadas: 0 }; }
  };
  const antes = (ls, a, b) => ls.indexOf(a) !== -1 && ls.indexOf(a) < ls.indexOf(b);
  const tareaEn = (r, t, veredicto) => r.resultado.tareas.some((x) => x.startsWith(`${t}: ${veredicto}`));

  // ── La regla del manifiesto ──
  const arranque = await correrFrente({ fase: 'disenar' });
  assert('20.1  SPEC-11 manifest passes the rule; mute agents → stops at Preparar after one agent',
    arranque.llamadas.length === 1 && /Detenida en Preparar/.test(arranque.resultado.detenida));

  const sinDestino = await lanza({ fase: 'disenar', manifiesto: conFrente((m) => { delete m.decisiones[0].lote; delete m.decisiones[0].criterio; }) });
  assert('20.2  decision without lote or aCargoDe → throws before any agent',
    sinDestino.error !== null && /sin destino/.test(sinDestino.error.message) && sinDestino.llamadas === 0);

  const zona = (id, glob) => lanza({ fase: 'disenar', manifiesto: conFrente((m) => { m.lotes.find((l) => l.id === id).archivos.push(glob); }) });
  const DS = 'donaciones-service/src/main/java/grupo5/donaciones';
  const solapado = await zona('g2-entidades', `${DS}/graphql/necesidades/Algo.java`);
  const carpeta = await zona('g2-entidades', `${DS}/seguridad`);
  const llaves = await zona('g2-entidades', `${DS}/{graphql,otro}/donaciones/X.java`);
  assert('20.3  two lotes of the same tanda with overlapping zones (file in a glob, plain folder, braces) → throws',
    [solapado, carpeta, llaves].every((x) => x.error !== null && /se solapan/.test(x.error.message)));

  const desordenado = await lanza({ fase: 'disenar', manifiesto: conFrente((m) => { m.lotes.find((l) => l.id === 'g2-armazon').tanda = 5; }) });
  const comillas = await lanza({ fase: 'disenar', manifiesto: conFrente((m) => { m.pr.titulo = 'E7: "GraphQL"'; }) });
  assert('20.4  lote with a tanda after a later task, or a PR title with quotes → throws',
    desordenado.error !== null && /tarea que viene después/.test(desordenado.error.message) &&
    comillas.error !== null && /titulo/.test(comillas.error.message));

  const sinAprobacion = await lanza({ fase: 'construir' });
  assert('20.5  construir without disenoAprobado → throws before any agent',
    sinAprobacion.error !== null && /disenoAprobado/.test(sinAprobacion.error.message) && sinAprobacion.llamadas === 0);

  // ── disenar ──
  const fDisenar = frenteFalso();
  const disenar = await correrFrente({ fase: 'disenar' }, fDisenar.responder);
  assert('20.6  disenar: design lote, integration, preauditor and auditor; no construction, no PR',
    ['diseño g1-esquema', 'integrar el diseño', 'preauditoría G1 r1', 'auditoría G1 r1'].every((l) => disenar.llamadas.includes(l)) &&
    !disenar.llamadas.includes('g2-armazon') && !disenar.llamadas.includes('PR') &&
    disenar.resultado.paraAprobar.length === 1 && /Primer control/.test(disenar.resultado.siguiente));
  assert('20.7  disenar saves estado.json with G1 in PASS, the design SHA and the control field',
    fDisenar.guardado.estado !== null && fDisenar.guardado.estado.tareas.G1.veredicto === 'PASS' &&
    fDisenar.guardado.estado.shaDiseno === SHA_A && Number.isInteger(fDisenar.guardado.estado.control));
  const ESTADO_DISENO = fDisenar.guardado.estado;
  const construirCon = (opciones = {}, args = {}) => {
    const f = frenteFalso({ estado: ESTADO_DISENO, ...opciones });
    return correrFrente({ fase: 'construir', disenoAprobado: SHA_A, ...args }, f.responder).then((r) => ({ ...r, guardado: f.guardado }));
  };

  // ── construir, camino feliz ──
  const construir = await construirCon();
  const lc = construir.llamadas;
  assert('20.8  construir: tandas in order, each task audited when its last tanda is integrated',
    antes(lc, 'g2-armazon', 'g2-necesidades') && antes(lc, 'g2-donaciones', 'g2-relaciones') &&
    antes(lc, 'g2-relaciones', 'auditoría G2 r1') && antes(lc, 'auditoría G2 r1', 'g3-limites') &&
    antes(lc, 'g4-gateway-contrato', 'auditoría G3 r1') && antes(lc, 'auditoría G3 r1', 'auditoría G4 r1') &&
    !lc.includes('auditoría G1 r1'));
  assert('20.9  construir: documentation, closing audit and, last, the closing gates; shaFinal is the commit the gates ran on; no push or PR',
    antes(lc, 'documentación', 'auditoría cierre r1') && antes(lc, 'auditoría cierre r1', 'cierre') && !lc.includes('PR') &&
    ['G1', 'G2', 'G3', 'G4'].every((t) => construir.guardado.estado.tareas[t].veredicto === 'PASS') &&
    construir.guardado.estado.cierre.veredicto === 'PASS' && construir.guardado.estado.shaFinal === SHA_CIERRE &&
    /Segundo control/.test(construir.resultado.siguiente) && construir.resultado.detenida === '');
  const etiqueta = (l) => construir.agentes.find((a) => a.label === l);
  assert('20.10 models by role: builders Opus medium in a worktree, integrator Sonnet high, preauditor Haiku, auditor Opus medium',
    etiqueta('g2-armazon').model === 'opus' && etiqueta('g2-armazon').effort === 'medium' && etiqueta('g2-armazon').isolation === 'worktree' &&
    etiqueta('integrar tanda 1').model === 'sonnet' && etiqueta('integrar tanda 1').effort === 'high' &&
    etiqueta('preauditoría G2 r1').model === 'haiku' && etiqueta('preauditoría G2 r1').agentType === 'preauditor' &&
    etiqueta('auditoría G2 r1').agentType === 'auditor' && etiqueta('auditoría G2 r1').effort === 'medium');
  const conStack = await construirCon({}, { stackDeLaRama: true });
  assert('20.11 Gate 3 runs only with stackDeLaRama',
    !etiqueta('cierre').prompt.includes('mvn verify -pl integration-tests') &&
    conStack.agentes.find((a) => a.label === 'cierre').prompt.includes('mvn verify -pl integration-tests'));
  assert('20.12 every builder gets the precedence regime, its zone and the approved contract; the task audit also looks at the integration fixes of its tandas',
    etiqueta('g2-donaciones').prompt.includes('gana el manifiesto') && etiqueta('g2-donaciones').prompt.includes('seguridad/Acceso.java') &&
    etiqueta('g2-donaciones').prompt.includes(GQL.contrato) && etiqueta('g2-donaciones').prompt.includes('D15') &&
    etiqueta('auditoría G2 r1').prompt.includes('integración tanda 3') && etiqueta('auditoría G3 r1').prompt.includes('integración tanda 4') &&
    antes(lc, 'integrar tanda 1', 'estado Integrar'));

  // ── construir, cuando algo falla ──
  const armazonMudo = await construirCon({ mudos: ['g2-armazon'] });
  assert('20.13 a lote that nobody delivers stops the later tandas and the later tasks',
    !armazonMudo.llamadas.includes('g2-necesidades') && !armazonMudo.llamadas.includes('g3-limites') &&
    !armazonMudo.llamadas.includes('cierre') && tareaEn(armazonMudo, 'G4', 'CHANGES_REQUIRED'));

  const noCumpleD08 = await construirCon({ noCumple: 'D08' });
  assert('20.14 a decision NO CUMPLE blocks: fixes and re-audits up to the cap, then the later tasks do not run',
    ['arreglos G2 r1', 'arreglos G2 r2', 'auditoría G2 r3'].every((l) => noCumpleD08.llamadas.includes(l)) &&
    !noCumpleD08.llamadas.includes('arreglos G2 r3') && !noCumpleD08.llamadas.includes('g3-limites') &&
    /G2 sin PASS/.test(noCumpleD08.resultado.detenida) &&
    noCumpleD08.resultado.decisionesDelUsuario.some((d) => d.startsWith('D08') && d.includes('no cumplida')));

  const parcial20 = await construirCon({ cambios: { 'integrar tanda 2': (r) => ({ ...r, integrados: ['g2-necesidades', 'g2-entidades'] }) } });
  const fantasma = await construirCon({ mudos: ['g2-donaciones'] });
  assert('20.15 a lote left out by the integrator, or claimed by it but never delivered, breaks its task',
    !parcial20.llamadas.includes('g2-relaciones') && tareaEn(parcial20, 'G2', 'CHANGES_REQUIRED (el lote g2-donaciones no se integró') &&
    !fantasma.guardado.estado.lotes.includes('g2-donaciones') && !fantasma.llamadas.includes('g2-relaciones'));

  const rojo = (r) => ({ ...r, gatesOk: false, detalle: 'un test en rojo' });
  const arreglado = await construirCon({ cambios: { 'integrar tanda 1': rojo } });
  assert('20.16 red gates after integrating: one fix, the gates run again, and the run goes on',
    antes(arreglado.llamadas, 'arreglo de gates tanda 1', 'gates tanda 1 después del arreglo') &&
    !arreglado.llamadas.includes('revertir tanda 1') && arreglado.llamadas.includes('g2-necesidades'));

  const sigueRojo = { 'integrar tanda 4': rojo, 'gates tanda 4 después del arreglo': (r) => ({ ...r, gatesOk: false, culpables: ['g3-limites'] }) };
  const revertido = await construirCon({ cambios: sigueRojo });
  assert('20.17 gates still red: only the lote that causes it is reverted, the gates run again, and the other task of the tanda is audited on its own',
    antes(revertido.llamadas, 'revertir tanda 4', 'gates tanda 4 después de revertir') &&
    !revertido.llamadas.includes('auditoría G3 r1') && revertido.llamadas.includes('auditoría G4 r1') &&
    tareaEn(revertido, 'G3', 'CHANGES_REQUIRED') && tareaEn(revertido, 'G4', 'PASS') &&
    !revertido.guardado.estado.lotes.includes('g3-limites'));

  const sinConfirmar = await construirCon({ cambios: { ...sigueRojo, 'revertir tanda 4': (r) => ({ ...r, detalle: 'Listo, HEAD quedó bien' }) } });
  const rojoAlFinal = await construirCon({ cambios: { ...sigueRojo, 'gates tanda 4 después de revertir': (r) => ({ ...r, gatesOk: false }) } });
  assert('20.18 a revert that does not return a SHA, or gates still red after it → the run stops and asks for a manual check',
    [sinConfirmar, rojoAlFinal].every((x) => /Revisala a mano/.test(x.resultado.detenida) && !x.llamadas.includes('documentación')));

  const auditorMudo = await construirCon({ mudos: ['auditoría G2 r1', 'auditoría G2 r2', 'auditoría G2 r3'] });
  const sinBloqueantes = await construirCon({ cambios: { 'auditoría G3 r1': (r, n) => (n === 1 ? { ...r, veredicto: 'CHANGES_REQUIRED' } : r) } });
  assert('20.19 a mute auditor never passes; CHANGES_REQUIRED without findings is audited again, with nothing to fix',
    tareaEn(auditorMudo, 'G2', 'CHANGES_REQUIRED') && !auditorMudo.llamadas.includes('arreglos G2 r1') && !auditorMudo.llamadas.includes('g3-limites') &&
    !sinBloqueantes.llamadas.includes('arreglos G3 r1') && sinBloqueantes.llamadas.includes('auditoría G3 r2') && tareaEn(sinBloqueantes, 'G3', 'PASS'));

  const SHA_F = 'f'.repeat(40);
  const disenoCambiado = await construirCon({}, { disenoAprobado: SHA_F });
  const disenoRechazado = await construirCon({ cambios: { 'auditoría G1 r1': (r) => ({ ...r, veredicto: 'CHANGES_REQUIRED', bloqueantes: ['el tipo Entidad expone representantes'] }) } }, { disenoAprobado: SHA_F });
  assert('20.20 design changed at the first control: audited again in one round, without preauditor or fixes; without PASS nothing is built',
    disenoCambiado.llamadas.includes('auditoría G1 r1') && !disenoCambiado.llamadas.includes('preauditoría G1 r1') &&
    disenoCambiado.llamadas.includes('g2-armazon') && disenoCambiado.guardado.estado.shaDiseno === SHA_F &&
    !disenoRechazado.llamadas.includes('arreglos G1 r1') && !disenoRechazado.llamadas.includes('g2-armazon') &&
    /diseño no está en PASS/.test(disenoRechazado.resultado.detenida));

  const cierreRojo = await construirCon({ cambios: { cierre: (r) => ({ ...r, gatesOk: false, gates: 'agent-check: 1 FAIL' }) } });
  const disenoTocado = await construirCon({ cambios: { cierre: (r) => ({ ...r, disenoIgual: false }) } });
  assert('20.21 closing gates red, or the design touched after the first control → the closing is not PASS',
    cierreRojo.guardado.estado.cierre.veredicto === 'CHANGES_REQUIRED' && /gates en rojo/.test(cierreRojo.resultado.detenida) &&
    disenoTocado.guardado.estado.cierre.veredicto === 'CHANGES_REQUIRED' && /diseño cambió/.test(disenoTocado.resultado.detenida));

  const noListo = await construirCon({ cambios: { preparar: (r) => ({ ...r, ok: false, motivo: 'hay cambios sin comitear' }) } });
  const sinDiseno = await construirCon({ cambios: { preparar: (r) => ({ ...r, disenoEnRama: false }) } });
  const perdidos = await construirCon({ estado: construir.guardado.estado, lotesEnRama: ['g1-esquema'] });
  const sobrantes = await construirCon({ lotesEnRama: ['g1-esquema', 'g2-armazon'] });
  const envuelto = await construirCon({ estado: '```json\n' + JSON.stringify(ESTADO_DISENO) + '\n```' , lotesEnRama: ['g1-esquema'] });
  assert('20.22 dirty tree, design not in the branch, or a branch that does not match the saved lotes (missing or extra) → does not start',
    noListo.llamadas.length === 1 && /cambios sin comitear/.test(noListo.resultado.detenida) &&
    sinDiseno.llamadas.length === 1 && /diseño aprobado/.test(sinDiseno.resultado.detenida) &&
    perdidos.llamadas.length === 1 && /no aparecen en la rama/.test(perdidos.resultado.detenida) &&
    sobrantes.llamadas.length === 1 && /ya están en la rama/.test(sobrantes.resultado.detenida) &&
    envuelto.llamadas.includes('g2-armazon'));

  // ── el estado entre corridas ──
  const truncado = await construirCon({ estado: JSON.stringify(ESTADO_DISENO).slice(0, 80) });
  const resumido = await construirCon({ estado: JSON.stringify({ ...ESTADO_DISENO, lotes: [] }) });
  assert('20.23 a truncated or altered estado.json is never taken as a fresh start',
    [truncado, resumido].every((x) => x.llamadas.length === 1 && /no se pudo leer entero/.test(x.resultado.detenida)));

  const malGuardado = await lanza({ fase: 'construir', disenoAprobado: SHA_A },
    frenteFalso({ estado: ESTADO_DISENO, cambios: { 'estado Integrar': (r) => ({ ...r, contenido: r.contenido.slice(0, 60) }) } }).responder);
  assert('20.24 the state read back from disk differs from what was sent → two attempts, then the run is cut',
    malGuardado.error !== null && /No se pudo guardar el estado/.test(malGuardado.error.message));

  const reanudado = await construirCon({ estado: noCumpleD08.guardado.estado, tareasVigentes: ['G1'] });
  assert('20.25 relaunching construir resumes: integrated lotes are not rebuilt, the task without PASS is audited again',
    !reanudado.llamadas.includes('g2-armazon') && reanudado.llamadas.includes('auditoría G2 r1') &&
    reanudado.llamadas.includes('g3-limites'));

  const duenoTocoG2 = await construirCon({ estado: construir.guardado.estado, tareasVigentes: ['G1', 'G3', 'G4'] });
  assert('20.26 a task that is no longer current (the owner committed on it) is audited again without rebuilding',
    duenoTocoG2.llamadas.includes('auditoría G2 r1') && !duenoTocoG2.llamadas.includes('g2-armazon') &&
    !duenoTocoG2.llamadas.includes('auditoría G3 r1') && duenoTocoG2.llamadas.includes('auditoría cierre r1'));

  // ── lotes condicionales y tipos de agente ──
  const CON_CONDICION = conFrente((m) => { m.lotes.find((l) => l.id === 'g4-gateway-contrato').condicion = 'con-gateway'; });
  const sinCondicion = await construirCon({}, { manifiesto: CON_CONDICION });
  const conCondicion = await construirCon({}, { manifiesto: CON_CONDICION, condiciones: ['con-gateway'] });
  assert('20.27 a conditional lote runs only with its condition; without it, its decisions are reported as not met',
    !sinCondicion.llamadas.includes('g4-gateway-contrato') && conCondicion.llamadas.includes('g4-gateway-contrato') &&
    sinCondicion.resultado.decisionesDelUsuario.some((d) => d.startsWith('D14') && d.includes('no cumplida: el lote g4-gateway-contrato no corrió')));

  const sinTipos = await correrFrente({ fase: 'disenar' }, frenteFalso({ sinTipos: true }).responder);
  assert('20.28 session without the preauditor and auditor agent types → generic agents with the same model',
    sinTipos.resultado.tareas.includes('G1: PASS') &&
    sinTipos.agentes.some((a) => a.label === 'auditoría G1 r1' && !a.agentType && a.model === 'opus'));

  // ── pr ──
  const ESTADO_FINAL = construir.guardado.estado;
  const prCon = (estado, head) => correrFrente({ fase: 'pr' }, frenteFalso({ estado, head }).responder);
  const prListo = await prCon(ESTADO_FINAL, ESTADO_FINAL.shaFinal);
  const agentePr = prListo.agentes.find((a) => a.label === 'PR');
  assert('20.29 pr with everything in PASS and HEAD at shaFinal → draft PR that lists the relaxed rules',
    prListo.resultado.pr === 'https://pr' && agentePr.prompt.includes('gh pr create --draft') &&
    agentePr.prompt.includes('Reglas del repo flexibilizadas') && agentePr.prompt.includes('--base ENTREGA_7'));
  const prMovido = await prCon(ESTADO_FINAL, 'd'.repeat(40));
  const prSinPass = await prCon(noCumpleD08.guardado.estado, noCumpleD08.guardado.estado.shaFinal);
  const prCierreRojo = await prCon(cierreRojo.guardado.estado, cierreRojo.guardado.estado.shaFinal);
  assert('20.30 pr with HEAD moved after the closing, a task without PASS, or the closing not in PASS → no push, no PR',
    prMovido.llamadas.length === 1 && /HEAD/.test(prMovido.resultado.detenida) &&
    prSinPass.llamadas.length === 1 && /sin PASS/.test(prSinPass.resultado.detenida) &&
    prCierreRojo.llamadas.length === 1 && /cierre no está en PASS/.test(prCierreRojo.resultado.detenida));
  const prAbreviado = await prCon(ESTADO_FINAL, ESTADO_FINAL.shaFinal.slice(0, 12));
  const disenoAbreviado = await construirCon({}, { disenoAprobado: SHA_A.slice(0, 10) });
  assert('20.31 an abbreviated SHA still matches: the PR opens and the design is not audited again',
    prAbreviado.resultado.pr === 'https://pr' && !disenoAbreviado.llamadas.includes('auditoría G1 r1'));

  resumen();
})().catch((e) => { console.error(e); failed++; resumen(); });

// ─── Summary ──────────────────────────────────────────────────────────────────

function resumen() {
  console.log('\n' + '═'.repeat(56));
  console.log(`PASS: ${passed}  │  FAIL: ${failed}`);
  console.log('═'.repeat(56));
  if (failed > 0) { console.error('\nTest suite FAILED'); process.exit(1); }
  else { console.log('\nTest suite PASSED'); process.exit(0); }
}

