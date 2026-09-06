# Dispatch: Worker Subdomain 4 (AI Guides, Prompts, Evals, Governance)

**Objective**:
Execute an adversarial audit of all 32 AI documentation files in `docs/IA/**` against repository rules and governance scripts.

**Owned Files (Exclusive Write Ownership)**:
- `docs/IA/*.md` (8 files)
- `docs/IA/prompts/*.md` (3 files)
- `docs/IA/evals/**` (17 files)
- `docs/IA/history/*` (3 files)
- `docs/IA/review/*` (1 file, e.g. `evaluator.md`)

**Mandatory Inputs**:
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\orchestrator_1\PROJECT.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_2\handoff.md`.

**Tasks & Acceptance Requirements**:
1. Audit all files in `docs/IA/` for consistency with `AGENTS.md` (evaluator policy, review contracts, SonarCloud preflight checklist, prompt rules).
2. Use `grepai` MCP tools (`call_mcp_tool` with ServerName: `grepai`, ToolName: `grepai_search`) to verify that any code snippets or references in `docs/IA/` match real Java code or configurations.
3. Verify that `docs/IA/review/evaluator.md` matches `AGENTS.md §7.4` and that no broken links or stale terms exist.
4. Run:
   - `node scripts/agent-check.js` (must PASS all checks).
   - `node scripts/tests/run-tests.js` (86/86 PASS).
   - `python scripts/validate_docs_links.py` (0 broken links).
5. Maintain epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
6. Write complete handoff report to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_4\handoff.md`.

DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## 2026-09-06T05:19:46Z
Received user prompt:
You are worker_subdomain_4.
Your working directory is c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_4.
Your parent is edbee326-cd86-464a-8638-feb6a5a74249.

MANDATORY INPUTS:
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md.
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_4\DISPATCH.md.
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_2\handoff.md.

YOUR OWNED FILES (Exclusive Write Ownership):
- docs/IA/*.md (8 files)
- docs/IA/prompts/*.md (3 files)
- docs/IA/evals/** (17 files)
- docs/IA/history/* (3 files)
- docs/IA/review/* (1 file)

TASKS:
1. Audit all files in `docs/IA/` for consistency with `AGENTS.md` (evaluator policy, review contracts, SonarCloud preflight checklist, prompt rules).
2. Use grepai MCP tools (`call_mcp_tool` with ServerName: "grepai", ToolName: "grepai_search") to verify that any code snippets or references in `docs/IA/` match real Java code or configurations.
3. Verify that `docs/IA/review/evaluator.md` matches `AGENTS.md §7.4` and that no broken links or stale terms exist.
4. Run:
   - `node scripts/agent-check.js` (must PASS all checks).
   - `node scripts/tests/run-tests.js` (86/86 PASS).
   - `python scripts/validate_docs_links.py` (0 broken links).
5. Maintain epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
6. Write complete handoff report to c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_4\handoff.md.
