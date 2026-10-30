# Diagnóstico del módulo `integration-tests` (E5)

> **Fecha:** 2026-10-29 · **Rama:** `E5_mejora_integration_tests` (base `origin/ENTREGA_5`, `8ed69e48`)
> **Alcance:** las 9 clases IT de `integration-tests/` (30 `@Test`) y su documentación. Por una decisión de la fase 2, también la corrección de las fechas de 4 eventos de Logística (§7).
> **Estado:** fase 1 (diagnóstico, §1–§6) y fase 2 (implementación de P1–P5, §7) terminadas.

Este informe evalúa la suite en cuatro ejes: cobertura de flujos E5, robustez, calidad del código de test, y costo y ejecución.
Toma como punto de partida los hallazgos y el roadmap que ya existen:

- [`auditoria-arquitectura-testing.md`](auditoria-arquitectura-testing.md): F-01 a F-12;
- [`auditoria/`](auditoria/README.md): AP-xx y QA-GAP-xx;
- [`preprod-fiel-roadmap.md`](preprod-fiel-roadmap.md): RM-xx;
- [`../adr/DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md): DTI-16 y DTI-22.

Cuando un hallazgo ya tiene id, se cita ese id y no se crea uno nuevo.
Los hallazgos nuevos usan el prefijo `DIT-`.

---

## 1. Línea base medida

`[VERIFIED]` Se hicieron 3 corridas de `./run-preprod-tests.sh`, el mismo punto de entrada que usa CI.
Cada corrida levanta un stack nuevo, corre `smoke` y después `contract,integration,e2e`, y baja el stack con `down -v`.
La corrida 1 compila los JARs; las corridas 2 y 3 usan `--skip-build`.

Después, con el stack de la corrida 4 todavía arriba, se corrió dos veces más `mvn verify -pl integration-tests -DskipTests=false -Dgroups=contract,integration,e2e` con `PREPROD_STRICT=true`, para ver si los datos que deja una corrida afectan a la siguiente.

| Corrida | Stack | Resultado | Salteados | Tiempo total | Tiempo de Failsafe |
|---|---|---|---|---|---|
| 1 (con compilación) | nuevo | **No corrió**: RabbitMQ no arrancó (DIT-11) | — | 528 s | — |
| 2 | nuevo | 30/30 en verde | 0 | 274 s | ≈27 s |
| 3 | nuevo | 30/30 en verde | 0 | 277 s | ≈33 s |
| 4 (2.º intento; el 1.º repitió DIT-11) | nuevo | 30/30 en verde | 0 | 242 s hasta el fin de la suite | ≈50 s |
| 5 | el mismo de la 4 | 26/26 en verde (sin `smoke`) | 0 | 102 s (Maven incluido) | ≈34 s |
| 6 | el mismo de la 4 | 26/26 en verde (sin `smoke`) | 0 | 45 s (Maven incluido) | ≈27 s |

Conclusiones:

- `[VERIFIED]` Con stack nuevo, la suite pasa: 3 de 3 corridas válidas sin fallos ni salteados, y sin avisos de contratos AMQP (`contratos-amqp-avisos.txt` no se generó).
- `[VERIFIED]` Tres corridas seguidas sobre el mismo stack dan el mismo resultado. Es el criterio (3) de RM-16, medido en el camino verde. Que pase cuando una corrida anterior falló a mitad de camino no se midió (DIT-04).
- `[VERIFIED]` No se observó inestabilidad en la suite. Todas las fallas vinieron del arranque del entorno local (DIT-11).
- `[OBSERVED]` En local (Windows), Failsafe corre `contract → e2e → integration`. En CI (Linux), el orden fue `e2e → integration → contract` (log 40815880445). `[INFERRED]` Failsafe no fija `runOrder`, así que el orden lo da el sistema de archivos.

Evidencia: `run-preprod-tests.sh` y los reportes de Failsafe de cada corrida, guardados fuera del repo durante la sesión de diagnóstico.

---

## 2. Cobertura de flujos E5

El enunciado de E5 pide persistencia políglota: una base relacional con ORM por servicio y una base documental para Logística ([`../entregas/README.md`](../entregas/README.md)).
El gate de E5 (RM-05) exige que `LogisticaIntegrationIT` y el E2E corran sin salteados con Logística sobre la base documental.
Ese criterio se cumple: las 3 corridas de §1 no tienen salteados.
Lo que sigue son flujos entre servicios que E5 da por funcionando y que la suite no ejercita, o ejercita por un atajo.

| Id | Hallazgo | Evidencia | Impacto |
|---|---|---|---|
| **F-01** (sigue abierto) | **El camino de vuelta Logística → Donaciones nunca se ejerce.** El E2E y dos tests de `CrossServiceCommunicationIT` llevan cada donación independiente a `ENTREGADA` con `PATCH` directo a Donaciones (actor `TRANSPORTISTA`). En producción, esos estados los aplica `LogisticaEventListener` cuando llegan `ruta.asignada`, `ruta.iniciada` y `entrega.exitosa`, con la verificación de origen del proveedor (`X-Proveedor-Id/Token`). | `FullDistributedDonationE2EIT.java:149-166`; `CrossServiceCommunicationIT.java:216-233`, `:433-450`; `donaciones-service/.../infrastructure/logistica/LogisticaEventListener.java`; `ProcesadorEventosLogistica.java:59-108` | **Alto.** Si se rompe el listener, la verificación de origen o la publicación de Logística (ahora sobre MongoDB), la suite sigue en verde. El comentario de `FullDistributedDonationE2EIT.java:135` todavía habla de una llamada Feign que ya no existe. |
| **QA-GAP-05** (parcial) | **Caminos de falla sin IT entre servicios.** Nadie ejerce `entrega.fallida` → `ENTREGA_FALLIDA` en Donaciones, ni `donacion.entrega-fallida.v1` y `donacion.vencida.v1` hacia Notificaciones. `NO_RECIBIDA` se prueba solo dentro de Logística. | `LogisticaIntegrationIT.java:75-119`; `ProcesadorEventosLogistica.java:111-120` | Medio-alto |
| **DIT-01** | **Incentivos se alimenta por un atajo REST.** Los tests de misiones, ranking e insignias envían donaciones con `POST /api/incentivos/donaciones`. En producción, Incentivos las recibe por AMQP (`donacion.segmentada.v1` y `donacion.recibida.v1`). `subio-categoria` y `donante-inactivo` no tienen IT. | `CrossServiceCommunicationIT.java:263-265`, `:309-311`; `incentivos-service/.../IncentivosEventosListener.java:70-100` | Medio. El cálculo de misiones queda probado, pero no su entrada real. |
| **DIT-02** | **El espía AMQP no dice qué contratos se ejercitaron.** Solo valida los mensajes que vio durante la clase E2E. No compara los tipos observados contra los 19 schemas `evento-*` ni exige los eventos que el flujo debería producir. Si un evento deja de publicarse, no aparece ninguna violación. | `EspiaAmqp.java`; `ContratosAmqp.java:65-76`; `FullDistributedDonationE2EIT.java:45-56`; `docs/arquitectura/contratos/schemas/` | Medio. Es la extensión natural de RM-08. |
| **DIT-03** | **La documentación promete más de lo que la suite prueba.** Según `integration-tests.md`, `ErrorHandlingIntegrationIT` cubre 409 y `fieldErrors` y `TracingContractIT` verifica la propagación. Los tests prueban 400 y 404 con `code` no nulo, y el header en `/v3/api-docs`. La propagación real es RM-12 (E7). | `integration-tests.md:56,63`; `ErrorHandlingIntegrationIT.java:23-45`; `TracingContractIT.java:16-37` | Bajo. Es deuda documental. |

`[INFERRED]` Fuera de alcance, ya registrado: un IT de aislamiento entre bases en el entorno integrado (RM-19 está implementado y verificado con Testcontainers por servicio, según [`preprod-fiel-roadmap.md`](preprod-fiel-roadmap.md) en la fila de estado de RM-19; ningún IT lo comprueba en preprod), traza AMQP de punta a punta (RM-12, E7), idempotencia, DLQ y desorden (RM-13 y DTI-22, E7), y el proveedor HTTP `externo`, que preprod no levanta ([`preprod-fiel.md`](preprod-fiel.md)).

---

## 3. Robustez

| Id | Hallazgo | Evidencia | Impacto |
|---|---|---|---|
| **DIT-04** | **Los tests comparten estado global y un fallo arrastra a los siguientes.** `[OBSERVED]` `ejecutarAsignacion` toma todas las necesidades activas sin satisfacer y todas las donaciones en depósito. `[INFERRED]` Si un test falla antes de aprobar su propuesta, deja una necesidad abierta en "No Perecederos" que compite en el matching del test siguiente. `testComplexE2EMultipleDonationsFlow` exige exactamente 2 fragmentaciones. El nombre único del bien no aísla: el algoritmo filtra por subcategoría. La planificación de Logística también es global. El orden de las clases cambia entre Windows y CI (§1). | `donaciones-service/.../PropuestaDeAsignacionService.java:74-91`; `AlgoritmoCompatibilidadSemantica.java:20`; `CrossServiceCommunicationIT.java:430`; `LogisticaIntegrationIT.java:90`; log de CI `40815880445` | Bajo-medio, latente. En el camino verde no se manifiesta: 3 corridas seguidas sobre el mismo stack pasaron (§1). El riesgo es que un fallo real aparezca como varios. Ya está planificado en RM-16 (E7): ejecución secuencial de los tests de estado global. |
| **AP-09** (desvío) | **Los timeouts están fijos y no coinciden con lo documentado.** `PollingUtils` usa 5 s, 8 s y 10 s fijos; la matriz de antipatrones declara 15 s. No hay un factor configurable para el stack híbrido o un runner lento. | `PollingUtils.java:23,53,84,114,146,178,219,251,291,321`; `auditoria/02-matriz-antipatrones.md:43` | Bajo hoy (§1). Medio para `--target hybrid`. |
| **DIT-05** | **Un sondeo crece con los datos.** `esperarEntregaCreadaParaDonacion` lista todas las entregas en cada intento, en lugar de filtrar por donación. | `PollingUtils.java:256` | Bajo |
| **DIT-12** (fase 2) | **Una entrega ajena bloquea la ruta entera.** `[OBSERVED]` El planificador pone todas las entregas sin ruta en el primer camión con capacidad. `VerificadorOrigenEventos` descarta el evento completo si una de sus donaciones no es del proveedor. `[INFERRED]` Por eso, una entrega creada por `POST /api/entregas` sin donación detrás hace que ninguna donación de esa ruta pase a `EN_TRASLADO`. `ContractIT` y `LogisticaIntegrationIT` dejaban entregas así pendientes. | `AsignadorDeEntregasPorDimension.java`; `PlanificacionService.java:107`; `VerificadorOrigenEventos.java` (`esOrigenValido`) | Alto para P1: el E2E fallaba según el orden de las clases. Se resolvió en los tests (§7). Que el servicio descarte el evento entero es una decisión de seguridad ("falla cerrado"); evaluarla queda fuera de este alcance. |
| **DIT-06** | **Los datos de prueba tienen poca entropía.** La patente del camión admite 900 valores; `randomDni` genera DNIs de longitud variable (7 a 9 dígitos). | `LogisticaIntegrationIT.java:123`; `TestIdGenerator.java:12-15` | Bajo. Solo afecta a un stack reutilizado. |

---

## 4. Calidad del código de test

| Id | Hallazgo | Evidencia | Propuesta |
|---|---|---|---|
| **DIT-07** | **Sondeos duplicados.** `PollingUtils` tiene 10 métodos casi iguales (≈345 líneas), cada uno con el mismo `try/catch` de diagnóstico. `esperarBajaDonanteEnIncentivos` (317-344) repite la condición del privado `esperarEstadoDonanteEnIncentivos` (213-243), que ya usa `esperarDonanteEliminadoEnIncentivos`, con 5 s en lugar de 10 s. | `PollingUtils.java` completo | Un método genérico `esperar(descripción, timeout, consulta, condición)` y los actuales como envoltorios de una línea. |
| **DIT-08** | **Escenarios repetidos.** "Persona + réplica + donante" aparece 8 veces. "Matching + buscar la propuesta + aprobar + transicionar fragmentaciones" aparece 3 veces, con unas 50 líneas cada una. | `CrossServiceCommunicationIT.java:144-233`, `:361-450`; `FullDistributedDonationE2EIT.java:62-166` | Un ayudante de escenarios en `utils/`, por ejemplo `crearDonante()` y `asignarYAprobar()`. |
| **DIT-09** | **Aserciones débiles o redundantes.** `DonationIntegrationIT` solo verifica que el id no sea nulo, y el flujo ya lo cubren otros tests. `PersonIntegrationIT` (humana) repite la réplica del ciclo de vida. El primer test de `TracingContractIT` es un caso del segundo. Las notificaciones se cuentan con `>= 3` o `>= 5`, sin mirar el tipo. | `DonationIntegrationIT.java:41`; `PersonIntegrationIT.java:19`; `TracingContractIT.java:16`; `CrossServiceCommunicationIT.java:239,454` | Fortalecer las aserciones (estado y tipo de notificación) en lugar de borrar tests; F-05 ya pide aserciones exactas. |
| **DIT-10** | **Tests sin stack dentro de Gate 3.** `testAllOpenApiSpecsLoadSuccessfully`, los pasos 1 a 4 del test adversarial y `testValidadorDeEventosAmqpAplicaLosSchemas` no usan servicios, pero solo corren con Docker. | `ContractIT.java:74-88`, `:178-246`, `:266-305` | Se dejan donde están: moverlos agrega una ejecución de Surefire al módulo y no gana nada medible (§5). Solo se registra. |

---

## 5. Costo y ejecución

| Medición | Valor | Fuente |
|---|---|---|
| Job `preprod-validation` en CI (3 corridas verdes del 2026-10-29) | 2:49 a 3:44 min | `gh run view` 40911114657, 40890907583, 40817582579 |
| Tests de Failsafe dentro del job | ≈25 s | log de CI 40815880445 |
| Ciclo local completo sin compilar (stack nuevo) | 242 a 277 s, de los cuales ≈27 a 50 s son de Failsafe | §1 |

**DIT-11 — el stack local no arranca de forma estable.** `[OBSERVED]` En 4 de 11 arranques locales del stack, sumando las fases 1 y 2 (Docker Desktop 29.8.2 sobre Windows), RabbitMQ terminó con `Error when reading /var/lib/rabbitmq/.erlang.cookie: eacces`. Compose marcó la dependencia como fallida y no corrió ningún test. El gate lo detectó bien: `total_tests == 0` falla. RabbitMQ no tiene volumen nombrado en `docker-compose.preprod.yml` y usa el volumen anónimo de la imagen. La causa raíz: `Insufficient evidence to determine`. Las corridas de CI (Linux) vistas no lo muestran. No es un problema de la suite y queda fuera del alcance de esta tarea, porque el compose es infraestructura.

`[INFERRED]` El costo lo domina el arranque del stack (imágenes, healthchecks, importación de n8n), no los tests.
Acelerar la suite no cambia el tiempo de CI de forma apreciable.
No se propone trabajo en este eje.
En las últimas 15 corridas de `main.yml`, `preprod-validation` falló una sola vez (40815880445), por una deriva real de contrato durante RM-08 (`ContractIT.testDonacionesPersonasContract`), no por un test inestable. Las demás corridas rojas fallaron en Sonar.

**Dependencias nuevas.** Ninguna propuesta de §6 necesita dependencias nuevas.
WireMock (QA-GAP-03, RM-10), Testcontainers (ADR de ambientes efímeros) y Toxiproxy (RM-18) ya tienen decisión o lugar en el roadmap.
Este diagnóstico no los reabre.

---

## 6. Propuestas para la fase 2 (gate)

Tal como se plantearon, solo cambian `integration-tests/` y la documentación de testing. La corrección de Logística se decidió después, en la fase 2 (§7).
Al momento del gate, ninguna creaba una decisión arquitectónica nueva: aplican patrones ya adoptados (clientes tipados, `PollingUtils`, espía RM-08).
No requieren ADR (Gate A de `AGENTS.md` §9.1).

| Prioridad | Propuesta | Cierra | Tamaño `[INFERRED]` |
|---|---|---|---|
| **P1** | **E2E por el camino real de Logística.** Después de aprobar: crear camión y chofer, ejecutar la planificación manual, esperar la ruta, pasarla a `EN_TRASLADO` y la entrega a `ENTREGADA`. Después, esperar en Donaciones `LISTA_PARA_ENTREGAR`, `EN_TRASLADO` y `ENTREGADA`, aplicados por los eventos, y en Incentivos la donación exitosa. Corregir el comentario de Feign. | F-01 | 1 test reescrito, 1 o 2 métodos de `LogisticaApiClient` y 1 sondeo |
| **P2** | **Camino de falla entre servicios.** Entrega `NO_RECIBIDA` en Logística → `ENTREGA_FALLIDA` en Donaciones y su notificación. | QA-GAP-05 (parcial) | 1 test nuevo |
| **P3** | **Refactor de `PollingUtils` y ayudante de escenarios**, sin cambiar lo que se verifica. Incluye un factor de timeout configurable (`-Dit.timeout.factor`, por defecto 1). | DIT-07, DIT-08, AP-09 | 2 utilidades y 4 clases IT |
| **P4** | **Cobertura de contratos AMQP en el espía.** Reportar qué tipos `evento-*` se observaron y exigir los que produce el flujo E2E (`donacion.asignada`, `entrega.solicitada`, `ruta.asignada`, `entrega.exitosa`, `donacion.recibida`). | DIT-02 | `ContratosAmqp` y el E2E |
| **P5** | **Aserciones y documentación.** Tipo de notificación en lugar de conteo; código de error esperado; `integration-tests.md` alineado con lo que se prueba (409, `fieldErrors`, propagación, timeouts, recuento). | DIT-03, DIT-09 | 3 o 4 clases y 1 doc |

**Para después de esta tarea:**

- DIT-01 (Incentivos por AMQP en los tests de misiones): con P1, el E2E ya ejercita `donacion.recibida.v1` hacia Incentivos. Mover misiones y ranking a la entrada real exige donaciones entregadas en tres meses distintos; queda por evaluar si alcanza con fijar la fecha de la donación (`DonacionTestDataBuilder.conFecha`).
- DIT-04 (estado global): RM-16, en E7.
- DIT-05, DIT-06 y DIT-10: bajo impacto; quedan registrados aquí.

### 6.1 Alternativas para P1

| Criterio (peso) | A. Manejar Logística por su API REST | B. Publicar los eventos de Logística desde el test | C. Dejar el atajo REST |
|---|---|---|---|
| Fidelidad al flujo de producción (0,4) | 5: pasa por el publicador de Logística sobre MongoDB, el broker, la verificación de origen y el listener | 3: se saltea Logística y su persistencia | 1 |
| Robustez (0,3) | 3: depende de la planificación global (DIT-04) | 4 | 5 |
| Costo de mantenimiento (0,2) | 4: usa clientes y endpoints que ya prueba `LogisticaIntegrationIT` | 2: el test tiene que conocer el token del proveedor y construir los mensajes | 5 |
| Diagnóstico ante un fallo (0,1) | 4: cada espera nombra el estado y el servicio | 3 | 2 |
| **Total** | **4,1** | 3,1 | 3,1 |

**Recomendada: A.**
B repetiría lo que ya cubren los tests de componente de Donaciones (`LogisticaEventListenerTest`, `ProcesadorEventosLogisticaTest`).
C deja F-01 abierto.

---

## 7. Fase 2: lo implementado

En el gate se eligieron P1 a P5. Durante la implementación se tomaron dos decisiones más:

- **DIT-12:** ningún test deja entregas ajenas pendientes.
- **Contratos de Logística:** se corrige el productor.

| Propuesta | Qué cambió | Archivos |
|---|---|---|
| P1 (F-01) | El E2E ya no cambia estados por REST. Logística crea la entrega desde el comando AMQP, planifica, saca la ruta y la entidad confirma. Donaciones pasa por `ListaParaEntregar`, `EnTraslado` y `Entregada` aplicando los eventos. Antes de iniciar la ruta, el test espera `ListaParaEntregar`, porque las colas de `ruta.asignada` y `ruta.iniciada` no garantizan orden. | `FullDistributedDonationE2EIT`, `LogisticaApiClient.confirmarEntrega`, `DonacionesApiClient.obtenerDonacionIndependiente` |
| P2 (QA-GAP-05) | Test nuevo: la entrega llega `NO_RECIBIDA` sin replanificar, Donaciones la pasa a `EntregaFallida` y el donante recibe una notificación con el motivo. | `FullDistributedDonationE2EIT` |
| P3 (DIT-07, DIT-08, AP-09) | `PollingUtils` queda con un solo método genérico y un factor de timeout `-Dit.timeout.factor`. Se borraron `esperarDonanteEliminadoEnIncentivos` y `esperarDonacionEstado`, que no tenían usos, y se sumaron `esperarDonacionIndependienteEstado` y `esperarNotificacionCon`. Escenarios compartidos en `BaseIT`: `crearDonante`, `crearEntidad`, `asignarYAprobar` y `entregarPorRest`. Datos de Logística en `LogisticaTestData`. | `PollingUtils`, `BaseIT`, `CrossServiceCommunicationIT`, `LogisticaTestData` |
| P4 (DIT-02) | Que falte uno de los 9 tipos de evento que publica el flujo del E2E es una violación de contrato, con la misma política que las demás: aviso en PR y fallo con `CONTRATOS_ESTRICTO=true`. El log lista los schemas que el E2E no ejercitó. | `ContratosAmqp.faltantes`/`sinEjercitar`, `FullDistributedDonationE2EIT` |
| P5 (DIT-03, DIT-09) | En los flujos de una sola donación, las notificaciones se verifican por evento (asignada, en camino, recibida, bienvenida, motivo de falla), no por cantidad. `testComplexE2EMultipleDonationsFlow` sigue contando (`>= 5`), porque son dos donaciones. Los errores se verifican por código del `ErrorCatalog` (`ERR-CSR-001`, `ERR-CSR-003`). `DonationIntegrationIT` verifica la persistencia con su donante. `integration-tests.md` quedó alineado con lo que se prueba. | `ErrorHandlingIntegrationIT`, `DonationIntegrationIT`, `integration-tests.md` |
| DIT-12 | `LogisticaIntegrationIT` lleva su entrega sin donación hasta `ENTREGADA` y absorbe el contrato vivo del `POST /api/entregas`, que estaba en `ContractIT`. | `LogisticaIntegrationIT`, `ContractIT` |
| Contratos de Logística | `[VERIFIED]` Con P4, el espía capturó por primera vez los eventos de vuelta de Logística: `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa` y `entrega.fallida` violaban sus schemas, porque publicaban `LocalDateTime` sin zona y el schema exige `date-time` RFC 3339. Se aplicó la convención de Donaciones: `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")`. `[VERIFIED]` Donaciones lee las fechas de los 4 eventos con y sin `Z`, así que los mensajes en vuelo siguen sirviendo. | 4 eventos en `logistica-service/.../dto/eventos/`; `EventosAmqpSerializacionTest` (Logística); `EventosLogisticaDeserializacionTest` (Donaciones); ejemplos de `docs/arquitectura/eventos-amqp.md` |

**Validación de la fase 2:**

| Gate | Comando | Resultado |
|---|---|---|
| Gate 3/4, aviso | `./run-preprod-tests.sh --skip-build` (con la imagen de Logística corregida) | `[VERIFIED]` 30/30 en verde, 0 salteados, sin avisos de contratos AMQP. El E2E vio los 8 tipos esperados (`entrega.solicitada.v1`, el noveno, se sumó después de la auditoría de cierre; `[INFERRED]` ya se publicaba, porque su schema no figuraba entre los no ejercitados). |
| Gate 3/4, estricto | `CONTRATOS_ESTRICTO=true ./run-preprod-tests.sh --skip-build` | `[VERIFIED]` 30/30 en verde al primer intento. |
| Gate 2, Logística | `mvn clean test -pl logistica-service -am` | `[VERIFIED]` 525 tests, 0 fallos (1 salteado preexistente). |
| Gate 2, Donaciones | `mvn clean test -pl donaciones-service -am` | 780 tests. 15 clases con contenedores fallaron por timeout de arranque: Docker Desktop no alcanzó a levantar a tiempo los contenedores en paralelo. `[VERIFIED]` Reintentadas con el reactor, las 15 y el test nuevo dieron 120/120 en verde. |
| Formato | `mvn spotless:check` en los 3 módulos tocados | `[VERIFIED]` sin violaciones |

`[OBSERVED]` Durante la fase 2, DIT-11 volvió a cortar 2 arranques del stack antes de correr un solo test. Ninguna corrida válida falló por inestabilidad de la suite.

**Lo que no se cubre:**

- DIT-01: misiones y ranking siguen entrando por el atajo REST.
- DIT-04: el estado global queda para RM-16.
- DIT-05, DIT-06 y DIT-10.
- DIT-11: el arranque de RabbitMQ en Docker Desktop.
- La política de descartar el evento entero (DIT-12, del servicio).
