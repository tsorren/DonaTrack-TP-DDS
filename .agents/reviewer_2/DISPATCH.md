# Dispatch: Reviewer 2 (Contracts, Code-Doc Consistency & Governance Rules)

**Objective**:
Independently review REST/AMQP contracts, code-documentation consistency against Java `@RestController`s, and governance compliance.

**Mandatory Inputs**:
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\orchestrator_1\PROJECT.md`.
- Review reports from `worker_subdomain_1`, `worker_subdomain_2`, `worker_subdomain_3`, and `worker_subdomain_4`.

**Tasks & Verification Checks**:
1. Run `node scripts/validate-contracts.js` and verify 79/79 PASS, 0 FAIL.
2. Run `node scripts/agent-check.js` and `node scripts/tests/run-tests.js` (86/86 PASS).
3. Run `mvn spotless:check` (BUILD SUCCESS across all 7 projects).
4. Verify zero discrepancy between Java `@RestController` / `@RequestMapping` mappings and Markdown documentation (`docs/arquitectura/contratos-rest.md` and `docs/arquitectura/diseno/auditoria-final-proyecto.md`).
5. Issue an explicit verdict: `APPROVE` or `REQUEST_CHANGES`.
6. Write complete report to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\reviewer_2\handoff.md`.
