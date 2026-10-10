# Transacciones Atómicas Cortas y Despacho Asíncrono de Notificaciones

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: persistencia, transacciones, transactional, asincrono, events, hikaricp, resiliencia, notificaciones

## Contexto y Problema

En `notificaciones-service`, actualmente no existe ninguna anotación `@Transactional` en las clases de servicio de aplicación (`PersonasService`, `NotificacionService`, `NotificacionGestor`). Al mismo tiempo, el ciclo de vida de una notificación vincula dos tipos de operaciones con requerimientos operativos antagónicos:
1. **Persistencia Transaccional Relacional:** Creación de entidades `Notificacion`, registro de auditoría en `notificacion_historial_estado`, persistencia de `Persona` y anonimización de datos sensibles en cumplimiento normativo.
2. **I/O de Red No Transaccional:** Despacho físico de mensajes a través de APIs y pasarelas externas (SendGrid para correo, Twilio para SMS, Meta/WhatsApp Business API) mediante `NotificacionRouter`.

Si se introduce una transacción de base de datos `@Transactional` de manera ingenua sobre todo el flujo orquestador, la conexión JDBC de base de datos permanece abierta y bloqueada durante el tiempo que demoran las llamadas HTTP externas (típicamente entre 300 ms y 5.000 ms). Dado que el tamaño estándar del pool de conexiones (HikariCP) en entornos de microservicios suele configurarse en 10 conexiones, una ráfaga de 10 peticiones concurrentes con latencia externa provocaría el agotamiento total del pool (*Connection Pool Starvation*), denegando el servicio a todas las demás operaciones de la aplicación (incluyendo endpoints de salud `/actuator/health`).

Por otra parte, la ausencia actual de transaccionalidad en operaciones compuestas, como `PersonasService.anonimizar(id)` —que actualiza la persona y luego itera persistiendo múltiples notificaciones asociadas—, expone al sistema a inconsistencias parciales si ocurre una falla intermedia, dejando datos confidenciales sin anonimizar y vulnerando la Ley de Protección de Datos Personales (Ley 25.326).

## Atributos de Calidad y Drivers de Decisión

* **Rendimiento y Escalabilidad:** Minimizar el tiempo de retención de conexiones JDBC en el pool de PostgreSQL.
* **Integridad y Consistencia de Datos:** Garantizar atomicidad estricta (*todo o nada*) en operaciones multi-entidad de base de datos.
* **Resiliencia y Disponibilidad:** Evitar que la latencia, timeouts o caídas de pasarelas externas de mensajería degraden la persistencia local ni bloqueen a los microservicios emisores (`donaciones-service`, `incentivos-service`).
* **Cumplimiento de Buenas Prácticas ORM:** Respetar la regla arquitectónica de aislar el I/O no transaccional fuera de los límites de transacción de base de datos.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Transacciones Atómicas Cortas y Despacho Asíncrono Desacoplado
Dividir el ciclo de vida del procesamiento en tres fases claramente delimitadas:
1. **Fase 1 (Recepción y Persistencia Inicial - Transacción Corta 1):** Al recibir `POST /notificaciones`, `NotificacionService.procesar()` ejecuta bajo `@Transactional` la inserción del evento en la tabla de deduplicación (`evento_procesado`) y persiste las instancias de `Notificacion` con estado inicial `PENDIENTE`. La transacción comitea inmediatamente (< 20 ms), liberando la conexión JDBC al pool, y se responde `202 Accepted` al cliente emisor.
2. **Fase 2 (Despacho Multicanal Asíncrono - Sin Conexión JDBC):** Mediante `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` y `@Async` (o Virtual Threads de Java 21), un worker desacoplado toma el ID de la notificación y ejecuta las llamadas a `NotificacionRouter` fuera de cualquier transacción de base de datos. Si una API externa tarda 3 segundos, ninguna conexión de PostgreSQL permanece retenida.
3. **Fase 3 (Actualización de Estado y Auditoría - Transacción Corta 2):** Tras obtener el resultado físico del envío, se abre una segunda transacción corta `@Transactional` exclusivamente para actualizar el estado a `ENVIADA` o `FALLIDA` y registrar el evento en `notificacion_historial_estado`.

Asimismo, en `PersonasService.anonimizar()`, se aplica `@Transactional` para asegurar que la anonimización de la persona y de la totalidad de sus notificaciones asociadas sea una operación atómica indivisible.

### Alternativa 2 (Descartada): Transacción Extendida de Extremo a Extremo (Transacción Larga con I/O de Red)
Anotar un único método de servicio con `@Transactional` abarcando la creación de la notificación, el envío físico por SendGrid/Twilio y el guardado final en base de datos.

*Motivo de descarte:* Viola el principio fundamental de persistencia de no realizar llamadas de red externas dentro de una transacción JDBC. Conduce rápidamente a *HikariCP Pool Starvation*, incrementa la probabilidad de interbloqueos (*deadlocks*) en base de datos y genera una falsa sensación de atomicidad: si la transacción de BD aborta por timeout luego de llamar a SendGrid, el correo electrónico ya fue transmitido físicamente por la red sin posibilidad de compensación o rollback real.

### Alternativa 3 (Descartada): Transacciones Delegadas Exclusivamente a Repositorios (Status Quo)
No declarar `@Transactional` en los servicios de aplicación y depender exclusivamente de la transacción individual a nivel de sentencia que Spring Data JPA provee por defecto en cada método de repositorio (`save()`, `saveAll()`).

*Motivo de descarte:* No ofrece garantías de atomicidad en operaciones de negocio que involucran múltiples llamadas a repositorios. En `PersonasService.anonimizar()`, si falla la base de datos a mitad del bucle de notificaciones, la persona queda anonimizada pero parte de sus alertas quedan intactas en base de datos con datos personales visibles, produciendo corrupción irreversible del estado y no-conformidad legal.

## Resultado de la Decisión

Se aprueba la **Alternativa 1: Transacciones Atómicas Cortas y Despacho Asíncrono Desacoplado**.

### Consecuencias Positivas

* **Tiempos de respuesta mínimos:** El endpoint `POST /notificaciones` libera al cliente en menos de 25 ms, comiteando la notificación en estado `PENDIENTE`.
* **Protección del Connection Pool:** Las conexiones de PostgreSQL solo se ocupan durante milisegundos para operaciones puras de `INSERT`/`UPDATE`, soportando alto throughput concurrente.
* **Integridad Transaccional Robusta:** `anonimizar()` y la deduplicación de eventos se ejecutan bajo límites de atomicidad estricta.
* **Trazabilidad y Resiliencia:** El desacoplamiento permite que si las APIs de mensajería experimentan una caída, las notificaciones permanecen registradas de forma segura en estado `PENDIENTE` para su posterior reintento.

### Consecuencias Negativas

* **Consistencia Eventual:** Existe una ventana de tiempo entre el `COMMIT` del estado `PENDIENTE` y la transición a `ENVIADA`/`FALLIDA`.
* **Necesidad de Scheduler de Recuperación:** Se requiere un proceso de background periódico (*watchdog/spooler*) que reintente notificaciones que pudieran haber quedado estancadas en `PENDIENTE` si el contenedor sufriera una terminación abrupta (`SIGKILL`) durante la Fase 2.

## Validación

1. **Pruebas de Bloqueo Transaccional:** Test de concurrencia simulando alta latencia (2.000 ms) en `CorreoAdapterSimulado` bajo una carga concurrente superior al tamaño de HikariCP, verificando que el pool de conexiones no se agota y los endpoints `/actuator/health` responden inmediatamente.
2. **Pruebas de Atomicidad en Anonimización:** Test unitario/integración en `PersonasServiceTest` verificando que ante un error forzado en el guardado de notificaciones, la anonimización de la `Persona` realiza rollback completo.
3. **Pruebas de Transición de Estados:** Verificación en `RepositoriosJpaTest` de que la notificación persiste su estado `PENDIENTE` y posteriormente añade su transición a `ENVIADA` en `notificacion_historial_estado`.

## Análisis de Alternativas

### Alternativa 1: Transacciones Atómicas Cortas + Despacho Asíncrono
* **Pros:** Uso óptimo de conexiones JDBC; desacoplamiento de dependencias externas; resiliencia ante proveedores caídos; garantías ACID reales en operaciones compuestas.
* **Contras:** Mayor complejidad al coordinar eventos asíncronos (`AFTER_COMMIT`) y manejo de consistencia eventual.

### Alternativa 2: Transacción Extendida con I/O
* **Pros:** Simplicidad conceptual inicial.
* **Contras:** Agotamiento catastrófico de recursos; latencia agregada inaceptable; imposibilidad de revertir efectos físicos en el mundo real.

### Alternativa 3: Transacciones a Nivel de Repositorio (Status Quo)
* **Pros:** Sin cambios en el código actual.
* **Contras:** Inconsistencias graves y estados parciales huérfanos ante fallos de persistencia en operaciones compuestas.
