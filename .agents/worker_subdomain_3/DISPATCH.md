# Dispatch: Worker Subdomain 3 (Architecture Decisions / ADRs)

## 2026-09-06T05:19:46Z

**Objective**:
Execute an adversarial audit of all 91 ADR files (`docs/adr/*` and microservice ADR subdirectories), verifying their assertions against Java source code via `grepai`, ensuring strict compliance with AGENTS.md §2, §9, and §13 (historical immutability of approved ADRs; only fix broken links, relative paths, and syntax errors).

**Owned Files (Exclusive Write Ownership)**:
- `docs/adr/*.md` (25 global ADRs and index files, e.g. `README.md`, `DEUDA_TECNICA.md`, etc.)
- `docs/adr/donaciones-service/*.md` (31 ADR files)
- `docs/adr/incentivos-service/*.md` (10 ADR files)
- `docs/adr/logistica-service/*.md` (9 ADR files)
- `docs/adr/notificaciones-service/*.md` (16 ADR files)

**Mandatory Inputs**:
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\orchestrator_1\PROJECT.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_2\handoff.md`.

**Tasks & Acceptance Requirements**:
1. Strictly uphold AGENTS.md §2 and §9: Approved ADRs and cátedra decisions are IMMUTABLE. Do NOT edit approved architectural decisions or rationale to match code. ONLY fix broken hyperlinks, relative paths (e.g. to `../../AGENTS.md`), and Markdown rendering/syntax errors.
2. Use `grepai` MCP tools (`call_mcp_tool` with ServerName: `grepai`, ToolName: `grepai_search`) to verify ADR assertions against real Java code and catalog any implementation drift as `[OBSERVED]` vs `[DOCUMENTED]` in `docs/adr/DEUDA_TECNICA.md` or your handoff report.
3. Verify that all ADR links to root `AGENTS.md` and between ADRs resolve with valid relative paths.
4. Run:
   - `node scripts/agent-check.js` (must PASS all ADR status and integrity checks).
   - `python scripts/validate_docs_links.py` (0 broken links).
5. Maintain epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
6. Write complete handoff report to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_3\handoff.md`.

DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
