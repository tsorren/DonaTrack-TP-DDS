# Dispatch: Worker Subdomain 1 (Core Architecture & Shared Kernel)

**Objective**:
Execute an adversarial audit and reconciliation of Subdomain 1 documents against Java source code using `grepai` semantic search.

**Owned Files (Exclusive Write Ownership)**:
- `docs/arquitectura/contratos-rest.md`
- `docs/arquitectura/contratos-amqp.md`
- `docs/arquitectura/modelo-de-dominio.md`
- `docs/arquitectura/logging-trazabilidad.md`
- `docs/arquitectura/seguridad-auditoria.md`
- `docs/arquitectura/principios-diseno-arquitectura.md`
- `docs/arquitectura/gestion-errores.md`
- `docs/arquitectura/perfiles-configuracion.md`
- `docs/arquitectura/politica-testing.md`
- `docs/arquitectura/tecnologias-y-frameworks.md`
- `docs/arquitectura/README.md`
- `docs/arquitectura/contratos/openapi-donaciones.yaml` (and other OpenAPI specs in that dir if endpoint reconciliation touches them)
- `common-lib/AGENTS.md`
- `AGENTS.md` (read-only governance, only touch if authorized)
- `Readme.md` / `README.md`
- `docs/README.md`
- `docs/ESTADO_DOCUMENTACION.md`
- `docs/context-index.md`

**Mandatory Inputs**:
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3\handoff.md` (contains the exact 23 @RestController mappings, 4 @RabbitListener bindings, and D1-D5 divergences).
- Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\orchestrator_1\PROJECT.md`.

**Tasks & Acceptance Requirements**:
1. Use `grepai` MCP tools (`call_mcp_tool` with ServerName: `grepai`, ToolName: `grepai_search`) to verify domain entities, Value Objects, Spring Boot controllers, interfaces, and AMQP keys against Java code.
2. Reconcile endpoint discrepancies in `docs/arquitectura/contratos-rest.md` and `docs/arquitectura/contratos/openapi-donaciones.yaml`:
   - D1: Reconcile `/api/items-normalizados` to match Java `ItemDonacionNormalizadoController.java`:
     - `GET /api/items-normalizados/pendientes`
     - `GET /api/items-normalizados/{id}`
     - `PATCH /api/items-normalizados/{id}`
   - D2: Add missing endpoints for Subcategorías aliases (`POST /api/subcategorias/{id}/aliases`, `DELETE /api/subcategorias/{id}/aliases/{alias}`) matching `SubcategoriasController.java`.
   - D3: Reconcile `GET /api/personas/{id}` in `donaciones-service` (in Java code only `POST /`, `GET /` with filter, `PUT /{id}`, `DELETE /{id}` exist; `GET /{id}` is in notificaciones-service).
   - D4: Reconcile HTTP status codes for `DELETE /api/categorias/{id}` and `DELETE /api/subcategorias/{id}` (200 OK with body in Java).
   - D5: Fix path parameter naming `{nombreInsignia}` in Insignias.
3. Validate:
   - `node scripts/validate-contracts.js` must yield 79/79 PASS, 0 FAIL.
   - `python scripts/validate_docs_links.py` must yield 0 broken links.
   - `mvn spotless:check` must pass.
4. Maintain epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
5. Write complete handoff report to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_1\handoff.md`.


## 2026-09-06T05:20:00Z
You are worker_subdomain_1.
Your working directory is c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_1.
Your parent is edbee326-cd86-464a-8638-feb6a5a74249.

MANDATORY INPUTS:
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md.
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_1\DISPATCH.md.
- Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3\handoff.md.

YOUR OWNED FILES (Exclusive Write Ownership):
- docs/arquitectura/*.md (including contratos-rest.md, contratos-amqp.md, modelo-de-dominio.md, etc.)
- docs/arquitectura/contratos/openapi-donaciones.yaml (and any other openapi specs in that dir if relevant)
- common-lib/AGENTS.md, Readme.md, docs/README.md, docs/ESTADO_DOCUMENTACION.md, docs/context-index.md

TASKS:
1. Use grepai MCP tools (`call_mcp_tool` with ServerName: "grepai", ToolName: "grepai_search") against Java source code to verify entities, VOs, interfaces, enums, REST endpoints, and AMQP keys.
2. Reconcile the 5 documented REST discrepancies from survey_explorer_3:
   - D1: Reconcile `/api/items-normalizados` to match Java `ItemDonacionNormalizadoController.java`:
     * `GET /api/items-normalizados/pendientes`
     * `GET /api/items-normalizados/{id}`
     * `PATCH /api/items-normalizados/{id}`
   - D2: Add missing endpoints for Subcategorías aliases (`POST /api/subcategorias/{id}/aliases`, `DELETE /api/subcategorias/{id}/aliases/{alias}`) matching `SubcategoriasController.java`.
   - D3: Reconcile `PersonasController` (`donaciones`): ensure documentation accurately reflects that `GET /api/personas/{id}` is not an individual endpoint, or document the actual behavior.
   - D4: Reconcile HTTP status codes for `DELETE /api/categorias/{id}` and `DELETE /api/subcategorias/{id}` (200 OK with body in Java).
   - D5: Fix path parameter naming `{nombreInsignia}` in Insignias.
3. Validate:
   - `node scripts/validate-contracts.js` -> 79/79 PASS, 0 FAIL.
   - `python scripts/validate_docs_links.py` -> 0 broken links.
   - `mvn spotless:check` -> BUILD SUCCESS.
4. Maintain epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
5. Write complete handoff report to c:\IdeaProjects\DonaTrack-TP-DDS\.agents\worker_subdomain_1\handoff.md.
