# Pedido al planificador externo después del commit

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística
- Tags: logística, transacciones, integración, planificación, callback

## Contexto y Problema

`PlanificacionService.iniciarPlanificacion` agrupa las entregas pendientes en lotes de hasta 100 (ADR `20260901-planificacion-de-rutas-asincrona-por-lotes-y-callback-rest`). Por cada lote guarda una `SolicitudPlanificacion` y le manda el pedido al planificador externo, que responde más tarde por el callback.

Con persistencia en PostgreSQL, el guardado y el envío ocurrían dentro de la misma transacción. Eso trajo dos problemas:

1. **Condición de carrera.** El planificador podía responder antes de que la transacción terminara. El callback abre otra transacción, busca la solicitud y no la encuentra, porque todavía no estaba confirmada: responde 404 y la solicitud queda `PENDIENTE` para siempre. Se reprodujo en pruebas locales (2 de 21 solicitudes quedaron trabadas).
2. **Atomicidad falsa.** Si fallaba un lote, la base deshacía también los lotes anteriores, pero esos pedidos ya habían salido y no se pueden deshacer. El planificador terminaba trabajando sobre solicitudes que no existían.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia:** nada sale hacia un sistema externo antes de que la base confirme lo que lo respalda.
* **Coherencia con el resto del sistema:** Notificaciones ya resolvió el mismo caso y Logística ya publica sus eventos de RabbitMQ después del commit.

## Alternativas Consideradas

* **Sacar `@Transactional` de `iniciarPlanificacion`:** cada `save` confirma solo. Es el cambio más chico, pero pierde la atomicidad entre lotes y es justamente la opción que descarta el ADR de Notificaciones.
* **Mantener la transacción y enviar después del commit:** se guardan todas las solicitudes juntas y recién al confirmar se envían los pedidos.
* **Outbox de solicitudes:** guardar el pedido en una tabla y que un proceso aparte lo envíe. Resuelve también la pérdida ante una caída, pero es desproporcionado para este caso.

## Resultado de la Decisión

Alternativa elegida: **mantener la transacción y enviar después del commit**.

`[OBSERVED]` El código actual ya implementa esta decisión.

`enviarPlanificacion` guarda la solicitud y publica un evento interno (`SolicitudPlanificacionRegistrada`). El método `despacharAlProveedor`, anotado con `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`, recibe ese evento y recién ahí llama al planificador. `fallbackExecution` hace que funcione igual en el perfil en memoria, donde no hay transacción.

Es el mismo mecanismo de `NotificacionGestor` (ADR `notificaciones-service/20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones`) y de `LogisticaEventPublisher` con RabbitMQ. La regla que queda para el servicio es que el límite de una transacción no incluye efectos sobre sistemas externos.

## Consecuencias

### Positivas

* El callback siempre encuentra la solicitud: con el cambio, 0 de 29 quedaron trabadas.
* "Se guardan todas o ninguna" vuelve a ser cierto, porque ningún pedido sale antes del commit.

### Negativas

* Si el servicio se cae entre el commit y el envío, la solicitud queda `PENDIENTE` sin pedido. No hay reintento automático: sus entregas siguen sin ruta y vuelven a entrar en la próxima corrida del planificador. El mismo riesgo existe en la publicación a RabbitMQ y está en discusión junto con el ADR grupal de Outbox.
