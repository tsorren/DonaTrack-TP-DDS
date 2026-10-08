# Bitácora — Rediseño de los roles Donante y EntidadBeneficiaria (Entrega 4)

> Registro vivo de la implementación de la issue #888: decisiones, preguntas, aclaraciones y discusiones, en orden. Para retomar el trabajo, leer primero el **TLDR**, después el **Estado por etapa** y la última entrada del **Registro**.
>
> **Documentos relacionados:** [Plan](plan-implementacion-roles-donante-entidad.md) · [Issue #888](https://github.com/tsorren/DonaTrack-TP-DDS/issues/888)
>
> **Rama:** `E4_donaciones_redisenoRoles` (desde `ENTREGA_4`)

---

## TLDR

> Última actualización: **2026-10-08** — Etapa 8 hecha (Gates 3 y 4 diferidos por falta de Docker): revisión independiente aplicada; Etapa 6 delegada a incentivos.

- **Qué estamos haciendo:** que el id de `Donante` y de `EntidadBeneficiaria` sea el de su persona (`donanteId = personaId`, `entidadId = juridicaId`). Así una persona no puede quedar registrada dos veces, dar de baja un rol no deja referencias colgadas y nadie puede volver EMPRESA a una entidad beneficiaria.
- **Cómo:** opción A3 de la issue (se mantienen las clases con id compartido), baja lógica con un bool `activo`, y una sola cola en incentivos para el ciclo de vida del donante.
- **Dónde estamos:** plan cerrado; Etapas 0 a 5 y 7 hechas, Etapa 6 delegada al equipo de incentivos, Etapa 8 (cierre) hecha. Baseline 437 tests; tras la Etapa 1, 442; tras la Etapa 3, 474; tras la Etapa 4, 492; tras la Etapa 5, 506; tras la revisión (Etapa 8), 511 en `donaciones-service` y reactor completo en verde. El ADR espera aprobación humana (`proposed`).
- **Bloqueante para seguir:** ninguno.
- **Pendiente ajeno:** Etapa 6 (cola única en incentivos), a cargo del equipo de incentivos; hasta entonces el riesgo de orden de eventos sigue abierto. **Próximo paso nuestro:** PR (lo abre el equipo) y correr los Gates 3 y 4 con Docker.

---

## Estado por etapa

| Etapa | Contenido | Estado |
|---|---|---|
| 0 — Baseline y rama | Rama desde `ENTREGA_4`, baseline, búsqueda en tests | ✅ Cerrada |
| 1 — Importador CSV | Dejar de pisar datos al reimportar (`actualizarParcial`) | ✅ Hecha |
| 2 — ADR y spec | ADR `proposed` (el plan hace de spec) | ✅ Hecha |
| 3 — Identidad compartida y unicidad | Roles con id compartido, `activo`, alta idempotente, REST 201/200, `ErrorCatalog` | ✅ Hecha |
| 4 — Baja, cascada y guardas | Baja lógica, cascada a necesidades, anonimización, auto-donación | ✅ Hecha |
| 5 — Custodia del tipo + importador (b) | Rechazar EMPRESA/sin dirección en entidad activa; el importador registra el rol | ✅ Hecha |
| 6 — Incentivos | Cola única con un solo consumidor | 🔀 Delegada al equipo de incentivos |
| 7 — Contratos y docs | Schemas (`donanteId` deprecated), OpenAPI, catálogos, DDL, diagramas | ✅ Hecha |
| 8 — Validación y cierre | Reactor completo, `integration-tests`, Sonar, revisión | ✅ Hecha (Gates 3 y 4 diferidos) |

---

## Decisiones

| # | Decisión | Fecha | Origen |
|---|---|---|---|
| D1 | El id del rol es el de la persona. | 2026-10-07 | Issue |
| D2 | Opción **A3**: se mantienen las clases `Donante` y `EntidadBeneficiaria` con id compartido (no flags en `Persona`). | 2026-10-07 | Equipo |
| D3 | Una jurídica no puede recibir su propia donación. | 2026-10-07 | Equipo |
| D4 | Al dar de alta de nuevo una entidad, sus necesidades no se reactivan. | 2026-10-07 | Equipo |
| D5 | Orden de eventos en incentivos: cola única con un solo consumidor (no comparar `fecha`). | 2026-10-07 | Equipo |
| D6 | Baja lógica con un único campo propio: `activo` (bool). Se descartó `fechaBaja`. | 2026-10-07 | Equipo |
| D7 | `PUT /api/entidades/{id}` solo revalida; si la entidad está de baja responde 409. Reactivar solo con `POST`. | 2026-10-07 | Equipo |
| D8 | Una entidad beneficiaria no puede ser EMPRESA; se aceptan ONG, INSTITUCION y GUBERNAMENTAL (como dice la issue). | 2026-10-07 | Equipo |
| D9 | El importador CSV nunca cambia el tipo jurídico de una persona existente; una celda vacía significa "dejar lo que hay". | 2026-10-07 | Equipo |
| D10 | La actualización parcial vive como método de servicio (`actualizarParcial`), sin endpoint nuevo. | 2026-10-07 | Equipo |
| D11 | Un solo ADR para toda la iniciativa (identidad, baja lógica y orden de eventos), y el plan hace de spec (no se crea `SPEC-0X`). | 2026-10-07 | Equipo |
| D12 | La no auto-donación se controla solo al aprobar la propuesta (no en el matching): se estimó que ocurre muy pocas veces. | 2026-10-08 | Equipo |
| D13 | Las respuestas de donante y entidad exponen `activo` (cambio aditivo). | 2026-10-08 | Equipo |
| D14 | El importador registra el rol de donante de una persona existente, pero **no reactiva** a un donante dado de baja (opción B): la fila cuenta como error y no se actualiza. | 2026-10-08 | Equipo |
| D15 | La Etapa 6 (cola única en incentivos) la implementa el equipo de incentivos; Donaciones solo deja la decisión y las instrucciones de entrega en el plan. | 2026-10-08 | Equipo |
| D16 | `donanteId` se marca `deprecated` en los 7 schemas que ya llevan `personaId`; en `donacion.segmentada` (sin `personaId`) solo se documenta que vale lo mismo. | 2026-10-08 | Equipo |
| D17 | Una persona anonimizada no puede volver a operar: registrar o reactivar un rol sobre ella da `409 PERSONA_ANONIMIZADA` (opción A del hallazgo A1 de la revisión). | 2026-10-08 | Equipo |

---

## Preguntas abiertas

Ninguna.

---

## Registro

### 2026-10-07 — Etapa 0: baseline y rama

- Rama `E4_donaciones_redisenoRoles` creada desde `ENTREGA_4` (`40a07571`).
- **Baseline `[VERIFIED]`:** `mvn test -pl donaciones-service,incentivos-service -am` → 437 tests, 0 fallos (60 en `common-lib`, 233 en incentivos).
- `ENTREGA_4` no tiene el broker de logística (12 commits en `E4_donaciones_broker`). El único archivo relevante para roles que se toca en ambas ramas es `PropuestaDeAsignacionService` (+37 líneas del broker): ahí se van a pisar al integrar.
- Búsqueda en tests: `new Donante(...)` y `new EntidadBeneficiaria(...)` aparecen en unos 15 archivos. El constructor de dos argumentos de `EntidadBeneficiaria` (permite ids desalineados) usado en `EntidadBeneficiariaRepositoryTest`, `AlgoritmoPrioridadSubAtendidosTest` y `EntidadBeneficiariaServiceTest` desaparece en la Etapa 3 y esos tests hay que reescribirlos. `CrossServiceCommunicationIT` ya usa el id que devuelve el POST.

### 2026-10-07 — Etapa 1: el importador deja de pisar datos

- **Problema `[VERIFIED]`:** 4 tests nuevos fallaron antes del arreglo. Reimportar una jurídica la pasaba de ONG a EMPRESA y le borraba los medios de contacto; reimportar una humana le cambiaba el nombre real por `Donante` y el apellido por `Anonimo`.
- **Causa:** el importador usaba `PersonasService.actualizarPersona`, que reemplaza todo (es el mismo que usa `PUT /api/personas/{id}`), y le pasaba los valores de relleno con los que completa una persona nueva (`Donante`, `Anonimo`, `Empresa S.A.`, `Rubro CSV`, tipo EMPRESA, dirección y representantes vacíos).
- **Arreglo:** nuevo `IPersonasService.actualizarParcial` + `PersonaMapper.mergeEntity` (lo vacío no se toca; medios se agregan sin borrar; el tipo jurídico no cambia) + `PersonaMapper.mapToActualizacionParcial(fila)` (arma el pedido solo con lo que la fila trae). `Persona.tieneMedioDeContacto` evita duplicar medios. `PUT /api/personas/{id}` no cambió.
- **Refactor interno:** `extraerNombreYApellido` separa lo informado de los rellenos; `resolverNombreYApellido` lo usa y mantiene el comportamiento para personas nuevas.
- **Verificación `[VERIFIED]`:** `mvn clean test -pl donaciones-service -am` → 442 tests, 0 fallos; `spotless:check` OK. Un test existente (`ImportadorServiceTest`) cambió de `verify(actualizarPersona)` a `verify(actualizarParcial)`, con la misma exigencia.
- **Hallazgo fuera de alcance:** toda `Persona` nace con un `Telefono` vacío de relleno (`Persona()` hace `mediosDeContacto.add(new Telefono())`). No se corrige acá (AGENTS.md §6). Registrar en la Etapa 7.
- **Pendiente de la parte b (Etapa 5):** que el importador registre el rol de donante cuando la persona ya existía; hoy crearía un donante duplicado.

### 2026-10-07 — Etapa 2: ADR `proposed`

- Escrito `docs/adr/donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md` (`Status: proposed`; la promoción a `accepted` la hace una persona al integrar el PR).
- Cubre: los tres defectos con su evidencia, las cinco alternativas (A0 a A4) con la tabla de puntajes de la issue, la decisión A3 con todas las reglas (identidad, `activo`, alta idempotente, custodia del tipo, guardas, cascada, anonimización, auto-donación, `PUT` que revalida), el orden de eventos (cola única frente a comparar `fecha`), consecuencias, validación y relación con los ADRs `20260521-personas`, `20260702` (`rejected`), `20260919`, mapeo ORM, DTI-01 y DTI-06.
- Sobre el ADR `20260702` (`rejected`): no se edita ni se reabre; el ADR nuevo responde en sus propios términos (baja lógica de un **rol** con id compartido, no de la persona).
- No se crea `SPEC-0X`: el plan tiene objetivo, alcance, restricciones y validación. Evita además un choque de numeración con `SPEC-04` de la rama del broker.
- Verificación: `node scripts/agent-check.js` → 67 PASS, 0 WARN, 0 FAIL; los links relativos del ADR resuelven.

### 2026-10-07 — Etapa 3: identidad compartida y unicidad

- **Dominio:** `Donante.id = personaId` y `EntidadBeneficiaria.id = juridicaId`. Ambos tienen `activo` (nace en `true`) con `estaActivo()`, `darDeBaja()` y `reactivar()`; los dos últimos devuelven `true` solo si hubo transición, para que el servicio sepa cuándo publicar un evento. Se eliminó el constructor de dos argumentos de `EntidadBeneficiaria` (permitía ids desalineados).
- **`EntidadBeneficiaria.validarApta(persona)`:** jurídica, tipo distinto de EMPRESA y con dirección; lanza `ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA`, `..._TIPO_INVALIDO` (`ERR-VAL-517`) o `..._SIN_DIRECCION` (`ERR-VAL-518`). La usan el alta y el `PUT`, y la va a usar la custodia del tipo (Etapa 5).
- **Alta idempotente:** `DonantesService.crearDonante` y `EntidadBeneficiariaService.crearEntidad` devuelven `ResultadoRegistro<T>(recurso, creado)`. No existe → crea (`creado=true`); existe activo → lo devuelve sin guardar ni publicar; existe de baja → lo reactiva (el donante publica `donante.registrado`). Los controllers traducen a **201** o **200**.
- **`PUT /api/entidades/{id}`:** exige `id == juridicaId` (si no, `ARGUMENTO_INVALIDO`), revalida con `validarApta` y no cambia el estado; si la entidad está de baja → `BusinessStateException(ENTIDAD_BENEFICIARIA_INACTIVA)` (`ERR-EST-519`, 409).
- **Recorte de alcance respecto del plan:** `DELETE` sigue siendo borrado físico hasta la Etapa 4 (con id compartido un `POST` posterior crea de nuevo); los códigos `DONANTE_INACTIVO` y `DONACION_A_SI_MISMO` se agregan cuando se usen. La rama «reactivar un inactivo» se prueba con roles sembrados de baja, porque todavía nada los da de baja.
- **Tests:** nuevos `DonanteTest` y casos en `EntidadBeneficiariaTest`, `DonantesServiceTest`, `EntidadBeneficiariaServiceTest` y los dos controller tests. En `EntidadBeneficiariaServiceTest` ajusté los fixtures para que `persona.getId() == juridicaId` (antes usaban ids distintos, algo que el modelo nuevo no admite); las aserciones no se debilitaron. `DonantesServiceTest.testCrearDonante` pasó a `resultado.recurso()` por el cambio de tipo de retorno.
- **Verificación `[VERIFIED]`:** `mvn clean test` (reactor completo, por tocar `common-lib`) → todos los módulos en verde (donaciones 474 tests, 0 fallos; 1 test omitido en notificaciones, preexistente); `mvn spotless:check` OK. Gate 3/4 `[DEFERRED_NO_DOCKER]` hasta la Etapa 8.
- **Pendiente de documentar (Etapa 7):** `catalogo-errores.md` con los tres códigos nuevos y `openapi-donaciones.yaml` con el `200` del `POST`.

### 2026-10-08 — Etapa 4: baja lógica, cascada, guardas y anonimización

- **`DELETE` es baja lógica:** `DonantesService.eliminarDonante` y `EntidadBeneficiariaService.eliminarEntidad` llaman a `darDeBaja()` y guardan (no hay `delete`). Solo si hubo transición: el donante publica `donante.dado-de-baja`; la entidad desactiva en cascada todas sus necesidades (`buscarNecesidadesPorEntidad`). Un segundo `DELETE` no repite eventos ni cascada; un id inexistente sigue dando 404. Nuevo `darDeBajaSiExiste(id)` en ambos servicios, tolerante a que la persona no tenga el rol.
- **Guardas:** `DonacionesService.cargarDonacion` rechaza un donante de baja (`DONANTE_INACTIVO`, `ERR-EST-211`); `NecesidadesService` rechaza una necesidad nueva para una entidad de baja (`ENTIDAD_BENEFICIARIA_INACTIVA`); `PropuestaDeAsignacionService.actualizarEstado(APROBADA)` valida antes de aprobar que la necesidad siga activa (`NECESIDAD_INACTIVA`, `ERR-EST-521`) y que ninguna donación sea de la propia entidad (`DONACION_A_SI_MISMO`, `ERR-EST-520`). El matching ya excluía necesidades inactivas (`findByEstaSatisfechaFalseActivaTrue`), así que la cascada alcanza para que no se generen propuestas nuevas.
- **No auto-donación (D12):** solo al aprobar. La alternativa de filtrarla en el matching exigía agregar `donanteId` a `DonacionIndependiente` (2 sitios de creación, 15 usos en tests) y tocar el Template Method del algoritmo; se descartó porque el caso es muy poco frecuente. La guarda es null-safe: una necesidad sin entidad no cuenta como auto-donación.
- **Anonimización:** `PersonasService.eliminarPersona` anonimiza y después llama a `darDeBajaSiExiste` de donantes y de entidades (nuevas dependencias del constructor).
- **`activo` en las respuestas (D13):** `DonanteOutputDTO(idDonante, persona, activo)` y `EntidadBeneficiariaOutputDTO(id, juridica, activo)`; cambio aditivo (AGENTS.md §8.2).
- **Tests:** se escribieron antes de la implementación, pero implementé todo antes de la primera compilación, así que no llegué a ver el estado rojo (en la Etapa 3 sí). Cambios en tests existentes: `testEliminarDonante` verificaba `delete(donante)` y ahora verifica baja lógica; las necesidades de `NecesidadesServiceTest` stubbean `estaActivo()` en los mocks; `actualizarEstado_cuandoEsAprobada` ahora provee una necesidad activa. Ninguna aserción se debilitó.
- **Verificación `[VERIFIED]`:** reactor completo `mvn clean test` en verde (donaciones 492 tests), `spotless:check` OK. Gate 3/4 `[DEFERRED_NO_DOCKER]`.
- **Pendiente de documentar (Etapa 7):** `catalogo-errores.md` (ahora seis códigos nuevos) y `openapi-donaciones.yaml` (`activo`, `200` del `POST`).

### 2026-10-08 — Etapa 5: custodia del tipo y la dirección + importador (parte b)

- **Custodia:** `PersonasService.actualizarPersona` valida, **antes** de mutar la persona, que una jurídica con entidad activa no quede como EMPRESA ni sin dirección. El tipo resultante es el del pedido o, si viene vacío, el actual; la dirección resultante es `null` si el pedido no la trae (en el PUT eso la borra). Si no cumple: `ENTIDAD_BENEFICIARIA_TIPO_INVALIDO` o `ENTIDAD_BENEFICIARIA_SIN_DIRECCION` (400). Si la entidad está de baja o la jurídica nunca fue entidad, no se aplica. Validar antes de mutar importa porque los repositorios en memoria devuelven la misma instancia: mutar y lanzar después dejaría el cambio aplicado (los tests comprueban que la persona queda intacta).
- **Una sola regla:** se extrajo `EntidadBeneficiaria.validarRequisitos(tipo, tieneDireccion)`, que usan tanto `validarApta` (alta) como la custodia (actualización). Nuevo `IEntidadBeneficiariaService.esEntidadActiva(id)` para que `PersonasService` no toque el repositorio de entidades.
- **`actualizarParcial` no necesita la guarda:** nunca cambia el tipo y no puede poner la dirección en `null`.
- **Importador, parte b (D14):** para una persona existente llama a `DonantesService.registrarSiNoExiste(id)`, que devuelve `CREADO`, `YA_REGISTRADO` o `DADO_DE_BAJA` (enum `EstadoRegistroDonante`). Si está de baja no se la reactiva ni se actualizan sus datos, y la fila se cuenta como error (`PROCESADO_CON_ERRORES`). La comparación con `DADO_DE_BAJA` es segura ante `null`, así que los tests existentes del importador, que mockean `donantesService`, siguen igual.
- **Tests:** primero (rojo confirmado: no compilaban), después implementación. Nuevos: 5 en `PersonasServiceTest`, 3 en `EntidadBeneficiariaTest`, 1 en `EntidadBeneficiariaServiceTest`, 3 en `DonantesServiceTest`, 2 en `ImportadorReimportacionTest`. Ninguno existente se modificó.
- **Verificación `[VERIFIED]`:** reactor completo `mvn clean test` en verde (donaciones 506 tests), `spotless:check` OK. Sin códigos nuevos en `common-lib`.

### 2026-10-08 — Etapa 6: delegada al equipo de incentivos

- Se leyó el código de `incentivos-service` para planteársela al equipo: `RabbitMQConfig` declara dos colas durables (`incentivos.donante-registrado`, `incentivos.donante-dado-de-baja`) y `IncentivosEventosListener` tiene un `@RabbitListener` por cada una; las dos colas no se referencian fuera de ese servicio. El `classMapper` ya mapea el header de tipo a clase con las routing keys como id, así que una cola única con `@RabbitHandler` por evento es viable.
- **Decisión (D15):** la implementa el equipo de incentivos porque es su servicio. Las instrucciones de entrega (cambios, tests, y el riesgo de las colas viejas durables, que no se borran solas del broker) quedaron en el plan, en la Etapa 6.
- **Riesgo abierto hasta que se implemente:** baja + alta rápida con el mismo id puede llegar al revés a incentivos y dejar un donante activo sin perfil. Se declara en el reporte final de la iniciativa.

### 2026-10-08 — Etapa 7: contratos y documentación

- **Schemas AMQP (D16):** `"deprecated": true` y descripción «vale lo mismo que `personaId`» en `donanteId` de `asignada`, `en-camino`, `entrega-fallida`, `recibida`, `vencida`, `donante-registrado` y `donante-dado-de-baja`; el campo sigue en `required` (la forma no cambia). En `segmentada`, que no lleva `personaId`, solo se documenta la igualdad. Los schemas usan `draft/2020-12`, que soporta `deprecated`; todos siguen siendo JSON válido.
- **REST:** `openapi-donaciones.yaml` y `contratos-rest.md` — `POST` idempotente 201/200 (donantes y entidades), el `{id}` pasa a ser `personaId` / `juridicaId`, `activo` en las respuestas, `PUT /api/entidades/{id}` que solo revalida (400/404/409), `DELETE` como baja lógica idempotente, y la nota de custodia en `PUT /api/personas/{id}` y de baja de roles en `DELETE /api/personas/{id}`. El OpenAPI no define schemas de componentes para estos DTOs, así que `activo` se documenta en las descripciones.
- **Errores:** `catalogo-errores.md` con `ERR-VAL-517`, `ERR-VAL-518`, `ERR-EST-211`, `ERR-EST-519`, `ERR-EST-520` y `ERR-EST-521`.
- **Mensajes:** `catalogo-mensajes.md` (nota `donanteId == personaId`, qué eventos lo deprecan y cuál no) y `matriz-productor-consumidor.md` (alta o reactivación; baja lógica o anonimización).
- **Modelo:** `aggregates-donaciones.md` (identidad compartida, `activo`, reglas), DDL de `decisiones_futuras_en_oleada_10.md` (`id` = PK y FK a `persona(id)` / `persona_juridica(id)`, `activo`, sin `persona_id` / `juridica_id`), `persistencia.md` y los tres `.puml` de donaciones (`diagrama-de-clases-donaciones.puml`, `donaciones-clases.puml`, `lucid/donaciones-clases.puml`).
- **No se tocó:** `informe-pre-der.md` (snapshot de la Fase 2), `diagrama_de_clases_servicio_de_donaciones_lucidchart.json` (export de Lucidchart), enunciados y ADRs aprobados.
- **Postman:** revisadas las colecciones; capturan `json.idDonante` del `POST` y, como la forma de la respuesta no cambió, siguen válidas. Los flujos crean una persona nueva por corrida, así que el `POST` de donante sigue dando 201.
- **Hallazgos fuera de alcance (no corregidos):** (1) toda `Persona` nace con un `Telefono` vacío de relleno (`Persona()`); (2) `docs/arquitectura/eventos-amqp.md` conserva ejemplos con `personaDonanteId`, un campo que el catálogo prohíbe y que no existe en los schemas actuales; (3) los `.puml` de donaciones eran copias casi idénticas entre sí y estaban desactualizados antes de este cambio (por ejemplo `Donante` mostraba `persona: Persona` y métodos que no existen).
- **Verificación `[VERIFIED]`:** `node scripts/agent-check.js` → 67 PASS, 0 FAIL; `mvn clean test` (reactor completo) en verde. Los tests de contrato contra Docker (`ContractIT`) quedan `[DEFERRED_NO_DOCKER]`.

### 2026-10-08 — Etapa 8: validación, revisión independiente y cierre

- **Gates `[VERIFIED]`:** `mvn clean test` (reactor completo) en verde; `spotless:check` OK; `node scripts/agent-check.js` 67 PASS. **Gates 3 y 4 `[DEFERRED_NO_DOCKER]`** (`docker info` falla en esta máquina): faltan `mvn verify -pl integration-tests -DskipTests=false` y `./run-preprod-tests.sh`. Revisión estática de los ITs: el builder de jurídicas crea ONG con dirección y los tests usan el id que devuelve el `POST`, así que no se detectaron incompatibilidades.
- **Sonar pre-flight (por lectura):** 2 hallazgos propios en tests nuevos (test sin aserción, claves repetidas del CSV), corregidos.
- **Mergeabilidad con `E4_donaciones_broker` (simulada con `git merge-tree`):** el código Java (`PropuestaDeAsignacionService`, su test y `ErrorCatalog`) se fusiona sin conflictos; hay 3 conflictos de documentación (`ESTADO_DOCUMENTACION.md`, `contratos-rest.md`, `matriz-productor-consumidor.md`) a resolver a mano.
- **Revisión `INDEPENDENT_REVIEW`:** un subagente recibió solo los artefactos (rango `ENTREGA_4..HEAD`, plan, ADR, política de revisión) y no mi razonamiento. Veredicto `CHANGES_REQUIRED`: 1 BLOCKING y 8 ADVISORY; puntaje del ADR 4.1/5.0 (estimado por el revisor). Contrastó ejecutando el reactor (506 tests en ese momento) y `spotless:check`.
  - **B1 (BLOCKING), corregido:** tres 409 nuevos (`POST /api/donaciones`, `POST /api/necesidades`, `PUT /api/asignaciones/propuestas/{id}/estado`) no estaban en `contratos-rest.md` ni en el OpenAPI. Mi Etapa 7 los había omitido.
  - **A1, corregido (D17):** reactivar un rol sobre una persona anonimizada la «resucitaba» y publicaba `donante.registrado` con `ANONIMIZADO`. Nueva `Persona.estaAnonimizada()` y `409 PERSONA_ANONIMIZADA` (`ERR-EST-108`) en `crearDonante` y `crearEntidad`. Limitación: se detecta por el valor `ANONIMIZADO` (no hay flag).
  - **A8, corregido:** `validarAprobable` corría antes que el chequeo de estado; ahora solo si la propuesta está `PENDIENTE` (una ya aprobada vuelve a dar el error de transición).
  - **A5/A6, corregidos:** encabezado del plan, resumen de la bitácora y tres frases del ADR desactualizados (`registrar`, «ni confirmar», «registrado como deuda» siendo falso).
  - **A2/A3, declarados:** falla parcial de la baja (cascada o evento) no reintentable, y carrera check-then-act en el alta; ambos se resuelven con una transacción al pasar a JPA. Agregados a las consecuencias del ADR.
  - **A4, declarado:** el orden de eventos baja/alta con incentivos sigue abierto hasta la Etapa 6 (a cargo del equipo de incentivos). El revisor recomienda no mergear a `main` sin ella o sin una nota de riesgo explícita en el PR.
  - **A7:** se agregaron tests para A1 (donante y entidad, incluida la no reactivación de un rol de baja) y para A8. No se agregó test de la falla parcial (A2): requeriría inyectar fallos que el modelo en memoria no tiene.
- **Verificación `[VERIFIED]` final:** donaciones 511 tests, 0 fallos; reactor completo en verde.

---

## Q&A

**P: ¿Podría haber un bool de activo/inactivo en lugar de `fechaBaja`? ¿Cuál es la diferencia?**
Sí. El bool guarda solo el estado actual; `fechaBaja` guarda además cuándo. Nadie lee el "cuándo" hoy (el evento de baja ya lleva `fecha`) y reactivar lo borraría igual, así que el bool es el cambio mínimo. Si más adelante piden auditoría de bajas, se agrega la fecha sin romper nada. → D6.

**P: Para `PUT /api/entidades/{id}`, ¿qué ventajas y desventajas tiene que solo revalide contra que también reactive?**
Solo revalidar: sin efectos laterales, un único camino de alta y reactivación (`POST`) y coherente con D4 (reactivar no devuelve las necesidades); la desventaja es que el PUT queda casi vacío y sobre una entidad inactiva falla. Revalidar y reactivar: cubre el ciclo completo y sigue la semántica REST de "dejá el recurso así", pero hay dos caminos de reactivación y un efecto escondido en un verbo que no lo sugiere. → D7.

**P: ¿Qué cambia en el importador CSV, en lenguaje coloquial?**
Con una persona que ya existe, hoy el importador "reemplaza todo con lo que trae el CSV", y como el CSV casi nunca trae todo, borra o cambia lo que no trae (dirección, representantes, tipo, medios, nombre). Ahora "completa y corrige lo que el CSV dice" y deja el resto como estaba.

**P: ¿El importador actualiza los datos que trae el CSV de una persona que ya existe?**
Sí, solo los que trae con valor (un teléfono o correo nuevo, un nombre corregido). Lo que el CSV no trae o trae vacío queda intacto, y el tipo de una jurídica no se modifica nunca. Una celda vacía significa "dejar lo que hay", no "borrar". → D9.

**P: ¿Por qué el importador no podría pisar el tipo de organización?**
Porque hoy el CSV no trae ninguna columna de tipo: el `EMPRESA` que llega es un valor por defecto que inventa el importador, no un dato. Pisar el tipo real con ese valor sería pisar un dato verdadero con uno falso. Si algún día el CSV trae el tipo, se aplicaría solo si viene con valor, con el límite de que una entidad beneficiaria activa no puede pasar a EMPRESA.

**P: ¿Cuáles son los tipos posibles de persona jurídica? ¿Solo ONG y empresa?**
No. `TipoJuridico` tiene cuatro valores: `GUBERNAMENTAL`, `ONG`, `EMPRESA` e `INSTITUCION`. La regla de la issue rechaza solo EMPRESA, así que se aceptan los otros tres aunque el enunciado habla de "organizaciones sin fines de lucro". Si el equipo quisiera ser estricto, aceptar solo ONG es un cambio de una línea. → D8.

**P: ¿El importador usaría el endpoint de PATCH en vez de PUT?**
No. El importador corre dentro de `donaciones-service` y llama directamente a métodos Java del servicio, sin HTTP. Lo nuevo es un método de servicio (`actualizarParcial`), no un endpoint. Agregar `PATCH /api/personas/{id}` sería un cambio de contrato público que nadie pidió; si más adelante se quiere, se expone ese mismo método. → D10.

**P: ¿Un solo ADR o dos? ¿El plan hace de spec o creo una `SPEC-0X`?**
Un solo ADR: la issue pide uno que cubra todo, y el orden de eventos queda como una sección propia (separarlo en dos agregaría papeleo sin cambiar la decisión). El plan ya tiene objetivo, alcance, restricciones y validación, que son los campos mínimos de una spec, así que se evita duplicar. → D11.

**P: ¿Por qué se marcaría `donanteId` como deprecated?**
Porque con el id compartido `donanteId` y `personaId` valen siempre lo mismo y los eventos llevan los dos: hay dos nombres para el mismo dato. Deprecar es el primer paso del ciclo de migración que exige AGENTS.md §8.2 (no se renombra ni elimina un campo sin ese ciclo): el campo se mantiene, pero se avisa a los consumidores que usen `personaId`. En `donacion.segmentada` no se puede deprecar todavía porque ese evento no lleva `personaId`; ahí solo se documenta. → D16.

**P: ¿Cuál es el problema de A1? ¿Una persona dada de baja queda anonimizada y luego se quiere crear de nuevo?**
No: hay dos bajas distintas. `DELETE /api/donantes/{id}` solo marca al donante como inactivo (se puede reactivar con un `POST`, y es lo esperado). `DELETE /api/personas/{id}` anonimiza a la persona (irreversible) y da de baja sus roles. El problema es que el **id viejo sigue existiendo** como cáscara: un cliente o script con ese id podía hacer `POST /api/donantes` y reactivar al donante, publicando `donante.registrado` con nombre `ANONIMIZADO`. Una persona real que quiera donar de nuevo se carga como persona nueva, con otro id, y no se ve afectada. → D17.
