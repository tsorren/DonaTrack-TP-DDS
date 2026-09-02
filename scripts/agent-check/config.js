'use strict';

// ─── Shared configuration for all governance checks ───────────────────────────
// Extend constants here; keep check logic in checks/.

const SKIP_DIRS = new Set(['.git', 'node_modules', 'target', '.mvn', '.idea', 'dist', 'build']);

// Root-relative paths where AGENTS.md is canonical or explicitly allowed.
// Wave 8 extends this list when nested AGENTS are introduced.
const AGENTS_ALLOWLIST = new Set(['AGENTS.md']);

// Path prefixes (forward-slash, relative to repo root) treated as historical archives.
const HISTORY_PREFIXES = ['docs/IA/history'];

// Terms whose presence in any active document is a regression (removed in Wave 6).
const STALE_TERMS = [
  'invoke_subagent',
  '[DEFERRED_WAVE_5]',
  '[DEFERRED_WAVE_6]',
  'Fallback Monoproceso',
];

// Active documents excluded from stale-terms scan (audit logs referencing removed terms).
const STALE_TERMS_EXCLUSIONS = new Set([
  'docs/ESTADO_DOCUMENTACION.md',
]);

// Canonical harness documents whose internal Markdown links are verified (Wave 7B).
const INTERNAL_LINK_SCOPE = [
  'AGENTS.md',
  'docs/context-index.md',
  'docs/README.md',
  'docs/IA/README.md',
  'docs/IA/review/evaluator.md',
  'docs/IA/04-checklist-antes-de-pr.md',
  'docs/adr/README.md',
  'docs/adr/DEUDA_TECNICA.md',
];

// Valid values for ADR Status field and DTI Decision status.
const VALID_ADR_STATUSES = new Set(['proposed', 'accepted', 'rejected', 'superseded']);

// Maven modules excluded from MODULE_WITHOUT_ROUTING check.
// integration-tests: CI infrastructure; not a bounded context that needs Service Context.
const ROUTING_MODULE_EXCLUSIONS = new Set(['integration-tests']);

// Maven modules excluded from temporal drift scanning.
// integration-tests: test infrastructure, no production domain code.
const TEMPORAL_DRIFT_SKIP_MODULES = new Set(['integration-tests']);

module.exports = {
  SKIP_DIRS,
  AGENTS_ALLOWLIST,
  HISTORY_PREFIXES,
  STALE_TERMS,
  STALE_TERMS_EXCLUSIONS,
  INTERNAL_LINK_SCOPE,
  VALID_ADR_STATUSES,
  ROUTING_MODULE_EXCLUSIONS,
  TEMPORAL_DRIFT_SKIP_MODULES,
};
