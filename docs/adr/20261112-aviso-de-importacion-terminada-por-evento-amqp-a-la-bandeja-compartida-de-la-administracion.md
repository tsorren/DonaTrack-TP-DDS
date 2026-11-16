# Aviso de importación terminada por un evento AMQP nuevo, dirigido a la bandeja compartida de la Administración

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación automática de la spec `R11-p2-campanita` con las opciones recomendadas, 2026-11-12, incluida la bandeja compartida y el correo por importación); el detalle de la construcción, el constructor del lote `p2-campanita` (agente, sin consulta); este registro, el cierre documental de la oleada P2 (agente). Pendiente de aprobación del equipo en el PR
- Tags: donaciones, notificaciones, amqp, contrato, eventos, cliente-liviano, r11, entrega-6

> **Escrito después del código**, en el cierre documental de la oleada P2 (2026-11-12), como desvío
> acordado del ciclo ARCHITECTURAL: la spec lo dejaba para el cierre de la oleada ("su ADR
> `proposed` al cerrar la oleada"). `[OBSERVED]` El código actual ya implementa esta decisión
> (`ImportadorService`, `DonacionesEventPublisher#publicarImportacionTerminada`,
> `EventoImportacionTerminadaV1` en los dos servicios, `DonacionEventListener` y
> `TipoEvento.IMPORTACION_TERMINADA`/`IMPORTACION_CORTADA`/`MENSAJE_CONTACTO`; OpenAPI de
> Notificaciones 1.4.0; esquema `evento-importacion-terminada-v1.schema.json`). Código existente no
> es aprobación: si el revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

R11-30 pide que la administración tenga campanita y que una de sus notificaciones avise que terminó
una importación de donantes. `[OBSERVED]` Antes del lote, Donaciones no publicaba nada al terminar
una importación (la corre `@Async`, en un hilo aparte), y la administración no tenía bandeja real:
`/admin-notificaciones` mostraba una lista de ejemplo. Sí existía una persona replicada de id fijo
para la Administración (`ID_ADMIN`), destinataria de los mensajes de contacto
([ADR del destinatario Administración](./20260702-modelado-del-destinatario-administracion-como-persona-replicada-de-id-fijo.md)).

Problema: ¿cómo se entera Notificaciones de que terminó una importación, a quién avisa y qué
contrato los une?

**Gate A:** un evento nuevo en el contrato de mensajería entre Donaciones y Notificaciones y tres
valores nuevos en un enum público. **Gate B:** contrato entre servicios (AMQP y REST) y una decisión
de privacidad (qué datos del archivo viajan).

## Alternativas Consideradas

1. **Evento `importacion.terminada.v1`** por `donaciones.exchange` hacia la cola
   `notificaciones.donaciones`, que solo cuenta el hecho; Notificaciones elige el destinatario
   (`ID_ADMIN`) y el tipo (elegida).
2. **Llamada REST** de Donaciones a `POST /api/notificaciones/eventos`.
3. **El evento lleva el destinatario** (el admin que subió el archivo, o `ID_ADMIN`).
4. **Una bandeja por cuenta de admin** en Notificaciones, en lugar de la compartida.
5. **Sin evento:** que la pantalla del admin consulte el estado de las importaciones y lo muestre
   como aviso.

## Decisión

Elegida la alternativa 1.

- **Mensaje.** Routing key `importacion.terminada.v1`, con los headers de los demás eventos
  (`message_id` nuevo por publicación, `__TypeId__` y `traceId`). Campos: `archivoId`, `resultado`,
  `totalFilas` (nulo si se cortó antes de contarlas), `procesadas`, `creadas`, `actualizadas`,
  `conError`, `subidoEn` y `fecha` (zona del sistema, como el resto de los eventos de Donaciones,
  para no chocar con el `@PastOrPresent` del consumidor). **No lleva** el nombre del archivo ni datos
  de donantes (riesgo R1 de la spec).
- **Resultado con nombres propios del evento** (`TERMINADA`, `TERMINADA_CON_ERRORES`, `CORTADA`) y
  no `EstadoArchivo`, para no atar el contrato al modelo interno de Donaciones.
- **Cuándo se publica.** Una vez, después del `try/catch` del procesamiento asincrónico, con el
  estado final ya guardado; si el guardado final falla y el archivo pasa a `ERROR`, sale `CORTADA`;
  si también falla ese guardado, no se publica nada. Si la publicación falla, queda un `WARN` y la
  importación conserva su estado.
- **Notificaciones** lo consume en `DonacionEventListener` con el inbox por `message_id` de siempre
  y crea `IMPORTACION_TERMINADA` o `IMPORTACION_CORTADA` para `ID_ADMIN`, con `referenciaId` =
  `archivoId`. El aviso de contacto pasa a llevar `MENSAJE_CONTACTO` (antes, tipo nulo). Los tres
  tipos nuevos no se aceptan por `POST /api/notificaciones/eventos`.
- **Bandeja compartida** de la Administración sobre `ID_ADMIN`: si un admin abre un aviso, queda
  leído para todos, y la bajada de la bandeja lo dice. En el cliente, el id es una propiedad
  (`cliente.notificaciones.administracion`, `ID_ADMIN` por defecto), no un endpoint nuevo.

## Consecuencias

- Bueno: reusa la exchange, la cola, la DLQ y la deduplicación que ya existían: no es un canal nuevo
  entre servicios. Si mañana hay bandejas por cuenta, cambia solo el consumidor.
- Bueno: ningún dato personal del padrón viaja por la cola.
- Malo: bandeja compartida; un aviso abierto por un admin deja de verse como nuevo para el resto.
- Malo: `persistirYPublicar` manda también el correo de `ID_ADMIN`: un correo por importación
  (aceptado en la pregunta 2 de la aprobación).
- Malo: la importación es `@Async` y sin outbox; si el proceso cae entre guardar y publicar, el aviso
  se pierde (igual que los demás eventos de Donaciones). El estado del archivo queda bien y se ve en
  Importaciones.
- Malo: `[INFERRED]` `fecha` usa la zona del sistema pero se serializa con `Z`; si Donaciones corre
  en una zona por delante de la de Notificaciones, el evento puede llegar "en el futuro", fallar el
  `@PastOrPresent` e ir a la DLQ. Es el mismo patrón que el resto de los eventos
  ([DTI-43](./DEUDA_TECNICA.md#dti-43)).
- Neutro: `[VERIFIED]` `run-preprod-tests.sh` en el cierre de la oleada (smoke 4/4; contrato, integración y E2E 26/26, stack propio) y el recorrido con
  login real de la administración (campanita con su número y bandeja real). No hay todavía un caso
  de contrato en `integration-tests` que publique este evento contra su esquema.

## Análisis de las alternativas descartadas

- **2. REST:** Donaciones ya avisa a Notificaciones por AMQP, con su cola y su deduplicación; una
  llamada síncrona desde un hilo asincrónico no agrega nada y suma un acoplamiento temporal.
- **3. Destinatario en el evento:** ata a Donaciones con la forma de las bandejas; el archivo
  tampoco guarda quién lo subió (sería migración y cambio de la subida).
- **4. Bandeja por cuenta:** pide copiar las cuentas del realm en Notificaciones y decidir a quién
  avisar; queda para el backlog.
- **5. Sin evento:** la campanita tendría que consultar Donaciones en cada pantalla y no habría
  registro del aviso; además, mezcla dos fuentes en una bandeja.

## Enlaces

- Spec: [R11-p2-campanita](../specs/completed/R11-p2-campanita-spec.md) (§3)
- Ficha: [r11-p2-campanita](../interfaz/flujos/r11-p2-campanita.md)
- Esquema: [evento-importacion-terminada-v1.schema.json](../arquitectura/contratos/schemas/evento-importacion-terminada-v1.schema.json)
- Catálogo de mensajes: [catalogo-mensajes.md](../entrega-4/integracion/catalogo-mensajes.md)
- Topología: [Pub/Sub AMQP y desacoplamiento de Notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md)
- Deuda: [DTI-43](./DEUDA_TECNICA.md#dti-43)
