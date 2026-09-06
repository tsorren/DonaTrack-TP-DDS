# Dispatch: Challenger 2 (Adversarial Contract, Endpoint & Codebase Alignment)

## 2026-09-06T05:31:27Z

**Objective**:
Empirically and adversarially challenge all REST and AMQP contract assertions against actual Spring Boot 3 Java code using `grepai` MCP tools.

**Mandatory Inputs**:
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\orchestrator_1\PROJECT.md`.

**Tasks & Verification Checks**:
1. Adversarially verify endpoints documented in `docs/arquitectura/contratos-rest.md`, `docs/arquitectura/contratos/openapi-*.yaml`, and `docs/arquitectura/diseno/auditoria-final-proyecto.md`:
   - Search with `grepai_search` MCP tools against Java source code.
   - Verify that all `@RestController` and `@RequestMapping` endpoints in Java are accounted for and that NO phantom endpoints exist in the documentation.
   - Verify HTTP status codes, request bodies, and path variables.
2. Verify all `@RabbitListener` queues, exchanges, and routing keys against `RabbitMQConfig.java` and `LogisticaEventListener.java`.
3. Run `node scripts/validate-contracts.js` (must pass 79/79).
4. Run `mvn spotless:check` (must pass).
5. Report any remaining discrepancies or vulnerabilities.
6. Issue an explicit verdict: `APPROVE` or `REQUEST_CHANGES`.
7. Write complete report to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\challenger_2\handoff.md`.
