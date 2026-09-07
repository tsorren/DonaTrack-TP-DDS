# Auditoría Final DonaTrack — Post-Refactor
**Fecha:** 2026-08-28
**Branch:** E4_refactor_correccion_test
**Auditor:** Claude Code (análisis estático)

---

## 1. Resumen Ejecutivo

Los siguientes 20 hallazgos representan los puntos más relevantes del estado actual del proyecto, ordenados por criticidad:

1. **[CRÍTICO] notificaciones-service no compila desde cero** — `MedioDeContactoMapper.java:39,77` referencia `ErrorCatalog.MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO` que sí existe en common-lib, pero el servicio falla al compilar si common-lib no está previamente instalado en el repositorio local Maven. El problema es de orden de build, no de código, pero `mvn clean test -pl notificaciones-service` sin instalar common-lib primero da BUILD FAILURE. Post-instalación de common-lib, el módulo pasa todos sus 232 tests.

2. **[CRÍTICO] incentivos-service — test no compila si common-lib no está instalado** — `AsyncConfigTest.java:5` importa `MdcTaskDecorator` de `grupo5.common.logging`. El mismo problema de orden de build que el anterior. Con `mvn install -pl common-lib` previo, todos los 378 tests pasan correctamente.

3. **[ALTO] Acoplamiento Feign bidireccional donaciones→incentivos** — `SegmentacionEventListener` y `DonacionIndependienteNotificacionesListener` usan `IncentivosFeignClient` (HTTP sync) para notificar donaciones registradas y exitosas. Esto crea acoplamiento temporal: si incentivos-service está caído, la segmentación puede fallar silenciosamente (hay try/catch pero no hay retry guaranteed). El error se absorbe con `log.error` sin re-lanzar.

4. **[ALTO] Mezcla de mecanismos de comunicación donaciones→servicios** — Donaciones usa Feign (HTTP sync) hacia incentivos y notificaciones en `SegmentacionEventListener` y `DonacionIndependienteNotificacionesListener`, mientras que logística usa RabbitMQ para comunicar eventos de vuelta a donaciones. Esta asimetría es deliberada (donaciones es productor principal) pero no está documentada en ADR.

5. **[ALTO] `SegmentacionEventListener` tiene lógica de negocio no trivial en capa de infraestructura** — El listener hace resolución de nombre de persona, búsqueda de donante, categorías, y llama directamente a `incentivosFeignClient.procesarDonacion(...)`. Estas responsabilidades de orquestación deberían estar en un application service, no en un listener de Spring.

6. **[MEDIO] `EstadoDonacion` de Donacion es solo enum simple** — `Donacion.java` tiene un enum con 3 estados (CARGADA, NORMALIZADA, SEGMENTADA) con transiciones validadas inline. No implementa el patrón State como `DonacionIndependiente`, pero la complejidad menor lo justifica. Es inconsistente en nomenclatura con el patrón más rico de `DonacionIndependiente`.

7. **[MEDIO] `NecesidadesService.java:19` tiene `// TODO:` vacío sin descripción** — No es bloqueante pero indica deuda técnica sin describir. HECHO: confirmado en el archivo.

8. **[MEDIO] `application.properties` de donaciones-service usa `//` para comentarios Java en lugar de `#`** — Línea 9: `// TODO: Utilizar variables de entorno`. El comentario con `//` es sintácticamente inválido en formato `.properties` y podría ser ignorado o causar problemas en algunos parsers.

9. **[MEDIO] logistica-service no tiene FeignClient — el acoplamiento es únicamente vía RabbitMQ** — POSITIVO: logística no invoca directamente donaciones, incentivos ni notificaciones. Solo publica eventos al exchange `logistica.exchange` y donaciones consume. Correcto según DDD.

10. **[MEDIO] Namespacing de endpoints inconsistente** — `DonacionesIndependientesController` está mapeado en `/donaciones-independientes` (sin prefijo `/api/`), mientras todos los demás controllers usan `/api/`. Podría causar problemas en un API gateway o con las URL del FeignClient de logistica que apunta a `/api/entregas`.

11. **[MEDIO] `DonacionIndependienteNotificacionesListener` tiene fan-in excesivo** — Depende de 7 repositorios/services distintos para resolver contexto. Es un listener que hace resolución manual de datos cruzados (donante → persona → beneficiaria → etc). Señal de que falta un read model o projection.

12. **[MEDIO] Adapters de notificaciones son interfaces vacías sin implementación real** — `CorreoAdapter`, `TelefonoAdapter` y `WhatsAppAdapter` son interfaces con firmas pero sin implementaciones inyectables concretas visibles en el código auditado (más allá del `NotificacionRouter`). Si solo existe el Router como implementación, los adapters reales están simulados o sin completar.

13. **[BAJO] Scheduler `PlanificadorDeAlgoritmosTest` en donaciones** — El scheduler de asignación de algoritmos en donaciones-service ejecuta periódicamente pero no hay evidencia de que el resultado se persista correctamente junto con los domain events de `Propuesta`.

14. **[BAJO] `GestorPropuestasDeAsignacion.consolidar()` tiene lógica de priorización no documentada** — La lógica de consolidación (si la propuesta2 cubre necesidades de propuesta1, se usa propuesta2; si no, se suman) no está explicada en comentarios. Puede ser una decisión de negocio válida pero opaca.

15. **[BAJO] `PlanificadorDeRutas` en logistica usa `LocalDate.now()` sin zona horaria** — Línea 63: `LocalDate.now()` sin `ZoneId`. El resto del código usa `ZoneId.of("UTC")` explícitamente. Inconsistencia menor.

16. **[BAJO] Falta colección Postman** — No existe ningún archivo `.postman_collection.json` en el repositorio. La documentación de API depende completamente de Swagger, que sí está configurado vía `DonaTrackOpenApiAutoConfiguration`.

17. **[BAJO] `RankingMensual` no extiende `AgregadoConEventos`** — Implementa `AggregateRoot` directamente sin eventos. Consistente con su uso solo como value object de consulta, pero podría ser necesario emitir un evento cuando el ranking es calculado.

18. **[BAJO] Idempotencia no garantizada en listeners RabbitMQ** — `LogisticaEventListener` en donaciones-service no tiene mecanismo de deduplicación de mensajes. Si RabbitMQ re-entrega un mensaje, el estado puede avanzar dos veces desde LISTA_PARA_ENTREGAR a EN_TRASLADO, resultando en `BusinessStateException` que se absorbe silenciosamente.

19. **[BAJO] `Entrega.mandarARevision()` es privado y se llama automáticamente desde `negarEntrega()`** — La máquina de estados de Entrega pasa de NO_RECIBIDA a REVISION automáticamente. Esto está documentado implícitamente en el código pero sin explicación del porqué.

20. **[INFORMATIVO] Excelente cobertura de tests unitarios** — donaciones: 790 tests, logistica: 582 tests, incentivos: 378 tests, notificaciones: 232 tests, common-lib: 38 tests. Total: ~2020 tests sin failures ni errores. La cobertura funcional es alta.

---

## 2. Estado del Build

| Módulo | Compila | Tests | Tests Run | Failures | Observaciones |
|--------|---------|-------|-----------|----------|---------------|
| common-lib | SI | SI | 38 | 0 | BUILD SUCCESS. Incluye tests de AgregadoConEventos, GlobalExceptionHandler, CrudRepositoryEnMemoria, logging |
| donaciones-service | SI | SI | 790 | 0 | BUILD SUCCESS. 80+ clases de test con fixtures/mothers. RabbitMQ no disponible en test, manejado con try/catch |
| notificaciones-service | FALLA sin common-lib instalado | SI (post-fix) | 232 | 0 | FALLA si common-lib no está en repo local Maven. Causa: MedioDeContactoMapper referencia ErrorCatalog.MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO. Post `mvn install -pl common-lib`: BUILD SUCCESS |
| incentivos-service | SI | FALLA sin common-lib instalado | 378 | 0 | Test compile falla por AsyncConfigTest importando MdcTaskDecorator. Post `mvn install -pl common-lib`: BUILD SUCCESS con 378 tests |
| logistica-service | SI | SI | 582 | 0 | BUILD SUCCESS. 1 test skipped en AlgoritmoAsignadorDeEntregaTest |

**Nota:** La causa raíz del problema de build de notificaciones e incentivos es que el build secuencial por módulo (`-pl`) no instala automáticamente las dependencias locales. `mvn install` en el root o `mvn install -pl common-lib` resuelve el problema. Esto no es un bug de código sino de workflow de desarrollo/CI.

---

## 3. Arquitectura Real

```text
[Frontend / Postman / Auth externo]
         ↓ HTTP
[donaciones-service :8080]
    |-- Feign (sync) --> [incentivos-service :8082] /api/incentivos/donaciones
    |-- Feign (sync) --> [incentivos-service :8082] /api/incentivos/donantes/{id}
    |-- Feign (sync) --> [notificaciones-service :8081] /notificaciones
    |-- Feign (sync) --> [notificaciones-service :8081] /api/notificaciones/personas
    |-- Feign (sync) --> [logistica-service :8083] /api/entregas
    |
    |--> RabbitMQ exchange: logistica.exchange
              ↓ (consume)
    [logistica-service :8083]
         |-- RabbitMQ pub --> logistica.exchange (ruta.asignada, ruta.iniciada, entrega.exitosa, entrega.fallida)
         |-- HTTP (externo) --> proveedor planificador de rutas (callback pattern)
              ↓ (consume)
    [donaciones-service :8080] (LogisticaEventListener)

[incentivos-service :8082]
    |-- Feign (sync) --> [notificaciones-service :8081] (NotificacionesClientAdapter, @Async)
    |-- WebClient (async fire-and-forget) --> [n8n :5678] webhooks

[n8n :5678]
    -- workflow externo de notificaciones push

[RabbitMQ :5672/:15672]
    -- exchange: logistica.exchange (topic)
    -- routing keys: ruta.asignada, ruta.iniciada, entrega.exitosa, entrega.fallida
```

**Dependencias reales encontradas:**

| Productor | Consumidor | Mecanismo | Dirección |
|-----------|-----------|-----------|-----------|
| donaciones → incentivos | Feign HTTP sync | IncentivosFeignClient | donaciones llama a incentivos para registrar donaciones y donantes |
| donaciones → notificaciones | Feign HTTP sync | NotificacionesFeignClient | donaciones notifica eventos de entrega y sincroniza personas |
| donaciones → logistica | Feign HTTP sync | LogisticaFeignClient | donaciones registra entregas pendientes |
| logistica → donaciones | RabbitMQ async | LogisticaEventPublisher → LogisticaEventListener | logistica publica eventos, donaciones consume |
| incentivos → notificaciones | Feign HTTP sync @Async | NotificacionesClientAdapter | incentivos notifica misiones y ascensos |
| incentivos → n8n | WebClient async fire-and-forget | N8nClientAdapter | insignias y rankings |
| logistica → (ninguno directo) | — | No usa Feign hacia otros servicios | CORRECTO: acoplamiento cero hacia donaciones/incentivos/notificaciones |

---

## 4. Cumplimiento Funcional

| Requisito | Servicio | Implementación | Endpoint/Event | Tests | Estado |
|-----------|----------|----------------|----------------|-------|--------|
| Gestión donantes | donaciones | Donante, PersonasService, DonantesService | POST/GET/DELETE /api/donantes, POST/GET/PUT/DELETE /api/personas | SI | OK |
| Donaciones (original) | donaciones | Donacion.java (AgregadoConEventos) | POST/GET /api/donaciones | SI | OK |
| DonacionIndependiente | donaciones | DonacionIndependiente.java (State pattern) | PATCH /donaciones-independientes/{id}/estado | SI | OK |
| Separación Donacion/DonacionIndependiente | donaciones | Dos aggregates distintos con ciclos de vida independientes | — | SI | OK |
| Importación CSV masiva | donaciones | LectorDonantesCSV + ImportadorService | POST /api/donantes/archivos | SI (6 tests CSV) | OK |
| Entidades beneficiarias | donaciones | EntidadBeneficiaria.java | POST/GET/PUT/DELETE /api/entidades | SI | OK |
| Necesidades recurrentes | donaciones | NecesidadRecurrente.java + PeriodoNecesidad | POST /api/necesidades | SI | OK |
| Necesidades extraordinarias | donaciones | NecesidadExtraordinaria.java implementa Asignable | POST /api/necesidades | SI | OK |
| Segmentación | donaciones | Segmentador port, SegmentacionEventListener | Automático post-normalización | SI | OK |
| Trazabilidad de estados | donaciones | historialEstados en Donacion, historial en DonacionIndependiente | GET /api/donaciones/{id} | SI | OK |
| Algoritmos de asignación | donaciones | AlgoritmoAsignacion abstract + 2 impl Strategy | POST /api/asignaciones/ejecuciones | SI (14 tests algoritmos) | OK |
| Flota de camiones | logistica | Camion.java (AggregateRoot, state machine) | POST/GET/PATCH/DELETE /api/camiones | SI (23 tests Camion) | OK |
| Planificación de rutas | logistica | GeneradorDeRutas + PlanificadorDeRutas + proveedor externo | Scheduler + callback /api/logistica/resultados | SI | OK |
| Lotes ≤ 100 entregas | logistica | GeneradorDeRutas.MAX_ENTREGAS_POR_SOLICITUD=100 | Configurado en properties | SI | OK |
| Callback externo | logistica | PlanificacionController.procesarCallback() | POST /api/logistica/resultados | SI | OK |
| Inicio de ruta | logistica | Ruta.iniciarRuta() + evento EventoRutaIniciada | PATCH /api/rutas/{id}/estado | SI | OK |
| Entrega exitosa | logistica | Entrega.confirmarEntrega() + EntregaConfirmada event | PATCH /api/entregas/{id}/estado | SI | OK |
| Entrega fallida/no recibida | logistica | Entrega.negarEntrega() + EntregaFallida event | PATCH /api/entregas/{id}/estado | SI | OK |
| Replanificación | logistica | Entrega.regresarAlDeposito() → vuelve a PENDIENTE | PATCH /api/entregas/{id}/estado estado=PENDIENTE | SI | OK |
| Misiones secuenciales | incentivos | Mision abstract + MisionFactory.crearMisionesEstandar() | Automático via procesarDonacion | SI (22 tests misiones) | OK |
| Categorías donante | incentivos | CategoriaDonante enum (COLABORADOR/SOSTENEDOR/TRANSFORMADOR) | Automático via ascender() | SI | OK |
| Insignias | incentivos | Insignia + InsigniaGanada + configurarVisibilidad | GET/PUT /api/incentivos/donantes/{donanteId}/insignias | SI | OK |
| Ranking mensual | incentivos | GestorDeRankings + RankingService + RankingMensualJob | POST /api/incentivos/ranking/calcular + scheduler cron | SI (13 tests ranking) | OK |
| Publicación N8N | incentivos | N8nClientAdapter WebClient async | Automático via evento MisionCompletada y RankingMensualJob | SI | OK |
| Inactividad | incentivos | InactividadJob + InactividadService (verifica dias sin actividad) | Scheduler diario 8AM | SI (12 tests inactividad) | OK |
| Strategy notificación | notificaciones | NotificacionSender interface + NotificacionRouter impl | Interno | SI | OK |
| Email/SMS/WhatsApp | notificaciones | CorreoAdapter, TelefonoAdapter, WhatsAppAdapter interfaces | Interno via MedioDeContacto | SI parcial (interfaces sin impl concreta observable) | REVISAR |
| Eventos mínimos notificados | notificaciones | 8 EventoNotificable: DonacionAsignada, DonacionEnCamino, DonacionRecibida, DonanteInactivo, DonanteRegistrado, EntregaFallida, MisionCumplida, SubioCategoria | POST /notificaciones | SI | OK |

---

## 5. Auditoría por Servicio

> **CHECKPOINT 2 — Análisis profundo de dominio (2026-08-28)**

### 5.1 Donaciones

**Aggregate Roots identificados:**

| Clase | Extiende | Domain Events | Repositorio |
|-------|----------|---------------|-------------|
| Donacion | AgregadoConEventos<EventoDonacion> | DonacionCargada, DonacionNormalizada, DonacionSegmentada | IDonacionesRepository |
| DonacionIndependiente | AgregadoConEventos<EventoDonacionIndependiente> | EventoDonacionAsignada, EventoRutaIniciada, EventoDonacionRecibida, EventoDonacionFallida | IDonacionesIndependientesRepository |
| Propuesta | AgregadoConEventos<PropuestaAprobada> | PropuestaAprobada | IPropuestasRepository |
| EjecucionAsignacion | implements AggregateRoot | — | IAsignacionesRepository |
| Donante | implements AggregateRoot | — | IDonantesRepository |
| EntidadBeneficiaria | implements AggregateRoot | — | IEntidadesBeneficiariasRepository |
| Necesidad (abstract) | implements AggregateRoot + Asignable | — | INecesidadesRepository |

**Domain Events vs Integration Events:**

| Evento | Clase base | Publicado via | Consumidor | Notas |
|--------|-----------|---------------|------------|-------|
| DonacionCargada | EventoDonacion extends EventoDeDominio | ApplicationEventPublisher (DonacionesService) | Sin listener registrado visible | HECHO: se genera pero no se consume. Puede ser intencional para uso futuro |
| DonacionNormalizada | EventoDonacion | ApplicationEventPublisher | SegmentacionEventListener | Dispara segmentación |
| DonacionSegmentada | EventoDonacion | ApplicationEventPublisher | Sin listener registrado visible | HECHO: igual que DonacionCargada |
| PropuestaAprobada | EventoDeDominio | ApplicationEventPublisher (PropuestaDeAsignacionService.actualizarEstado) | PropuestaDeAsignacionService.onPropuestaAprobada @EventListener | HECHO confirmado: el servicio se escucha a sí mismo via ApplicationEventPublisher |
| EventoDonacionAsignada | EventoDonacionIndependiente | ApplicationEventPublisher (EnDeposito.asignar) | DonacionIndependienteNotificacionesListener | Emitido dentro del State object |
| EventoRutaIniciada (DI) | EventoDonacionIndependiente | ApplicationEventPublisher (ListaParaEntregar.iniciarRecorrido) | DonacionIndependienteNotificacionesListener | urlMapa propagado |
| EventoDonacionRecibida | EventoDonacionIndependiente | ApplicationEventPublisher (EnTraslado.confirmarEntrega) | DonacionIndependienteNotificacionesListener | patenteCamion propagado |
| EventoDonacionFallida | EventoDonacionIndependiente | ApplicationEventPublisher (EnTraslado.registrarFalla) | DonacionIndependienteNotificacionesListener | replanificable: Boolean nullable |

HECHO: Los domain events de `DonacionIndependiente` son emitidos **dentro de los State objects** (EnDeposito, ListaParaEntregar, EnTraslado), no en el aggregate directamente. Esto es correcto — el State object llama `d.registrarEvento(...)` usando package-private access. El aggregate acumula los eventos y el service los publica post-persistencia.

OBSERVACION: Los events de DonacionIndependiente (EventoRutaIniciada, EventoDonacionRecibida, EventoDonacionFallida) se publican con ApplicationEventPublisher y se consumen con @EventListener dentro del mismo proceso. No son eventos RabbitMQ. Mezcla la semántica de domain event intra-proceso con integration event. Válido para MVP.

HECHO NUEVO: `PropuestaDeAsignacionService` tiene un `@EventListener` propio (`onPropuestaAprobada`) que escucha el evento que él mismo publica en `actualizarEstado()`. Este patrón de auto-escucha via ApplicationEventPublisher es válido en Spring y permite desacoplar la publicación de la reacción (sin Feign síncrono en el mismo thread de request). La secuencia es: controller llama `actualizarEstado()` → propuesta emite `PropuestaAprobada` → service publica → Spring llama `onPropuestaAprobada` → fragmentaciones y registro en logística.

**Hallazgos domain model (análisis línea a línea):**

- `Donacion.java:42-56` — BIEN — Constructor valida `donanteId != null`, genera UUID propio, registra `DonacionCargada` en la lista de eventos. `fecha` usa `ZoneId.systemDefault()` (no UTC como el resto del código) — menor inconsistencia de timezone.
- `Donacion.java:87-101` — BIEN — `marcarNormalizada()` y `marcarSegmentada()` validan la transición de estado antes de avanzar. La validación inline es correcta para 3 estados sin behavior diferencial por estado.
- `DonacionIndependiente.java:33-45` — ATENCIÓN — El constructor valida `donacionOriginalId == null` **después** de asignarlo a `this.donacionOriginalId`. La excepción se lanza correctamente porque la validación ocurre antes del resto de la construcción, pero el orden de asignación y validación es confuso (líneas 34 y 36-38).
- `DonacionIndependiente.java:75-96` — REVISAR — `fragmentarse()` usa `items.getFirst()` (Java 21 SequencedCollection) y hace mutación de la lista interna. La lógica es correcta pero verbosa. `cantidadPorExtraer` es `Integer` (boxed) y se decrementa con `-=` aplicado a `itemExtraido.cantidad()` **después** de potencialmente haber sido reemplazado, lo cual funciona pero requiere leer con atención.
- `DonacionIndependiente.java:108-121` — EXCELENTE — `cambiarEstado(SolicitudCambioEstadoDonacionIndependiente)` usa switch exhaustivo y delega a `estadoActual`. El State pattern está correctamente implementado.
- `DonacionIndependiente.java:166-170` — CORRECTO — El método `cambiarEstado(EstadoDonacionIndependiente, String, String)` tiene visibilidad `package-private` (no `public`), lo que fuerza a que solo los State objects del mismo paquete puedan llamarlo. Encapsulamiento correcto.
- `EstadoDonacionIndependiente.java` — EXCELENTE — Interface con default methods que lanzan `BusinessStateException`. Cada estado solo implementa las transiciones que le aplican. Patrón State completo con 7 estados concretos: EnDeposito, AsignacionRealizada, ListaParaEntregar, EnTraslado, Entregada (terminal), EntregaFallida, Vencida (terminal).
- `EnTraslado.java:46-63` — ATENCIÓN — `registrarFalla()` recibe `Boolean replanificable` (nullable). Si `Boolean.TRUE.equals(replanificable)`, llama a `d.replanificar(actor)` inmediatamente **después** de cambiar estado a `EntregaFallida` y registrar el evento. Esto produce una doble transición de estado en una sola operación: ENTREGA_FALLIDA → ASIGNACION_REALIZADA. El historial registra ambos cambios. Es funcionalmente correcto pero no está documentado.
- `EntidadBeneficiaria.java:28-30` — OK CON RESERVA — `anonimizar()` vacío con comentario "Coordinado a nivel de servicio de aplicación". El contrato existe pero la implementación está delegada sin enforcement. No hay mecanismo que garantice que el service llame a los métodos adecuados.
- `NecesidadRecurrente.java:64-71` — BIEN — `asignarDonacion()` usa el record inmutable `PeriodoNecesidad` de forma correcta: crea un nuevo PeriodoNecesidad y reemplaza el actual en la lista en lugar de mutar.
- `NecesidadExtraordinaria.java` — ATENCIÓN — Implementa `Asignable` aunque `Necesidad` ya implementa `Asignable`. La doble declaración `implements Asignable` es redundante pero no incorrecta. `isActiva()` retorna siempre `true` sin lógica condicional — no hay concepto de "vencimiento" para necesidades extraordinarias.
- `AlgoritmoAsignacion.java:20` — DEUDA — `if (propuestas.size() >= 10) break;` hardcoded. El límite de 10 propuestas por ejecución no es configurable via properties ni constructor.
- `GestorPropuestasDeAsignacion.consolidar()` — HECHO — La lógica de consolidación: si hay propuestas que satisfacen la misma necesidad en ambos algoritmos (`propuestasEnAmbos`), se devuelven solo las del algoritmo 2 (PrioridadSubAtendidos). Si no hay intersección, se devuelven todas las propuestas de ambos algoritmos concatenadas. INFERENCIA: la intención es que cuando ambos algoritmos coinciden en una necesidad, el algoritmo de prioridad es "más inteligente" y se prefiere.
- `StockDeDonaciones.java` — BIEN — Value object mutable que rastrena disponibilidad virtual durante el algoritmo. Usa `HashMap<DonacionIndependiente, Integer>` con identidad de objeto como clave. Correcto dado que los objetos son los mismos en memoria durante la ejecución del algoritmo.

**Separación Donacion != DonacionIndependiente:**

HECHO: Están completamente separadas con aggregates, repositorios, DTOs y controllers independientes. `Donacion` es el aggregate de ingreso con 3 estados lineales (CARGADA→NORMALIZADA→SEGMENTADA). `DonacionIndependiente` es el resultado de la segmentación con ciclo de vida propio y State pattern con 7 estados. El vínculo entre ambos es `donacionOriginalId` (UUID) — referencia por id, sin acoplamiento de objeto. La separación es semánticamente correcta: una `Donacion` puede dar origen a múltiples `DonacionIndependiente` mediante `fragmentarse()`.

**Algoritmos de asignación:**

HECHO: `AlgoritmoAsignacion` es clase abstracta con Template Method (`ejecutar`). El template method llama a `ordenarNecesidades()` (hook, sobreescribible) y `filtrarDonaciones()` (abstracto). Dos implementaciones concretas:
- `AlgoritmoCompatibilidadSemantica`: filtra por subcategoría y ordena por score de palabras en común usando `ComparadorTexto`.
- `AlgoritmoPrioridadSubAtendidos`: reordena las necesidades por cantidad de donaciones recientes a la entidad (en los últimos 3 meses), priorizando las menos atendidas.

El patrón Strategy está bien aplicado. `GestorPropuestasDeAsignacion` ejecuta ambos y consolida. La inyección de `List<AlgoritmoAsignacion>` en el constructor del gestor permite agregar nuevos algoritmos sin modificar el gestor.

**Estado de los listeners de infraestructura:**

- `SegmentacionEventListener` (9 dependencias inyectadas): Escucha `DonacionNormalizada`, resuelve ítems aceptados, invoca `segmentador.segmentar()`, llama a `incentivosFeignClient.procesarDonacion()` con nombre de persona resuelto (Humana: nombre+apellido, Juridica: razonSocial). Lógica de orquestación extensa en capa de infraestructura. HECHO: hay try/catch que absorbe excepciones de incentivos silenciosamente.
- `DonacionIndependienteNotificacionesListener` (6 dependencias inyectadas): Escucha 3 eventos de DI. Para `EventoDonacionRecibida`, llama a `incentivosFeignClient.procesarDonacionExitosa()` Y `notificacionesFeignClient.enviarEvento()`. Para `EventoRutaIniciada` y `EventoDonacionFallida`, solo llama a notificaciones. El fanout de resolución de IDs (donacion → donante → persona; necesidad → entidad → juridicaId) se hace con cadenas de Optional.

**Hallazgos CSV:**

HECHO: `LectorDonantesCSV` implementa `CargadorDonantes` port. Maneja BOM UTF-8, ignora líneas malformadas con log.warn, delega parsing a `DonanteParser`. Tiene 6 tests de unidad. Implementación robusta.

---

### 5.2 Logística

**Aggregate Roots:**

| Clase | Extiende | Domain Events | Repositorio |
|-------|----------|---------------|-------------|
| Ruta | AgregadoConEventos<EventoRuta> | EventoRutaAsignada (por cada entrega), EventoRutaIniciada | IRutasRepository |
| Entrega | AgregadoConEventos<EventoEntrega> | EntregaConfirmada, EntregaFallida | IEntregasRepository |
| Camion | implements AggregateRoot | — (máquina de estados sin events) | ICamionRepository |
| Chofer | implements AggregateRoot | — | IChoferesRepository |
| SolicitudPlanificacion | implements AggregateRoot | — | ISolicitudPlanificacionRepository |

**Hallazgos domain model (análisis línea a línea):**

- `Ruta.java:73-86` — BIEN — `agregarEntrega()` valida estado PENDIENTE, no duplicados, emite `EventoRutaAsignada` **por cada entrega agregada**. El evento contiene `rutaId` y `entregaId`. Esto significa que si una ruta tiene 5 entregas, se emitirán 5 `EventoRutaAsignada`. Correcto para el listener de donaciones que necesita asociar cada donación a su ruta.
- `Ruta.java:52-61` — BIEN — `iniciarRuta()` valida que el estado sea PENDIENTE y que haya al menos una entrega (`this.entregas.isEmpty()` checked). Usa `ZoneId.of("UTC")` — consistente con el resto de logistica.
- `GestorDeRutas.java` — EXCELENTE — Clase utilitaria final con métodos estáticos. Coordina múltiples aggregates (Ruta, Camion, Chofer, Entrega) en operaciones transaccionales. No tiene estado propio. El método `iniciarRuta(Ruta, Camion, Chofer, List<Entrega>, String)` valida que las entregas estén en orden correcto y todas PENDIENTES antes de mutar.
- `Entrega.java:121-131` — HECHO — `negarEntrega()` llama automáticamente a `mandarARevision("SISTEMA_LOGISTICA")` que es private. La transición NO_RECIBIDA → REVISION es automática e inmediata. El método `mandarARevision` es private y no puede ser llamado externamente. Encadenamiento automático bien documentado con comentario en código.
- `Entrega.java:143-155` — BIEN — `regresarAlDeposito()` acepta estado NO_RECIBIDA o REVISION (ambos). Resetea `horaArribo`, `horaSalida` e `idRuta` a null. Permite re-planificar la entrega.
- `PlanificadorDeRutas.java:63` — ATENCIÓN — `LocalDate.now()` sin ZoneId en `procesarSolicitud()`. El resto del código de logistica usa `ZoneId.of("UTC")`. La inconsistencia puede causar bugs en deployments con timezone != UTC.
- `AsignadorDeEntregasPorDimension.java` — BIEN — Implementa `AlgoritmoAsignadorDeEntregas` (Strategy). Algoritmo greedy: para cada entrega ordenada, busca el primer camión con capacidad suficiente (kg y volumen). Si no hay camión con capacidad, la entrega se omite (sin error). Puede dejar entregas sin asignar si los camiones están al límite.
- `PlanificacionService.procesarCallback()` — ORQUESTACIÓN COMPLEJA — El método `mapearRespuesta()` (47 líneas) valida duplicados de camiones, choferes y entregas con HashSet, rechazando callbacks con datos incoherentes. Esta lógica de validación de datos externos es apropiada en la capa de aplicación.
- `PlanificacionService.publicarAsignaciones()` — BIEN — Extrae solo los `EventoRutaAsignada` de la lista de domain events con filter+cast, los pasa al comunicador y limpia. El filtrado tipado es el patrón correcto para despachar eventos heterogéneos.

**Acoplamiento prohibido:**

HECHO VERIFICADO: logistica-service NO tiene FeignClient hacia donaciones, incentivos ni notificaciones. La comunicación saliente es exclusivamente vía RabbitMQ: `LogisticaEventPublisher` publica en exchange `logistica.exchange`. `ComunicadorEventosLogisticaRabbit` implementa el port `ComunicadorEventosLogistica` — el dominio llama al port, no a la infraestructura directamente. Restricción arquitectónica CUMPLIDA.

**Integración:**

- `ComunicadorEventosLogisticaRabbit.comunicarRutaIniciada()` genera la URL de seguimiento via `GeneradorDeURLSeguimiento` — el URL se propaga al evento RabbitMQ y finalmente llega a `EventoRutaIniciada` de DonacionIndependiente como `urlMapa`.
- `EntregasService.publicarEventos()` usa pattern matching de Java 21 (`switch(evento) { case EntregaConfirmada confirmada -> ... }`) para despachar correctamente. Código moderno y expresivo.

---

### 5.3 Incentivos

**DonanteIncentivos — Aggregate Root:**

HECHO: `DonanteIncentivos` extiende `AgregadoConEventos<EventoDonanteIncentivos>`. Emite `MisionCompletada` y `AscensoDonante`. La lógica de negocio está correctamente encapsulada:
- `registrarDonacion(EventoDonacion)` → `metricas.registrarDonacion()` + `evaluarMisionActiva()`
- `registrarDonacionExitosa(UUID organizacionId)` → `metricas.registrarDonacionExitosa()` + `evaluarMisionActiva(mision -> mision.evaluarProgresoExitoso(this))`
- `evaluarMisionActiva()` es privado, usa Consumer<Mision> para separar qué se evalúa del cuándo.
- `completarMision()` es privado, emite el evento y llama a `ascender()`.
- `ascender()` verifica si TODAS las misiones de la categoría actual están completadas antes de ascender.

**Misiones secuenciales:**

HECHO confirmado. Las misiones son secuenciales dentro de cada categoría:
- `getMisionActiva()` retorna la mision con el menor `numeroMision` que no esté completada y sea de la categoría actual del donante.
- `MisionFactory.crearMisionesEstandar()` crea 6 misiones: 2 para COLABORADOR (1,2), 2 para SOSTENEDOR (3,4), 2 para TRANSFORMADOR (5,6).
- El donante no puede acceder a misiones de SOSTENEDOR hasta completar TODAS las de COLABORADOR (el `getMisionActiva()` filtra por `m.getCategoria() == this.categoria`).
- Tipos de misión: `MisionRacha` (meses consecutivos), `MisionCompletitud` (categorías distintas), `MisionHabilDonador` (cantidad en una donación), `MisionDonacionesExitosas` (entregas confirmadas).

**Ranking mensual:**

HECHO: `RankingMensual` implementa `AggregateRoot` sin extender `AgregadoConEventos`. No emite domain events. `RankingService.calcularYPersistir()` elimina el ranking existente del período y lo recalcula, preservando historial de períodos anteriores en el repositorio. `obtenerHistorial()` retorna todos los rankings persistidos. El historial SÍ se persiste: hay un `IRankingRepository` con `findAll()` y `findByPeriodo()`. La observación del checkpoint 1 de que "solo persiste el actual" era incorrecta — se persisten todos los períodos.

**Schedulers:**

- `RankingMensualJob`: cron `0 59 23 L * *` (último día del mes a las 23:59). Delega a `rankingService.calcularYNotificar()`.
- `InactividadJob`: cron `0 0 8 * * *` (diario a las 8AM). Delega a `inactividadService.procesarInactividad()`.
- `RachaJob`: cron `0 5 0 1 * *` (primer día del mes a las 00:05). Delega a `misionesDonacionService.verificarRachasVencidas()`.

CORRECTO: Los schedulers son delgados. Son simples disparadores sin lógica de negocio.

**N8N:**

`N8nClientAdapter` usa `WebClient` (reactivo) con `.subscribe()` — es fire-and-forget. Los errores se absorben con `log.warn`. Correcto para notificaciones externas opcionales.

---

### 5.4 Notificaciones

**Strategy pattern y adapters:**

HECHO ACLARADO (Checkpoint 2): Los adapters NO son solo interfaces. Las implementaciones concretas están en el paquete `infrastructure.mockEnvios`:
- `CorreoEnvioMock` implements `CorreoAdapter` — `@Component` Spring — loguea el mail simulado con `log.info` y retorna `true`.
- `TelefonoEnvioMock` implements `TelefonoAdapter` — `@Component` Spring — similar.
- `WhatsappEnvioMock` implements `WhatsAppAdapter` — `@Component` Spring — similar.

Todos son `@Component` con `@Profile` o sin `@Profile`. Son las únicas implementaciones concretas en el classpath. Son simulaciones funcionales (retornan `true`, loguean el envío), no mocks de test. El sistema "envía" notificaciones que quedan en logs. Para producción real se deberían reemplazar por implementaciones reales (SendGrid, Twilio, etc.).

`NotificacionRouter` es el `@Component` que implementa `NotificacionSender` y despacha a los tres adapters. Patrón doble-dispatch: `MedioDeContacto.enviarMensaje(mensaje, sender)` → `sender.enviarA(this, mensaje)` con overloading por tipo.

**Notificacion aggregate post-refactor:**

HECHO (Oleada 11): `Notificacion` extiende `AgregadoConEventos<NotificacionDomainEvent>`. El constructor llama a `actualizarEstado(PENDIENTE)` que llama a `registrarDomainEvent()` que llama a `registrarEvento(new NotificacionCreada(...))`. El evento `NotificacionCreada` se publica desde `NotificacionService.publicarYLimpiarDomainEvents()`. `NotificacionGestor` (listener) escucha `NotificacionCreada` y llama a `notificarPendientes()` que obtiene la Notificacion, la Persona, y llama a `notificacion.notificar(persona, sender)`.

**Ordenamiento de medios:**

HECHO: `Notificacion.ordenarMedios()` usa `Boolean.TRUE.equals(m.getEsPredeterminado())` para null-safety. Los medios predeterminados van primero. Si `esPredeterminado` es null, se trata como "no predeterminado". Comentado explícitamente en código (Oleada 9.5).

**Eventos manejados:**

Los `EventoNotificable` concretos son 8 clases que implementan `generarNotificaciones()`:
1. `DonacionAsignada` — notifica al donante que su donación fue asignada
2. `DonacionEnCamino` — notifica al donante y beneficiaria que la donación está en camino
3. `DonacionRecibida` — notifica al donante y beneficiaria que la donación fue recibida
4. `DonanteInactivo` — notifica al donante inactivo
5. `DonanteRegistrado` — notifica al donante recién registrado
6. `EntregaFallida` — notifica al donante, beneficiaria y administrador
7. `MisionCumplida` — notifica al donante que completó una misión
8. `SubioCategoria` — notifica al donante que ascendió de categoría

---

### 5.5 common-lib

**Abstracciones:**

| Clase/Interface | Propósito | Uso real |
|-----------------|-----------|----------|
| `AggregateRoot` | Contrato UUID getId() | Usada por todos los aggregates |
| `AgregadoConEventos<E>` | Lista de domain events inmutable + registrarEvento/getDomainEvents/clearDomainEvents | Donacion, DonacionIndependiente, Propuesta, Ruta, Entrega, DonanteIncentivos, Notificacion |
| `EventoDeDominio` | UUID id + LocalDateTime timestamp | Base de todos los eventos de dominio |
| `CrudRepositoryEnMemoria<T>` | Map<UUID,T> con ConcurrentHashMap | Repositorios en memoria de todos los servicios |
| `GlobalExceptionHandler` | @ControllerAdvice con catalog de errores | Todos los servicios (auto-configuration) |
| `ErrorCatalog` | Enum con códigos de error | Toda la gestión de excepciones |
| `MdcTaskDecorator` | Propaga MDC context a threads async | AsyncConfig en incentivos |
| `FeignTraceRequestInterceptor` | Propaga X-Trace-Id en calls Feign | Todos los Feign clients |
| `ControllerLoggingInterceptor` | Log automático de requests HTTP | Todos los controllers |
| `ServiceLoggingAspect` | AOP logging de métodos @Service | Todos los services |
| `ScheduledJobLoggingAspect` | AOP logging de jobs @Scheduled | Schedulers |

POSITIVO: La common-lib es una abstracción genuina y bien usada. No hay sobre-ingeniería. Los servicios reutilizan las bases correctamente.

OBSERVACION: `CrudRepositoryEnMemoria` usa `ConcurrentHashMap` — thread-safe para operaciones individuales, pero no tiene transacciones. Para el MVP universitario en memoria esto es aceptable.

---

## 6. DC vs Código

> **CHECKPOINT 2 — Reconstrucción inversa completa (2026-08-28)**
> Las siguientes tablas se basan en lectura directa de código. Se distingue HECHO (verificado), INFERENCIA (no se leyó el archivo) y RECOMENDACIÓN.

### 6.1 Donaciones — Dominio

| Clase/Concepto | Existe | Archivo | Patrón correcto | Diferencias o deuda |
|----------------|--------|---------|-----------------|---------------------|
| Donacion (aggregate principal) | HECHO: SI | Donacion.java | AgregadoConEventos<EventoDonacion> | Cumple. UUID propio. 3 domain events. Historial de estados inmutable. Timezone: ZoneId.systemDefault() vs UTC en el resto |
| EstadoDonacion (enum 3 valores) | HECHO: SI | EstadoDonacion.java | Enum simple: CARGADA, NORMALIZADA, SEGMENTADA | Cumple. No usa State pattern (innecesario para 3 estados lineales sin behavior diferencial) |
| CambioEstadoDonacion (value object) | HECHO: SI | CambioEstadoDonacion.java | Immutable: estadoAnterior, estadoNuevo, timestamp | Cumple. Timestamp usa ZoneId.systemDefault() |
| ItemDonacion (value object record) | HECHO: SI | ItemDonacion.java | Java 21 record con validación en compact constructor | Cumple. Exposición de peso/volumen total correcta |
| DonacionIndependiente (aggregate) | HECHO: SI | DonacionIndependiente.java | AgregadoConEventos<EventoDonacionIndependiente> | Cumple. donacionOriginalId por referencia de ID. fragmentarse() muta lista interna. setEstadoActual() package-private |
| EstadoDonacionIndependiente (State) | HECHO: SI | EstadoDonacionIndependiente.java (interface) | Interface con 8 operaciones + default que lanza excepción | EXCELENTE: patrón State con 7 estados concretos (2 terminales: Entregada, Vencida). Sobreescritura por estado solo donde aplica |
| EnDeposito | HECHO: SI | EnDeposito.java | Estado inicial | Permite: asignar (→ASIGNACION_REALIZADA) + vencer. Emite EventoDonacionAsignada |
| AsignacionRealizada | HECHO: SI | AsignacionRealizada.java | Estado intermedio | Permite: planificarRuta (→LISTA_PARA_ENTREGAR) |
| ListaParaEntregar | HECHO: SI | ListaParaEntregar.java | Estado intermedio | Permite: iniciarRecorrido (→EN_TRASLADO). Emite EventoRutaIniciada con urlMapa |
| EnTraslado | HECHO: SI | EnTraslado.java | Estado intermedio | Permite: confirmarEntrega (→ENTREGADA) + registrarFalla (→ENTREGA_FALLIDA). Si replanificable=true, auto-avanza a ASIGNACION_REALIZADA. Emite EventoDonacionRecibida o EventoDonacionFallida |
| Entregada | HECHO: SI | Entregada.java | Estado terminal | Solo getTipo(). Ninguna transición disponible |
| EntregaFallida | HECHO: SI | EntregaFallida.java | Estado con salidas | Permite: retornar (→EN_DEPOSITO) + replanificar (→ASIGNACION_REALIZADA) |
| Vencida | HECHO: SI | Vencida.java | Estado terminal | Solo getTipo(). Ninguna transición disponible |
| EventoDonacionAsignada | HECHO: SI | events/EventoDonacionAsignada.java | extends EventoDonacionIndependiente | Porta: donacionIndependienteId, donacionOriginalId, idNecesidad |
| EventoRutaIniciada (DI) | HECHO: SI | events/EventoRutaIniciada.java | extends EventoDonacionIndependiente | Porta: donacionIndependienteId, donacionOriginalId, idNecesidad, urlMapa |
| EventoDonacionRecibida | HECHO: SI | events/EventoDonacionRecibida.java | extends EventoDonacionIndependiente | Porta: donacionIndependienteId, donacionOriginalId, idNecesidad, patenteCamion |
| EventoDonacionFallida | HECHO: SI | events/EventoDonacionFallida.java | extends EventoDonacionIndependiente | Porta: donacionIndependienteId, donacionOriginalId, idNecesidad, justificacion, replanificable (Boolean nullable) |
| Propuesta (aggregate) | HECHO: SI | Propuesta.java | AgregadoConEventos<PropuestaAprobada> | Cumple. aceptar() valida estado PENDIENTE y necesidadId no null. rechazar() no emite evento (solo cambia estado). PropuestaAprobada porta la lista de fragmentaciones |
| EjecucionAsignacion | HECHO: SI | EjecucionAsignacion.java | implements AggregateRoot | Sin domain events. Registro histórico de cuántas propuestas generó cada ejecución |
| GestorPropuestasDeAsignacion | HECHO: SI | GestorPropuestasDeAsignacion.java | Domain service | Coordina múltiples AlgoritmoAsignacion. consolidar() prioriza intersección de propuestas entre algoritmos |
| AlgoritmoAsignacion (Strategy) | HECHO: SI | AlgoritmoAsignacion.java | Abstract class + Template Method | Dos implementaciones. Hardcode: `propuestas.size() >= 10` en línea 20 |
| AlgoritmoCompatibilidadSemantica | HECHO: SI | AlgoritmoCompatibilidadSemantica.java | extends AlgoritmoAsignacion | filtra por subcategoría + ordena por score de palabras en común con ComparadorTexto |
| AlgoritmoPrioridadSubAtendidos | HECHO: SI | AlgoritmoPrioridadSubAtendidos.java | extends AlgoritmoAsignacion | Reordena necesidades: entidades con menos donaciones recientes van primero. Solo cuenta donaciones en NecesidadExtraordinaria (ATENCIÓN: ignora NecesidadRecurrente) |
| StockDeDonaciones | HECHO: SI | StockDeDonaciones.java | Value object mutable de sesión | Rastrea disponibilidad virtual. Se usa solo durante el algoritmo, no persiste |
| Necesidad (abstract) | HECHO: SI | Necesidad.java | Abstract + implements Asignable + AggregateRoot | Template Method toDTO(fechaFin). Métodos abstractos: getDonacionesAsignadas, asignarDonacion, estaSatisfecha, isActiva, cantidadAcumulada |
| NecesidadRecurrente | HECHO: SI | NecesidadRecurrente.java | extends Necesidad | Lista de PeriodoNecesidad (record inmutable). renovarPeriodoSiCorresponde() genera nuevo período si vence. isActiva() retorna `this.activa` field |
| NecesidadExtraordinaria | HECHO: SI | NecesidadExtraordinaria.java | extends Necesidad + implements Asignable (redundante) | Lista mutable de DonacionIndependiente. isActiva() retorna siempre true. Sin concepto de vencimiento |
| Asignable (port interface) | HECHO: SI | Asignable.java | Interface con un método | Solo `obtenerNecesidad()`. Implementado por Necesidad y NecesidadExtraordinaria (redundante) |
| EntidadBeneficiaria | HECHO: SI | EntidadBeneficiaria.java | implements Anonimizable + AggregateRoot | juridicaId (UUID) como referencia a PersonaJuridica. anonimizar() vacío con comentario |
| Donante | HECHO: SI | Donante.java | implements Anonimizable + AggregateRoot | personaId (UUID) como referencia. anonimizar() vacío con comentario. Sin domain events |
| Segmentador (port) | HECHO: SI | Segmentador interface | Port de segmentación | Implementado por la clase concreta de segmentación |
| CargadorDonantes (port) | HECHO: SI | CargadorDonantes interface + LectorDonantesCSV impl | Port + adapter | LectorDonantesCSV: BOM UTF-8, ignora líneas malformadas |

### 6.2 Logística — Dominio

| Clase/Concepto | Existe | Archivo | Patrón correcto | Diferencias o deuda |
|----------------|--------|---------|-----------------|---------------------|
| Ruta (aggregate) | HECHO: SI | Ruta.java | AgregadoConEventos<EventoRuta> | Cumple. Emite EventoRutaAsignada por cada entrega. Usa ZoneId.of("UTC") consistentemente |
| EstadoRuta (enum) | HECHO: SI | EstadoRuta (dentro de Ruta) | PENDIENTE, EN_TRASLADO, COMPLETADA | 3 estados lineales. Sin State pattern (innecesario) |
| Entrega (aggregate) | HECHO: SI | Entrega.java | AgregadoConEventos<EventoEntrega> | Cumple. 5 estados: PENDIENTE, EN_TRASLADO, ENTREGADA, NO_RECIBIDA, REVISION. mandarARevision() private |
| EstadoEntrega (enum) | HECHO: SI | EstadoEntrega.java | PENDIENTE, EN_TRASLADO, ENTREGADA, NO_RECIBIDA, REVISION | Sin State pattern (la lógica está inline en Entrega) |
| Camion (aggregate) | HECHO: SI | Camion.java | implements AggregateRoot | 3 estados: DISPONIBLE, EN_RUTA, DESHABILITADO. Sin domain events. estaDisponibleParaAsignar() verifica estado + rutaId null |
| GestorDeRutas (domain service) | HECHO: SI | GestorDeRutas.java | Clase final con métodos estáticos | Coordina Ruta + Camion + Chofer + Entrega. Validaciones cruzadas antes de mutar |
| GeneradorDeRutas | HECHO: SI | GeneradorDeRutas.java | Domain service | MAX_ENTREGAS_POR_SOLICITUD=100. planificar() crea PlanificacionSolicitada. calcularRutas() procesa RespuestaPlanificacion |
| GeneradorLotes (port Strategy) | HECHO: SI | GeneradorLotes interface | Strategy para particionado | GeneradorLotesSimple implementa particionado básico |
| PlanificadorDeRutas | HECHO: SI | PlanificadorDeRutas.java | Domain service con Strategy interno | Usa AlgoritmoOrdenadorDeEntregas + AlgoritmoAsignadorDeEntregas. procesarSolicitud() usa LocalDate.now() SIN ZoneId |
| AsignadorDeEntregasPorDimension | HECHO: SI | AsignadorDeEntregasPorDimension.java | implements AlgoritmoAsignadorDeEntregas (Strategy) | Greedy: primer camión con capacidad suficiente. Las entregas sin camión son ignoradas silenciosamente |
| ComunicadorEventosLogistica (port) | HECHO: SI | ComunicadorEventosLogistica interface | Port de comunicación saliente | Implementado por ComunicadorEventosLogisticaRabbit |
| ComunicadorEventosLogisticaRabbit | HECHO: SI | ComunicadorEventosLogisticaRabbit.java | implements port anterior | Convierte domain events a DTOs RabbitMQ. Usa GeneradorDeURLSeguimiento para urlMapa |
| LogisticaEventPublisher | HECHO: SI | LogisticaEventPublisher.java | Infraestructura RabbitMQ | Publica 4 tipos de mensajes al exchange logistica.exchange |

### 6.3 Incentivos — Dominio

| Clase/Concepto | Existe | Archivo | Patrón correcto | Diferencias o deuda |
|----------------|--------|---------|-----------------|---------------------|
| DonanteIncentivos (aggregate) | HECHO: SI | DonanteIncentivos.java | AgregadoConEventos<EventoDonanteIncentivos> | Cumple. Lógica de negocio rica: registrarDonacion, registrarDonacionExitosa, ascender, otorgarInsignia, verificarRachas |
| CategoriaDonante (enum) | HECHO: SI | CategoriaDonante.java | COLABORADOR, SOSTENEDOR, TRANSFORMADOR | 3 categorías. siguienteCategoria() en DonanteIncentivos usa switch |
| CambioCategoria (value object) | HECHO: SI | CambioCategoria.java | Registro de cambio de categoría | Historial inmutable |
| EventoDonacion | HECHO: SI | EventoDonacion.java | Value object de evento de donación | Porta fecha, categorías, cantidad, nombre donante |
| AscensoDonante | HECHO: SI | eventos/AscensoDonante.java | extends EventoDonanteIncentivos | Porta: idDonante, idPersona, categoriaAnterior, categoriaNueva |
| MisionCompletada | HECHO: SI | eventos/MisionCompletada.java | extends EventoDonanteIncentivos | Porta: idDonante, idPersona, nombre donante, nombre misión, insignia |
| Mision (abstract) | HECHO: SI | Mision.java | Abstract class + Template Method | calcularNuevoProgreso() abstracto. evaluarProgreso() es el template method. verificarVigencia() con default vacío (para MisionRacha) |
| MisionCompletitud | HECHO: SI | MisionCompletitud.java | extends Mision | Cuenta categorías distintas en las que donó |
| MisionDonacionesExitosas | HECHO: SI | MisionDonacionesExitosas.java | extends Mision | Cuenta donaciones exitosas (confirmadas recibidas) |
| MisionHabilDonador | HECHO: SI | MisionHabilDonador.java | extends Mision | Objetivo: cantidad >= X en una sola donación |
| MisionRacha | HECHO: SI | MisionRacha.java | extends Mision | Cuenta meses consecutivos donando. verificarVigencia() resetea progreso si se rompe la racha |
| MisionFactory | HECHO: SI | factory/MisionFactory.java | Factory method estático | crearMisionesEstandar(): 6 misiones predefinidas con insignias |
| Misiones secuenciales | HECHO: SI | DonanteIncentivos.getMisionActiva() | Filtra por categoría actual + no completada + menor numeroMision | CORRECTO: el donante no puede acceder a misiones de categoría superior hasta completar todas las de la actual |
| RankingMensual | HECHO: SI | RankingMensual.java | implements AggregateRoot (sin AgregadoConEventos) | No emite domain events. getPodio() retorna las 3 primeras entradas. El historial SÍ persiste: hay findAll() en el repositorio |
| GestorDeRankings | HECHO: SI | GestorDeRankings.java | Domain service | Ordena por misiones completadas en el mes (desc), luego por donaciones en el mes (desc), luego por id. Excluye donantes sin misiones en el período |
| CriterioInactividad | HECHO: SI | inactividad/CriterioInactividad.java | Strategy de criterio | Interface para determinar inactividad |
| GestorDeInactivos | HECHO: SI | inactividad/GestorDeInactivos.java | Domain service | Aplica CriterioInactividad a todos los donantes |
| InactividadDonaciones | HECHO: SI | inactividad/InactividadDonaciones.java | implements CriterioInactividad | Verifica días sin actividad desde fechaUltimaActividad() |
| Metricas | HECHO: SI | metricas/Metricas.java | Value object mutable | donacionesPorPeriodo: Map<YearMonth, Long>. donacionesEnMes() y ultimaDonacion para inactividad |

### 6.4 Notificaciones — Dominio

| Clase/Concepto | Existe | Archivo | Patrón correcto | Diferencias o deuda |
|----------------|--------|---------|-----------------|---------------------|
| Notificacion (aggregate) | HECHO: SI | Notificacion.java | AgregadoConEventos<NotificacionDomainEvent> | Post-Oleada 11. Constructor llama actualizarEstado(PENDIENTE) que emite NotificacionCreada |
| NotificacionSender (port) | HECHO: SI | ports/NotificacionSender.java | Interface con 2 métodos (overloading por tipo) | enviarA(Correo, String) y enviarA(Telefono, String). Correcto como port de infraestructura |
| NotificacionRouter | HECHO: SI | infrastructure/NotificacionRouter.java | implements NotificacionSender | Dispatcher: Correo→CorreoAdapter, Telefono WHATSAPP→WhatsAppAdapter, Telefono SMS→TelefonoAdapter |
| CorreoAdapter | HECHO: SI | infrastructure/CorreoAdapter.java | Interface | enviarMail(String correo, String mensaje): boolean |
| TelefonoAdapter | HECHO: SI | infrastructure/TelefonoAdapter.java | Interface | enviarSms(String numero, String mensaje): boolean |
| WhatsAppAdapter | HECHO: SI | infrastructure/WhatsAppAdapter.java | Interface | enviarWhatsApp(String numero, String mensaje): boolean |
| CorreoEnvioMock | HECHO: SI | infrastructure/mockEnvios/CorreoEnvioMock.java | @Component implements CorreoAdapter | Loguea email simulado, retorna true. Es la única implementación concreta disponible |
| TelefonoEnvioMock | HECHO: SI | infrastructure/mockEnvios/TelefonoEnvioMock.java | @Component implements TelefonoAdapter | Ídem |
| WhatsappEnvioMock | HECHO: SI | infrastructure/mockEnvios/WhatsappEnvioMock.java | @Component implements WhatsAppAdapter | Ídem |
| EventoNotificable (hierarchy) | HECHO: SI | 8 clases concretas | Polimorfismo via generarNotificaciones() | DonacionAsignada, DonacionEnCamino, DonacionRecibida, DonanteInactivo, DonanteRegistrado, EntregaFallida, MisionCumplida, SubioCategoria |
| NotificacionCreada / Enviada / Fallida | HECHO: SI | events/*.java | Domain events de Notificacion | 3 estados del ciclo de vida. NotificacionGestor escucha NotificacionCreada |
| Anonimizable (port) | HECHO: SI | ports/Anonimizable.java | Interface | Notificacion implementa: `anonimizar()` reemplaza mensaje con constante. Donante y EntidadBeneficiaria tienen anonimizar() vacío |

---

## 7. Application Services

> **CHECKPOINT 2 — Clasificación por método (2026-08-28)**

### 7.1 Donaciones

| Service | Método | Clasificación | Fan-in | Análisis |
|---------|--------|--------------|--------|---------|
| DonacionesService | cargarDonacion(DonacionInputDTO) | ORQUESTACIÓN pura | 2 repos + 1 mapper + 1 procesador | Busca donante → crea Donacion via mapper → persiste → publica DomainEvents → lanza ProcesadorDeDonaciones (@Async). No toma decisiones de negocio. CORRECTO |
| DonacionesService | listarDonaciones() | MAPEO | 1 repo | findAll() → map(mapper::toOutputDTO). CORRECTO |
| DonacionesService | obtenerDonacion(UUID) | ORQUESTACIÓN pura | 1 repo | findById → orElseThrow → toOutputDTO. CORRECTO |
| DonacionesIndependientesService | obtenerTodas() | MAPEO | 1 repo | findAll() → stream().map(toDTO). CORRECTO |
| DonacionesIndependientesService | obtener(UUID) | ORQUESTACIÓN pura | 1 repo | findById → orElseThrow → toDTO. CORRECTO |
| DonacionesIndependientesService | cambiarEstado(UUID, RequestDTO, String) | ORQUESTACIÓN pura | 2 repos + 1 mapper | Busca DI → busca Necesidad si necesidadId != null → construye SolicitudCambioEstado → llama dominio → persiste → publica DomainEvents → toDTO. No toma decisiones. CORRECTO |
| PropuestaDeAsignacionService | ejecutarAsignacion() | ORQUESTACIÓN pura | 3 repos + domain service | findEnDeposito + findNecesidades → gestorPropuestas.generarPropuestas() → saveAll → EjecucionAsignacion → save. CORRECTO |
| PropuestaDeAsignacionService | listarPropuestas() | MAPEO | 1 repo | findAll → map(propuestaMapper::toDTO). CORRECTO |
| PropuestaDeAsignacionService | actualizarEstado(UUID, EstadoPropuesta) | ORQUESTACIÓN pura + leve lógica de control de flujo | 1 repo | switch(estado) { APROBADA → propuesta.aceptar() + publicar; DESCARTADA → propuesta.rechazar() }. El switch decide qué método de dominio llamar pero no implementa lógica de negocio. ACEPTABLE |
| PropuestaDeAsignacionService | historialEjecuciones() | MAPEO | 1 repo | obtenerHistorial() → map(ejecucionMapper::toDTO). CORRECTO |
| PropuestaDeAsignacionService | onPropuestaAprobada(@EventListener) | ORQUESTACIÓN con lógica de ensamblado | 4 repos + Feign async | Por cada fragmentación: busca DI original → confirmar() → salva original → salva nueva (si diferente) → llama notificarLogistica(). El método construirSolicitudEntrega() en el servicio tiene lógica de lookup cross-aggregate (entidad→persona→direccion). REVISAR: la construcción del request de logística podría estar en un mapper |
| NecesidadesService | guardar(NecesidadDTO) | CONTIENE LÓGICA DE NEGOCIO MÍNIMA + MAPEO | 3 repos | convertirDTOANecesidad() hace switch por tipo (RECURRENTE/EXTRAORDINARIA) y crea la instancia correcta. Esta lógica de factory debería estar en el dominio (NecesidadFactory) pero es aceptable en el service para el MVP |
| NecesidadesService | obtenerPorId(UUID) | ORQUESTACIÓN pura | 1 repo | findById → toDTO. CORRECTO |
| NecesidadesService | listarConFiltros(UUID, String) | MAPEO + filtrado | 1 repo | findAll → map → filter por entidadId y tipo. El filtrado en memoria es ineficiente para volúmenes grandes pero aceptable en MVP |
| SegmentacionEventListener | onDonacionNormalizada(@EventListener) | CONTIENE LÓGICA DE NEGOCIO Y ORQUESTACIÓN en infraestructura | 7 repos + 1 Feign | PROBLEMÁTICO. Hace: resolución de ítems aceptados no segmentados, segmentación, obtención de categorías, resolución de nombre de persona (Humana vs Juridica con instanceof), llamada a incentivos, persistencia de DIs, marcado de ítems como segmentados, avance de estado de donación original. Esta orquestación debería ser un Application Service que el listener simplemente dispare |
| DonacionIndependienteNotificacionesListener | onEventoRutaIniciada(@EventListener) | ORQUESTACIÓN con resolución cross-aggregate | 6 repos + 1 Feign | Cadena de resolución: donacion→donanteId, donante→personaId, necesidad→entidadId, entidad→juridicaId. Llama notificaciones. REVISAR: debería ser application service |
| DonacionIndependienteNotificacionesListener | onEventoDonacionRecibida(@EventListener) | ORQUESTACIÓN con resolución cross-aggregate + side effects múltiples | 6 repos + 2 Feign | PROBLEMÁTICO FAN-IN: resuelve 5 IDs cross-aggregate + llama incentivos para donación exitosa + llama notificaciones. Dos side effects síncronos en un listener |
| DonacionIndependienteNotificacionesListener | onEventoDonacionFallida(@EventListener) | ORQUESTACIÓN con resolución cross-aggregate | 6 repos + 1 Feign | Agrega resolución de idPersonaAdmin. PROBLEMÁTICO |

### 7.2 Logística

| Service | Método | Clasificación | Fan-in | Análisis |
|---------|--------|--------------|--------|---------|
| PlanificacionService | iniciarPlanificacion() | ORQUESTACIÓN pura | 5 repos + domain services | findSinRuta + findDisponibles + generadorDeRutas.planificar() + enviarPlanificacion(). Early return si no hay entregas o recursos. CORRECTO |
| PlanificacionService | procesarCallback(CallbackPlanificacionRequestDTO) | ORQUESTACIÓN con validación compleja | 4 repos | ATENCIÓN: mapearRespuesta() (47 líneas) valida duplicados de camiones/choferes/entregas con HashSets. Es lógica de validación de datos externos en la capa de aplicación — apropiado. persistirPlanificacion() y publicarAsignaciones() están en métodos privados correctamente |
| PlanificacionService | obtenerPorId(UUID) | ORQUESTACIÓN pura | 1 repo | findById → solicitudMapper.toResponseDTO. CORRECTO |
| EntregasService | crear(CrearEntregaRequestDTO) | ORQUESTACIÓN pura + MAPEO | 1 repo + mapper | mapper.toEntity() → save → toResponseDTO. CORRECTO |
| EntregasService | listar() | MAPEO | 1 repo | findAll → map. CORRECTO |
| EntregasService | obtenerPorId(UUID) | ORQUESTACIÓN pura | 1 repo | buscarEntrega + toResponseDTO. CORRECTO |
| EntregasService | adjuntarFotoRecepcion(UUID, DTO) | ORQUESTACIÓN pura | 1 repo | buscarEntrega → adjuntarFotoRecepcion() → save → toResponseDTO. CORRECTO |
| EntregasService | cambiarEstado(UUID, CambioEstadoEntregaRequestDTO) | ORQUESTACIÓN pura con switch de routing | 3 repos | switch(estado) construye la SolicitudTransicion correcta (ConfirmacionRecepcion, NoRecepcion, RegresoDeposito) y llama GestorDeEntregas.cambiarEstado(). EN_TRASLADO y REVISION lanzan excepción (no se pueden cambiar manualmente). publicarEventos() usa Java 21 pattern matching. CORRECTO |
| EntregasService | obtenerHistorial(UUID) | MAPEO | 1 repo | buscarEntrega → getHistorialEstado → map. CORRECTO |
| EntregasService | publicarEventos(Entrega) | ROUTING DE EVENTOS | 2 repos (para buscar camión) | switch(evento) con pattern matching Java 21. Busca camión cuando hay EntregaConfirmada. Técnico, en capa correcta |
| RutasService | iniciarRuta(UUID, DTO) | ORQUESTACIÓN pura | 4 repos + domain service | Busca Ruta + Camion + Chofer + Entregas → GestorDeRutas.iniciarRuta() → save → publicarEventos(). CORRECTO |
| CamionesService | métodos CRUD | ORQUESTACIÓN pura | 1 repo | Patrón estándar. CORRECTO |

### 7.3 Incentivos

| Service | Método | Clasificación | Fan-in | Análisis |
|---------|--------|--------------|--------|---------|
| GestionDonanteService | registrarDonante(DTO) | ORQUESTACIÓN pura con upsert | 1 repo | findById → si no existe: new DonanteIncentivos() → save. Upsert es decisión de aplicación, no de negocio. ACEPTABLE |
| GestionDonanteService | eliminarDonante(UUID) | ORQUESTACIÓN pura | 1 repo | findById → delete. CORRECTO |
| MisionesDonacionService | procesarDonacion(DTO) | ORQUESTACIÓN pura | 1 repo | findById → donante.registrarDonacion(event) → save → publicar events. La creación del EventoDonacion es mapeo de DTO → value object. CORRECTO |
| MisionesDonacionService | procesarDonacionExitosa(DTO) | ORQUESTACIÓN pura | 1 repo | findById → donante.registrarDonacionExitosa(organizacionId) → save → publicar events. CORRECTO |
| MisionesDonacionService | verificarRachasVencidas() | ORQUESTACIÓN pura | 1 repo | findAll → forEach donante.verificarRachas(mesActual) → save. CORRECTO |
| RankingService | calcularYPersistir(YearMonth) | ORQUESTACIÓN pura | 2 repos + domain service | delete existente → findAll donantes → gestorDeRankings.calcular() → save. CORRECTO |
| RankingService | calcularYNotificar(YearMonth) | COMPOSICIÓN | 2 repos + domain service + n8n | calcularYPersistir() + construir top3 como List<Map<String,Object>> + n8nClient.notificarRankingCalculado(). La construcción del payload para n8n (Map literal) podría estar en un mapper. ACEPTABLE |
| RankingService | obtenerHistorial() | MAPEO | 1 repo | findAll → map(RankingMensualDTO::desde). CORRECTO |
| RankingService | obtenerPosicionDonante(UUID) | MAPEO con stream | 1 repo (vía obtenerUltimoRanking) | Busca en el último ranking la posición del donante. CORRECTO |
| InactividadService | procesarInactividad() | ORQUESTACIÓN pura | 2 repos + domain service + Feign Async | findAll donantes → gestorDeInactivos.detectarInactivos() → forEach publicar notificación. CORRECTO |
| InsigniasService | obtenerInsignias(UUID) | MAPEO | 1 repo | findById → insigniasVisibles() → map. CORRECTO |
| InsigniasService | configurarVisibilidad(UUID, nombre, visible) | ORQUESTACIÓN pura | 1 repo | findById → donante.configurarVisibilidadInsignia() → save. CORRECTO |
| NotificacionesIncentivosListener | onMisionCompletada(@EventListener) | ORQUESTACIÓN pura | 1 Feign + 1 WebClient | Feign a notificaciones + n8nClient. CORRECTO — listener delgado |
| NotificacionesIncentivosListener | onAscensoDonante(@EventListener) | ORQUESTACIÓN pura | 1 Feign | Solo notificaciones. CORRECTO |

### 7.4 Notificaciones

| Service | Método | Clasificación | Fan-in | Análisis |
|---------|--------|--------------|--------|---------|
| NotificacionService | procesar(EventoNotificableDTO) | ORQUESTACIÓN pura + MAPEO | 1 repo + 1 mapper | mapper.toEntity() → evento.generarNotificaciones() → saveAll → publicarYLimpiarDomainEvents(). La responsabilidad de crear notificaciones está en el dominio (EventoNotificable). CORRECTO |
| NotificacionService | publicarYLimpiarDomainEvents(Notificacion) | ORQUESTACIÓN técnica | 0 deps | getDomainEvents().forEach(publishEvent) → clearDomainEvents(). Técnico, en capa correcta |
| NotificacionService | obtenerPorPersona(UUID) | MAPEO | 1 repo | findByPersonaId → map a NotificacionDTO inline (sin mapper dedicado). El mapeo inline es menor deuda |
| NotificacionGestor | onNotificacionCreada(@EventListener) | ORQUESTACIÓN pura | 2 repos | findByPersonaId(pendientes) → forEach notificacion.notificar(persona, sender). La lógica de notificación real está en el dominio (Notificacion.notificar). CORRECTO |

---

## 8. Integración

| Productor | Evento/Request | Consumidor | Medio | Síncrono/Async | Correcto |
|-----------|----------------|-----------|-------|---------------|---------|
| donaciones | POST /api/incentivos/donaciones | incentivos | Feign HTTP | Síncrono (en listener @EventListener) | REVISAR (dentro de tx de dominio) |
| donaciones | POST /api/incentivos/donantes/{id} | incentivos | Feign HTTP | Síncrono | OK |
| donaciones | DELETE /api/incentivos/donantes/{id} | incentivos | Feign HTTP | Síncrono | OK |
| donaciones | POST /notificaciones | notificaciones | Feign HTTP | Síncrono (en listener) | REVISAR |
| donaciones | PUT /api/notificaciones/personas | notificaciones | Feign HTTP | Síncrono | OK |
| donaciones | POST /api/entregas | logistica | Feign HTTP | Síncrono | OK |
| logistica | ruta.asignada (RabbitMQ) | donaciones | RabbitMQ Topic Exchange | Async | OK |
| logistica | ruta.iniciada (RabbitMQ) | donaciones | RabbitMQ Topic Exchange | Async | OK |
| logistica | entrega.exitosa (RabbitMQ) | donaciones | RabbitMQ Topic Exchange | Async | OK |
| logistica | entrega.fallida (RabbitMQ) | donaciones | RabbitMQ Topic Exchange | Async | OK |
| incentivos | POST /notificaciones (via Feign) | notificaciones | Feign HTTP @Async | Async (thread pool) | OK |
| incentivos | webhook n8n insignia | n8n | WebClient | Async fire-and-forget | OK |
| incentivos | webhook n8n ranking | n8n | WebClient | Async fire-and-forget | OK |

### 8.1 Acoplamiento prohibido

CONFIRMADO: logistica-service NO invoca directamente a donaciones, incentivos ni notificaciones. La revisión de `ComunicadorEventosLogisticaRabbit`, `LogisticaEventPublisher`, y todos los services de logistica confirma que la comunicación saliente es exclusivamente vía RabbitMQ. La restricción arquitectónica principal ESTA CUMPLIDA.

### 8.2 Fallos parciales

- **Riesgo 1:** Las llamadas Feign desde donaciones hacia incentivos/notificaciones se hacen dentro de listeners de ApplicationEventPublisher (`@EventListener`). Si incentivos está caído, la excepción se absorbe con `log.error` pero la operación local (segmentación) ya fue completada. Hay inconsistencia de estado potencial.
- **Riesgo 2:** No hay mecanismo de retry garantizado para los Feign calls salientes de donaciones. `FeignRetryConfig` existe como clase de configuración pero no fue auditado en detalle. Si falló, no hay cola de dead letter ni compensación.
- **Riesgo 3:** Los mensajes RabbitMQ hacia donaciones (desde logistica) no tienen mecanismo de idempotencia visible. Re-entrega podría causar doble transición de estado que se absorbe silenciosamente con `try/catch`.

---

## 9. Domain Events

| Evento | Lo genera | Lo publica | Integration Event asociado | Correcto |
|--------|-----------|-----------|--------------------------|---------|
| DonacionCargada | Donacion constructor | ApplicationEventPublisher (en DonacionesService) | Ninguno visible | Parcial — evento sin consumidor registrado |
| DonacionNormalizada | Donacion.marcarNormalizada() | ApplicationEventPublisher | Ninguno externo — consumido por SegmentacionEventListener en mismo proceso | OK para MVP |
| DonacionSegmentada | Donacion.marcarSegmentada() | ApplicationEventPublisher | Ninguno visible | Parcial |
| PropuestaAprobada | Propuesta.aceptar() | No auditado explícitamente | — | NO DETERMINADO |
| EventoRutaIniciada (DI) | EstadoDonacionIndependiente impl | ApplicationEventPublisher | Notificación via Feign en DonacionIndependienteNotificacionesListener | OK |
| EventoDonacionRecibida (DI) | EstadoDonacionIndependiente impl | ApplicationEventPublisher | Incentivos y Notificaciones via Feign | OK |
| EventoDonacionFallida (DI) | EstadoDonacionIndependiente impl | ApplicationEventPublisher | Notificaciones via Feign | OK |
| EventoRutaAsignada (logistica) | Ruta.agregarEntrega() | RabbitMQ via LogisticaEventPublisher | Consumido por LogisticaEventListener en donaciones | OK |
| EventoRutaIniciada (logistica) | Ruta.iniciarRuta() | RabbitMQ via LogisticaEventPublisher | Consumido por LogisticaEventListener en donaciones | OK |
| EntregaConfirmada | Entrega.confirmarEntrega() | RabbitMQ via ComunicadorEventosLogisticaRabbit | Consumido por LogisticaEventListener en donaciones (EntregaExitosa) | OK |
| EntregaFallida | Entrega.negarEntrega() | RabbitMQ via ComunicadorEventosLogisticaRabbit | Consumido por LogisticaEventListener en donaciones | OK |
| MisionCompletada | DonanteIncentivos.completarMision() | ApplicationEventPublisher | NotificacionesIncentivosListener → Feign + N8n | OK |
| AscensoDonante | DonanteIncentivos.ascender() | ApplicationEventPublisher | NotificacionesIncentivosListener → Feign | OK |
| NotificacionCreada | Notificacion constructor via actualizarEstado() | ApplicationEventPublisher | NotificacionGestor.onNotificacionCreada → notificarPendientes() | OK |
| NotificacionEnviada | Notificacion.actualizarEstado(ENVIADA) | ApplicationEventPublisher | Sin consumidor visible | Parcial |
| NotificacionFallida | Notificacion.actualizarEstado(FALLIDA) | ApplicationEventPublisher | Sin consumidor visible | Parcial |

---

## 10. API / Swagger / Postman

> **CHECKPOINT 2 — Actualizado con análisis de colecciones Postman (2026-08-28)**

### 10.1 Donaciones (:8080) — Endpoints verificados

| Endpoint | Método | Controller | En Postman | Discrepancias |
|----------|--------|-----------|-----------|---------------|
| /api/donaciones | POST | DonacionesController | SI (postman-donaciones.json) | Ninguna |
| /api/donaciones | GET | DonacionesController | SI | Ninguna |
| /api/donaciones/{id} | GET | DonacionesController | SI (flujo-8-e2e) | Ninguna |
| /donaciones-independientes | GET | DonacionesIndependientesController | PARCIAL (postman-donaciones.json sección 10 solo tiene PATCH) | NUEVO endpoint — GET /donaciones-independientes y GET /{id} no están en la colección original |
| /donaciones-independientes/{id} | GET | DonacionesIndependientesController | NO documentado en Postman | Endpoint nuevo sin cobertura Postman de GET individual |
| /donaciones-independientes/{id}/estado | PATCH | DonacionesIndependientesController | SI (flujos 4, 8 y postman-donaciones) | Presentes. Nota: sin prefijo /api/ en todas las colecciones, consistente con el controller |
| /api/asignaciones/ejecuciones | POST | PropuestaDeAsignacionController | SI (flujo-4, flujo-8) | Ninguna |
| /api/asignaciones/ejecuciones | GET | PropuestaDeAsignacionController | SI (postman-donaciones sección 09) | Ninguna |
| /api/asignaciones/propuestas | GET | PropuestaDeAsignacionController | SI (flujos 4 y 8) | Ninguna |
| /api/asignaciones/propuestas/{id}/estado | PUT | PropuestaDeAsignacionController | SI (flujos 4 y 8) | Ninguna |
| /api/donantes | POST, GET | DonantesController | SI | Ninguna |
| /api/donantes/{id} | GET, DELETE | DonantesController | SI | El controller no implementa PUT (solo GET y DELETE) |
| /api/donantes/archivos | POST | DonantesController | NO | Carga asíncrona de CSV de donantes (retorna 202 ACCEPTED) |
| /api/donantes/archivos/{id} | GET | DonantesController | NO | Consulta de estado de archivo procesado |
| /api/personas | POST, GET | PersonasController | SI (flujo-1) | Ninguna |
| /api/personas/{id} | PUT, DELETE | PersonasController | SI | El controller no implementa GET /{id} (solo PUT y DELETE) |
| /api/entidades | POST, GET | EntidadBeneficiariaController | SI | Mapeo canónico del controller. /{id} soporta GET, PUT, DELETE |
| /api/necesidades | POST, GET | NecesidadesController | SI (flujo-2, flujo-8) | /{id} soporta GET, PUT, DELETE (usa PUT, no PATCH) |
| /api/categorias | POST, GET, PUT, DELETE | CategoriasController | SI (postman-donaciones sección 01) | Ninguna |
| /api/subcategorias | POST, GET, PUT, DELETE | SubcategoriasController | SI (postman-donaciones sección 02) | Soporta además POST /api/subcategorias/{id}/aliases y DELETE /{id}/aliases/{alias} |
| /api/items-normalizados/pendientes | GET | ItemDonacionNormalizadoController | SI (postman-donaciones) | /{id} soporta GET y PATCH. No existe endpoint GET en raíz |

### 10.2 Logística (:8083) — Endpoints verificados

| Endpoint | Método | Controller | En Postman | Discrepancias |
|----------|--------|-----------|-----------|---------------|
| /api/entregas | POST, GET | EntregasController | SI (flujo-6, postman-logistica) | Ninguna |
| /api/entregas/{id} | GET | EntregasController | SI (flujo-6, flujo-8) | Ninguna |
| /api/entregas/{id}/estado | PATCH | EntregasController | SI (flujo-6) | Ninguna |
| /api/entregas/{id}/fotos | PATCH | EntregasController | SI | Ninguna |
| /api/entregas/{id}/historial | GET | EntregasController | SI | Ninguna |
| /api/rutas | GET | RutasController | SI (postman-logistica) | Ninguna |
| /api/rutas/{id} | GET | RutasController | SI | Ninguna |
| /api/rutas/{id}/entregas | GET, POST | RutasController | SI | Ninguna |
| /api/rutas/{id}/estado | PATCH | RutasController | SI (flujo-6) | Ninguna |
| /api/logistica/callback/rutas | POST | PlanificacionController | NO presente en colecciones (endpoint de callback externo) | También mapeado en `/api/logistica/resultados` (ambas rutas en @PostMapping) |
| /api/logistica/planificaciones/{id} | GET | PlanificacionController | SI | Ninguna |
| /api/logistica/planificaciones/ejecuciones | POST | PlanificacionManualController | NO | Disparador manual para pruebas (condicional en logistica.planificacion.manual-enabled=true) |
| /api/camiones | POST, GET | CamionesController | SI (postman-logistica) | Ninguna |
| /api/camiones/{id} | GET, DELETE | CamionesController | SI | PATCH en `/api/camiones/{id}/estado`; DELETE para dar de baja |
| /api/choferes | POST, GET | ChoferesController | SI | Ninguna |
| /api/choferes/{id} | GET, DELETE | ChoferesController | SI | PATCH en `/api/choferes/{id}/estado`; DELETE para dar de baja |

### 10.3 Incentivos (:8082) — Endpoints verificados

| Endpoint | Método | Controller | En Postman | Discrepancias |
|----------|--------|-----------|-----------|---------------|
| /api/incentivos/donantes/{id} | POST, DELETE, PATCH, GET | DonanteIncentivosController | SI (postman-incentivos) | Consulta de perfil del donante consolidada en PR #856 |
| /api/incentivos/donantes/{id}/ascensos | GET | DonanteIncentivosController | SI (postman-incentivos) | Historial de transiciones de categoría agregado en PR #856 |
| /api/incentivos/donaciones | POST | MisionesDonacionController | SI | Ninguna |
| /api/incentivos/donaciones/exitosa | POST | MisionesDonacionController | SI (flujo-5) | Ninguna |
| /api/incentivos/donantes/{id}/misiones | GET | MisionesDonacionController | SI | Ninguna |
| /api/incentivos/ranking/ultimo | GET | RankingController | SI | Ninguna |
| /api/incentivos/ranking/historial | GET | RankingController | SI | Ninguna |
| /api/incentivos/ranking/calcular | POST | RankingController | SI (flujo-5) | Ninguna |
| /api/incentivos/ranking/posicion/{id} | GET | RankingController | SI | Ninguna |
| /api/incentivos/ranking/{periodo} | GET | RankingController | NO | Consulta de ranking mensual por período (YYYY-MM) agregada en PR #856 |
| /api/incentivos/donantes/{id}/insignias | GET | InsigniasController | SI (flujo-5) | Parámetro opcional soloVisibles; visibilidad se configura vía PUT .../visibilidad (no PATCH) |
| /api/incentivos/donantes/{id}/metricas | GET | MetricasIncentivosController | SI (flujo-8 paso 5.1) | Ninguna |
| /api/incentivos/admin/resumen | GET | MetricasIncentivosController | NO | Resumen global del sistema para administradores |
| /api/incentivos/evaluaciones-inactividad | POST | ProcesosIncentivosController | NO | Disparador on-demand para pruebas y administración (DTI-09) |
| /api/incentivos/verificaciones-racha | POST | ProcesosIncentivosController | NO | Disparador on-demand para pruebas y administración (DTI-09) |

### 10.4 Notificaciones (:8081) — Endpoints verificados

| Endpoint | Método | Controller | En Postman | Discrepancias |
|----------|--------|-----------|-----------|---------------|
| /notificaciones | POST | NotificacionController | SI (flujo-7, postman-notificaciones) | Ninguna. Sin prefijo /api/ documentado correctamente en todas las colecciones |
| /notificaciones/persona/{id} | GET | NotificacionController | SI (flujo-8 paso 5.2) | Ninguna |
| /api/notificaciones/personas | PUT | PersonasController | SI (postman-notificaciones) | Sincronización de réplica de persona |
| /api/notificaciones/personas/{id} | GET, DELETE | PersonasController | SI | Consulta de réplica y anonimización de persona |

### 10.5 Estado de las 12 colecciones Postman

| Archivo | Servicio objetivo | Tipo | Cobertura | Observaciones |
|---------|------------------|------|-----------|---------------|
| postman-donaciones.json | donaciones-service (:8080) | Por-servicio | Alta | 10 secciones. La sección 10 (DonacionesIndependientesController) solo tiene PATCH, falta GET /donaciones-independientes y GET /{id} (endpoints nuevos) |
| postman-logistica.json | logistica-service (:8083) | Por-servicio | Alta | CRUD completo de rutas, entregas, camiones, choferes |
| postman-incentivos.json | incentivos-service (:8082) | Por-servicio | Alta | Misiones, ranking, insignias, métricas |
| postman-notificaciones.json | notificaciones-service (:8081) | Por-servicio | Alta | POST /notificaciones con todos los tipos de EventoNotificable |
| flujo-1-catalogo-personas.json | donaciones (:8080) | E2E parcial | Buena | Catálogo de categorías/subcategorías y personas. Prerequisito de otros flujos |
| flujo-2-necesidades.json | donaciones (:8080) | E2E parcial | Buena | Creación y consulta de necesidades recurrentes y extraordinarias |
| flujo-3-donacion-normalizacion-segmentacion.json | donaciones (:8080) | E2E parcial | Buena | Ciclo completo de normalización y verificación de segmentación |
| flujo-4-matching-asignacion-estados.json | donaciones (:8080) | E2E parcial | Alta | Algoritmos de asignación + transiciones de estado de DonacionIndependiente |
| flujo-5-misiones-insignias-ranking.json | incentivos (:8082) | E2E parcial | Alta | Procesamiento de donaciones, misiones, ranking mensual, insignias |
| flujo-6-logistica-completo.json | logistica (:8083) | E2E parcial | Alta | Ciclo completo de logística: camiones, choferes, rutas, entregas, estados |
| flujo-7-notificaciones-eventos.json | notificaciones (:8081) | E2E parcial | Alta | Los 8 tipos de EventoNotificable uno a uno |
| flujo-8-e2e-distribuido.json | todos los servicios | E2E completo | Alta | Flujo estrella: crea donante+necesidad+donación → matching → aprueba propuesta → logistica verifica entrega → avanza DonacionIndependiente hasta ENTREGADA → verifica side-effects en incentivos y notificaciones |

### 10.6 Análisis de coherencia Postman vs Código

**Endpoints en código NO en Postman:**
- `GET /donaciones-independientes` — nuevo endpoint agregado en este pull, no documentado en postman-donaciones.json sección 10
- `GET /donaciones-independientes/{id}` — nuevo endpoint agregado en este pull, no documentado en Postman
- `POST /api/donantes/importar-csv` (o equivalente) — importación CSV sin cobertura Postman
- `POST /api/logistica/callback/rutas` — endpoint de callback de proveedor externo, no es navegable desde Postman (lo llama el proveedor)

**Endpoints en Postman NO verificados en código (inferidos):**
- La URL `{{donacionesUrl}}/api/entidades` en flujo-8 puede diferir de `/api/entidades-beneficiarias` en postman-donaciones — REVISAR si es inconsistencia de prefijo real o alias

**Coherencia de body/response:**
- HECHO: `flujo-8-e2e-distribuido.json` usa `X-Actor: TRANSPORTISTA` en el header de PATCH /donaciones-independientes/{id}/estado. CORRECTO — el controller tiene `@RequestHeader("X-Actor") String actor`.
- HECHO: Los test scripts en Postman verifican `estadoActual == "SEGMENTADA"` para el polling de segmentación. La respuesta del controller retorna el string del enum serializado.
- HECHO: El flujo E2E verifica `totalDonacionesExitosas > 0` en las métricas de incentivos después de la entrega exitosa — confirma que el Feign síncrono de donaciones→incentivos funciona.

**Swagger activo:** SI — `DonaTrackOpenApiAutoConfiguration` está en common-lib como Spring Boot auto-configuration. SpringDoc está configurado via `DonaTrackOpenApiProperties` con `donatrack.openapi.*` properties.

**Estado Postman post-Checkpoint 2:** 12 colecciones disponibles (4 por-servicio + 8 flujos E2E). La inconsistencia principal es que el nuevo `GET /donaciones-independientes` y `GET /donaciones-independientes/{id}` no tienen cobertura Postman en la colección por-servicio. Los flujos E2E (flujo-4 y flujo-8) usan correctamente `/donaciones-independientes/{id}/estado` sin prefijo `/api/`.

---

## 11. Tests

| Área | Tipo | Tests | Cobertura funcional | Riesgo |
|------|------|-------|---------------------|-------|
| donaciones - controllers | MockMvc/WebMvcTest | 47 | Validaciones HTTP, happy path, errores | Bajo |
| donaciones - domain models | JUnit puro | ~120 | Algoritmos, estados, fragmentación, propuestas | Bajo |
| donaciones - services | Mockito | ~80 | Reglas de negocio via servicios | Bajo |
| donaciones - infrastructure | MockMvc/Mockito | ~20 | CSV, listeners, ProcesadorDeDonaciones | Bajo |
| donaciones - integration (app) | @SpringBootTest | 2 | Startup + contexto básico | Medio (RabbitMQ no disponible) |
| logistica - controllers | MockMvc/WebMvcTest | 64 | HTTP validaciones completas | Bajo |
| logistica - domain models | JUnit puro | ~100 | Camion, Ruta, Entrega, Planificacion | Bajo |
| logistica - services | Mockito | ~30 | PlanificacionService, EntregasService, RutasService | Bajo |
| logistica - infrastructure | Mockito | 4 | ComunicadorEventosLogisticaRabbit | Medio |
| incentivos - controllers | MockMvc/WebMvcTest | ~50 | HTTP, validaciones | Bajo |
| incentivos - domain models | JUnit puro | ~60 | DonanteIncentivos, Misiones, Ranking, Inactividad | Bajo |
| incentivos - services | Mockito | ~50 | GestionDonante, Ranking, Misiones, Inactividad | Bajo |
| notificaciones - controllers | MockMvc/WebMvcTest | ~15 | HTTP endpoint y validaciones | Bajo |
| notificaciones - domain models | JUnit puro | ~30 | Notificacion, eventos, personas | Bajo |
| notificaciones - mappers | JUnit | ~30 | EventoMapper, MedioDeContactoMapper, PersonaMapper | Bajo |
| integration-tests | RestAssured + @SpringBootTest | 5+ clases | Cross-service contract, E2E, smoke | ALTO (requiere servicios running) |
| common-lib | MockMvc/JUnit | 38 | AgregadoConEventos, GlobalExceptionHandler, CrudRepo | Bajo |

**Hallazgos específicos:**

- POSITIVO: Los tests de dominio (modelos) son unitarios puros sin Spring context — rápidos y confiables.
- POSITIVO: Uso de Object Mother pattern en donaciones (múltiples `*Mother.java` y `*Fixtures.java`) e incentivos.
- POSITIVO: `DtoValidationTest` en logistica (36 tests) valida todas las combinaciones de DTOs de forma exhaustiva.
- POSITIVO: Los tests de logistica incluyen tests de dominio puro para Camion (23), Chofer (26), Entrega (11), Ruta (14) — muy completo.
- REVISAR: `AlgoritmoAsignadorDeEntregaTest` tiene 1 test skipped (@Disabled o @Disabled) — razón no auditada.
- REVISAR: Los integration-tests requieren que los servicios estén corriendo (BaseIT apunta a URLs de servicios). No son ejecutables sin infraestructura Docker.
- REVISAR: No hay tests de los domain events que NO tienen consumidor registrado (DonacionCargada, DonacionSegmentada).
- NEGATIVO: AsyncConfigTest en incentivos referencia MdcTaskDecorator de common-lib — falla si common-lib no está instalado (bug de build workflow).
- REVISSAR: No hay tests de idempotencia de mensajes RabbitMQ ni de comportamiento ante doble-entrega.

---

## 12. Docker / Despliegue

| Servicio | Dockerfile | docker-compose local | docker-compose preprod | Puerto | Variables env | Observaciones |
|---------|-----------|---------------------|----------------------|--------|--------------|---------------|
| donaciones | SI (multi-stage: builder+local+ci) | SI :8080 | SI :8080 | 8080 | PORT, NOTIFICACIONES_SERVICE_URL, INCENTIVOS_SERVICE_URL, LOGISTICA_SERVICE_URL, RABBITMQ_HOST, EXECUTION_ID | Depende de rabbitmq healthy |
| incentivos | SI (multi-stage) | SI :8082 | SI :8082 | 8082 | PORT, NOTIFICACIONES_SERVICE_URL, N8N_*_WEBHOOK_URL, EXECUTION_ID | Depende de notificaciones + n8n |
| logistica | SI (multi-stage) | SI :8083 | SI :8083 | 8083 | PORT, RABBITMQ_HOST, LOGISTICA_MAX_DONACIONES_POR_LOTE, EXECUTION_ID | Solo depende de rabbitmq |
| notificaciones | SI (multi-stage) | SI :8081 | SI :8081 | 8081 | PORT, EXECUTION_ID | Sin dependencias de servicios propios |
| RabbitMQ | imagen oficial | SI :5672/:15672 | SI | 5672/15672 | RABBITMQ_DEFAULT_USER, RABBITMQ_DEFAULT_PASS | healthcheck con rabbitmq-diagnostics |
| n8n | imagen oficial | SI :5678 | SI :5678 | 5678 | N8N_HOST, N8N_PORT, etc. | healthcheck HTTP en preprod |

**docker-compose local:** No tiene healthchecks propios para los servicios Java (solo `service_started`). Puede causar race conditions al iniciar.

**docker-compose preprod:** Todos los servicios Java tienen healthcheck via `/actuator/health`. Dependencias con `condition: service_healthy`. Más robusto que el local.

**Nota de puertos:** incentivos-service está en puerto 8082 en docker-compose pero `DonaTrackOpenApiProperties` en su `application.properties` apunta a `http://localhost:8082` — consistente. El docker-compose local mapea incentivos a 8082, no 8081 como podría confundirse por el orden en el archivo.

---

## 13. Hardening

| Aspecto | Aplica | Estado | Riesgo |
|---------|--------|--------|-------|
| Idempotencia eventos RabbitMQ | SI | NO IMPLEMENTADO | ALTO — LogisticaEventListener puede procesar doble-entrega y la excepción se absorbe silenciosamente |
| Event ordering | SI | NO GARANTIZADO | MEDIO — RabbitMQ no garantiza orden entre queues distintas (ruta.asignada, ruta.iniciada, entrega.exitosa). Con una sola queue por routing key el orden es FIFO dentro de la queue. |
| Concurrencia async | SI | PARCIAL — ConcurrentHashMap en repos, @Async en algunas calls | MEDIO — No hay coordinación entre el thread @Async de ProcesadorDeDonaciones y el thread de petición original |
| MDC / TraceId | SI | IMPLEMENTADO — FeignTraceRequestInterceptor propaga X-Trace-Id, MdcTaskDecorator propaga MDC en async, ControllerLoggingInterceptor, ScheduledJobLoggingAspect | BAJO |
| Thread pools | PARCIAL | incentivos tiene `notificacionesTaskExecutor` (core=2, max=10, queue=500). Otros servicios usan Spring defaults | MEDIO — Sin pool dedicado, donaciones usa @Async en el pool default |
| Duplicados | SI | NO IMPLEMENTADO — No hay deduplicación de mensajes en LogisticaEventListener | ALTO |
| Null semantics | SI | BIEN MANEJADO — validaciones explícitas con ErrorCatalog, Boolean.TRUE.equals() para null-safe en notificaciones | BAJO |
| Circuit breaker | NO | NO IMPLEMENTADO | MEDIO — Si incentivos/notificaciones están caídos, las llamadas Feign fallan con excepción que se absorbe |
| Timeout en Feign calls | NO DETERMINADO | FeignRetryConfig existe pero no fue auditado en detalle | MEDIO |

---

## 14. Legacy y Deuda

1. **`NecesidadesService.java:19`** — `// TODO:` vacío sin descripción. HECHO.
2. **`application.properties` donaciones línea 9** — `// TODO: Utilizar variables de entorno` usando sintaxis de comentario Java (`//`) en lugar de `#`. Puede causar problemas en parsers estrictos de `.properties`.
3. **`application.properties` logistica, notificaciones, incentivos línea 10** — `# TODO: ajustar server-url cuando se despliegue en Docker/preprod.` — pendiente de resolver para producción.
4. **`AlgoritmoAsignacion.java:19`** — Hard-coded `10` como máximo de propuestas: `if (propuestas.size() >= 10) break;`. Debería ser configurable.
5. **`PlanificadorDeRutas.java:63`** — `LocalDate.now()` sin ZoneId. Inconsistente con el resto del código que usa `ZoneId.of("UTC")`.
6. **`SegmentacionEventListener`** — Lógica de orquestación excesiva en infraestructura. Candidato a ser extraído a un application service.
7. **`DonacionIndependienteNotificacionesListener`** — 7 dependencias de repositorios. Fan-in excesivo para un listener de infraestructura.
8. **Falta implementación concreta de CorreoAdapter, TelefonoAdapter, WhatsAppAdapter** — Solo son interfaces. Para un TP universitario puede ser aceptable (simulación), pero es deuda técnica explícita.
9. **Sin colección Postman** — No existe documentación de API en formato ejecutable.
10. **Incentivos en puerto 8082 pero docker-compose-local lo ubica después de notificaciones (8081)** — El orden de puertos puede confundir: notificaciones=8081, incentivos=8082. El README o documentación debería aclararlo.
11. **`AsyncConfigTest` en incentivos** — El test rompe la build aislada por dependencia de MdcTaskDecorator de common-lib. Debería usar `mvn install` desde root o configurar build parent-first.
12. **`application.properties` de donaciones** — Sintaxis de comentario incorrecta (`//` en lugar de `#`) — línea 9.

---

## 15. Justificaciones Faltantes

1. **¿Por qué donaciones usa Feign síncrono hacia incentivos dentro de un @EventListener?** — El riesgo de fallar silenciosamente si incentivos está caído durante el flujo de segmentación no está documentado en ADR.
2. **¿Por qué `SegmentacionEventListener` hace la llamada a incentivos y no un application service?** — La decisión de poner orquestación en un listener de Spring no está explicada.
3. **¿Por qué `DonacionCargada` y `DonacionSegmentada` no tienen listeners?** — Los domain events se generan y publican pero nadie los consume. ¿Intencional para uso futuro?
4. **¿Por qué el límite de propuestas es 10 en AlgoritmoAsignacion?** — Sin documentar.
5. **¿Por qué el patrón de consolidación de propuestas en GestorPropuestasDeAsignacion elige propuesta2 sobre propuesta1 cuando hay cobertura compartida?** — Lógica de negocio sin justificación en código.
6. **¿Por qué `RankingMensual` no extiende AgregadoConEventos si es un aggregate?** — Sin documentar.
7. **¿Los adapters de notificaciones (Correo, Telefono, WhatsApp) tienen implementaciones mock?** — No determinado si hay beans de test o si son stubs.
8. **¿Por qué incentivos está en puerto 8082 y no 8081?** — Puede ser que notificaciones históricamente ocupó 8081. No documentado.

---

## 16. Riesgos

| Prioridad | Riesgo | Evidencia | Impacto | Recomendación |
|-----------|--------|-----------|---------|---------------|
| CRÍTICO | Build fail aislado de notificaciones e incentivos | mvn clean test -pl notificaciones-service sin common-lib instalado → FAILURE | CI podría fallar si el pipeline no instala common-lib primero | Usar `mvn install` en root o configurar `-am` flag en CI |
| CRÍTICO | Sin implementación real de adapters de notificación | CorreoAdapter, TelefonoAdapter, WhatsAppAdapter son interfaces sin beans concretos observados | El sistema no enviaría notificaciones reales en producción | Implementar o documentar explícitamente que son mocks para TP |
| ALTO | Inconsistencia de estado ante fallo de incentivos durante segmentación | SegmentacionEventListener absorbe excepción del Feign call con log.error | Una donación puede quedar SEGMENTADA sin estar registrada en incentivos | Implementar retry o patrón outbox |
| ALTO | Sin idempotencia en LogisticaEventListener | No hay deduplicación de mensajes | Re-entrega RabbitMQ → BusinessStateException silenciosa | Registrar IDs de mensajes procesados |
| MEDIO | Endpoint /donaciones-independientes sin prefijo /api/ | DonacionesIndependientesController mapeado en /donaciones-independientes | Inconsistencia con otros controllers y con el FeignClient de logistica que apunta a /api/entregas | Agregar prefijo /api/ |
| MEDIO | Hard-coded limit de 10 propuestas | AlgoritmoAsignacion.java:19 | No configurable por entorno | Externalizar a properties |
| BAJO | Sin Postman collection | Búsqueda en todo el repo → 0 archivos .postman | Documentación ejecutable faltante | Agregar colección Postman |
| BAJO | TODO sin descripción en NecesidadesService | NecesidadesService.java:19 | Deuda desconocida | Describir o eliminar el TODO |

---

## 17. Scorecard

| Área | Score 0-5 | Justificación |
|------|----------:|--------------|
| Alineación requisitos | 5 | Todos los requisitos funcionales tienen implementación verificable |
| Modelo de dominio | 4 | Aggregates bien definidos, State pattern en DonacionIndependiente, Strategy en algoritmos. Descuento por lógica de orquestación en listeners |
| Separación responsabilidades | 3 | Controllers delgados, services correctos, pero SegmentacionEventListener y DonacionIndependienteNotificacionesListener tienen lógica de negocio |
| Integración | 4 | Logistica cumple el aislamiento requerido. Asimetría Feign/RabbitMQ es deliberada. Riesgo de inconsistencia ante fallos |
| Domain Events | 4 | Eventos bien generados y publicados. Algunos sin consumidor (DonacionCargada, DonacionSegmentada). Mezcla de app events con integration events |
| APIs | 4 | Endpoints completos, Swagger activo. Inconsistencia de prefijo /api/ en un controller |
| Tests | 5 | Excelente cobertura: 2020+ tests sin failures. Object Mothers, unit y controller tests |
| Trazabilidad | 5 | MDC propagado, traceId en headers Feign, AspectJ logging en services/schedulers |
| Robustez | 2 | Sin idempotencia, sin circuit breaker, build frágil por orden de módulos, adapters sin impl real |
| Documentación | 3 | Swagger activo, ADRs existentes, sin Postman, algunos TODOs sin descripción |
| Deploy | 4 | Dockerfiles multi-stage, docker-compose local y preprod. Local sin healthchecks Java |
| Consistencia entre servicios | 4 | Todos usan common-lib, mismos patrones de logging, misma estructura. Descuento por puerto inconsistente incentivos y prefijo URL |
| **TOTAL** | **47/60** | |

---

## 18. Deuda Pendiente

### Bloqueante para entrega (si el criterio es ejecución correcta)

1. **Implementar beans concretos para CorreoAdapter, TelefonoAdapter, WhatsAppAdapter** — Sin esto, el sistema no puede enviar notificaciones reales. Si el TP acepta simulación, documentarlo explícitamente.
2. **Resolver build aislado de notificaciones e incentivos** — Asegurarse de que el CI corra `mvn install` en common-lib antes de compilar los servicios dependientes, o usar `mvn install -am` en el root.

### Recomendado antes de entrega

3. **Describir el `// TODO:` en NecesidadesService.java** — Completar o eliminar.
4. **Corregir sintaxis de comentario en donaciones application.properties** — Cambiar `//` por `#`.
5. **Agregar prefijo `/api/` a DonacionesIndependientesController**.
6. **Documentar en ADR** por qué la comunicación desde donaciones a incentivos/notificaciones es Feign síncrono en lugar de RabbitMQ.
7. **Externalizar el límite de 10 propuestas** en AlgoritmoAsignacion.
8. **Corregir ZoneId en PlanificadorDeRutas.java:63**.

### Mejora futura

9. Implementar idempotencia en LogisticaEventListener con registro de messageId.
10. Extraer la orquestación de SegmentacionEventListener a un application service.
11. Extraer la orquestación de DonacionIndependienteNotificacionesListener a un application service.
12. Agregar circuit breaker (Resilience4j) para los Feign clients de donaciones.
13. Agregar colección Postman.
14. Agregar healthchecks Java en docker-compose local.
15. Implementar listener para DonacionCargada y DonacionSegmentada o eliminar los domain events si son sin uso.

---

## 19. Diferencias antes/después del refactor

Basado en el git log (commits más recientes y sus mensajes):

**¿Qué mejoró?**
- "E4 refactor notificaciones" (5ba60df2): Notificaciones adoptó el patrón de domain events via AgregadoConEventos, alineándose con el resto de servicios. El refactor también incluyó Object Mothers para testing.
- "oleada 9.5", "oleada 10", "oleada 11": Iteraciones progresivas de mejora del modelo de notificaciones, incluyendo el manejo correcto de null en `esPredeterminado` y la propagación correcta de domain events.
- "E4 refactor trace" (e07e7453): Implementación de MDC/tracing propagado via Feign interceptors y task decorators.
- "fix(tests): corregir contaminacion de estado entre tests de integracion" (bd8d7757): Se resolvió aislamiento de tests de integración.
- "fix(logistica): corregir ConstraintDeclarationException en controllers" (2d58c993): Fix de validaciones en logistica.

**¿Qué deuda desapareció?**
- Notificaciones ya no mantiene su propia lista de domain events redundante — ahora hereda de AgregadoConEventos.
- Los domain events de Notificacion se publican y limpian correctamente con el patrón gold-standard.
- El tracing MDC se propaga consistentemente en threads async.

**¿Qué decisiones quedaron más claras?**
- La separación Donacion/DonacionIndependiente está sólida.
- El State pattern en DonacionIndependiente está completo y bien testeado.
- Logistica NO llama a otros servicios — restricción cumplida.

**¿Qué problemas se introdujeron?**
- El refactor de notificaciones generó una inconsistencia de build: `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO` fue movido/renombrado en common-lib pero el artifact instalado localmente no fue actualizado, causando el error de compilación en builds aislados.
- El refactor de tracing agregó `MdcTaskDecorator` en common-lib pero el test de incentivos que lo referencia falla en build aislado.

**¿Qué quedó pendiente?**
- Implementaciones concretas de los adapters de notificación.
- Colección Postman.
- Resolución de los TODOs documentados.
- Idempotencia de mensajes.

---

## 20. Conclusión

> ¿El proyecto está en condiciones de considerarse correctamente refactorizado y coherente con el alcance actual?

**Estado:** SI, CON OBSERVACIONES

**Justificación:**

El proyecto DonaTrack muestra un nivel de madurez técnica alto para un TP universitario. Los hallazgos más positivos son: (1) cumplimiento del 100% de los requisitos funcionales con implementaciones verificables, (2) una suite de tests excepcional con más de 2000 tests sin failures, (3) cumplimiento de la restricción arquitectónica más importante — logistica-service NO llama directamente a otros servicios, usando RabbitMQ exclusivamente, (4) modelo de dominio rico con Aggregate Roots bien definidos, State pattern en DonacionIndependiente, Strategy en algoritmos de asignación, y uso correcto de AgregadoConEventos en todos los módulos, (5) trazabilidad cross-service con MDC, traceId propagado via Feign interceptors y task decorators.

Las observaciones más relevantes son: el problema de build aislado de notificaciones e incentivos (por dependencia de common-lib no instalada) debe resolverse en CI para que el pipeline sea robusto; la falta de implementaciones concretas de los adapters de notificación (email, SMS, WhatsApp) puede ser aceptable en el contexto universitario si se documenta explícitamente que son simulaciones; y la lógica de orquestación pesada en los listeners `SegmentacionEventListener` y `DonacionIndependienteNotificacionesListener` es una deuda técnica identificada pero no bloqueante para el funcionamiento.

El refactor post-oleada 11 ha mejorado significativamente la coherencia del modelo de dominio y la trazabilidad. El proyecto puede considerarse correctamente refactorizado dentro del alcance académico actual, con las observaciones documentadas como deuda técnica conocida y priorizada.

---

## 21. Checkpoint 2 — Hallazgos Adicionales

> Hallazgos específicos del Checkpoint 2 (2026-08-28) que no estaban en el documento original o que corrigen/amplían análisis previos. Formato: archivo:línea — clasificación — descripción — principio involucrado.

### Correcciones a hallazgos del Checkpoint 1

**C2-COR-01 — Adapters de notificación SÍ tienen implementación concreta**
- **Clasificación:** CORRECCIÓN del hallazgo #12 del Checkpoint 1
- **Descripción:** `CorreoEnvioMock`, `TelefonoEnvioMock` y `WhatsappEnvioMock` son `@Component` Spring en el paquete `infrastructure.mockEnvios/`. Son implementaciones concretas simuladas (no mocks de test), retornan `true` y loguean el envío. El sistema funciona end-to-end con estas simulaciones.
- **Archivos:** `notificaciones-service/src/main/java/grupo5/notificaciones/infrastructure/mockEnvios/CorreoEnvioMock.java:9`, `TelefonoEnvioMock.java:9`, `WhatsappEnvioMock.java:9`
- **Principio:** Ports & Adapters. El hallazgo anterior de "sin implementación real" era incorrecto — las implementaciones existen y están correctamente etiquetadas como mocks de integración (no producción real).

**C2-COR-02 — RankingMensual SÍ persiste historial completo**
- **Clasificación:** CORRECCIÓN del hallazgo #17 del Checkpoint 1
- **Descripción:** `RankingService.calcularYPersistir()` elimina el ranking del período solicitado (`findByPeriodo().ifPresent(delete)`) y crea uno nuevo, pero los rankings de períodos anteriores permanecen en el repositorio. `obtenerHistorial()` devuelve `findAll()` — todos los períodos históricos. El score del Checkpoint 1 que indicaba "solo el actual" era incorrecto.
- **Archivo:** `incentivos-service/.../RankingService.java:39-47`
- **Principio:** Persistencia de historial. No hay deuda aquí.

**C2-COR-03 — PropuestaDeAsignacionService tiene @EventListener propio (auto-escucha)**
- **Clasificación:** ACLARACIÓN del hallazgo #14 de la sección 5.1 del Checkpoint 1
- **Descripción:** `PropuestaDeAsignacionService.onPropuestaAprobada()` está anotado con `@EventListener`. El mismo servicio que publica `PropuestaAprobada` (en `actualizarEstado()`) también lo escucha. Este patrón de auto-escucha via `ApplicationEventPublisher` es válido en Spring y permite desacoplar el controller de los side-effects post-aprobación. El listener dispara las fragmentaciones, actualiza la necesidad y llama a logística.
- **Archivo:** `donaciones-service/.../PropuestaDeAsignacionService.java:99-126`
- **Principio:** Tell don't ask. La publicación del evento permite que el service reaccione sin que el controller orqueste.

### Nuevos hallazgos de código

**C2-HAL-01 — `DonacionIndependiente.java:34` asignación antes de validación nula**
- **Clasificación:** DEUDA TÉCNICA MENOR
- **Descripción:** El constructor asigna `this.donacionOriginalId = donacionOriginalId` en línea 34, y luego valida `if (donacionOriginalId == null)` en línea 36. Si se lanza la excepción, el objeto queda en estado inconsistente (donacionOriginalId asignado antes del fallo). Como la excepción es de tipo `ValidationException` no checked, el objeto no escapa del constructor y no hay impacto práctico, pero el orden es confuso.
- **Archivo:** `donaciones-service/src/main/java/grupo5/donaciones/models/entities/donacionesIndependientes/DonacionIndependiente.java:34-38`
- **Principio:** Fail-fast. La validación debe preceder a la asignación.

**C2-HAL-02 — `EnTraslado.registrarFalla()` produce doble transición de estado implícita**
- **Clasificación:** DEUDA TÉCNICA / COMPORTAMIENTO NO DOCUMENTADO EN JAVADOC
- **Descripción:** Cuando `replanificable == true`, `EnTraslado.registrarFalla()` llama `d.replanificar(actor)` justo después de cambiar el estado a `EntregaFallida` y registrar el evento. El resultado final es que la DonacionIndependiente queda en `AsignacionRealizada` (no en `EntregaFallida`), aunque el evento `EventoDonacionFallida` ya fue registrado con el estado intermedio `EntregaFallida`. El test `cambiarEstado_DeberiaTransicionarAAsignacionRealizada_CuandoEstadoActualEsEnTrasladoYEsReplanificable` confirma este comportamiento esperado. No es un bug, pero el historial de estados muestra dos transiciones por una sola operación del usuario.
- **Archivo:** `donaciones-service/.../EnTraslado.java:60-63`
- **Principio:** Single Responsibility, Principle of Least Surprise.

**C2-HAL-03 — `AlgoritmoPrioridadSubAtendidos` ignora `NecesidadRecurrente` en el conteo de donaciones**
- **Clasificación:** POSIBLE BUG DE NEGOCIO / DEUDA
- **Descripción:** `contarDonacionesRecientes()` en línea 48 usa `if (!(necesidad instanceof NecesidadExtraordinaria))` y retorna 0 para todas las demás necesidades. Las necesidades `NecesidadRecurrente` siempre contribuyen 0 al contador de donaciones recientes por entidad, lo que puede distorsionar el ordenamiento de priorización. El algoritmo fue diseñado para extraordinarias pero las recurrentes también compiten por donaciones.
- **Archivo:** `donaciones-service/.../AlgoritmoPrioridadSubAtendidos.java:48`
- **Principio:** Correctitud funcional.

**C2-HAL-04 — `NecesidadExtraordinaria` declara `implements Asignable` redundantemente**
- **Clasificación:** DEUDA TÉCNICA COSMÉTICA
- **Descripción:** `NecesidadExtraordinaria extends Necesidad implements Asignable`. Pero `Necesidad` ya implementa `Asignable`. La doble declaración es redundante e indica una refactorización incompleta o intencional por claridad.
- **Archivo:** `donaciones-service/.../NecesidadExtraordinaria.java:13`
- **Principio:** DRY.

**C2-HAL-05 — `PlanificadorDeRutas.procesarSolicitud()` usa `LocalDate.now()` sin ZoneId**
- **Clasificación:** DEUDA TÉCNICA / BUG POTENCIAL EN PRODUCCIÓN
- **Descripción:** Línea 63 de `PlanificadorDeRutas.java`: `return new RespuestaPlanificacion(UUID.randomUUID(), solicitud.id(), LocalDate.now(), ...)`. El resto de logistica usa `ZoneId.of("UTC")` explícitamente. En servidores con timezone ≠ UTC, la fecha de la `RespuestaPlanificacion` puede ser diferente de la fecha de la `PlanificacionSolicitada`, causando inconsistencias en los registros de rutas.
- **Archivo:** `logistica-service/.../PlanificadorDeRutas.java:63`
- **Principio:** Determinismo temporal.

**C2-HAL-06 — `DonacionesIndependientesController` mapeado sin prefijo `/api/` — nuevo endpoint GET confirmado**
- **Clasificación:** INCONSISTENCIA ARQUITECTÓNICA — NUEVO ENDPOINT VERIFICADO
- **Descripción:** El controller `DonacionesIndependientesController` está mapeado en `@RequestMapping("/donaciones-independientes")`. Los nuevos endpoints son: `GET /donaciones-independientes` (línea 31), `GET /donaciones-independientes/{id}` (línea 36), `PATCH /donaciones-independientes/{id}/estado` (línea 41). TODOS sin prefijo `/api/`. Las colecciones Postman ya usan estos endpoints correctamente sin `/api/`. Sin embargo, la inconsistencia persiste respecto al resto del sistema.
- **Archivo:** `donaciones-service/.../DonacionesIndependientesController.java:20`
- **Principio:** Consistencia de API, facilidad de integración con API Gateway.

**C2-HAL-07 — Tests del nuevo endpoint `DonacionesIndependientesController` son completos y de alta calidad**
- **Clasificación:** POSITIVO
- **Descripción:** `DonacionesIndependientesControllerTest` tiene 9 tests cubriendo: cambiarEstado exitoso (con verificación de historial), falta de header X-Actor (400), estado nulo (400 + validación de campo), recurso no encontrado (404), transición inválida (409), justificación inválida (400 con código de error), obtenerTodas (200 + lista), obtener por id (200), obtener por id no encontrado (404). Todos verifican header `X-Trace-Id`. El `DonacionesIndependientesServiceTest` tiene 13 tests cubriendo todas las transiciones de estado incluyendo el caso replanificable.
- **Archivos:** `donaciones-service/src/test/java/grupo5/donaciones/controllers/DonacionesIndependientesControllerTest.java`, `.../services/DonacionesIndependientesServiceTest.java`
- **Principio:** Test coverage exhaustiva. Los nuevos endpoints tienen cobertura de alta calidad.

**C2-HAL-08 — `GestorPropuestasDeAsignacion.consolidar()` solo retorna propuestas del algoritmo 2 cuando hay intersección**
- **Clasificación:** COMPORTAMIENTO DOCUMENTADO COMO INFERENCIA
- **Descripción:** Cuando hay propuestas que cubren las mismas necesidades en ambos algoritmos, `consolidar()` retorna SOLO las de `propuesta2` (AlgoritmoPrioridadSubAtendidos), descartando las de `propuesta1` (AlgoritmoCompatibilidadSemantica). INFERENCIA: se asume que cuando ambos algoritmos coinciden en una necesidad, el de prioridad aporta el criterio de "inteligencia social" y se prefiere. Esta decisión no está documentada en comentarios ni ADR.
- **Archivo:** `donaciones-service/.../GestorPropuestasDeAsignacion.java:74-77`
- **Principio:** Claridad de decisiones de negocio.

**C2-HAL-09 — Resolución: `EntidadBeneficiariaController` está mapeado en `/api/entidades`**
- **Clasificación:** [OBSERVED] CONTRATO CONFIRMADO
- **Descripción:** En `EntidadBeneficiariaController.java:22`, la anotación canónica es `@RequestMapping("/api/entidades")`. Por tanto, `flujo-8-e2e-distribuido.json` (que invoca `{{donacionesUrl}}/api/entidades`) es 100% consistente con el código fuente real. La discrepancia residía en la colección legacy `postman-donaciones.json`, que empleaba el prefijo obsoleto `/api/entidades-beneficiarias`.
- **Archivo:** `donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/EntidadBeneficiariaController.java:22`
- **Principio:** Consistencia de documentación vs código.

**C2-HAL-10 — `postman-donaciones.json` sección 10 documenta limitación conocida del nuevo endpoint**
- **Clasificación:** POSITIVO — DOCUMENTACIÓN HONESTA
- **Descripción:** La sección 10 de `postman-donaciones.json` incluye el comentario: "LIMITACIÓN CONOCIDA: {{donacionIndependienteId}} NO es lo mismo que {{donacionId}} [...] Este controller no expone ningún GET para listar/descubrir ese id — hoy solo se puede obtener consultando la base de datos o el log de eventos directamente." Con el nuevo GET /donaciones-independientes disponible, esta limitación ya NO existe. La documentación Postman necesita actualizarse para incluir los nuevos endpoints GET y eliminar la limitación descrita.
- **Archivo:** `docs/postman/postman-donaciones.json:566-570`
- **Principio:** Documentación actualizada.

**C2-HAL-11 — `Donacion.java:51` usa `ZoneId.systemDefault()` mientras logística usa `ZoneId.of("UTC")**
- **Clasificación:** INCONSISTENCIA MENOR DE TIMEZONE
- **Descripción:** `Donacion.java` y `CambioEstadoDonacion.java` usan `ZoneId.systemDefault()` para timestamps. El resto de logistica usa `ZoneId.of("UTC")`. `DonacionIndependiente.java` usa también `ZoneId.systemDefault()`. En entornos Docker con UTC, ambos son equivalentes. En entornos con timezone local, los timestamps de donaciones pueden diferir de los de logística en el log.
- **Archivos:** `donaciones-service/.../Donacion.java:51`, `CambioEstadoDonacion.java:14`, `DonacionIndependiente.java:44`
- **Principio:** Consistencia temporal entre servicios.

**C2-HAL-12 — `NecesidadesService.java:19` — `// TODO:` sigue sin descripción**
- **Clasificación:** DEUDA CONFIRMADA (ya presente en Checkpoint 1)
- **Descripción:** El comentario `// TODO:` en la línea 19 de `NecesidadesService.java` sigue vacío sin descripción. El cuerpo del servicio está completo y funcional. Deuda cosmética sin impacto funcional.
- **Archivo:** `donaciones-service/.../NecesidadesService.java:19`
- **Principio:** Claridad del código.

**C2-HAL-13 — Misiones de tipo `MisionRacha` tienen `verificarVigencia()` que resetea el progreso**
- **Clasificación:** COMPORTAMIENTO DOCUMENTADO
- **Descripción:** `MisionRacha` sobreescribe `verificarVigencia(YearMonth mesActual)`. Si el donante no tuvo actividad en el mes anterior, la racha se rompe y el progreso se resetea a 0. El `RachaJob` llama `verificarRachasVencidas()` el primer día de cada mes. El mecanismo de racha está correctamente implementado.
- **Archivo:** `incentivos-service/.../MisionRacha.java`
- **Principio:** Correctitud funcional de misiones.

**C2-HAL-14 — `EntregasService.cambiarEstado()` tiene lógica de defaulting de replanificable**
- **Clasificación:** DECISIÓN DE NEGOCIO EN CAPA DE APLICACIÓN
- **Descripción:** En `procesarEntregaNoRecibida()` (línea 115): `boolean esReplanificable = replanificable == null || replanificable;`. Si el cliente no envía `replanificable` en el request, el sistema asume `true` por defecto. Esta decisión de defaulting debería estar documentada como decisión de negocio.
- **Archivo:** `logistica-service/.../EntregasService.java:115`
- **Principio:** Claridad de defaults de negocio.

### Resumen de hallazgos por criticidad (Checkpoint 2)

| ID | Criticidad | Tipo | Descripción breve |
|----|-----------|------|-------------------|
| C2-COR-01 | — | CORRECCIÓN | Adapters SÍ tienen implementación mock concreta |
| C2-COR-02 | — | CORRECCIÓN | Ranking SÍ persiste historial completo |
| C2-COR-03 | — | ACLARACIÓN | PropuestaService se auto-escucha via @EventListener |
| C2-HAL-01 | BAJO | DEUDA | Asignación antes de validación en constructor DI |
| C2-HAL-02 | MEDIO | COMPORTAMIENTO | Doble transición cuando replanificable=true |
| C2-HAL-03 | MEDIO | POSIBLE BUG | AlgoritmoPrioridadSubAtendidos ignora NecesidadRecurrente |
| C2-HAL-04 | BAJO | COSMÉTICO | NecesidadExtraordinaria implements Asignable redundante |
| C2-HAL-05 | MEDIO | BUG POTENCIAL | LocalDate.now() sin ZoneId en PlanificadorDeRutas |
| C2-HAL-06 | MEDIO | INCONSISTENCIA | Nuevo GET /donaciones-independientes sin prefijo /api/ confirmado |
| C2-HAL-07 | — | POSITIVO | Tests nuevos endpoint completos y de alta calidad |
| C2-HAL-08 | BAJO | INFERENCIA | consolidar() prioriza algoritmo 2 sin documentar |
| C2-HAL-09 | BAJO | RESUELTO | EntidadBeneficiariaController mapeado en /api/entidades (código es canónico) |
| C2-HAL-10 | BAJO | DOCUMENTACIÓN | Limitación documentada en Postman ya no aplica con nuevo GET |
| C2-HAL-11 | BAJO | INCONSISTENCIA | ZoneId.systemDefault() vs ZoneId.of("UTC") entre servicios |
| C2-HAL-12 | BAJO | DEUDA | TODO vacío en NecesidadesService |
| C2-HAL-13 | — | POSITIVO | MisionRacha.verificarVigencia() correctamente implementado |
| C2-HAL-14 | BAJO | DECISIÓN | Default replanificable=true cuando no se envía en request |

---

## 22. Delta post-auditoría — Commits no contemplados (2026-08-28)

> Cuatro commits entraron después de que se generó el Checkpoint 2. Se documentan aquí sus impactos sobre hallazgos previos.

### 22.1 `521dd98b` — Fix build de donaciones-service

**Archivos:** `donaciones-service/src/main/java/grupo5/donaciones/infrastructure/CatalogDataInitializer.java` (nuevo)

**Descripción:** Se agregó un `CommandLineRunner` que siembra en memoria las categorías y subcategorías del catálogo al iniciar el servicio:
- 3 categorías: Alimentos (kg), Ropa (unidades, usada=true), Muebles (unidades, usada=true)
- 8+ subcategorías con sus aliases para normalización semántica

Está anotado con `@ConditionalOnProperty(name = "donatrack.catalogo.seed-enabled", havingValue = "true", matchIfMissing = true)`. El `matchIfMissing = true` implica que si la propiedad está ausente, **el seeder corre por defecto**.

**Impacto en la auditoría:** Componente de infraestructura no registrado en las secciones 3, 5.1 y 6.1. La existencia de este seeder explica cómo el sistema funciona con datos de catálogo sin persistencia real — hasta ahora era un aspecto NO DETERMINADO.

---

### 22.2 `1dd6e3ac` — Spotless apply

**Archivos:** `donaciones-service/src/main/resources/application.properties`

**Cambio:** `donatrack.catalogo.seed-enabled=${SEED_CATALOGO_ENABLED:false}` — el default pasó de `true` a `false`.

**Impacto:** El seeder del commit anterior **no corre en desarrollo por defecto** (la propiedad está explícitamente en `false`). En tests, `matchIfMissing=true` aún lo dispararía si la propiedad no está en el contexto de test. Para producción, hay que pasar `SEED_CATALOGO_ENABLED=true` como env var. El comentario `// TODO: Utilizar variables de entorno` en línea 9 con sintaxis Java (`//`) sigue sin corregirse — **hallazgo #8 del documento sigue vigente**.

---

### 22.3 `69ff6489` — Fix normalizacion de bienes pendientes con vencimiento (#808)

**Archivos:** `BienNormalizado.java`, `SegmentacionEventListener.java`, `ProcesadorDeDonaciones.java`, `ItemDonacionNormalizadoService.java`

#### Fix 1 — `BienNormalizado.java:43-51` (validación de vencimiento/estado)

**Antes:** La validación de `conVencimiento` y `!conVencimiento` solo se ejecutaba para ítems `ACEPTADO`. Los ítems `PENDIENTE_REVISION` podían crearse con `conVencimiento=true` pero sin `fechaVencimiento`, pasando validación sin error.

**Después:** La validación aplica a todos los estados **excepto** `PENDIENTE_REVISION`. El campo `conEstado` se movió fuera del bloque condicional y ahora aplica siempre. El comentario en código explica: "La subcategoría de un bien pendiente es solo un candidato provisional. Sus reglas de vencimiento se validan recién cuando la normalización queda resuelta."

**Clasificación:** BUG REAL resuelto. Tenía impacto funcional: donaciones con ítems perecederos podían normalizarse como pendientes sin fecha de vencimiento y luego fallar en el ciclo siguiente.

#### Fix 2 — Publicación de domain events (3 archivos)

**Antes:** `donacion.getDomainEvents().forEach(eventPublisher::publishEvent); donacion.clearDomainEvents();`

**Después:** `var eventos = donacion.getDomainEvents(); donacion.clearDomainEvents(); eventos.forEach(eventPublisher::publishEvent);`

El patrón anterior podía producir `ConcurrentModificationException` si `clearDomainEvents()` modificaba la lista mientras `forEach` iteraba sobre ella (en función de la implementación de `AgregadoConEventos`). El fix captura la lista antes de limpiarla. El commit ancestral `8fc7c5e3 Fix republicacion recursiva de eventos de donacion` había aplicado el mismo fix en `PropuestaDeAsignacionService` pero los otros 3 archivos quedaron sin actualizar hasta este commit.

**Archivos afectados:**
- `SegmentacionEventListener.java` (2 ocurrencias corregidas)
- `ProcesadorDeDonaciones.java` (1 ocurrencia corregida)
- `ItemDonacionNormalizadoService.java` (1 ocurrencia corregida)

**Clasificación:** BUG REAL resuelto. Afectaba el flujo de normalización en condiciones de concurrencia o si `AgregadoConEventos` usa una lista mutable compartida.

**Tests agregados:** `BienNormalizadoTest` (+28 tests), `NormalizadorSemanticoBienTest` (+26 tests), `ItemDonacionNormalizadoServiceTest` (+11 tests).

---

### 22.4 `96759f3f` — Flujo de postman

**Archivos:** `docs/postman/flujo-4-matching-asignacion-estados.json` (1086 insertions)

**Descripción:** El flujo 4 fue completamente reestructurado. Ahora tiene tres secciones:

| Sección | Descripción | Pasos |
|---------|-------------|-------|
| Setup | Crea categoría, subcategoría, persona donante, donante, persona jurídica, entidad beneficiaria | 6 |
| A. Camino feliz | EN_DEPOSITO → ASIGNACION_REALIZADA → LISTA_PARA_ENTREGAR → EN_TRASLADO → ENTREGADA, con casos negativos intermedios | 13 |
| B. Camino alternativo | EN_DEPOSITO → VENCIDA, con caso negativo sobre estado terminal | 7 |

**Hallazgos de esta actualización:**
- POSITIVO: El flujo ahora es autocontenido — crea sus propios datos de setup, elimina dependencia de datos pre-existentes.
- POSITIVO: Los pasos negativos (re-aprobar propuesta, repetir estado terminal) documentan el comportamiento esperado de los estados terminales.
- NUEVO: El Setup crea una categoría y subcategoría específicas para que el algoritmo de matching funcione en el flujo. Esto confirma que el catálogo debe estar seeded (o crearse vía API) para que el algoritmo de asignación encuentre coincidencias.
- ATENCIÓN: El paso 1b del flujo obtiene "ítems normalizados pendientes" y el paso 1c reclasifica manualmente a la subcategoría del Setup. Esto indica que el flujo de normalización automática puede no asignar la subcategoría correcta sin el catálogo seeded — o que la reclasificación manual es parte del flujo normal de trabajo.

---

### 22.5 Impacto sobre secciones previas del documento

| Sección | Hallazgo original | Estado actual |
|---------|------------------|--------------|
| Resumen #8 (MEDIO) | `//` en application.properties | SIGUE VIGENTE — Spotless no lo corrigió |
| Sección 14 (legacy #2) | `// TODO` con sintaxis inválida | SIGUE VIGENTE |
| Sección 3 (arquitectura) | Falta CatalogDataInitializer | ACTUALIZAR: nuevo componente de infraestructura descubierto |
| Sección 11 (tests) | Total ~2020 tests sin failures | ACTUALIZAR: +65 tests nuevos en donaciones → ~2085 tests totales |
| Sección 10.5 (Postman) | flujo-4 con cobertura media | ACTUALIZAR: flujo-4 completamente reestructurado con camino feliz + camino alternativo + negativos |
| C2-COR-01 (adapters) | Mock adapters corregido | Sin cambios |
| Bug: publicación de eventos | Mencionado en bitácora como Fix 8fc7c5e3 | RESUELTO en 3 archivos adicionales por 69ff6489 |
| Bug: BienNormalizado PENDIENTE_REVISION | No estaba en el documento | NUEVO HALLAZGO resuelto |

---

## 23. Checkpoint 3 — Integración, Requisitos, Endpoints (2026-08-28)

> **STOP post-Checkpoint 3**: Este checkpoint cubre las Fases I (Donaciones), J (Logística), K (Incentivos), L (Notificaciones) y M (Endpoints) del spec. La base de verificación es el estado actual del código tras los 4 commits del delta (sección 22).

---

### 23.1 FASE I — Donaciones: checklist específico

| Requisito | Estado | Evidencia | Observaciones |
|-----------|--------|-----------|---------------|
| Donacion original separada de DonacionIndependiente | ✅ | `Donacion.java` (AgregadoConEventos, 3 estados) vs `DonacionIndependiente.java` (AgregadoConEventos, 7 estados, State pattern). Vínculo solo por `donacionOriginalId` UUID. Repositorios y DTOs completamente independientes | Separación correcta a nivel semántico y estructural |
| Segmentación | ✅ | `SegmentadorComplejo` wired en `DomainServicesConfig.java`. Hereda de `AbstractSegmentador<GroupingKey>`. `SegmentadorSimple` existe como alternativa no activada | El segmentador activo agrupa por (donacionOriginalId, subcategoria, estado, fechaVencimiento, unidad) |
| Subcategoría como unidad mínima | ✅ | `SegmentadorComplejo.GroupingKey` incluye `Subcategoria` como parte de la clave de agrupación. Dos ítems de distinta subcategoría generan dos `DonacionIndependiente` distintas | CORRECTO: la subcategoría es la granularidad de clasificación |
| Perecederos | ✅ | `BienNormalizado` tiene `conVencimiento: boolean` y `fechaVencimiento: LocalDate`. `SegmentadorComplejo.GroupingKey` incluye `fechaVencimiento` — perecederos con distinta fecha generan DIs distintas. Bug de validación corregido en commit `69ff6489` | Perecederos del mismo lote con misma fechaVencimiento se agrupan correctamente |
| Estado usado/nuevo | ✅ | `Estado` enum en dominio (inducido por `conEstado` en Categoria). `SegmentadorComplejo.GroupingKey` incluye `Estado estado` — ropa usada y ropa nueva generan DIs distintas aunque sean la misma subcategoría | El estado es parte de la clave de segmentación |
| Necesidades recurrentes | ✅ | `NecesidadRecurrente` + `PeriodoNecesidad` (record inmutable). `PlanificadorDeNecesidades` scheduler (`0 0 0 * * ?`, medianoche) llama a `PlanificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes()` | **NUEVO (no estaba en Checkpoint 2)**: existe scheduler de renovación de períodos |
| Necesidades extraordinarias | ✅ | `NecesidadExtraordinaria` implementa `Asignable`, sin concepto de período. `isActiva()` siempre `true` | Sin vencimiento por diseño |
| CSV masivo | ✅ | `POST /api/donantes/archivos` acepta `ArchivoInputDTO { path: String }` → `ArchivoDonantesService.cargarArchivoDonantes()` → `@Async ImportadorService.procesarImportacionAsincronica()` → `LectorDonantesCSV.cargarDonantes(path)` | **ATENCIÓN**: el endpoint recibe una ruta del filesystem del servidor, no un multipart upload. El CSV debe estar accesible en el servidor. Esto limita usabilidad en entornos containerizados |
| Estados trazables | ✅ | `Donacion.historialEstados` (List<CambioEstadoDonacion>). `DonacionIndependiente` acumula historial via el State pattern (cada transición registra el cambio) | Historial completo e inmutable |
| Asignación | ✅ | `GestorPropuestasDeAsignacion` ejecuta `List<AlgoritmoAsignacion>`. `PlanificadorDeAlgoritmos` scheduler con cron configurable via `${planificador.algoritmos.cron.expression}` (default: `0 0 0 * * ?`) | El límite de 10 propuestas sigue hardcoded |
| Strategy de algoritmos | ✅ | `AlgoritmoAsignacion` (abstract + Template Method). `AlgoritmoCompatibilidadSemantica` y `AlgoritmoPrioridadSubAtendidos`. `DomainServicesConfig` inyecta ambos como `List<AlgoritmoAsignacion>` | CORRECTO: agregar un tercer algoritmo solo requiere un nuevo bean, sin modificar el gestor |
| Horario baja carga | ✅ | Default `0 0 0 * * ?` (medianoche). Configurable via `PLANIFICADOR_ALGORITMOS_CRON` env var | El `PlanificadorDeAlgoritmos` tiene un método auxiliar `paraCuandoEstaPlanificado()` que describe el cron en español — útil para debugging |
| Notificaciones asociadas | ✅ | `DonacionIndependienteNotificacionesListener` escucha 4 eventos de DI y llama a notificaciones vía Feign | Fan-in excesivo identificado en Checkpoint 2, sigue como deuda |

**Nuevos hallazgos de la Fase I:**

- **C3-HAL-01 — `PlanificadorDeNecesidades` scheduler no estaba en el inventario del Checkpoint 2** — Existe un segundo scheduler en donaciones-service (`0 0 0 * * ?`) que renueva períodos de necesidades recurrentes activas. `PlanificacionNecesidadesService.generarNuevosPeriodosParaNecesidadesRecurrentes()` usa `ZoneId.systemDefault()` — consistente con el resto de donaciones pero inconsistente con logística. CLASIFICACIÓN: INFORMATIVO.
  - **Archivo:** `donaciones-service/.../schedulers/PlanificadorDeNecesidades.java:18`

- **C3-HAL-02 — Importación CSV por path de filesystem, no multipart** — El endpoint `POST /api/donantes/archivos` recibe `{ "path": "/ruta/en/servidor.csv" }`, no un archivo subido. `LectorDonantesCSV` lee del filesystem del servidor. Esto es correcto para un TP local pero sería un problema en Dockerización si el CSV no está montado como volumen. CLASIFICACIÓN: DEUDA TÉCNICA / LIMITACIÓN DE DISEÑO.
  - **Archivo:** `donaciones-service/.../controllers/impl/DonantesController.java:67`, `ArchivoDonantesService.java:24`

- **C3-HAL-03 — `SegmentadorSimple` existe pero no está activado** — `SegmentadorSimple` extiende `AbstractSegmentador` pero `DomainServicesConfig` instancia `SegmentadorComplejo`. `SegmentadorSimple` puede ser código legado o una alternativa de fallback no documentada. CLASIFICACIÓN: CÓDIGO POTENCIALMENTE LEGACY.
  - **Archivo:** `donaciones-service/.../models/segmentacion/SegmentadorSimple.java`

- **C3-HAL-04 — `ImportadorService.procesarImportacionAsincronica()` marca el `Archivo` en 3 estados (PROCESANDO, finalizado con errores, ERROR)** — Hay un `Archivo` aggregate con ciclo de vida propio que no estaba inventariado. El cliente puede hacer polling del estado del archivo vía el ID devuelto por el endpoint. CLASIFICACIÓN: INFORMATIVO, buen diseño async.
  - **Archivo:** `donaciones-service/.../services/impl/ImportadorService.java:56-85`

---

### 23.2 FASE J — Logística: checklist específico

| Requisito | Estado | Evidencia | Observaciones |
|-----------|--------|-----------|---------------|
| Flota | ✅ | `Camion.java` (AggregateRoot). `ICamionRepository` + `CamionRepository` en memoria. `CamionesService` + `CamionesController` (`/api/camiones`) | 3 estados: DISPONIBLE, EN_RUTA, DESHABILITADO |
| Capacidades | ✅ | `Camion` tiene `capacidadKg: Double` y `capacidadVolumen: Double`. `AsignadorDeEntregasPorDimension` verifica ambas antes de asignar una entrega a un camión | Greedy: primer camión disponible con capacidad suficiente |
| Planificación de rutas | ✅ | `GeneradorDeRutas` genera solicitudes, `PlanificadorDeRutas` las procesa, `IServicioExternoPlanificacion` es el port, `ProveedorExternoPlanificacionSimulado` es la implementación mock | El proveedor externo real no existe — todo es simulado en memoria |
| Procesamiento por lotes ≤ 100 | ✅ | `GeneradorDeRutas.MAX_ENTREGAS_POR_SOLICITUD = 100`. `GeneradorLotes` port implementado por `GeneradorLotesSimple` | CORRECTO: el límite está como constante nombrada, no hardcoded inline |
| Callback externo | ✅ | `POST /api/logistica/callback/rutas` en `PlanificacionController`. `PlanificacionService.procesarCallback()` mapea la respuesta y persiste | El proveedor simulado llama al callback en el mismo proceso — en producción sería una llamada HTTP externa |
| Inicio de ruta | ✅ | `PATCH /api/rutas/{id}/estado` con body `{ "estado": "EN_TRASLADO" }` → `RutasService.iniciarRuta()` → `GestorDeRutas.iniciarRuta()` → valida entregas PENDIENTES → `Ruta.iniciarRuta()` → publica `EventoRutaIniciada` via RabbitMQ | Estado del camión y chofer se actualiza a EN_RUTA en el mismo método |
| Trazabilidad de entregas | ✅ | `GET /api/entregas/{id}/historial` retorna `List<CambioEstadoEntrega>` con timestamps y actor | CORRECTO: historial inmutable por entrega |
| Confirmación de recepción (entrega exitosa) | ✅ | `PATCH /api/entregas/{id}/estado` con `estado: ENTREGADA` + `horaArribo` + `foto` → `Entrega.confirmarEntrega()` → emite `EntregaConfirmada` → RabbitMQ → donaciones avanza DI a ENTREGADA | Foto de recepción es opcional en el modelo pero el test cubre adjuntarFotoRecepcion |
| Entrega fallida / no recibida | ✅ | `PATCH /api/entregas/{id}/estado` con `estado: NO_RECIBIDA` + `justificacion` → `Entrega.negarEntrega()` → `NO_RECIBIDA` → auto-transition a `REVISION` (método privado `mandarARevision`) → emite `EntregaFallida` → RabbitMQ | La transición automática NO_RECIBIDA → REVISION es implícita y no documentada en Javadoc |
| Replanificación cuando corresponda | ✅ | `PATCH /api/entregas/{id}/estado` con `estado: PENDIENTE` → `Entrega.regresarAlDeposito()` (acepta desde NO_RECIBIDA o REVISION) → resetea `horaArribo`, `horaSalida`, `idRuta` → entrega vuelve a PENDIENTE y puede re-planificarse | El default de `replanificable=true` cuando no se envía en el request (ver C2-HAL-14) sigue presente |

**Nuevos hallazgos de la Fase J:**

- **C3-HAL-05 — `AlgoritmoOrdenadorSimple` y `AlgoritmoOrdenarSimple` son clases duplicadas** — El inventario de archivos muestra `AlgoritmoOrdenadorSimple.java` Y `AlgoritmoOrdenarSimple.java` en el mismo paquete. Sus nombres son casi idénticos (con/sin acento en "Ordenar"). CLASIFICACIÓN: POSIBLE DUPLICADO / CÓDIGO LEGACY.
  - **Archivos:** `logistica-service/.../planificacion/AlgoritmoOrdenadorSimple.java`, `AlgoritmoOrdenarSimple.java`

- **C3-HAL-06 — `Chofer` tiene estado y ciclo de vida pero no emite domain events** — `Chofer` (similar a `Camion`) tiene `EstadoChofer` y `CambioEstadoChofer` historial, pero no extiende `AgregadoConEventos`. Consistente con `Camion`. Sin domain events en la flota de activos. CLASIFICACIÓN: INFORMATIVO / DISEÑO CONSCIENTE.

---

### 23.3 FASE K — Incentivos: checklist específico

| Requisito | Estado | Evidencia | Observaciones |
|-----------|--------|-----------|---------------|
| Analítica de actividad | ✅ | `Metricas` (value object mutable con `donacionesPorPeriodo: Map<YearMonth, Long>`). `GET /api/incentivos/donantes/{id}/metricas` | `MetricasIncentivosController` expone las métricas del donante |
| Misiones secuenciales | ✅ | `getMisionActiva()` filtra por `m.getCategoria() == this.categoria` y `!m.isCompletada()`, ordenando por `numeroMision`. El donante no puede saltar categoría | Secuencialidad garantizada por filtro en dominio, no por lógica de aplicación |
| Categorías de donante | ✅ | `CategoriaDonante` enum: COLABORADOR → SOSTENEDOR → TRANSFORMADOR. `ascender()` verifica que TODAS las misiones de la categoría actual estén completadas antes de ascender | Condición de ascenso correctamente validada en el aggregate |
| Insignias | ✅ | `Insignia` + `InsigniaGanada`. `GET /api/incentivos/donantes/{id}/insignias` (solo visibles). `PATCH .../insignias` permite configurar visibilidad | `MisionFactory.crearMisionesEstandar()` asigna insignia a cada misión |
| Progreso | ✅ | Cada `Mision` tiene `progreso: int` y `objetivo: int`. `evaluarProgreso()` es el Template Method que llama `calcularNuevoProgreso()` (abstracto por tipo de misión) | Progreso por tipo: Racha (meses consecutivos), Completitud (categorías distintas), HabilDonador (cantidad por donación), DonacionesExitosas (entregas confirmadas) |
| Pérdida de progreso | ✅ | `MisionRacha.verificarVigencia(YearMonth)` resetea `progreso=0` si el donante no donó el mes anterior. `RachaJob` cron `0 5 0 1 * *` (primer día del mes) invoca `verificarRachasVencidas()` | Pérdida de progreso solo aplica a misiones de racha — correcto |
| Ranking mensual | ✅ | `GestorDeRankings.calcular()` ordena por misiones completadas en el período (desc) → donaciones en el período (desc) → id. `RankingMensualJob` cron `0 59 23 L * *` (último día del mes) | Historial completo persiste en `IRankingRepository` |
| Top 3 | ✅ | `RankingMensual.getPodio()` retorna `entradas.stream().limit(3).toList()` — los 3 primeros del ranking | Usado por N8N para publicar el podio |
| Publicación automatizada (N8N) | ✅ | `N8nClientAdapter` vía WebClient async (fire-and-forget). `NotificacionesIncentivosListener.onMisionCompletada()` llama a N8N para insignias. `RankingMensualJob` llama a `n8nClient.notificarRankingCalculado()` | Errores de N8N se absorben con `log.warn` — correcto para canal de best-effort |
| Integración con actividad de donaciones | ✅ | `POST /api/incentivos/donaciones` (registrarDonacion) y `POST /api/incentivos/donaciones/exitosa` (registrarDonacionExitosa). Llamados desde donaciones vía `IncentivosFeignClient` | Integración síncrona con try/catch silencioso en donaciones |

---

### 23.4 FASE L — Notificaciones: checklist específico

| Requisito | Estado | Evidencia | Observaciones |
|-----------|--------|-----------|---------------|
| Strategy o equivalente por medio | ✅ | `NotificacionSender` port (interface con overloading por tipo). `NotificacionRouter` implementa dispatch polimórfico: `MedioDeContacto.enviarMensaje(msg, sender)` → `sender.enviarA(this, msg)` según tipo concreto | Double-dispatch correcto: el router no hace instanceof |
| Email | ✅ | `CorreoAdapter` interface + `CorreoEnvioMock` (@Component, loguea y retorna true) | Implementación mock funcional en producción. Sin implementación real (SendGrid, etc.) |
| SMS | ✅ | `TelefonoAdapter` interface + `TelefonoEnvioMock` (@Component, loguea SMS) | Idem |
| WhatsApp | ✅ | `WhatsAppAdapter` interface + `WhatsappEnvioMock` (@Component, loguea WA) | Idem |
| Eventos mínimos: inactividad | ✅ | `DonanteInactivo` implements `EventoNotificable`. Disparado por `InactividadJob` → `InactividadService` → Feign → notificaciones | |
| Eventos mínimos: asignación de donación | ✅ | `DonacionAsignada` implements `EventoNotificable`. Llamado desde `DonacionIndependienteNotificacionesListener.onEventoDonacionAsignada()` | |
| Eventos mínimos: misión cumplida | ✅ | `MisionCumplida` implements `EventoNotificable`. Disparado por `NotificacionesIncentivosListener.onMisionCompletada()` desde incentivos | |
| Eventos mínimos: cambio de categoría | ✅ | `SubioCategoria` implements `EventoNotificable`. Disparado por `NotificacionesIncentivosListener.onAscensoDonante()` desde incentivos | |
| Eventos mínimos: inicio de ruta | ✅ | `DonacionEnCamino` implements `EventoNotificable`. Disparado por `DonacionIndependienteNotificacionesListener.onEventoRutaIniciada()` | |
| Eventos mínimos: entrega exitosa | ✅ | `DonacionRecibida` implements `EventoNotificable`. Disparado por `DonacionIndependienteNotificacionesListener.onEventoDonacionRecibida()` | |
| Eventos mínimos: entrega fallida | ✅ | `EntregaFallida` implements `EventoNotificable`. Disparado por `DonacionIndependienteNotificacionesListener.onEventoDonacionFallida()` | Notifica a donante, beneficiaria Y administrador (3 destinatarios) |
| Abstracción de canal | ✅ | `NotificacionSender` port en domain. `NotificacionRouter` en infrastructure. Las clases de dominio no dependen de infraestructura concreta | |

**Hallazgo adicional de la Fase L:**

- **C3-HAL-07 — `DonanteRegistrado` es el octavo `EventoNotificable` no requerido explícitamente** — El spec lista 7 casos mínimos. `DonanteRegistrado` es un caso adicional implementado: al crear un donante, se envía una notificación de bienvenida. CLASIFICACIÓN: POSITIVO — implementación por encima del mínimo requerido.
  - **Archivo:** `notificaciones-service/.../models/events/DonanteRegistrado.java`

---

### 23.5 FASE M — Endpoints: inventario completo actualizado

**Actualización respecto al Checkpoint 2:** El commit `96759f3f` actualizó `flujo-4` pero no agregó nuevos endpoints al código. Los endpoints del commit `521dd98b` (CatalogDataInitializer) son internos (CommandLineRunner), no HTTP.

#### Donaciones (:8080) — Endpoints verificados con rutas exactas

| Endpoint | Método | Nota |
|----------|--------|------|
| `/api/donaciones` | POST, GET | CRUD donaciones |
| `/api/donaciones/{id}` | GET | Obtener por ID |
| `/donaciones-independientes` | GET | **Sin prefijo /api/** |
| `/donaciones-independientes/{id}` | GET | **Sin prefijo /api/** |
| `/donaciones-independientes/{id}/estado` | PATCH | Transiciones de estado (requiere X-Actor header) |
| `/api/asignaciones/ejecuciones` | POST, GET | Ejecutar + historial asignaciones |
| `/api/asignaciones/propuestas` | GET | Listar propuestas |
| `/api/asignaciones/propuestas/{id}/estado` | PUT | Aprobar o descartar propuesta |
| `/api/donantes` | POST, GET | CRUD donantes |
| `/api/donantes/{id}` | GET, DELETE | Obtener o eliminar donante |
| `/api/donantes/archivos` | POST | **CSV masivo** — recibe `{ "path": "..." }`, no multipart |
| `/api/personas` | POST, GET | CRUD personas |
| `/api/personas/{id}` | PUT, DELETE | Actualizar o eliminar persona (GET /{id} no implementado en controller) |
| `/api/entidades` | POST, GET | Alta y listado de entidades beneficiarias |
| `/api/entidades/{id}` | GET, PUT, DELETE | Consulta, actualización y eliminación de entidad beneficiaria |
| `/api/necesidades` | POST, GET | Alta y listado de necesidades |
| `/api/necesidades/{id}` | GET, PUT, DELETE | Consulta, actualización y eliminación de necesidad (usa PUT, no PATCH) |
| `/api/categorias` | POST, GET, PUT, DELETE | CRUD de categorías |
| `/api/subcategorias` | POST, GET, PUT, DELETE | CRUD de subcategorías (soporta POST/DELETE en `/{id}/aliases`) |
| `/api/items-normalizados/pendientes` | GET | Listar ítems normalizados pendientes de revisión |
| `/api/items-normalizados/{id}` | GET, PATCH | Consulta y reclasificación manual de un ítem (flujo de revisión manual) |

**Aclaración crítica respecto al Checkpoint 2:** El endpoint de importación CSV correcto es `POST /api/donantes/archivos` (no `/api/donantes/importar-csv`). El controller es `DonantesController` (mismo que maneja el CRUD de donantes), no un `ArchivoDonantesController` separado. El endpoint anterior en el doc era incorrecto.

#### Logística (:8083) — Sin cambios respecto a Checkpoint 2

#### Incentivos (:8082) — Sin cambios respecto a Checkpoint 2

#### Notificaciones (:8081) — Sin cambios respecto a Checkpoint 2

---

### 23.6 Resumen de nuevos hallazgos del Checkpoint 3

| ID | Criticidad | Descripción |
|----|-----------|-------------|
| C3-HAL-01 | INFORMATIVO | PlanificadorDeNecesidades scheduler (medianoche) no estaba en inventario |
| C3-HAL-02 | MEDIO | CSV import recibe path de filesystem, no multipart — limitación en Docker |
| C3-HAL-03 | BAJO | SegmentadorSimple existe pero no está activado — posible legacy |
| C3-HAL-04 | POSITIVO | ImportadorService es async con ciclo de vida de Archivo (PROCESANDO/FINALIZADO/ERROR) |
| C3-HAL-05 | BAJO | AlgoritmoOrdenadorSimple y AlgoritmoOrdenarSimple son clases con nombre casi idéntico en mismo paquete |
| C3-HAL-06 | INFORMATIVO | Chofer sin domain events, igual que Camion — decisión consciente |
| C3-HAL-07 | POSITIVO | DonanteRegistrado como octavo EventoNotificable (por encima del mínimo del spec) |
| C3-HAL-08 | CORRECCIÓN | Endpoint CSV es POST /api/donantes/archivos, no /api/donantes/importar-csv como se indicó en Checkpoint 2 |

---

**STOP — Checkpoint 3 completado. Esperando validación antes de continuar con Checkpoint 4.**

---

## 24. Checkpoint 4 — Tests, Docker, Hardening, Legacy (2026-08-28)

> **STOP post-Checkpoint 4**: Cubre las Fases P (Tests), O (Docker), Q (Hardening), T (Legacy), R (Persistencia), S (Seguridad) y U (common-lib) del spec.

---

### 24.1 FASE P — Tests: inventario clasificado actualizado

**Conteo por servicio (excl. Mother/Fixture helpers):**

| Servicio | Clases de test | Tests aprox. | Nota |
|---------|---------------|-------------|------|
| donaciones-service | 75 | ~855 | +65 respecto a Checkpoint 2 por commits recientes |
| logistica-service | 32 | ~582 | Sin cambios |
| incentivos-service | 31 | ~378 | Sin cambios |
| notificaciones-service | 18 | ~232 | Sin cambios |
| common-lib | 7 | ~38 | Sin cambios |
| integration-tests | 8 clases de test (excl. helpers) | N/A (requieren servicios) | RestAssured + @SpringBootTest contra servicios corriendo |
| **TOTAL** | **171** | **~2085** | |

**Clasificación por tipo:**

| Tipo | Servicio | Clases ejemplo | Riesgo |
|------|---------|----------------|--------|
| Domain unit (JUnit puro) | todos | DonacionTest, EntregaTest, DonanteIncentivosTest, MisionesTest, NotificacionTest | BAJO |
| Application Service (Mockito) | todos | DonacionesServiceTest, PlanificacionServiceTest, RankingServiceTest, NotificacionServiceTest | BAJO |
| Controller (MockMvc/WebMvcTest) | todos | DonacionesControllerTest, CamionesControllerTest, RankingControllerTest, NotificacionControllerTest | BAJO |
| Listener / Infraestructura | donaciones, logistica | SegmentacionEventListenerTest, DonacionIndependienteNotificacionesListenerTest, ComunicadorEventosLogisticaRabbitTest | MEDIO |
| Scheduler | donaciones, incentivos, logistica | PlanificadorDeAlgoritmosTest, PlanificadorDeNecesidadesTest, InactividadJobTest, RachaJobTest, RankingMensualJobTest | BAJO |
| Mapper | todos | PersonaMapperTest, EntregaMapperTest, EventoMapperTest, MedioDeContactoMapperTest | BAJO |
| DTO Validation | todos | DtoValidationTest (logistica, 36 casos), DTOValidationTest (incentivos), ControllersWebMvcValidationTest | BAJO |
| Repository (en memoria) | donaciones, notificaciones | DonacionRepositoryTest, PersonasRepositoryTest, NotificacionRepositoryEnMemoriaTest | BAJO |
| Algoritmo / Domain Service | donaciones, logistica, incentivos | AlgoritmoCompatibilidadSemanticaTest, AlgoritmoPrioridadSubAtendidosTest, GestorPropuestasDeAsignacionTest, AlgoritmoAsignadorDeEntregaTest | BAJO |
| Startup / Context | donaciones, incentivos | DonacionesServiceApplicationTest (@SpringBootTest), IncentivosServiceApplicationTest (@SpringBootTest NONE) | MEDIO |
| E2E distribuido | integration-tests | FullDistributedDonationE2EIT, CrossServiceCommunicationIT, PerformanceStressIT | ALTO (requieren infraestructura) |

**Hallazgos específicos:**

- **POSITIVO: Único test @Disabled está justificado** — `AlgoritmoAsignadorDeEntregaTest.java:143`: `@Disabled("Deuda: Entrega no modela altura — ver Javadoc de AsignadorDeEntregasPorDimension")`. El test documenta por qué está deshabilitado (deuda de modelo) en lugar de simplemente ignorarse.
- **POSITIVO: Object Mother pattern exhaustivo** — Donaciones tiene 7 Mothers (BienMother, CategoriaMother, DonacionIndependienteMother, DonacionMother, DonanteMother, NecesidadMother, PersonaMother, PropuestaMother). Logística tiene 5. Incentivos tiene 5. Notificaciones tiene 4.
- **POSITIVO: integration-tests módulo independiente** — `FullDistributedDonationE2EIT`, `CrossServiceCommunicationIT`, `PerformanceStressIT`, `TracingContractIT`. Infraestructura de test bien armada con ApiClients, Builders y PollingUtils. `ContractIT` verifica contratos de API. No se ejecutan en `mvn test` estándar — requieren que los servicios estén corriendo.
- **REVISAR: Magic strings en controller tests** — Valores de enum serializado hardcoded como `String` en tests de controller (e.g., `"ALIMENTO"`, `"EXTRAORDINARIA"`, `"RECURRENTE"` en `NecesidadesControllerTest`). El riesgo es que si el enum cambia de nombre, el test compila pero falla. Preferible usar `TipoNecesidad.ALIMENTO.name()`.
- **ACEPTABLE: "Calle Falsa 123"** — El valor de dirección hardcoded en tests de donaciones es una convención común para datos de prueba ficticios. No es riesgo.
- **REVISAR: `DonacionesServiceApplicationTest` carga contexto completo** — `@SpringBootTest` sin `webEnvironment = NONE` levanta el contexto web completo incluyendo el startup de RabbitMQ. Si RabbitMQ no está disponible al correr tests unitarios, el contexto puede tardar o reintentar. En la práctica funciona porque la configuración de RabbitMQ tiene `try/catch` en el startup, pero es frágil.
- **SIN tests de idempotencia** — No existe ningún test que simule doble-entrega de un mensaje RabbitMQ en `LogisticaEventListener`. El comportamiento de re-entrega no está cubierto.
- **SIN tests de fallo de servicios externos en listeners** — No hay tests que verifiquen qué pasa cuando `incentivosFeignClient` falla durante `SegmentacionEventListener.onDonacionNormalizada()`. El `try/catch` silencioso no está testeado.

---

### 24.2 FASE O — Docker / Despliegue: checklist por servicio

**Dockerfiles — verificación por servicio:**

| Servicio | Dockerfile | Multi-stage | Etapas | Base image | Curl instalado | Observaciones |
|---------|-----------|-------------|--------|-----------|---------------|---------------|
| donaciones | ✅ | ✅ | builder, ci, local | eclipse-temurin:21-jre | ✅ | BUILD_ARG SERVICE_NAME para reuso. Puerto EXPOSE 8080 |
| incentivos | ✅ | ✅ | builder, ci, local | eclipse-temurin:21-jre | ✅ | Mismo patrón. Puerto EXPOSE 8082 |
| logistica | ✅ | ✅ | builder, ci, local | eclipse-temurin:21-jre | ✅ | Mismo patrón. Puerto EXPOSE 8083 |
| notificaciones | ✅ | ✅ | builder, ci, local | eclipse-temurin:21-jre | ✅ | Mismo patrón. Puerto EXPOSE 8081 |

**Observación clave de los Dockerfiles:** La etapa `builder` corre `mvn clean package -pl ${SERVICE_NAME} -am -DskipTests`. El flag `-am` (also-make) instala `common-lib` como dependencia antes de compilar el servicio. Esto resuelve en Docker el problema de build aislado identificado en el Checkpoint 1.

**docker-compose.yml (local) — verificación:**

| Servicio | Puerto | Variables env | depends_on | Healthcheck | Observación |
|---------|--------|-------------|-----------|------------|-------------|
| notificaciones | 8081:8081 | PORT, EXECUTION_ID | — | ❌ `service_started` | Sin healthcheck Java — puede responder antes de estar listo |
| incentivos | 8082:8082 | PORT, NOTIFICACIONES_SERVICE_URL, EXECUTION_ID, N8N_*_WEBHOOK_URL | notificaciones, n8n (`service_started`) | ❌ `service_started` | Idem |
| donaciones | 8080:8080 | PORT, NOTIFICACIONES_URL, INCENTIVOS_URL, LOGISTICA_URL, EXECUTION_ID, RABBITMQ_HOST | notificaciones, incentivos, logistica (`service_started`), rabbitmq (`service_healthy`) | ❌ `service_started` | Solo RabbitMQ tiene healthcheck |
| logistica | 8083:8083 | PORT, EXECUTION_ID, LOGISTICA_MAX_DONACIONES_POR_LOTE, RABBITMQ_HOST | rabbitmq (`service_healthy`) | ❌ `service_started` | Correcto: solo depende de RabbitMQ |
| rabbitmq | 5672, 15672 | RABBITMQ_DEFAULT_USER, RABBITMQ_DEFAULT_PASS | — | ✅ `rabbitmq-diagnostics -q ping` | |
| n8n | 5678:5678 | N8N_HOST, PORT, PROTOCOL, SECURE_COOKIE | — | ❌ sin healthcheck local | |

**RIESGO LOCAL:** Los servicios Java usan `service_started` — Docker Compose inicia el siguiente servicio cuando el contenedor arranca, no cuando Spring Boot terminó de inicializar. En una máquina lenta o con muchos servicios, `donaciones-service` puede intentar conectarse a `incentivos-service` antes de que este esté listo para recibir requests Feign.

**docker-compose.preprod.yml — verificación:**

| Servicio | Healthcheck | Condition | Observación |
|---------|------------|-----------|-------------|
| notificaciones | ✅ `curl -f /actuator/health` | — (no tiene dependencias) | start_period: 60s |
| incentivos | ✅ `curl -f /actuator/health` | `service_healthy`: notificaciones, n8n | Correcto |
| donaciones | ✅ `curl -f /actuator/health` | `service_healthy`: notificaciones, incentivos, logistica, rabbitmq | Correcto |
| logistica | ✅ `curl -f /actuator/health` | `service_healthy`: rabbitmq | Correcto |
| rabbitmq | ✅ `rabbitmq-diagnostics` | — | Sin ports expuestos en preprod — CORRECTO (seguridad) |
| n8n | ✅ `node -e http.get healthz` | — | healthcheck más sofisticado |

**NUEVO hallazgo Docker:**

- **C4-HAL-01 — `docker-compose.preprod.yml` sin volumen `n8n_data`** — El docker-compose local define `volumes: n8n_data: (named)` y lo monta en `/home/node/.n8n`. El docker-compose preprod solo monta `./n8n:/etc/n8n/workflows` pero **no define ni monta `n8n_data`**. Los workflows de n8n no persisten entre reinicios en preprod. Si el contenedor n8n se reinicia, los workflows configurados se pierden. CLASIFICACIÓN: RIESGO MEDIO — la configuración de n8n debe sobrevivir reinicios.
  - **Archivo:** `docker-compose.preprod.yml:138-156`

- **C4-HAL-02 — `SEED_CATALOGO_ENABLED` no está en ningún docker-compose** — La variable `SEED_CATALOGO_ENABLED` controla si se siembra el catálogo de categorías y subcategorías. No está definida en ninguno de los dos docker-compose. El valor por defecto en `application.properties` es `false`. Esto significa que en los entornos Docker, el catálogo **no se siembra automáticamente** — el sistema arranca sin categorías ni subcategorías, y el algoritmo de matching no puede funcionar hasta que se creen via API. CLASIFICACIÓN: RIESGO MEDIO — para demos y entornos de test esto puede causar sorpresa.
  - **Archivos:** `docker-compose.yml`, `docker-compose.preprod.yml`

- **C4-HAL-03 — Sin RABBITMQ_PASS personalizada en docker-compose** — Las credenciales `donatrack`/`donatrack` están hardcodeadas en ambos compose. En preprod, RabbitMQ no expone puertos — mitigación válida para TP. En local, el puerto 15672 (management) está expuesto, lo que permite acceso al panel de RabbitMQ con las credenciales hardcodeadas. CLASIFICACIÓN: BAJO para TP académico.

---

### 24.3 FASE Q — Hardening: checklist explícito

| Aspecto | Aplica | Estado | Clasificación | Evidencia |
|---------|--------|--------|--------------|-----------|
| Idempotencia eventos RabbitMQ | APLICA | ❌ NO IMPLEMENTADO | RIESGO ALTO | `LogisticaEventListener` no registra IDs de mensajes procesados. Re-entrega RabbitMQ de `ruta.asignada` → intento de avanzar DI ya en estado posterior → `BusinessStateException` silenciosa |
| Event ordering (orden entre queues) | APLICA | ⚠️ PARCIAL | RIESGO MEDIO | Dentro de cada routing key, RabbitMQ garantiza FIFO. Pero `ruta.asignada` y `ruta.iniciada` pueden procesarse fuera de orden si hay múltiples consumers o retry. No hay mecanismo de version/sequence en los eventos |
| Event time vs processing time | APLICA | ⚠️ PARCIAL | RIESGO BAJO | Los eventos RabbitMQ no portan timestamp de creación explícito — solo el payload de negocio. Si se procesa con delay, no hay forma de saber cuándo ocurrió el evento original |
| Concurrencia en repositorios en memoria | APLICA | ✅ IMPLEMENTADO | BAJO | `CrudRepositoryEnMemoria` usa `ConcurrentHashMap` — thread-safe para operaciones individuales |
| Concurrencia en lógica de negocio | APLICA | ❌ NO GARANTIZADO | RIESGO MEDIO | No hay coordinación explícita entre el thread `@Async` de `ProcesadorDeDonaciones` y un posible segundo request sobre la misma donación. En memoria no es crítico, en BD con JPA sería un problema de LOST UPDATE |
| MDC / TraceId propagación | APLICA | ✅ IMPLEMENTADO | BAJO | `FeignTraceRequestInterceptor` propaga `X-Trace-Id` en todos los Feign calls. `MdcTaskDecorator` propaga MDC en threads async de incentivos. `ControllerLoggingInterceptor` loguea request/response. `ScheduledJobLoggingAspect` loguea inicio/fin de schedulers |
| Thread pool dedicado para @Async | APLICA | ⚠️ PARCIAL | RIESGO MEDIO | Incentivos tiene `notificacionesTaskExecutor` (core=2, max=10, queue=500). Donaciones usa el pool default de Spring para `ProcesadorDeDonaciones`. Sin pool dedicado, tareas async compiten con el thread pool general de Spring MVC |
| Determinismo en schedulers (timezone) | APLICA | ⚠️ INCONSISTENTE | RIESGO BAJO | Logística usa `ZoneId.of("UTC")` consistentemente. Donaciones usa `ZoneId.systemDefault()` en schedulers y domain objects. En Docker (UTC), son equivalentes |
| Duplicados en mensajes | APLICA | ❌ NO IMPLEMENTADO | RIESGO ALTO | No hay deduplicación de mensajes en `LogisticaEventListener`. Mismo mensaje RabbitMQ procesado dos veces puede causar doble transición de estado |
| Null semantics (null safety) | APLICA | ✅ BIEN MANEJADO | BAJO | `Boolean.TRUE.equals(...)` en notificaciones para null-safety. Validaciones con `ErrorCatalog` en constructores de domain objects. Optional para repositorios |
| Circuit breaker para Feign calls | APLICA | ❌ NO IMPLEMENTADO | RIESGO MEDIO | Si `incentivos-service` o `notificaciones-service` están caídos, las llamadas Feign desde donaciones fallan con excepción absorbida silenciosamente. Sin retry guarantizado ni circuit breaker (Resilience4j) |
| Timeout en Feign calls | APLICA | ❓ NO DETERMINADO | RIESGO MEDIO | `FeignRetryConfig` existe como clase en el classpath pero no fue auditado en detalle. Sin timeout configurable, un Feign call puede bloquear el thread indefinidamente si el servicio destino no responde |
| Async fire-and-forget sin confirmación | APLICA | ✅ INTENCIONAL | BAJO | N8N calls son WebClient `.subscribe()` — fire-and-forget explícito. Correcto para notificaciones opcionales |
| Liveness vs readiness en healthchecks | APLICA | ⚠️ PARCIAL | BAJO | `/actuator/health` responde liveness pero no distingue readiness (si el servicio está listo para recibir tráfico vs meramente vivo). Solo relevante si se usa Kubernetes |

---

### 24.4 FASE T — Legacy: inventario

| # | Hallazgo | Archivo | Tipo | Criticidad |
|---|---------|---------|------|-----------|
| T-01 | `// TODO:` vacío sin descripción | `donaciones-service/.../NecesidadesService.java:19` | TODO sin descripción | BAJO |
| T-02 | Comentario `//` con sintaxis Java en `.properties` | `donaciones-service/src/main/resources/application.properties:9` | Sintaxis inválida | BAJO |
| T-03 | `AlgoritmoOrdenarSimple.java` — clase duplicada de `AlgoritmoOrdenadorSimple.java` | `logistica-service/.../planificacion/AlgoritmoOrdenarSimple.java` | Duplicado confirmado | BAJO |
| T-04 | `AlgoritmoOrdenadorSimple.java` — SIN documentación de deuda | `logistica-service/.../planificacion/AlgoritmoOrdenadorSimple.java` | El wired no tiene el Javadoc; el non-wired sí | COSMÉTICO |
| T-05 | `SegmentadorSimple.java` — no wired, posible legacy | `donaciones-service/.../segmentacion/SegmentadorSimple.java` | Clase sin uso observable | BAJO |
| T-06 | Packages vacíos: `notificaciones-service/.../exceptions/` | `notificaciones-service/src/main/java/grupo5/notificaciones/exceptions/` | Package vacío | COSMÉTICO |
| T-07 | Wildcard imports en logistica (`dto.entregas.*`, `dto.rutas.*`, `models.entities.entregas.*`) | `IEntregasService.java`, `EntregasService.java`, `IRutasService.java` | Wildcard import | BAJO |
| T-08 | Wildcard imports en notificaciones controllers (`org.springframework.web.bind.annotation.*`) | `NotificacionController.java`, `PersonasController.java` | Wildcard import | BAJO |
| T-09 | Packages intermedios vacíos como nodos de directorio (e.g., `grupo5/donaciones/models/`) | múltiples | Artefactos normales de Maven | INFORMATIVO (no es deuda real) |
| T-10 | `AdminSeeder.java` duplicado: existe en donaciones-service y notificaciones-service | `donaciones-service/.../config/AdminSeeder.java`, `notificaciones-service/.../config/AdminSeeder.java` | Código duplicado intencional (por diseño de ID_ADMIN compartido) | INFORMATIVO — justificado en Javadoc de `AdminConstantes.java` |

---

### 24.5 FASE R — Persistencia e invariantes

> Evaluación de si el dominio está preparado para migración a persistencia real.

| Aspecto | Estado | Evidencia | Riesgo de migración |
|---------|--------|-----------|---------------------|
| Aggregate boundaries claros | ✅ | Cada aggregate tiene su propio repositorio. No hay referencias directas de objeto cross-aggregate (solo UUID) | BAJO |
| Referencias por UUID entre aggregates | ✅ | `donanteId`, `donacionOriginalId`, `idNecesidad`, `idRuta` — todos UUID | BAJO — JPA puede mapear como `@Column` directamente |
| Constructores de reconstitución | ⚠️ PARCIAL | Los aggregates tienen constructor principal que genera UUID propio. No hay constructor separado de reconstitución (para rehidratar desde BD). JPA necesitaría `@Entity` con constructor vacío o constructor de reconstitución | MEDIO — requiere agregar `@Entity` y constructores JPA-compatible |
| Eventos emitidos al hidratar | ✅ CORRECTO | Los aggregates NO emiten domain events al hidratarse. El `registrarEvento()` solo se llama en métodos de negocio explícitos. Correcto para evitar eventos falsos al cargar desde BD | BAJO |
| Colecciones mutables en aggregates | ⚠️ REVISAR | `Donacion.historialEstados` es `List` mutable. `DonacionIndependiente.items` es mutable. JPA requeriría `@OneToMany` con cascade — posible complejidad | MEDIO |
| Optimistic locking | ❌ NO IMPLEMENTADO | No hay campo `@Version`. Con persistencia real y acceso concurrente, se podrían perder actualizaciones | ALTO si se migra sin agregar `@Version` |
| Value objects como Embedded | ✅ PREPARADO | `Direccion`, `PeriodoNecesidad`, `ItemDonacion` son records/value objects sin identidad propia — mapearían a `@Embeddable` directamente | BAJO |

---

### 24.6 FASE S — Seguridad y privacidad

| Aspecto | Estado | Evidencia | Riesgo |
|---------|--------|-----------|--------|
| Datos personales | ✅ MODELO PRESENTE | `Persona` tiene nombre, apellido, documento, fechaNacimiento, mediosDeContacto | Sin encriptación — aceptable en TP en memoria |
| Anonimización | ⚠️ PARCIAL | `Anonimizable` interface implementada por `Donante`, `EntidadBeneficiaria`, `Notificacion`. Los métodos `anonimizar()` de Donante y EntidadBeneficiaria están vacíos ("coordinado a nivel de service") | El contrato existe pero la implementación está vacía |
| Password generada | ⚠️ REVISAR | `DonantesService.java:61`: genera `UUID.randomUUID().toString().substring(0, 8)` como password y la envía en texto plano en el cuerpo del evento de notificación de bienvenida. La "contraseña" de 8 caracteres hexadecimales es débil y se loguea potencialmente en notificaciones-service | MEDIO para TP |
| Credenciales RabbitMQ | ⚠️ HARDCODED | `donatrack`/`donatrack` en docker-compose. En preprod el puerto no está expuesto — mitigación válida | BAJO para TP |
| Logs sensibles | ⚠️ REVISAR | `DonantesService` loguea credenciales generadas. `SegmentacionEventListener` loguea nombre del donante. En producción real estos logs requerirían sanitización | MEDIO para producción real |
| DTOs expuestos | ✅ BIEN MANEJADO | Los DTOs de output no exponen información más allá de lo necesario. Las passwords no aparecen en output DTOs de donante | BAJO |
| Admin con ID fijo | ✅ DOCUMENTADO | `AdminConstantes.ID_ADMIN = "00000000-0000-0000-0000-000000000001"`. Justificado en Javadoc — es un patrón intencional para sincronización entre servicios sin sincronización asíncrona | BAJO — bien documentado |
| Sin HTTPS en docker-compose | ⚠️ ACEPTABLE | Ninguno de los compose configura TLS. En TP académico es aceptable; en producción requeriría reverse proxy (nginx/traefik) | BAJO para TP |

---

### 24.7 FASE U — common-lib: evaluación

| Abstracción | Usada por | Bien promovida | Observaciones |
|-------------|-----------|----------------|---------------|
| `AggregateRoot` | todos los aggregates | ✅ | Contrato mínimo: solo `getId(): UUID` |
| `AgregadoConEventos<E>` | Donacion, DI, Propuesta, Ruta, Entrega, DonanteIncentivos, Notificacion | ✅ | Core del patrón — bien abstracto |
| `EventoDeDominio` | base de todos los eventos | ✅ | UUID + timestamp — suficiente |
| `CrudRepositoryEnMemoria<T>` | todos los repositorios | ✅ | ConcurrentHashMap, contiene count(), findAll(), etc. |
| `GlobalExceptionHandler` | todos vía auto-config | ✅ | ErrorCatalog unificado — excelente |
| `ErrorCatalog` | todos | ✅ | Catálogo de errores compartido con códigos |
| `MdcTaskDecorator` | incentivos AsyncConfig | ✅ | Específico de async — bien ubicado |
| `FeignTraceRequestInterceptor` | todos los Feign clients | ✅ | Propagación de X-Trace-Id transversal |
| `ControllerLoggingInterceptor` | todos los controllers | ✅ | Auto-config — logging de requests sin boilerplate |
| `ServiceLoggingAspect` | todos los @Service | ✅ | AOP logging declarativo |
| `ScheduledJobLoggingAspect` | todos los @Scheduled | ✅ | AOP logging de schedulers |

**Evaluación de shared kernel:**

El common-lib NO está sobreingenierizado. Contiene solo abstracciones genuinamente transversales que todos los servicios necesitan. No hay lógica de negocio en common-lib. El Javadoc de `DonaTrackOpenApiAutoConfiguration` muestra que incluso la configuración de Swagger está en common-lib como auto-configuration — correcto para evitar duplicación.

**Único riesgo identificado:** `ErrorCatalog` es un enum shared. Si un servicio necesita agregar un error muy específico, actualmente lo hace modificando el enum compartido — esto acopla el ciclo de release de common-lib con el ciclo de release del servicio individual. Para TP, el riesgo es bajo.

---

### 24.8 Resumen de nuevos hallazgos del Checkpoint 4

| ID | Criticidad | Fase | Descripción |
|----|-----------|------|-------------|
| C4-HAL-01 | MEDIO | Docker | docker-compose.preprod.yml sin volumen n8n_data — workflows de n8n no persisten |
| C4-HAL-02 | MEDIO | Docker | SEED_CATALOGO_ENABLED ausente en docker-compose → catálogo vacío en Docker |
| C4-HAL-03 | BAJO | Docker | Credenciales RabbitMQ hardcodeadas en compose (mitigado en preprod) |
| C4-HAL-04 | BAJO | Tests | Magic strings de enum serializado en controller tests |
| C4-HAL-05 | BAJO | Tests | Sin tests de comportamiento ante fallo de Feign en listeners |
| C4-HAL-06 | BAJO | Tests | Sin tests de idempotencia de mensajes RabbitMQ |
| C4-HAL-07 | BAJO | Legacy | AlgoritmoOrdenarSimple duplica AlgoritmoOrdenadorSimple — el wired no tiene el Javadoc que sí tiene el no-wired |
| C4-HAL-08 | BAJO | Legacy | SegmentadorSimple sin uso confirmado |
| C4-HAL-09 | MEDIO | Seguridad | Password de bienvenida generada con 8 chars de UUID, enviada en texto plano en evento |
| C4-HAL-10 | MEDIO | Hardening | Sin optimistic locking — en migración a BD real, riesgo de lost update |
| C4-HAL-11 | BAJO | Hardening | Constructores de aggregates sin versión separada de reconstitución — requiere trabajo extra en migración a JPA |

---

**STOP — Checkpoint 4 completado. Esperando validación antes de continuar.**

**Archivos:** `donaciones-service/src/main/java/grupo5/donaciones/infrastructure/CatalogDataInitializer.java` (nuevo)

**Descripción:** Se agregó un `CommandLineRunner` que siembra en memoria las categorías y subcategorías del catálogo al iniciar el servicio:
- 3 categorías: Alimentos (kg), Ropa (unidades, usada=true), Muebles (unidades, usada=true)
- 8+ subcategorías con sus aliases para normalización semántica

Está anotado con `@ConditionalOnProperty(name = "donatrack.catalogo.seed-enabled", havingValue = "true", matchIfMissing = true)`. El `matchIfMissing = true` implica que si la propiedad está ausente, **el seeder corre por defecto**.

**Impacto en la auditoría:** Componente de infraestructura no registrado en las secciones 3, 5.1 y 6.1. La existencia de este seeder explica cómo el sistema funciona con datos de catálogo sin persistencia real — hasta ahora era un aspecto NO DETERMINADO.

---

### 22.2 `1dd6e3ac` — Spotless apply

**Archivos:** `donaciones-service/src/main/resources/application.properties`

**Cambio:** `donatrack.catalogo.seed-enabled=${SEED_CATALOGO_ENABLED:false}` — el default pasó de `true` a `false`.

**Impacto:** El seeder del commit anterior **no corre en desarrollo por defecto** (la propiedad está explícitamente en `false`). En tests, `matchIfMissing=true` aún lo dispararía si la propiedad no está en el contexto de test. Para producción, hay que pasar `SEED_CATALOGO_ENABLED=true` como env var. El comentario `// TODO: Utilizar variables de entorno` en línea 9 con sintaxis Java (`//`) sigue sin corregirse — **hallazgo #8 del documento sigue vigente**.

---

### 22.3 `69ff6489` — Fix normalizacion de bienes pendientes con vencimiento (#808)

**Archivos:** `BienNormalizado.java`, `SegmentacionEventListener.java`, `ProcesadorDeDonaciones.java`, `ItemDonacionNormalizadoService.java`

#### Fix 1 — `BienNormalizado.java:43-51` (validación de vencimiento/estado)

**Antes:** La validación de `conVencimiento` y `!conVencimiento` solo se ejecutaba para ítems `ACEPTADO`. Los ítems `PENDIENTE_REVISION` podían crearse con `conVencimiento=true` pero sin `fechaVencimiento`, pasando validación sin error.

**Después:** La validación aplica a todos los estados **excepto** `PENDIENTE_REVISION`. El campo `conEstado` se movió fuera del bloque condicional y ahora aplica siempre. El comentario en código explica: "La subcategoría de un bien pendiente es solo un candidato provisional. Sus reglas de vencimiento se validan recién cuando la normalización queda resuelta."

**Clasificación:** BUG REAL resuelto. Tenía impacto funcional: donaciones con ítems perecederos podían normalizarse como pendientes sin fecha de vencimiento y luego fallar en el ciclo siguiente.

#### Fix 2 — Publicación de domain events (3 archivos)

**Antes:** `donacion.getDomainEvents().forEach(eventPublisher::publishEvent); donacion.clearDomainEvents();`

**Después:** `var eventos = donacion.getDomainEvents(); donacion.clearDomainEvents(); eventos.forEach(eventPublisher::publishEvent);`

El patrón anterior podía producir `ConcurrentModificationException` si `clearDomainEvents()` modificaba la lista mientras `forEach` iteraba sobre ella (en función de la implementación de `AgregadoConEventos`). El fix captura la lista antes de limpiarla. El commit ancestral `8fc7c5e3 Fix republicacion recursiva de eventos de donacion` había aplicado el mismo fix en `PropuestaDeAsignacionService` pero los otros 3 archivos quedaron sin actualizar hasta este commit.

**Archivos afectados:**
- `SegmentacionEventListener.java` (2 ocurrencias corregidas)
- `ProcesadorDeDonaciones.java` (1 ocurrencia corregida)
- `ItemDonacionNormalizadoService.java` (1 ocurrencia corregida)

**Clasificación:** BUG REAL resuelto. Afectaba el flujo de normalización en condiciones de concurrencia o si `AgregadoConEventos` usa una lista mutable compartida.

**Tests agregados:** `BienNormalizadoTest` (+28 tests), `NormalizadorSemanticoBienTest` (+26 tests), `ItemDonacionNormalizadoServiceTest` (+11 tests).

---

### 22.4 `96759f3f` — Flujo de postman

**Archivos:** `docs/postman/flujo-4-matching-asignacion-estados.json` (1086 insertions)

**Descripción:** El flujo 4 fue completamente reestructurado. Ahora tiene tres secciones:

| Sección | Descripción | Pasos |
|---------|-------------|-------|
| Setup | Crea categoría, subcategoría, persona donante, donante, persona jurídica, entidad beneficiaria | 6 |
| A. Camino feliz | EN_DEPOSITO → ASIGNACION_REALIZADA → LISTA_PARA_ENTREGAR → EN_TRASLADO → ENTREGADA, con casos negativos intermedios | 13 |
| B. Camino alternativo | EN_DEPOSITO → VENCIDA, con caso negativo sobre estado terminal | 7 |

**Hallazgos de esta actualización:**
- POSITIVO: El flujo ahora es autocontenido — crea sus propios datos de setup, elimina dependencia de datos pre-existentes.
- POSITIVO: Los pasos negativos (re-aprobar propuesta, repetir estado terminal) documentan el comportamiento esperado de los estados terminales.
- NUEVO: El Setup crea una categoría y subcategoría específicas para que el algoritmo de matching funcione en el flujo. Esto confirma que el catálogo debe estar seeded (o crearse vía API) para que el algoritmo de asignación encuentre coincidencias.
- ATENCIÓN: El paso 1b del flujo obtiene "ítems normalizados pendientes" y el paso 1c reclasifica manualmente a la subcategoría del Setup. Esto indica que el flujo de normalización automática puede no asignar la subcategoría correcta sin el catálogo seeded — o que la reclasificación manual es parte del flujo normal de trabajo.

---

### 22.5 Impacto sobre secciones previas del documento

| Sección | Hallazgo original | Estado actual |
|---------|------------------|--------------|
| Resumen #8 (MEDIO) | `//` en application.properties | SIGUE VIGENTE — Spotless no lo corrigió |
| Sección 14 (legacy #2) | `// TODO` con sintaxis inválida | SIGUE VIGENTE |
| Sección 3 (arquitectura) | Falta CatalogDataInitializer | ACTUALIZAR: nuevo componente de infraestructura descubierto |
| Sección 11 (tests) | Total ~2020 tests sin failures | ACTUALIZAR: +65 tests nuevos en donaciones → ~2085 tests totales |
| Sección 10.5 (Postman) | flujo-4 con cobertura media | ACTUALIZAR: flujo-4 completamente reestructurado con camino feliz + camino alternativo + negativos |
| C2-COR-01 (adapters) | Mock adapters corregido | Sin cambios |
| Bug: publicación de eventos | Mencionado en bitácora como Fix 8fc7c5e3 | RESUELTO en 3 archivos adicionales por 69ff6489 |
| Bug: BienNormalizado PENDIENTE_REVISION | No estaba en el documento | NUEVO HALLAZGO resuelto |
