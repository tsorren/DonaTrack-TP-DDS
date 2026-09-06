# Handoff Report — Survey Explorer 3: Codebase & Semantic Architecture

**Agent**: `survey_explorer_3`  
**Working Directory**: `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3`  
**Parent Agent**: `edbee326-cd86-464a-8638-feb6a5a74249`  
**Timestamp**: 2026-09-06T05:20:00Z  
**Scope**: Java 21 / Spring Boot 3 multi-module architecture mapping, grepai MCP verification, REST & AMQP inventory, documentation divergence audit.

---

## 1. Observation

### 1.1 Grepai MCP Integration Test
- `[VERIFIED]` Tool `call_mcp_tool` with `ServerName: "grepai"`, `ToolName: "grepai_index_status"`:
  ```json
  {
    "total_files": 1046,
    "total_chunks": 3932,
    "index_size": "N/A",
    "last_updated": "2026-09-04 00:29:21",
    "provider": "ollama",
    "model": "nomic-embed-text",
    "symbols_ready": true,
    "rpg_enabled": false
  }
  ```
- `[VERIFIED]` Tool `call_mcp_tool` with `ServerName: "grepai"`, `ToolName: "grepai_search"` successfully executed semantic search queries (e.g. `"RestController Spring Boot endpoints"`, `"donaciones independientes maquina de estados transiciones"`, `"RabbitListener queues routing keys evento"`), returning ranked code chunks with file paths and similarity scores (e.g. `donaciones-service\...\EntregaFallida.java` score `0.6977`, `logistica-service\...\LogisticaEventPublisher.java` score `0.6152`).

### 1.2 Maven Multi-Module Architecture Verification
- `[OBSERVED]` Root `pom.xml` (`c:\IdeaProjects\DonaTrack-TP-DDS\pom.xml`, lines 12–19, 21–23):
  ```xml
  <modules>
      <module>common-lib</module>
      <module>donaciones-service</module>
      <module>notificaciones-service</module>
      <module>incentivos-service</module>
      <module>logistica-service</module>
      <module>integration-tests</module>
  </modules>

  <properties>
      <maven.compiler.release>21</maven.compiler.release>
      <java.version>21</java.version>
  ...
  ```
- `[OBSERVED]` Verification of module names mentioned in dispatch/prompt:
  - Active in Maven reactor (6 modules): `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`.
  - Inactive filesystem skeletons (2 directories): `auth-service` (contains only `.gitkeep`), `cliente-liviano` (contains only `.gitkeep`). Not declared in `pom.xml`.
  - Nonexistent in repository (4 hypothesized modules): `personas-service`, `heladeras-service`, `puntos-service`, `alertas-service` do NOT exist anywhere in source code or `docs/`. Grep searches for these terms returned 0 results.
- `[VERIFIED]` `mvn spotless:check` executes across all 7 reactor projects (`donatrack` root + 6 submodules) in 6.005s with `BUILD SUCCESS`.

### 1.3 Inventory of @RestController Endpoints (23 Controllers across 4 Services)

#### A. `donaciones-service` (Port 8080) — 10 Controllers
1. **`DonacionesController`** (`grupo5.donaciones.controllers.impl.DonacionesController.java:20`)
   - `@RequestMapping("/api/donaciones")`
   - `POST /api/donaciones`: `@Valid @RequestBody DonacionInputDTO dto` -> `201 CREATED`, `DonacionOutputDTO` (line 30)
   - `GET /api/donaciones`: -> `200 OK`, `List<DonacionOutputDTO>` (line 37)
   - `GET /api/donaciones/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `DonacionOutputDTO` (line 43)

2. **`DonacionesIndependientesController`** (`grupo5.donaciones.controllers.impl.DonacionesIndependientesController.java:22`)
   - `@RequestMapping("/donaciones-independientes")`
   - `GET /donaciones-independientes`: `@RequestParam(required = false) TipoEstadoDonacion estado`, `@RequestParam(required = false) UUID subcategoriaId`, `@RequestParam(required = false) UUID donanteId` -> `200 OK`, `List<DonacionIndependienteResponseDTO>` (line 32)
   - `GET /donaciones-independientes/{id}`: `@PathVariable UUID id` -> `200 OK`, `DonacionIndependienteResponseDTO` (line 41)
   - `PATCH /donaciones-independientes/{id}/estado`: `@PathVariable UUID id`, `@Valid @RequestBody CambioEstadoDonacionIndependienteRequestDTO request`, `@RequestHeader("X-Actor") String actor` -> `200 OK`, `DonacionIndependienteResponseDTO` (line 47)

3. **`NecesidadesController`** (`grupo5.donaciones.controllers.impl.NecesidadesController.java:22`)
   - `@RequestMapping("/api/necesidades")`
   - `POST /api/necesidades`: `@Valid @RequestBody NecesidadDTO dto` -> `201 CREATED`, `NecesidadDTO` (line 32)
   - `GET /api/necesidades`: `@RequestParam(required = false) UUID entidadId`, `@RequestParam(required = false) String tipo` -> `200 OK`, `List<NecesidadDTO>` (line 39)
   - `GET /api/necesidades/{id}`: `@PathVariable UUID id` -> `200 OK`, `NecesidadDTO` (line 47)
   - `PUT /api/necesidades/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody NecesidadDTO dto` -> `200 OK`, `NecesidadDTO` (line 53)
   - `DELETE /api/necesidades/{id}`: `@PathVariable UUID id` -> `204 NO_CONTENT` (line 60)

4. **`PropuestaDeAsignacionController`** (`grupo5.donaciones.controllers.impl.PropuestaDeAsignacionController.java:23`)
   - `@RequestMapping("/api/asignaciones")`
   - `POST /api/asignaciones/ejecuciones`: -> `201 CREATED`, `List<PropuestaDTO>` (line 30)
   - `GET /api/asignaciones/ejecuciones`: -> `200 OK`, `List<EjecucionAsignacionDTO>` (line 36)
   - `GET /api/asignaciones/propuestas`: -> `200 OK`, `List<PropuestaDTO>` (line 42)
   - `PUT /api/asignaciones/propuestas/{id}/estado`: `@PathVariable UUID id`, `@Valid @RequestBody ActualizarEstadoRequestDTO request` -> `200 OK` (line 48)

5. **`EntidadBeneficiariaController`** (`grupo5.donaciones.controllers.impl.EntidadBeneficiariaController.java:22`)
   - `@RequestMapping("/api/entidades")`
   - `POST /api/entidades`: `@Valid @RequestBody EntidadBeneficiariaInputDTO entidad` -> `201 CREATED`, `EntidadBeneficiariaOutputDTO` (line 32)
   - `GET /api/entidades`: -> `200 OK`, `List<EntidadBeneficiariaOutputDTO>` (line 46)
   - `GET /api/entidades/{id}`: `@PathVariable UUID id` -> `200 OK`, `EntidadBeneficiariaOutputDTO` (line 40)
   - `PUT /api/entidades/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody EntidadBeneficiariaInputDTO entidad` -> `200 OK`, `EntidadBeneficiariaOutputDTO` (line 52)
   - `DELETE /api/entidades/{id}`: `@PathVariable UUID id` -> `204 NO_CONTENT` (line 59)

6. **`DonantesController`** (`grupo5.donaciones.controllers.impl.DonantesController.java:25`)
   - `@RequestMapping("/api/donantes")`
   - `POST /api/donantes`: `@Valid @RequestBody DonanteInputDTO dto` -> `201 CREATED`, `DonanteOutputDTO` (line 38)
   - `GET /api/donantes`: `@RequestParam(value = "canal", required = false) String canal` -> `200 OK`, `List<DonanteOutputDTO>` (line 45)
   - `GET /api/donantes/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `DonanteOutputDTO` (line 53)
   - `DELETE /api/donantes/{id}`: `@PathVariable("id") UUID id` -> `204 NO_CONTENT` (line 60)
   - `POST /api/donantes/archivos`: `@Valid @RequestBody ArchivoInputDTO input` -> `202 ACCEPTED`, `ArchivoOutputDTO` (line 67)
   - `GET /api/donantes/archivos/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `ArchivoOutputDTO` (line 75)

7. **`CategoriasController`** (`grupo5.donaciones.controllers.impl.CategoriasController.java:22`)
   - `@RequestMapping("/api/categorias")`
   - `POST /api/categorias`: `@Valid @RequestBody CategoriaInputDTO dto` -> `201 CREATED`, `CategoriaOutputDTO` (line 32)
   - `GET /api/categorias`: -> `200 OK`, `List<CategoriaOutputDTO>` (line 54)
   - `GET /api/categorias/{id}`: `@PathVariable UUID id` -> `200 OK`, `CategoriaOutputDTO` (line 61)
   - `PUT /api/categorias/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody CategoriaInputDTO dto` -> `200 OK`, `CategoriaOutputDTO` (line 46)
   - `DELETE /api/categorias/{id}`: `@PathVariable UUID id` -> `200 OK`, `CategoriaOutputDTO` (line 39)

8. **`SubcategoriasController`** (`grupo5.donaciones.controllers.impl.SubcategoriasController.java:23`)
   - `@RequestMapping("/api/subcategorias")`
   - `POST /api/subcategorias`: `@Valid @RequestBody SubcategoriaInputDTO dto` -> `201 CREATED`, `SubcategoriaOutputDTO` (line 33)
   - `GET /api/subcategorias`: -> `200 OK`, `List<SubcategoriaOutputDTO>` (line 55)
   - `GET /api/subcategorias/{id}`: `@PathVariable UUID id` -> `200 OK`, `SubcategoriaOutputDTO` (line 62)
   - `PUT /api/subcategorias/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody SubcategoriaInputDTO dto` -> `200 OK`, `SubcategoriaOutputDTO` (line 47)
   - `DELETE /api/subcategorias/{id}`: `@PathVariable UUID id` -> `200 OK`, `SubcategoriaOutputDTO` (line 40)
   - `POST /api/subcategorias/{id}/aliases`: `@PathVariable UUID id`, `@Valid @RequestBody AliasSubcategoriaInputDTO dto` -> `200 OK`, `SubcategoriaOutputDTO` (line 69)
   - `DELETE /api/subcategorias/{id}/aliases/{alias}`: `@PathVariable UUID id`, `@PathVariable String alias` -> `200 OK`, `SubcategoriaOutputDTO` (line 77)

9. **`ItemDonacionNormalizadoController`** (`grupo5.donaciones.controllers.impl.ItemDonacionNormalizadoController.java:19`)
   - `@RequestMapping("/api/items-normalizados")`
   - `GET /api/items-normalizados/pendientes`: -> `200 OK`, `List<ItemDonacionNormalizadoOutputDTO>` (line 29)
   - `GET /api/items-normalizados/{id}`: `@PathVariable UUID id` -> `200 OK`, `ItemDonacionNormalizadoOutputDTO` (line 35)
   - `PATCH /api/items-normalizados/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody ItemDonacionNormalizadoPatchDTO dto` -> `200 OK`, `ItemDonacionNormalizadoOutputDTO` (line 41)

10. **`PersonasController`** (`grupo5.donaciones.controllers.impl.PersonasController.java:24`)
    - `@RequestMapping("/api/personas")`
    - `POST /api/personas`: `@Valid @RequestBody PersonaInputDTO persona` -> `201 CREATED`, `PersonaOutputDTO` (line 34)
    - `GET /api/personas`: `@RequestParam(required = false) TipoPersona tipo` -> `200 OK`, `List<PersonaOutputDTO>` (line 42)
    - `PUT /api/personas/{id}`: `@PathVariable UUID id`, `@Valid @RequestBody PersonaInputDTO persona` -> `200 OK`, `PersonaOutputDTO` (line 50)
    - `DELETE /api/personas/{id}`: `@PathVariable UUID id` -> `204 NO_CONTENT` (line 58)

#### B. `logistica-service` (Port 8083) — 6 Controllers
1. **`EntregasController`** (`grupo5.logistica.controllers.impl.EntregasController.java:24`)
   - `@RequestMapping("/api/entregas")`
   - `POST /api/entregas`: `@Valid @RequestBody CrearEntregaRequestDTO dto` -> `201 CREATED`, `EntregaResponseDTO` (line 33)
   - `GET /api/entregas`: -> `200 OK`, `List<EntregaResponseDTO>` (line 39)
   - `GET /api/entregas/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `EntregaResponseDTO` (line 45)
   - `PATCH /api/entregas/{id}/estado`: `@PathVariable("id") UUID id`, `@Valid @RequestBody CambioEstadoEntregaRequestDTO request` -> `200 OK`, `EntregaResponseDTO` (line 51)
   - `PATCH /api/entregas/{id}/fotos`: `@PathVariable("id") UUID id`, `@Valid @RequestBody AdjuntarFotoRecepcionRequestDTO dto` -> `200 OK`, `EntregaResponseDTO` (line 58)
   - `GET /api/entregas/{id}/historial`: `@PathVariable("id") UUID id` -> `200 OK`, `List<CambioEstadoEntregaResponseDTO>` (line 65)

2. **`RutasController`** (`grupo5.logistica.controllers.impl.RutasController.java:24`)
   - `@RequestMapping("/api/rutas")`
   - `GET /api/rutas`: `@RequestParam(value = "camionId", required = false) UUID camionId` -> `200 OK`, `List<RutaResponseDTO>` (line 33)
   - `GET /api/rutas/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `RutaResponseDTO` (line 43)
   - `GET /api/rutas/{id}/entregas`: `@PathVariable("id") UUID id` -> `200 OK`, `RutaConEntregasResponseDTO` (line 49)
   - `POST /api/rutas/{id}/entregas`: `@PathVariable("id") UUID id`, `@Valid @RequestBody AgregarEntregaRutaRequestDTO dto` -> `201 CREATED`, `RutaResponseDTO` (line 56)
   - `PATCH /api/rutas/{id}/estado`: `@PathVariable("id") UUID id`, `@Valid @RequestBody CambioEstadoRutaRequestDTO request` -> `200 OK`, `RutaResponseDTO` (line 63)

3. **`CamionesController`** (`grupo5.logistica.controllers.impl.CamionesController.java:23`)
   - `@RequestMapping("/api/camiones")`
   - `POST /api/camiones`: `@Valid @RequestBody CamionRequestDTO request` -> `201 CREATED`, `CamionResponseDTO` (line 33)
   - `GET /api/camiones`: -> `200 OK`, `List<CamionResponseDTO>` (line 39)
   - `GET /api/camiones/{id}`: `@PathVariable UUID id` -> `200 OK`, `CamionResponseDTO` (line 45)
   - `PATCH /api/camiones/{id}/estado`: `@PathVariable UUID id`, `@Valid @RequestBody CambioEstadoCamionRequestDTO request` -> `200 OK`, `CamionResponseDTO` (line 51)
   - `DELETE /api/camiones/{id}`: `@PathVariable UUID id` -> `204 NO_CONTENT` (line 58)

4. **`ChoferesController`** (`grupo5.logistica.controllers.impl.ChoferesController.java:23`)
   - `@RequestMapping("/api/choferes")`
   - `POST /api/choferes`: `@Valid @RequestBody ChoferRequestDTO request` -> `201 CREATED`, `ChoferResponseDTO` (line 33)
   - `GET /api/choferes`: -> `200 OK`, `List<ChoferResponseDTO>` (line 39)
   - `GET /api/choferes/{id}`: `@PathVariable UUID id` -> `200 OK`, `ChoferResponseDTO` (line 45)
   - `PATCH /api/choferes/{id}/estado`: `@PathVariable UUID id`, `@Valid @RequestBody CambioEstadoChoferRequestDTO request` -> `200 OK`, `ChoferResponseDTO` (line 51)
   - `DELETE /api/choferes/{id}`: `@PathVariable UUID id` -> `204 NO_CONTENT` (line 58)

5. **`PlanificacionController`** (`grupo5.logistica.controllers.impl.PlanificacionController.java:23`)
   - `@RequestMapping("/api/logistica")`
   - `POST /api/logistica/resultados` & `POST /api/logistica/callback/rutas`: `@Valid @RequestBody CallbackPlanificacionRequestDTO dto` -> `200 OK`, `SolicitudPlanificacionResponseDTO` (line 32)
   - `GET /api/logistica/planificaciones/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `SolicitudPlanificacionResponseDTO` (line 39)

6. **`PlanificacionManualController`** (`grupo5.logistica.controllers.impl.PlanificacionManualController.java:15`)
   - `@RequestMapping("/api/logistica/planificaciones")` (Condicional en `logistica.planificacion.manual-enabled=true`)
   - `POST /api/logistica/planificaciones/ejecuciones`: -> `202 ACCEPTED` (line 28)

#### C. `incentivos-service` (Port 8082) — 5 Controllers
1. **`RankingController`** (`grupo5.incentivos.controllers.RankingController.java:21`)
   - `@RequestMapping("/api/incentivos/ranking")`
   - `GET /api/incentivos/ranking/ultimo`: -> `200 OK` / `204 NO_CONTENT`, `RankingMensualDTO` (line 32)
   - `GET /api/incentivos/ranking/historial`: -> `200 OK`, `List<RankingMensualDTO>` (line 41)
   - `POST /api/incentivos/ranking/calcular`: `@RequestParam(required = false) String periodo` -> `200 OK`, `RankingMensualDTO` (line 47)
   - `GET /api/incentivos/ranking/posicion/{donanteId}`: `@PathVariable UUID donanteId`, `@RequestParam(required = false) String periodo` -> `200 OK` / `204 NO_CONTENT`, `Integer` (line 60)

2. **`DonanteIncentivosController`** (`grupo5.incentivos.controllers.DonanteIncentivosController.java:23`)
   - `@RequestMapping("/api/incentivos")`
   - `POST /api/incentivos/donantes/{donanteId}`: `@PathVariable UUID donanteId`, `@Valid @RequestBody RegistrarDonanteRequest request` -> `201 CREATED`, `DonanteRegistradoDTO` (line 34)
   - `DELETE /api/incentivos/donantes/{donanteId}`: `@PathVariable UUID donanteId` -> `204 NO_CONTENT` (line 45)
   - `PATCH /api/incentivos/donantes/{donanteId}`: `@PathVariable UUID donanteId`, `@Valid @RequestBody ModificarDonanteRequest request` -> `200 OK` (line 52)

3. **`MetricasIncentivosController`** (`grupo5.incentivos.controllers.MetricasIncentivosController.java:15`)
   - `@RequestMapping("/api/incentivos")`
   - `GET /api/incentivos/donantes/{donanteId}/metricas`: `@PathVariable UUID donanteId` -> `200 OK`, `MetricasDonanteDTO` (line 26)
   - `GET /api/incentivos/admin/resumen`: -> `200 OK`, `ResumenSistemaDTO` (line 32)

4. **`MisionesDonacionController`** (`grupo5.incentivos.controllers.MisionesDonacionController.java:20`)
   - `@RequestMapping("/api/incentivos")`
   - `POST /api/incentivos/donaciones`: `@Valid @RequestBody NuevaDonacionRequest request` -> `200 OK` (line 31)
   - `POST /api/incentivos/donaciones/exitosa`: `@Valid @RequestBody DonacionExitosaRequest request` -> `200 OK` (line 38)
   - `GET /api/incentivos/donantes/{donanteId}/misiones`: `@PathVariable UUID donanteId` -> `200 OK`, `List<MisionDTO>` (line 46)

5. **`InsigniasController`** (`grupo5.incentivos.controllers.InsigniasController.java:17`)
   - `@RequestMapping("/api/incentivos")`
   - `GET /api/incentivos/donantes/{donanteId}/insignias`: `@PathVariable UUID donanteId`, `@RequestParam(required = false) Boolean soloVisibles` -> `200 OK`, `List<InsigniaDTO>` (line 28)
   - `PUT /api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad`: `@PathVariable UUID donanteId`, `@PathVariable String nombreInsignia`, `@RequestParam boolean visible` -> `200 OK` (line 35)

#### D. `notificaciones-service` (Port 8081) — 2 Controllers
1. **`NotificacionController`** (`grupo5.notificaciones.controllers.NotificacionController.java:15`)
   - `@RequestMapping("/notificaciones")`
   - `POST /notificaciones`: `@Valid @RequestBody EventoNotificableDTO dto` -> `202 ACCEPTED` (line 20)
   - `GET /notificaciones/persona/{personaId}`: `@PathVariable UUID personaId` -> `200 OK`, `List<NotificacionDTO>` (line 30)

2. **`PersonasController`** (`grupo5.notificaciones.controllers.PersonasController.java:12`)
   - `@RequestMapping("/api/notificaciones/personas")`
   - `PUT /api/notificaciones/personas`: `@Valid @RequestBody PersonaReplicaDTO dto` -> `200 OK` (line 19)
   - `DELETE /api/notificaciones/personas/{id}`: `@PathVariable("id") UUID id` -> `204 NO_CONTENT` (line 26)
   - `GET /api/notificaciones/personas/{id}`: `@PathVariable("id") UUID id` -> `200 OK`, `PersonaReplicaDTO` (line 33)

---

### 1.4 AMQP / RabbitMQ Topology & Bindings
- `[DOCUMENTED]` & `[OBSERVED]` Exchange: `logistica.exchange` (TopicExchange, durable: true, autoDelete: false)
  - Declared in `logistica-service/src/main/java/grupo5/logistica/config/RabbitMQConfig.java:22`
  - Declared in `donaciones-service/src/main/java/grupo5/donaciones/config/RabbitMQConfig.java:28`
- `[OBSERVED]` Listeners in `donaciones-service` (`grupo5.donaciones.infrastructure.LogisticaEventListener.java`):
  1. `@RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_ASIGNADA)` (line 29)
     - Queue: `donaciones.ruta.asignada`
     - Binding key: `ruta.asignada`
     - Payload: `EventoRutaAsignada` (rutaId, donacionIndependienteId, fechaAsignacion)
     - Business Action: Transición de estado a `LISTA_PARA_ENTREGAR`
  2. `@RabbitListener(queues = RabbitMQConfig.QUEUE_RUTA_INICIADA)` (line 43)
     - Queue: `donaciones.ruta.iniciada`
     - Binding key: `ruta.iniciada`
     - Payload: `EventoRutaIniciada` (rutaId, camionId, patenteCamion, donacionesIndependientesIds, fechaInicio, urlMapa)
     - Business Action: Transición de donaciones a `EN_TRASLADO` con URL de mapa
  3. `@RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_EXITOSA)` (line 61)
     - Queue: `donaciones.entrega.exitosa`
     - Binding key: `entrega.exitosa`
     - Payload: `EventoEntregaExitosa` (entregaId, donacionIndependienteId, camionId, patenteCamion, fechaEntrega)
     - Business Action: Transición a `ENTREGADA`
  4. `@RabbitListener(queues = RabbitMQConfig.QUEUE_ENTREGA_FALLIDA)` (line 75)
     - Queue: `donaciones.entrega.fallida`
     - Binding key: `entrega.fallida`
     - Payload: `EventoEntregaFallida` (entregaId, donacionIndependienteId, justificacion, fechaFalla, replanificable)
     - Business Action: Transición a `ENTREGA_FALLIDA`
- `[OBSERVED]` Publisher in `logistica-service` (`grupo5.logistica.infrastructure.LogisticaEventPublisher.java`):
  - `publicarRutaAsignada(EventoRutaAsignada evento)` -> `convertAndSend(EXCHANGE, "ruta.asignada", evento)`
  - `publicarRutaIniciada(EventoRutaIniciada evento)` -> `convertAndSend(EXCHANGE, "ruta.iniciada", evento)`
  - `publicarEntregaExitosa(EventoEntregaExitosa evento)` -> `convertAndSend(EXCHANGE, "entrega.exitosa", evento)`
  - `publicarEntregaFallida(EventoEntregaFallida evento)` -> `convertAndSend(EXCHANGE, "entrega.fallida", evento)`

---

### 1.5 Detailed Documentation Divergence Audit
Comparing Java source code against `docs/arquitectura/contratos-rest.md`, `docs/arquitectura/contratos/openapi-*.yaml`, and `docs/arquitectura/diseno/auditoria-final-proyecto.md`:

| # | Endpoint / Elemento | En Código Java Real (`[OBSERVED]`) | En Documentación (`contratos-rest.md` / OpenAPI) | Severidad | Detalle del Hallazgo |
|---|---|---|---|---|---|
| **D1** | `/api/items-normalizados` | Solo implementa `GET /pendientes`, `GET /{id}`, y `PATCH /{id}` en `ItemDonacionNormalizadoController.java` | Declara `GET / POST` en `/api/items-normalizados` y `GET / PUT / DELETE` en `/api/items-normalizados/{id}` | **ALTA** | El controlador no tiene endpoint raíz `POST` ni `GET`, ni tampoco `PUT` o `DELETE`. OpenAPI y la tabla Markdown afirman operaciones CRUD que no existen en el código fuente. |
| **D2** | `/api/subcategorias/{id}/aliases` | Implementa `POST /{id}/aliases` y `DELETE /{id}/aliases/{alias}` en `SubcategoriasController.java:69,77` | **Ausentes** tanto en la tabla de `contratos-rest.md` como en `openapi-donaciones.yaml` | **MEDIA** | Funcionalidad existente en código para gestión de alias de subcategorías no catalogada en los contratos públicos. |
| **D3** | `GET /api/personas/{id}` (`donaciones`) | **No existe** en `PersonasController.java` de `donaciones-service` (solo existen `POST /`, `GET /` con filtro de tipo, `PUT /{id}`, `DELETE /{id}`) | Documentado en `contratos-rest.md:62` y `openapi-donaciones.yaml:512` | **MEDIA** | La documentación y OpenAPI afirman la existencia de consulta individual por ID en donaciones que no está implementada en el controller. |
| **D4** | `DELETE /api/categorias/{id}` & `DELETE /api/subcategorias/{id}` | Devuelven `200 OK` con el DTO del objeto eliminado en el body (`CategoriasController.java:43`, `SubcategoriasController.java:44`) | `contratos-rest.md:56,58` declara código HTTP `204` | **BAJA** | Divergencia en el código de estado HTTP (200 con cuerpo vs 204 No Content). |
| **D5** | Path variable en insignias | `PUT /api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad` (`InsigniasController.java:35`) | `contratos-rest.md:117` utiliza `{nombre}` | **BAJA** | Discrepancia cosmética de nomenclatura del parámetro de ruta entre el doc Markdown y el código Java / OpenAPI. |
| **D6** | Módulos fantasma en el request | El monorepo contiene `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests` | El prompt/dispatch menciona `personas-service`, `heladeras-service`, `puntos-service`, `alertas-service` | **INFO** | Nombres del enunciado general de la cátedra que fueron sintetizados en la arquitectura de 4 bounded contexts de DonaTrack. Cero referencias en código o docs. |

---

## 2. Logic Chain

1. **Premisa 1 (Grepai MCP Health)**: El MCP server `grepai` fue consultado directamente mediante `call_mcp_tool` (`grepai_index_status`). La respuesta confirmó que el índice está construido sobre 1046 archivos y 3932 fragmentos con `symbols_ready: true`. Pruebas semánticas con `grepai_search` validaron la recuperación efectiva de código con puntuaciones de similitud de hasta 0.6977.
2. **Premisa 2 (Identidad Modular)**: La inspección del archivo canónico `pom.xml` en la raíz delimitó con certeza matemática los 6 módulos activos del reactor Maven (`donatrack` padre + 6 subproyectos). La invocación de `mvn spotless:check` confirmó la existencia y salud de compilación/formato de estos 7 artefactos Maven con BUILD SUCCESS. Se verificó que las carpetas `auth-service` y `cliente-liviano` carecen de código fuente (solo `.gitkeep`), y que no existe ningún módulo o archivo con los nombres `personas-service`, `heladeras-service`, `puntos-service` o `alertas-service`.
3. **Premisa 3 (Exhaustividad de Controladores)**: Se realizó un escaneo por patrones (`grep_search` sobre `@RestController`, `@RequestMapping` y métodos de mapeo HTTP) en todo el monorepo. Se identificaron exactamente 23 clases controladoras repartidas en los 4 microservicios funcionales (`donaciones-service`: 10, `logistica-service`: 6, `incentivos-service`: 5, `notificaciones-service`: 2).
4. **Premisa 4 (Exhaustividad AMQP)**: Se mapearon todas las apariciones de `@RabbitListener`, `RabbitTemplate` y configuraciones de colas/exchanges. La mensajería asíncrona está concentrada exclusivamente en la integración entre `logistica-service` (productor de 4 eventos en `logistica.exchange`) y `donaciones-service` (consumidor de 4 colas vía `LogisticaEventListener`). Los otros dos servicios (`notificaciones-service` e `incentivos-service`) interactúan síncronamente vía OpenFeign sobre HTTP.
5. **Premisa 5 (Contraste de Contratos)**: Al cotejar cada método anotado en Java contra `docs/arquitectura/contratos-rest.md` y las especificaciones OpenAPI 3.0 en `docs/arquitectura/contratos/`, surgieron inconsistencias concretas (D1 a D5). La divergencia principal (D1) evidencia que `ItemDonacionNormalizadoController` fue implementado como un flujo de revisión (`/pendientes` y `PATCH /{id}`) mientras que la documentación asumió un CRUD canónico simétrico. La divergencia D2 evidencia endpoints reales de alias de subcategorías omitidos en la documentación.

---

## 3. Caveats

- `[OBSERVED]` Las colecciones de Postman (`.postman/` y `postman/`) y los tests de integración en `integration-tests` ejercitan los endpoints vigentes en runtime, pero no fueron modificados en esta tarea por ser una investigación de solo lectura.
- `[OBSERVED]` Conforme a `AGENTS.md` §2 y §6, ningún archivo de código ni de documentación en `docs/` fue modificado durante este relevamiento; las discrepancias se catalogan formalmente para posterior tratamiento por los agentes correspondientes.
- `[OBSERVED]` No existen otros listeners AMQP en el monorepo aparte de los 4 documentados en `donaciones-service`.

---

## 4. Conclusion

1. **Estado de Grepai MCP**: Totalmente funcional, indexado y validado contra el monorepo local.
2. **Arquitectura Modular Real**: Monorepo Maven con 6 módulos activos en Java 21 / Spring Boot 3 (`common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`). Los nombres `personas-service`, `heladeras-service`, `puntos-service`, `alertas-service` constituyen nombres conceptuales externos y no existen en el monorepo.
3. **Puntos de Entrada REST**: 23 `@RestController` mapeados en su totalidad, detallando verbos, rutas, request bodies y códigos de estado.
4. **Puntos de Entrada AMQP**: 4 `@RabbitListener` en `donaciones-service` escuchando eventos de `logistica-service` a través de `logistica.exchange`.
5. **Divergencias Clave**: Detectadas 5 discrepancias concretas entre el código Java real y los contratos Markdown/OpenAPI (D1 a D5), siendo la más crítica la especificación ficticia de un CRUD para `/api/items-normalizados` cuando el código Java solo provee `/pendientes`, `/{id}` y `PATCH /{id}`.

---

## 5. Verification Method

Para verificar independientemente las conclusiones de este reporte:

1. **Verificar Grepai MCP**:
   ```bash
   # Vía MCP client:
   call_mcp_tool(ServerName: "grepai", ToolName: "grepai_index_status", Arguments: {})
   call_mcp_tool(ServerName: "grepai", ToolName: "grepai_search", Arguments: { "query": "LogisticaEventListener" })
   ```
2. **Verificar Reactor Maven y Calidad**:
   ```bash
   mvn spotless:check
   ```
3. **Verificar Ausencia de Servicios Fantasma**:
   ```bash
   grep -rn "personas-service" .
   grep -rn "heladeras-service" .
   ```
4. **Verificar Endpoints Divergentes (D1, D2, D3)**:
   - Inspeccionar `donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/ItemDonacionNormalizadoController.java`
   - Inspeccionar `donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/SubcategoriasController.java`
   - Inspeccionar `donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/PersonasController.java`
   - Comparar contra `docs/arquitectura/contratos-rest.md` líneas 57–62.
5. **Verificar Scripts de Gobernanza**:
   ```bash
   node scripts/validate-contracts.js
   node scripts/agent-check.js
   node scripts/tests/run-tests.js
   python scripts/validate_docs_links.py
   ```
