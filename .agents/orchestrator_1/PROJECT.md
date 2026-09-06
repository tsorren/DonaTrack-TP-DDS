# Project: DonaTrack Markdown Adversarial Audit

## Architecture
- **Tech Stack**: Java 21 / Spring Boot 3 Maven Multi-Module Reactor.
- **Active Modules**: `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`.
- **Target Documentation**: Exactly 173 Markdown files (~1.76 MB, 29,189 lines) partitioned across 4 subdomains.
- **Verification Engine**: `grepai` MCP tools (`grepai_search`, `grepai_index_status`) semantic search against real Java source code.
- **Verification Scripts**:
  - `python scripts/validate_docs_links.py` (link integrity)
  - `node scripts/validate-contracts.js` (contract & schema integrity)
  - `node scripts/agent-check.js` & `node scripts/tests/run-tests.js` (governance integrity)
  - `mvn spotless:check` (code formatting integrity)

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | F1: Baseline Health Survey | Verify baseline scripts, count & syntax scan of 173 MD files, test grepai MCP | M1 | Survey |
| 2 | F2: Subdomain 1 Audit & Sync | Core Architecture & Shared Kernel (17 files): reconcile endpoints in `contratos-rest.md` against real Java `@RestController`s, fix D1-D5 discrepancies | M2 | R1.1, R2 |
| 3 | F3: Subdomain 2 Audit & Sync | Design, Wave Logs, CI/CD, DevOps & Testing (33 files): verify design docs against Java classes, CI/CD workflows, testing harnesses | M3 | R1.2, R2 |
| 4 | F4: Subdomain 3 Audit & Sync | Architecture Decisions / ADRs (91 files): verify ADR status vs implementation, relative links to root AGENTS.md, preserve historical immutability | M4 | R1.3, R3 |
| 5 | F5: Subdomain 4 Audit & Sync | AI Guides, Prompts, Evals, Governance (32 files): verify evaluator docs, prompt rules, checklists, governance alignment | M5 | R1.4 |
| 6 | F6: Global Verification & Gate | Execute all acceptance scripts (`validate_docs_links.py`, `validate-contracts.js`, `agent-check.js`, `run-tests.js`, `mvn spotless:check`), verify zero discrepancies | M6 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Baseline & Discovery | Survey scripts, inventory all 173 MD files, test grepai MCP | none | DONE |
| 2 | M2: Subdomain 1 (Core Architecture) | Audit 17 files (`docs/arquitectura/*`, `common-lib/AGENTS.md`, root `AGENTS.md`, `Readme.md`), reconcile REST endpoints & AMQP contracts | M1 | IN_PROGRESS |
| 3 | M3: Subdomain 2 (Design & DevOps) | Audit 33 files (`docs/arquitectura/diseno/*`, `docs/auditoria/*`, `docs/cicd/*`, `docs/testing/*`, `.github/*`, etc.) | M1 | PLANNED |
| 4 | M4: Subdomain 3 (ADRs) | Audit 91 files (`docs/adr/*` global and per-service), verify immutability, fix relative paths | M1 | PLANNED |
| 5 | M5: Subdomain 4 (AI Governance) | Audit 32 files (`docs/IA/*`), verify evaluator policies, review contracts, checklists | M1 | PLANNED |
| 6 | M6: Global Acceptance & Gate | Execute full suite of verification scripts, adversarial checks, auditor sign-off | M2, M3, M4, M5 | PLANNED |

## Interface Contracts & Governance Rules
- **Epistemic Taxonomy**: Mandatory classification in all reports: `[OBSERVED]`, `[DOCUMENTED]`, `[INFERRED]`, `[PROPOSED]`, `[REJECTED]`, `[VERIFIED]`.
- **Historical Immutability**: Historical cátedra records and approved ADRs are immutable. Only fix broken hyperlinks, relative paths, and syntax errors.
- **REST Discrepancy Zero-Tolerance**: Any endpoint described in Markdown documentation MUST exactly reflect Java `@RestController` / `@RequestMapping` methods, paths, and status codes.
- **Grepai Semantic Search Requirement**: All validations of entities, VOs, interfaces, enums, REST endpoints, and AMQP keys MUST be verified via `grepai` MCP tools against actual Java source code.
