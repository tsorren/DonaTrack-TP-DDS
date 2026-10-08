# Plan de implementación — Rediseño de los roles Donante y EntidadBeneficiaria (issue #888)

> Issue: [#888 — Donaciones: rediseño de los roles Donante y EntidadBeneficiaria (identidad, baja y unicidad)](https://github.com/tsorren/DonaTrack-TP-DDS/issues/888). Etiquetas: `donaciones`, `incentivos`, `requerimiento`, `prioridad:alta`.
>
> **Nivel de tarea:** ARCHITECTURAL (AGENTS.md §7.0). Cambia la semántica de ids en contratos REST y AMQP, el ciclo de vida de un rol cross-service (donaciones → incentivos) y requiere un ADR `proposed`. Revisión: `ENHANCED_REVIEW_REQUIRED`.
>
> **Etiquetas epistémicas (AGENTS.md §3):** `[OBSERVED]` leído en el código · `[DOCUMENTED]` en un ADR/doc · `[INFERRED]` deducido · `[PROPOSED]` no existe aún · `[A VERIFICAR]` supuesto a confirmar en la etapa indicada.
>
> **Estado:** plan aprobado en sus decisiones (D1–D7); pendiente de ejecución etapa por etapa. No se tocó código. Cada etapa se implementa solo después de que la apruebes.

### Decisiones tomadas

| # | Decisión | Origen |
|---|---|---|
| D1 | El id del rol es el de la persona: `donanteId = personaId`, `entidadId = juridicaId`. | Issue (ganó con cualquier combinación razonable de pesos) |
| D2 | **A3**: se mantienen las clases `Donante` y `EntidadBeneficiaria` con id compartido (`@MapsId` en JPA). No se unifica en flags de `Persona`. | Equipo |
| D3 | Una jurídica **no** puede recibir su propia donación, aunque sea donante y beneficiaria a la vez. | Equipo |
| D4 | Al dar de alta de nuevo una entidad, sus necesidades **no** se reactivan: se vuelven a registrar. | Equipo |
| D5 | Orden de eventos en incentivos: **cola única con un solo consumidor** (ver §3). | Equipo |
| — | Rama de trabajo: `E4_donaciones_redisenoRoles`, creada desde `ENTREGA_4`. | Equipo |
| D6 | Los roles llevan **un único campo propio: `activo` (bool)** (baja lógica). Ver §1.2. Se descartó `fechaBaja`: nadie lee el "cuándo" (el evento de baja ya lleva `fecha`) y reactivar lo borraría igual. | Equipo |
| D7 | `PUT /api/entidades/{id}` **solo revalida**; si la entidad está de baja responde 409 `ENTIDAD_BENEFICIARIA_INACTIVA`. Reactivar se hace solo con `POST`. | Equipo |

---

## 0. Estado de partida (`[OBSERVED]`)

- `Donante(id, personaId)` y `EntidadBeneficiaria(id, juridicaId)` generan un `UUID.randomUUID()` propio. `anonimizar()` está vacío en ambas.
- `DonanteMapper.toEntity` solo verifica que la persona exista; `DonantesService.crearDonante` siempre crea y siempre publica `donante.registrado`. No hay chequeo de unicidad.
- `EntidadBeneficiariaService.crearEntidad` solo verifica `instanceof Juridica`. `actualizarEntidad` hace `new EntidadBeneficiaria(id, input.juridicaId())`, o sea que hoy permite que `id` y `juridicaId` apunten a cosas distintas.
- `eliminarDonante` y `eliminarEntidad` hacen `repository.delete`. `eliminarDonante` publica `donante.dado-de-baja`; `eliminarEntidad` no publica nada ni toca las necesidades.
- `PropuestaDeAsignacionService.resolverDatosBeneficiario` captura la excepción y devuelve `null`; entonces `publicarDonacionAsignada` solo hace `log.warn` y `return`. Si la entidad se borró, `donacion.asignada.v1` no se publica.
- `PersonasService.eliminarPersona` llama a `persona.anonimizar()` y no toca los roles: una ONG anonimizada sigue activa en el matching.
- `Juridica.actualizar(razonSocial, tipo, rubro)` acepta cualquier tipo. `PersonaMapper.crearPersonaJuridica` crea siempre `TipoJuridico.EMPRESA` con rubro `"Rubro CSV"`.
- `ImportadorService.procesarFilaIndividual`, en el camino de update, arma un `JuridicaInputDTO` con **dirección `null`** y **representantes `List.of()`**. `PersonaMapper.updateEntity` los aplica tal cual (`actualizarDireccion(null)`, `limpiarRepresentantes()`, `limpiarMediosDeContacto()`). Es decir, reimportar pisa dirección, representantes, medios y tipo. Además, en el camino de update no se registra el rol de donante.
- Incentivos: `donante.registrado.v1` y `donante.dado-de-baja.v1` llegan por **colas distintas** (`incentivos.donante-registrado`, `incentivos.donante-dado-de-baja`). `GestionDonanteService.registrarDonante` ignora el alta si el perfil ya existe; `darDeBaja` borra el perfil físicamente.
- Alcance medido (búsqueda por texto, incluye menciones que no cambian): 39 archivos main y 25 de test en donaciones; 47 main y 21 de test en incentivos (tratan `donanteId` como id opaco); 9 en `integration-tests`. La rama ya incluye el broker de logística (Etapas 0–6), lo que corrió algunas líneas respecto del commit `40a07571` que cita la issue.

---

## 1. Diseño objetivo (A3)

### 1.1 Identidad

- `Donante`: `id == personaId`. El constructor deja de generar un UUID aleatorio. `personaId()` queda como alias de `getId()` (así no se rompen los 3 saltos `donanteId → personaId` en esta iteración; ver §6).
- `EntidadBeneficiaria`: `id == juridicaId`. Se elimina el constructor `(id, juridicaId)`, que permitía ids desalineados.
- Unicidad **por construcción**: `repository.save` es un upsert por id, y en JPA la PK es FK a `persona(id)` (`@MapsId`).

### 1.2 Baja lógica (D6)

A3 en la issue dice "clases sin campos", pero sus propias reglas de §«Riesgos» exigen distinguir donantes/entidades **activos**. Con borrado físico:
- se reintroduce la referencia colgada (defecto 2), o
- con la FK `RESTRICT` de E4 no se puede dar de baja a nadie que tenga historial.

Decisión (D6): agregar a ambos roles **solo** `activo` (bool, `true` al registrar), con `darDeBaja()`, `reactivar()` y `estaActivo()`. `DELETE /api/donantes/{id}` y `DELETE /api/entidades/{id}` pasan a ser baja lógica. La issue (A2) objetó este campo por circular *con ids propios*: con id compartido deja de serlo, porque la persona nunca se borra y `activo` representa algo que la `Persona` no sabe decir (que dejó de operar como donante o beneficiaria sin ser anonimizada).

> **Conflicto documental a resolver en el ADR:** `docs/adr/donaciones-service/20260702-alcance-operaciones-rest-entidad-beneficiaria.md` (`rejected`) evaluó y descartó la baja lógica de la entidad. El ADR nuevo tiene que explicar por qué esta propuesta es distinta (rol revocable sin anonimizar la `Jurídica`, con id compartido) y no puede basarse en editar ese ADR (AGENTS.md §2).

### 1.3 Reglas

| Regla | Dónde se hace cumplir |
|---|---|
| Registrar donante es idempotente: existe y activo → 200 sin evento; no existe → 201 + `donante.registrado`; existe inactivo → reactiva, 200 + `donante.registrado` | `DonantesService` |
| Solo persona humana o jurídica existente puede ser donante | `DonantesService` |
| Entidad: la persona debe ser `Juridica`, `tipo != EMPRESA` (se aceptan ONG, INSTITUCION y GUBERNAMENTAL, siguiendo la issue) y tener dirección | `EntidadBeneficiariaService` (nuevos errores de `ErrorCatalog`) |
| Mientras la entidad esté activa, su `Juridica` no puede pasar a EMPRESA ni quedarse sin dirección | `PersonasService.actualizarPersona`, validando contra el input **antes** de mutar (los repos en memoria devuelven la misma instancia: mutar y después lanzar deja el estado cambiado) |
| Solo donantes activos cargan donaciones; solo entidades activas registran necesidades | `DonacionesService.cargarDonacion`, `NecesidadesService` |
| Baja de entidad ⇒ `Necesidad.desactivar()` en cascada | `EntidadBeneficiariaService` |
| `Propuesta` ya creada con necesidad inactiva no se puede aceptar/confirmar | `PropuestaDeAsignacionService` (la `Propuesta` solo conoce el id de la necesidad) |
| Anonimizar persona ⇒ baja de sus roles + eventos + cascada | `PersonasService.eliminarPersona` |
| D3: una donación no puede asignarse a una necesidad de su propia donante (`donacion.donanteId == necesidad.entidadId`) | Matching (`AlgoritmoAsignacion`) y confirmación; con id compartido es una comparación directa |

---

## 2. Etapas

Cada etapa termina con Gate 1/2 en verde, revisión según nivel y tu aprobación antes de pasar a la siguiente. Los commits los hacés vos.

### Etapa 0 — Baseline y rama

- Crear la rama `E4_donaciones_redisenoRoles` desde `ENTREGA_4`. Los cambios sin commitear del broker (`bitacora-broker-logistica.md`, `plan-implementacion-broker-logistica.md`) **no** deben viajar a la rama nueva: se resuelve antes de crearla (commit en `E4_donaciones_broker` o stash). Este plan y su fila en `ESTADO_DOCUMENTACION.md` sí viajan.
- `ENTREGA_4` no tiene el broker de logística: si el baseline o los tests de integración dependen de él, se reporta.
- Baseline: `mvn test -pl donaciones-service,incentivos-service -am`. Registrar `BASELINE_GREEN` / `RED`.
- Búsqueda exhaustiva en **todo** `src/test` (no solo lo evidente) de `new Donante(`, `new EntidadBeneficiaria(`, `getId()` de roles, fixtures y mocks de `IDonantesRepository` / `IEntidadesBeneficiariasRepository`.

### Etapa 1 — Importador CSV: dejar de pisar datos (prerrequisito, parte a)

> **Estado: implementada y verificada** (`[VERIFIED]`, 442 tests en verde en `donaciones-service`, `spotless:check` OK). Mecanismo final: nuevo `IPersonasService.actualizarParcial` + `PersonaMapper.mergeEntity` / `mapToActualizacionParcial`; el importador usa ese camino y `PUT /api/personas/{id}` no cambió. Tests: `ImportadorReimportacionTest`. Nota: toda `Persona` nace con un `Telefono` vacío de relleno (`Persona()`), por eso los tests comparan contra la cantidad de medios previa.

- En el camino de update, **no** enviar dirección/representantes/medios/tipo si el CSV no los trae; partir de los datos actuales de la persona y aplicar solo las columnas presentes y no vacías. Nunca modificar `tipo` de una jurídica existente (el CSV no trae esa columna: el `EMPRESA` actual es un valor por defecto del importador, no un dato). Una celda vacía significa "dejar lo que hay", nunca "borrar".
- No se puede registrar el rol de donante en update todavía: con ids propios eso crearía duplicados. Eso es la parte b (Etapa 5).
- Tests: reimportar una jurídica con dirección, representantes y tipo `ONG` conserva todo; reimportar humana conserva medios.
- Nivel STANDARD. No requiere ADR.

### Etapa 2 — ADR `proposed` y spec

> **Estado: hecha.** Un solo ADR `proposed`: [`20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria`](../../adr/donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md). Este plan hace de spec (no se crea `SPEC-0X`). `node scripts/agent-check.js` en verde.

- `docs/adr/donaciones-service/2026XXXX-identidad-compartida-roles-donante-entidad.md` (Log4brains, estado `proposed`).
- Cubre: D1–D6, el orden de eventos (§3), CRUD de entidades, relación con `20260702` (rejected), `20260521-personas`, `20260919-convencion-canonica-identificadores…`, `20260901-estrategia-de-mapeo-orm…` y `…dti-01…surrogate-keys-para-jpa`.
- Sin código. Sin esta etapa aprobada no avanzamos (la implementación sobre un ADR `proposed` es posible, pero con riesgo de rollback; vos decidís si la aceptás).

### Etapa 3 — Donaciones: identidad compartida y unicidad

> **Estado: implementada y verificada** (`[VERIFIED]`: reactor completo `mvn clean test` en verde, `spotless:check` OK; `donaciones-service` pasa de 442 a 474 tests). Alcance final de esta etapa: identidad compartida, `activo` en el dominio, alta idempotente con reactivación, `POST` 201/200 (`ResultadoRegistro`), validación de entidad apta (`EntidadBeneficiaria.validarApta`), `PUT /api/entidades/{id}` que solo revalida, y tres códigos en `ErrorCatalog` (`ERR-VAL-517`, `ERR-VAL-518`, `ERR-EST-519`). **Se movieron a la Etapa 4:** `DELETE` como baja lógica (en la Etapa 3 sigue siendo borrado físico) y los códigos `DONANTE_INACTIVO` / `DONACION_A_SI_MISMO`, que se agregan cuando se usen. `EntidadBeneficiaria.registrar` se descartó por redundante: el servicio valida con `validarApta` y construye con el id de la jurídica.

- `common-lib` (`ErrorCatalog`): `DONANTE_INACTIVO`, `ENTIDAD_BENEFICIARIA_INACTIVA`, `ENTIDAD_BENEFICIARIA_TIPO_INVALIDO`, `ENTIDAD_BENEFICIARIA_SIN_DIRECCION`, `DONACION_A_SI_MISMO` (códigos y mapeo HTTP en `GlobalExceptionHandler` según el rango libre). Obliga a validar con el reactor completo.
- `Donante` / `EntidadBeneficiaria`: id compartido, `activo`, `darDeBaja()`, `reactivar()`, `estaActivo()`; `EntidadBeneficiaria.registrar(Juridica)` valida tipo y dirección.
- `DonantesService.crearDonante` y `EntidadBeneficiariaService.crearEntidad`: idempotentes (ver §1.3). Para el 201/200 el servicio devuelve un resultado `(dto, creado)` y el controller lo traduce.
- `PUT /api/entidades/{id}`: exige `id == juridicaId`, revalida tipo/dirección y no cambia el estado del rol (D7). Si la entidad está inactiva → 409 `ENTIDAD_BENEFICIARIA_INACTIVA`. La edición real de datos va por `/api/personas/{id}`.
- Repos en memoria: `save` por id; `delete` deja de usarse desde los servicios.
- Eventos: `donanteId` pasa a valer lo mismo que `personaId` (forma sin cambios).

### Etapa 4 — Baja, cascada, guardas y anonimización

> **Estado: implementada y verificada** (`[VERIFIED]`: reactor completo en verde, `spotless:check` OK; `donaciones-service` 492 tests). Se hizo en una sola etapa (sin dividir). `DELETE` de donante y de entidad son baja lógica e idempotentes; la baja de entidad desactiva sus necesidades; guardas en `cargarDonacion` (`DONANTE_INACTIVO`), en el alta de necesidades (`ENTIDAD_BENEFICIARIA_INACTIVA`) y al aprobar una propuesta (`NECESIDAD_INACTIVA`, `DONACION_A_SI_MISMO`); `PersonasService.eliminarPersona` da de baja los roles; `DonanteOutputDTO` y `EntidadBeneficiariaOutputDTO` exponen `activo`. **Decisión:** la no auto-donación se controla solo al aprobar (el matching puede seguir generando esas propuestas; se estimó muy infrecuente). Códigos nuevos: `ERR-EST-211`, `ERR-EST-520`, `ERR-EST-521`.

- `eliminarDonante` → `darDeBaja()` + `donante.dado-de-baja` solo si hubo transición. `eliminarEntidad` → `darDeBaja()` + cascada de `Necesidad.desactivar()` (`[A VERIFICAR]` si `INecesidadesRepository` ya tiene búsqueda por `entidadId`; si no, se agrega).
- Guardas de §1.3 en `DonacionesService`, `NecesidadesService`, `PropuestaDeAsignacionService` (aceptar y confirmar fragmentación) y D3 en el matching.
- `PersonasService.eliminarPersona`: baja de roles activos + eventos + cascada.
- `DonacionMapper`: ya no devuelve `donante: null`, porque el rol sigue existiendo. `resolverDatosBeneficiario` deja de caer en el `null` silencioso por entidad borrada; para entidad **inactiva** se define el comportamiento (rechazar la confirmación antes de llegar ahí).

### Etapa 5 — Custodia del tipo + importador (parte b)

> **Estado: implementada y verificada** (`[VERIFIED]`: reactor completo en verde, `spotless:check` OK; `donaciones-service` 506 tests). `PersonasService.actualizarPersona` valida (antes de mutar) que una jurídica con entidad activa no pase a EMPRESA ni se quede sin dirección, reutilizando la regla de dominio `EntidadBeneficiaria.validarRequisitos`. El importador registra el rol de donante de una persona existente con `registrarSiNoExiste` y **no reactiva** a un donante de baja (esa fila cuenta como error y no se actualiza). Sin códigos nuevos.

- `PersonasService.actualizarPersona`: rechaza EMPRESA y dirección `null` si la jurídica es entidad activa.
- `ImportadorService`: en update también registra el rol de donante (ahora idempotente).
- Nivel STANDARD dentro de la iniciativa.

### Etapa 6 — Incentivos: orden de eventos (D5)

Ver §3. Cambios: una cola `incentivos.donante-ciclo-de-vida` con los dos bindings, un único listener con concurrencia 1, retiro de las dos colas viejas y de sus listeners.

### Etapa 7 — Contratos y documentación

- `docs/arquitectura/contratos/schemas/*.schema.json`: marcar `donanteId` como *deprecated* en los 8 schemas.
- `openapi-donaciones.yaml` (semántica de `{id}`, 201/200), `catalogo-mensajes.md`, `matriz-productor-consumidor.md`, `catalogo-errores.md`, `aggregates-donaciones.md`, `decisiones_futuras_en_oleada_10.md` (DDL: `donante` y `entidad_beneficiaria` con PK = FK, `activo BOOLEAN NOT NULL DEFAULT TRUE`), `.puml` de clases de donaciones, `persistencia.md`.
- Sincronizar `docs/ESTADO_DOCUMENTACION.md` y `docs/README.md` (mandatorio por AGENTS.md §6). Las colecciones Postman de `docs/testing/postman` que usen ids de rol.
- No se editan ADRs aprobados ni enunciados.

### Etapa 8 — Validación y cierre

- `mvn spotless:check`, `mvn clean test` (reactor completo, por tocar `common-lib`), `mvn verify -pl integration-tests -DskipTests=false` (incluye `CrossServiceCommunicationIT`; sin Docker se declara `[DEFERRED_NO_DOCKER]`), pre-flight SonarCloud (`docs/IA/07-…`), Review Contract (`ENHANCED_REVIEW_REQUIRED`) y reporte (`docs/IA/04-…`).

---

## 3. Decisión D5 — orden de eventos en incentivos

**Problema:** con id reusado, `baja → alta` rápido puede llegar a incentivos como `alta → baja` (colas distintas): el alta se ignora (el perfil existe) y la baja borra el perfil. El donante queda activo y sin perfil de incentivos.

| | Cola única, un consumidor | Comparar `fecha` |
|---|---|---|
| Cómo funciona | Los dos eventos van a una cola con dos bindings. RabbitMQ conserva el orden de publicación y un solo consumidor los procesa en orden | Cada evento trae `fecha`; incentivos descarta el que sea anterior al último procesado de ese donante |
| Cambios | Topología (cola nueva, bindings, listener unificado) | Modelo de incentivos: guardar la fecha del último evento |
| Debilidad | Se rompe si se escalan consumidores o hay reencolado (hoy no hay ninguno). Hay que retirar las colas viejas, que si quedan acumulan mensajes | **Falla con una baja ya ejecutada:** un alta atrasada recrearía el perfil de alguien dado de baja, porque la baja borró el perfil y no queda dónde comparar. Hace falta una lápida (baja lógica en incentivos) |
| `fecha` | — | `LocalDateTime` sin zona, resolución limitada, empates posibles |
| Complejidad | Baja | Media-alta |

**Recomendación (`[PROPOSED]`): cola única.** Cubre exactamente el riesgo con menos piezas y no depende de relojes. La comparación por `fecha` queda como defensa adicional solo si más adelante se escala a varios consumidores.

---

## 4. Contratos

| Contrato | Tipo de cambio (AGENTS.md §8.2) |
|---|---|
| AMQP: 8 schemas con `donanteId` | Forma sin cambios; `donanteId` pasa a ser igual a `personaId` y se marca *deprecated*. Refactor de semántica, no de forma |
| REST `/api/donantes/{id}`, `/api/entidades/{id}` | **`{id}` cambia de significado** (pasa a ser `personaId` / `juridicaId`). En memoria no hay datos persistidos que migrar, pero cualquier cliente o colección que guarde ids de rol queda obsoleto |
| `POST /api/donantes` | 201 si crea, 200 si ya existía (aditivo) |
| `DELETE` | Pasa a baja lógica: la respuesta no cambia, el registro persiste |
| Incentivos | Modelo sin cambios; cambia la topología de colas |

## 5. Riesgos

1. **Tests y fixtures:** muchos construyen `Donante` o `EntidadBeneficiaria` con ids aleatorios y los usan como id de rol. Hay que revisar los 25 archivos de test de donaciones y los de `integration-tests`.
2. **Re-alta:** incentivos crea un perfil nuevo y se pierde el historial, igual que hoy. Conservarlo requeriría `donante.reactivado.v1` (fuera de alcance).
3. **Condición de carrera** check-then-act en la custodia del tipo (§1.3). Aceptable en memoria; con JPA se resuelve con la transacción.
4. **Colas viejas huérfanas** en RabbitMQ si no se eliminan: acumulan mensajes. En el flujo local se recrea el broker con `run-preprod-tests.sh`.

## 6. Fuera de alcance (a registrar como hallazgo, no corregir)

- #849: credenciales en texto plano en el payload de `donante.registrado` (`DonantesService.crearDonante` arma `"Usuario: … / Password: …"`). Mismo evento; conviene coordinarlo si se toca el contrato.
- Simplificar los 3 saltos `donanteId → personaId` (`DonacionMapper`, `PropuestaDeAsignacionService`, `DonacionesIndependientesNotificacionesService`): con id compartido son identidad. Se dejan por minimalismo (§6 de AGENTS.md).

## 7. Puntos abiertos

Ninguno. D5, D6, D7 y la rama están cerrados. Lo único previo a la Etapa 0 es que commitees los cambios del broker (hecho por el usuario).
