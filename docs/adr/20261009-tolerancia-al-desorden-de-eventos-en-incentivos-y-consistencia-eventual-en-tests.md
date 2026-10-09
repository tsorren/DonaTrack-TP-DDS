# Tolerancia al desorden de eventos en Incentivos y consistencia eventual en los tests

- Status: proposed
- Date: 2026-10-09
- Deciders: Pendiente de decisión grupal
- Tags: arquitectura, amqp, rabbitmq, incentivos, consistencia-eventual, dlq, testing, integration-tests, entrega-4

## Contexto y Problema

Al correr la suite `integration-tests` contra un stack híbrido (Donaciones, Notificaciones e Incentivos en local; Logística en Render; RabbitMQ en CloudAMQP) fallaron 4 tests de `CrossServiceCommunicationIT` con `404 ERR-EST-702` (`DONANTE_INCENTIVOS_NO_ENCONTRADO`). La misma suite pasa contra `docker-compose.preprod.yml`.

`[OBSERVED]` El log de `incentivos-service` de esa corrida muestra la carrera:

```
11:03:54.545  GET /api/incentivos/donantes/a833a2db-…/metricas  → 404 (ERR-EST-702)
11:03:54.576  Evento donante.registrado recibido (donante a833a2db-…)
```

El test consultó a Incentivos 31 ms antes de que llegara el evento que da de alta al donante. Con el RabbitMQ local la entrega tarda alrededor de 1 ms y la carrera no se ve; con un broker remoto, sí.

Al analizar la causa aparecieron dos problemas de distinta gravedad:

1. **Incentivos depende de un orden entre colas que RabbitMQ no garantiza.** `[OBSERVED]` `IncentivosEventosListener` consume cada evento de una cola propia (`incentivos.donante-registrado`, `incentivos.donacion-segmentada`, `incentivos.donacion-recibida`, `incentivos.persona-sincronizada`, `incentivos.donante-dado-de-baja`). RabbitMQ conserva el orden dentro de una cola, no entre colas. Si `donacion.segmentada` o `donacion.recibida` llega antes que `donante.registrado`, `MisionesDonacionService.obtenerDonante` lanza `DONANTE_INCENTIVOS_NO_ENCONTRADO`. La `rabbitListenerContainerFactory` de `incentivos-service` tiene `setDefaultRequeueRejected(false)` y esas colas no declaran `x-dead-letter-exchange`, así que **el mensaje se descarta**: la donación no suma a métricas, misiones ni insignias, y no queda rastro fuera del log. En la misma corrida, `donante.registrado` (`54.783`) le ganó a `donacion.segmentada` (`54.887`) por unos 100 ms.
2. **Los tests suponen propagación inmediata.** `[OBSERVED]` Varios tests leen de un servicio inmediatamente después de escribir en otro, sin esperar a que el evento se propague. `PollingUtils` ya ofrece esperas acotadas, pero su uso depende de que quien escribe el test se acuerde. El entorno de CI no lo detecta porque su broker no tiene latencia.

El problema 1 es un riesgo de producción; el 2 es la razón por la que no se detectó antes.

## Atributos de Calidad y Drivers de Decisión

* **Fiabilidad:** ningún evento de dominio se pierde en silencio por llegar fuera de orden.
* **Observabilidad:** un evento que no se pudo procesar queda visible y se puede reprocesar.
* **Determinismo de la suite:** un test que depende de una carrera falla siempre, no a veces.
* **Minimalismo suficiente y control de dependencias** ([AGENTS.md](../../AGENTS.md) §6).
* **Coherencia** con la topología AMQP vigente ([20260911](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md)), que ya usa DLQ en Notificaciones.

## Relaciones Arquitectónicas

1. **Extiende a Incentivos:** el clúster DLQ de [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md), hoy aplicado solo a Notificaciones (`notificaciones.dlx`).
2. **Se apoya en:** [20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos](./20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md): reintentar un evento exige que su procesamiento sea idempotente.
3. **Aplica en la suite:** [20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest](./20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md) y [20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes](./20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md).
4. **Deuda relacionada:** [`DEUDA_TECNICA.md`](./DEUDA_TECNICA.md), DTI-15.

## Alternativas Consideradas

### Decisión 1: cómo tolera Incentivos un evento que llega antes que el alta del donante

| Alternativa | Veredicto |
|---|---|
| Status quo (rechazar sin requeue ni DLQ) | ❌ Pierde datos de negocio en silencio. |
| **A. Reintento con espera creciente y DLQ por cola** | ✅ **Recomendada.** Cambio de configuración acotado, reutiliza el patrón DLQ de Notificaciones y además da visibilidad a cualquier rechazo, no solo a este. |
| B. Alta provisoria del donante | ⏳ Complementaria. Elimina la dependencia del orden, pero cambia el dominio. |
| C. Una sola cola para todos los eventos de Incentivos | ❌ Garantiza el orden solo con un consumidor, y solo si Donaciones publica en orden causal, algo que no se verificó. Cambia la topología y el listener. |
| D. Requeue inmediato (`defaultRequeueRejected=true`) | ❌ Un mensaje que nunca va a poder procesarse queda en un bucle caliente que consume CPU y red. |

**A. Reintento con espera creciente y DLQ.** Ante `DONANTE_INCENTIVOS_NO_ENCONTRADO` el mensaje se reintenta con espera creciente (orientativo: 1, 2, 4, 8 y 16 s; unos 30 s en total). Agotados los intentos, va a una DLQ propia de la cola de origen. Los errores de contrato o validación van a la DLQ sin reintentar.
- Pros: no toca el modelo de dominio; deja trazabilidad; el plazo cubre cualquier latencia razonable del broker.
- Contras: el reintento dentro del listener bloquea el hilo consumidor mientras espera. Si eso resulta un problema, se reemplaza por una cola de espera con TTL que devuelve el mensaje a la cola original (sin dependencias nuevas, pero con más topología). Hay que confirmar que `procesarDonacion` y `procesarDonacionExitosa` sean idempotentes ante un reintento.

**B. Alta provisoria.** Si llega un evento de un donante desconocido, se crea `DonanteIncentivos` sin nombre y `registrarDonante` pasa a completar un donante existente en lugar de fallar.
- Pros: no depende del orden ni de plazos; nunca hay reintentos.
- Contras: introduce un estado intermedio (donante sin nombre) que el ranking y las notificaciones deben tolerar; cambia la semántica de `registrarDonante`; un evento con un `donanteId` erróneo crea un donante fantasma.

### Decisión 2: cómo se evita que un test suponga propagación inmediata

| Alternativa | Veredicto |
|---|---|
| Status quo (convención informal) | ❌ Es lo que falló. |
| **Esperas obligatorias más una regla de ArchUnit** | ✅ **Recomendada.** Las lecturas de un servicio distinto al escrito pasan por `PollingUtils`, cuyas esperas devuelven la `Response` final para que el test compruebe sobre ella. Una regla de ArchUnit en `integration-tests` prohíbe que las clases `*IT` llamen directamente a los métodos de lectura de `IncentivosApiClient` y `NotificacionesApiClient`. ArchUnit ya es dependencia del proyecto (`archunit.version` 1.3.0 en el `pom.xml` raíz). |
| Reintentos automáticos dentro de los clientes HTTP de test | ❌ Enmascara los 404 que un test sí espera (`esperarBajaDonanteEnIncentivos`). |
| Pausas fijas (`Thread.sleep`) | ❌ Lentas cuando sobran y frágiles cuando no alcanzan; `PollingUtils` ya resuelve lo mismo con Awaitility. |

### Decisión 3: cómo se hace visible una carrera antes de llegar a la nube

| Alternativa | Veredicto |
|---|---|
| Status quo (broker local sin latencia) | ❌ Oculta las carreras. |
| **Toxiproxy en `docker-compose.preprod.yml`** entre los servicios y RabbitMQ, con unos 200 ms de latencia | ⏳ **Recomendada, sujeta a aprobación**: agrega una imagen Docker ([AGENTS.md](../../AGENTS.md) §6 exige aprobación previa). |
| Correr la suite híbrida (`run-hybrid-tests.sh`) en CI | ❌ Requiere secretos de CloudAMQP y Render en CI y depende del arranque en frío de Render. |
| Test de regresión dedicado al desorden | ✅ **Complementaria y obligatoria con A o B:** un test de componente de Incentivos con Testcontainers que publica `donacion.segmentada` antes que `donante.registrado` y verifica que la donación termina contando. |

## Resultado de la Decisión

Propuesta, pendiente de decisión grupal:

1. **Incentivos:** reintento con espera creciente y DLQ por cola (alternativa A). La alta provisoria (B) queda como evolución si se quiere independencia total del orden.
2. **Tests:** las lecturas cruzadas pasan siempre por `PollingUtils`, y lo controla una regla de ArchUnit. `[OBSERVED]` Ya se aplicó la parte inmediata: `PollingUtils.esperarDonanteEnIncentivos` y su uso en los 4 tests que fallaron.
3. **Regresión:** test de componente que fuerza el desorden de eventos en Incentivos.
4. **Entorno:** Toxiproxy en preprod, sujeto a la aprobación de la imagen nueva.

Orden de implementación sugerido: 1 → 3 → 2 → 4. El punto 1 cierra el riesgo de producción.

## Consecuencias

### Positivas

* Un evento fuera de orden deja de perderse; los que no se pueden procesar quedan en una DLQ consultable.
* La suite de integración deja de depender de la latencia del broker.
* Una carrera nueva en los tests se detecta al compilar (ArchUnit) o en preprod (Toxiproxy), no al probar contra la nube.

### Negativas

* Más topología en Incentivos: una DLQ por cola y su exchange de dead letter.
* Un evento que espera al alta del donante se procesa con hasta unos 30 s de demora.
* La DLQ necesita un procedimiento de revisión; sin él, solo cambia dónde se acumulan los problemas.
* Toxiproxy agrega una imagen y algunos segundos a cada corrida de preprod.

## Trabajo Futuro

* Alta provisoria del donante (alternativa B).
* Procedimiento para revisar y reprocesar las DLQ de Incentivos y Notificaciones.
* Revisar si otros consumidores tienen el mismo patrón de colas separadas sin DLQ. `[OBSERVED]` `DEUDA_TECNICA.md`, DTI-14, ítem 8 describe un caso análogo en las colas de vuelta de Logística en Donaciones.
