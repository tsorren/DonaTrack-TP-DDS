# Dispatch: Survey Explorer 2 (Markdown Inventory & Structural Syntax)

**Objective**:
Map and inventory all Markdown files in the repository (targeted count ~173), partition them into the 4 subdomains, and check structural syntax (unclosed code blocks, malformed headers, relative links).

**Scope & Responsibilities**:
1. Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
2. Find and inventory all `.md` files in the repository (excluding `.agents/` and external/git ignored files).
3. Partition them into the 4 subdomains:
   - Subdomain 1: Core Architecture & Shared Kernel (`docs/arquitectura/*`, `common-lib/AGENTS.md`, root `AGENTS.md`, `Readme.md` / `README.md`, etc.)
   - Subdomain 2: Design, Wave Logs, CI/CD, DevOps, Testing (`docs/arquitectura/diseno/*`, `docs/auditoria/*`, `docs/cicd/*`, `docs/testing/*`, `.github/scripts/*`)
   - Subdomain 3: Architecture Decisions (ADRs) (`docs/adr/*` and microservice ADRs in subdirectories)
   - Subdomain 4: AI Guides, Prompts, Evals, Governance (`docs/IA/*`)
4. Detect any unclosed triple-backtick blocks (```) or malformed Markdown headers across all Markdown files.
5. Check relative paths from ADRs to `AGENTS.md`.
6. Write findings to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_2\handoff.md`.
7. Notify orchestrator via `send_message` when complete.

## 2026-09-06T05:12:18Z
Received dispatch from parent orchestrator:
Execute markdown file inventory targeting ~173 files, partition into 4 subdomains, scan for unclosed code blocks and malformed headers, check relative links from ADRs to AGENTS.md, produce handoff.md.

