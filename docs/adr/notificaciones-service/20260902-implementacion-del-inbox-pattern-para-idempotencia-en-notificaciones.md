# Implementación del Transactional Inbox Pattern para Idempotencia en Notificaciones

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: idempotencia, inbox-pattern, event-id, feign, at-least-once, deduplicacion, notificaciones

## Contexto y Problema

En DonaTrack, la integración entre microservicios hacia `notificaciones-service` se produce mediante peticiones HTTP REST (`POST /notificaciones`) disparadas por clientes Feign desde `donaciones-service` e `incentivos-service`. Estos clientes cuentan con políticas de tolerancia a fallos y reintentos automáticos configuradas en `FeignRetryConfig` (hasta 5 reintentos ante timeouts o desconexiones transitorias de red).

En cualquier sistema distribuido sobre redes no confiables, la semántica de transporte subyacente es **Al Menos Una Vez (*At-Least-Once Delivery*)**:
* Si `notificaciones-service` procesa exitosamente una petición pero la conexión TCP se interrumpe antes de que la respuesta HTTP `202 Accepted` sea recibida por el emisor, este último interpreta la falla como un error de red transitorio y reintenta el despacho.
* Como el contrato `EventoNotificableDTO` actual carece de una clave de idempotencia (`eventId`), el servicio receptor interpreta cada reintento como un nuevo evento de negocio independiente.

Esto genera una anomalía crítica de cara al usuario final: cada reintento de red inserta nuevas entidades en la tabla relacional `notificacion` y vuelve a despachar mensajes repetidos por Correo, SMS y WhatsApp (ej. un donante recibiendo múltiples alertas idénticas de *"Tu donación fue asignada"*).

Para resolver esta vulnerabilidad, se requiere dotar al servicio de un mecanismo de **Consumidor Idempotente (*Idempotent Consumer*)** a través del patrón **Transactional Inbox**.

## Atributos de Calidad y Drivers de Decisión

* **Integridad y Consistencia de Negocio:** Garantizar semántica de procesamiento efectivamente única (*Effectively-Once Processing*), asegurando que la recepción múltiple de un mismo mensaje no altere el estado ni duplique efectos colaterales.
* **Tolerancia a Fallos y Reintentos Seguros:** Permitir que los emisores utilicen políticas agresivas de reintentos de red sin temor a saturar de spam a los usuarios.
* **Rendimiento e I/O Mínimo:** Validar la existencia previa con costo $O(1)$ indexado en base de datos.
* **Alineación con el Shared Kernel y ADRs Transversales:** Implementar localmente en el esquema `notificaciones` la decisión transversal formulada en el ADR [`20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md`](../20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md).

## Alternativas Consideradas

### Alternativa 1 (Elegida): Transactional Inbox Pattern con `eventId: UUID` y Tabla `evento_procesado`
1. **Contrato de Entrada:** Incorporar el campo obligatorio `@NotNull UUID eventId()` en la interfaz sellada `EventoNotificableDTO` y en todos sus DTOs derivados. El `eventId` debe ser generado por el emisor una única vez por evento de negocio real (persistiendo a través de los sucesivos reintentos HTTP de Feign).
2. **Esquema Relacional (Flyway):** Crear la tabla de Inbox en el esquema `notificaciones`:
   ```sql
   CREATE TABLE evento_procesado (
       event_id UUID PRIMARY KEY,
       procesado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
   );
   ```
3. **Persistencia Atómica (Inbox):** En `NotificacionService.procesar()`, dentro de la transacción corta de recepción (Fase 1 del ADR [`20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md`](./20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md)):
   * Se verifica la existencia del `eventId` en `evento_procesado`.
   * Si ya existe, se aborta la creación de notificaciones y se retorna inmediatamente `202 Accepted` de forma idempotente y silenciosa.
   * Si no existe, se inserta el `event_id` y se persisten las nuevas `Notificacion` en estado `PENDIENTE` dentro del mismo `COMMIT`.

### Alternativa 2 (Descartada): Deduplicación por Huella Natural o Hash de Contenido
Calcular un hash SHA-256 o evaluar unicidad en base de datos combinando `personaId`, tipo de evento, detalle y fecha sin modificar el contrato DTO de entrada.

*Motivo de descarte:* Es no-determinista y propenso a errores graves:
* *Falsos Positivos:* Dos donaciones distintas pero del mismo tipo registradas para un donante en el mismo minuto generarían el mismo hash de negocio, descartando erróneamente un evento legítimo.
* *Falsos Negativos:* Ligeras diferencias en la serialización de timestamps de milisegundos entre el intento original y el reintento impedirían detectar el duplicado real.

### Alternativa 3 (Descartada): Deduplicación Volátil en Memoria con Cache / TTL (Redis / ConcurrentHashMap)
Guardar los IDs de eventos procesados en una estructura de datos en memoria o en un servidor Redis con tiempo de expiración (TTL de 10 minutos).

*Motivo de descarte:* Rompe la garantía de durabilidad ante reinicios del contenedor en Docker Compose. Si el servicio se reinicia por mantenimiento o falla transitoria, la cache se vacía y cualquier reintento rezagado volvería a ingresar como si fuera nuevo, duplicando las alertas. Asimismo, introduce dependencias externas de infraestructura no justificadas para la escala del servicio.

### Alternativa 4 (Descartada): Idempotencia Asumida en el Emisor (Status Quo)
No implementar mecanismos de deduplicación en el consumidor y asumir que los clientes no enviarán peticiones repetidas.

*Motivo de descarte:* Incompatible con la realidad de los sistemas distribuidos. En cuanto `FeignRetryConfig` entra en acción, la duplicación física de mensajes se produce inexorablemente.

## Resultado de la Decisión

Se aprueba la **Alternativa 1: Transactional Inbox Pattern con `eventId: UUID` y Tabla `evento_procesado`**.

### Consecuencias Positivas

* **Inmunidad ante Tormentas de Reintentos:** `donaciones-service` e `incentivos-service` pueden reintentar libremente ante fallos de red sin riesgo de inundar al usuario con mensajes repetidos.
* **Procesamiento Effectively-Once:** La base de datos relacional actúa como árbitro definitivo de unicidad a través de la clave primaria `event_id`.
* **Desempeño Óptimo:** La verificación por clave primaria indexada en PostgreSQL toma menos de 1 milisegundo.

### Consecuencias Negativas

* **Evolución del Contrato DTO:** Requiere actualizar de forma retrocompatible `EventoNotificableDTO` en `notificaciones-service` y sincronizar los payloads emitidos por los Feign Clients en los demás microservicios.
* **Crecimiento de la Tabla de Inbox:** La tabla `evento_procesado` crecerá con el tiempo, requiriendo a largo plazo una política de purga o retención (ej. eliminar eventos de más de 30 días mediante un cron job de mantenimiento).

## Validación

1. **Test de Reintento Idempotente:** Prueba de integración en `NotificacionControllerTest` enviando dos peticiones HTTP consecutivas con idéntico payload y el mismo `eventId`. Verificar que la primera llamada retorna `202 Accepted` e inserta filas en `notificacion`, y la segunda retorna `202 Accepted` sin crear registros adicionales ni disparar nuevos envíos multicanal.
2. **Integridad Relacional de Clave Primaria:** Verificación en `RepositoriosJpaTest` de que un intento de doble inserción del mismo `event_id` dentro de una transacción es rechazado por restricción de clave primaria en PostgreSQL.

## Análisis de Alternativas

### Alternativa 1: Transactional Inbox con eventId
* **Pros:** Robustez matemática garantizada por el motor relacional; compatibilidad total con *At-Least-Once Delivery*; persistencia atómica con el estado de negocio.
* **Contras:** Requiere incorporar `eventId` en los DTOs y mantener la tabla `evento_procesado`.

### Alternativa 2: Hash de Contenido
* **Pros:** No requiere alterar los DTOs de entrada.
* **Contras:** Riesgo inaceptable de falsos positivos y falsos negativos de deduplicación.

### Alternativa 3: Cache en Memoria / Redis
* **Pros:** Búsqueda en memoria ultrarrápida.
* **Contras:** Pérdida de protección ante caídas o reinicios del microservicio.
