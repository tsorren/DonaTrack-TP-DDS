# Cancelación de las entregas que no salieron: comando por el outbox, misma cola y una entrega activa por donación

- Status: proposed
- Date: 2026-11-09
- Deciders: tsorren (pendiente de aprobación); redactado por el agente en HB-19
- Tags: donaciones, logistica, necesidades, amqp, broker, outbox, comandos, idempotencia, trazabilidad, entrega-6

> **Origen:** HB-19 del plan de huecos sin auth de E6
> ([requerimientos E6](../requerimientos/requerimientos-e6.md), fila HB-19), deuda
> [DTI-31](./DEUDA_TECNICA.md#dti-31). Refina el ADR `proposed`
> [baja de una necesidad con entregas en curso](./20261102-baja-de-una-necesidad-con-entregas-en-curso.md),
> que ya decidió el qué (B1, "según si salió") y el canal (C1, comando y confirmación por evento).
> Este ADR decide el cómo: lo que aparece al bajar B1 y C1 al código. No reemplaza ese ADR.

## Contexto y Problema

El ADR de la baja dejó decidido que, al dar de baja una necesidad, lo que ya salió llega y lo que no
salió se cancela y vuelve al depósito. Al mirar el código para implementarlo aparecen cinco
huecos que ese ADR no resuelve:

- `[OBSERVED]` El outbox del broker (`outbox_logistica`, `EntradaOutboxLogistica`) solo sabe mandar
  pedidos de entrega: cada entrada lleva los `DatosEntregaLogistica` y el relay llama a
  `IProveedorLogistica.enviar`. No hay lugar para un segundo comando.
- `[OBSERVED]` Logística consume el pedido de entrega de una cola propia
  (`logistica.<instancia>.entregas.solicitadas`). Si la cancelación va por otra cola, puede llegar
  antes que el pedido que cancela.
- `[OBSERVED]` Logística permite una sola entrega por donación, para siempre:
  `uq_entrega_donacion UNIQUE (id_donacion)` (V1, D7), `@Indexed(unique = true)` en Mongo, y el
  listener descarta un pedido si `existsByIdDonacion`. Una donación cancelada que el algoritmo
  vuelve a asignar no podría tener una entrega nueva en nuestra Logística.
- `[OBSERVED]` En Donaciones, la solicitud del broker queda activa mientras no sea `FALLIDA`
  (`findActivaPorDonacion`, índice `uq_solicitud_entrega_activa`). Una donación que vuelve al
  depósito y se reasigna no podría pedir una entrega nueva: el broker la ignora.
- `[OBSERVED]` `EstadoEntrega` no tiene un estado de cancelación. Lo usan el `switch` de
  `EntregasService.cambiarEstado`, el tipo `estado_entrega` de Postgres (V1), los tres motores de
  persistencia, `openapi-logistica.yaml` y los DTO de salida.

Problema: ¿cómo se implementa la cancelación de punta a punta sin perder donaciones, sin abrir
canales nuevos y sin dejar a la donación sin poder volver a entregarse?

## Atributos de Calidad y Drivers de Decisión

Pesos: consistencia (ninguna donación perdida ni en dos lugares) 30 · respeta los canales ya
decididos (broker, vuelta por mensajería, sin canal nuevo) 20 · robustez ante fallas (mensajes
perdidos, reintentos, carreras) 20 · aprovecha lo donado (llega a otra entidad) 15 · costo 15.
Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`).

## Relaciones Arquitectónicas

1. **Refina:** [baja de una necesidad con entregas en curso](./20261102-baja-de-una-necesidad-con-entregas-en-curso.md)
   (`proposed`). Mantiene B1 y C1; agrega D3 a D6.
2. **Extiende:** [broker de integración con Logística](./20261007-broker-de-integracion-con-logistica.md)
   (`proposed`): segundo tipo de entrada en el mismo outbox y segundo método en el puerto del proveedor.
3. **Extiende:** [vuelta por mensajería con identidad](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md)
   (`proposed`): quinto evento de vuelta, `entrega.cancelada`, firmado igual que los otros cuatro.
4. **Ajusta:** la regla D7 del DER de Logística ("una entrega por donación") pasa a "una entrega
   **activa** por donación". Las cancelaciones quedan como historia.
5. **Respeta:** [ciclo de vida de la entrega](./logistica-service/20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md)
   (`accepted`), [idempotencia en el consumo de eventos](./20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md),
   [outbox transaccional](./20260901-patron-transactional-outbox-para-consistencia-eventual.md) y la
   [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`).
6. **Se apoya en:** [ciclo de vida de la necesidad](./donaciones-service/20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato.md)
   (`proposed`, HB-18): la guarda usa `Necesidad.admiteAsignaciones`.

## Alternativas Consideradas

Pesos: Cons. 30 · Canales 20 · Robustez 20 · Aprovecha 15 · Costo 15.

### D1. Canal de la cancelación (sincrónico o asincrónico)

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **A1. Asincrónico: comando por el broker y confirmación por evento (C1)** | 5 | 5 | 4 | 5 | 3 | **4,50** |
| A2. Sincrónico: Donaciones llama a un endpoint REST de cancelación de Logística | 4 | 1 | 2 | 5 | 3 | 3,00 |
| A3. Donaciones libera la donación en el acto y manda el comando sin esperar (C2) | 2 | 4 | 3 | 5 | 4 | 3,35 |

### D2. Qué entregas cuentan como "no salieron"

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **S1. Las `PENDIENTE` de Logística, con o sin ruta sin iniciar (donación `ASIGNACION_REALIZADA` o `LISTA_PARA_ENTREGAR`)** | 5 | 5 | 4 | 5 | 2 | **4,35** |
| S2. Solo las que no tienen ruta (`ASIGNACION_REALIZADA`); una ruta planificada sigue | 5 | 5 | 5 | 2 | 4 | 4,40 |
| S3. También las que están en revisión (`NO_RECIBIDA`, `REVISION`) | 3 | 4 | 3 | 5 | 1 | 3,20 |

S2 queda apenas primera porque no toca rutas. Se descarta igual: contradice B1, que el usuario
eligió el 2026-11-02 ("según si salió": una ruta planificada todavía no salió).

### D3. Cómo viaja el comando

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **O1. Mismo outbox del broker, con un `tipo` por entrada (`SOLICITUD` o `CANCELACION`)** | 5 | 5 | 4 | 5 | 3 | **4,50** |
| O2. La marca de la donación hace de outbox, con un reenviador propio hasta la confirmación | 5 | 4 | 5 | 5 | 2 | 4,35 |
| O3. Publicar después del commit, sin outbox | 3 | 4 | 2 | 5 | 5 | 3,60 |

### D4. Orden entre el pedido y su cancelación

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **Q1. Misma cola en Logística (segundo binding) y la cancelación sale recién con la solicitud `ENVIADA`** | 5 | 5 | 4 | 5 | 4 | **4,65** |
| Q2. Cola aparte, con una "lápida" en Logística para la cancelación que llega antes que el pedido | 4 | 5 | 4 | 5 | 2 | 4,05 |

### D5. Volver a entregar una donación cancelada

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **R1. Una entrega activa por donación: índice único parcial (sin las `CANCELADA`) y solicitud `CANCELADA` en Donaciones** | 5 | 5 | 4 | 5 | 3 | **4,50** |
| R2. Reabrir la misma entrega con el destino nuevo | 3 | 5 | 3 | 5 | 4 | 3,85 |
| R3. Diferirlo como deuda | 2 | 5 | 2 | 1 | 5 | 2,90 |

### D6. Idempotencia y `traceId`

| Alternativa | Cons. | Canales | Rob. | Aprov. | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **I1. Logística vuelve a publicar la confirmación si la entrega ya está `CANCELADA`; Donaciones deduplica como hoy; el `traceId` de la baja viaja en la entrada del outbox** | 5 | 5 | 4 | 5 | 4 | **4,65** |
| I2. Tabla de comandos procesados en Logística | 5 | 5 | 4 | 5 | 2 | 4,35 |
| I3. Ignorar el comando repetido, sin volver a confirmar | 4 | 5 | 3 | 5 | 5 | 4,30 |

## Resultado de la Decisión

Se eligen **A1, S1, O1, Q1, R1 e I1** (`[PROPOSED]`, a aprobar por el usuario).

1. **Baja (Donaciones).** `NecesidadesService.eliminar` desactiva la necesidad y, en la misma
   transacción, recorre sus donaciones `ASIGNACION_REALIZADA` y `LISTA_PARA_ENTREGAR`:
   - con solicitud `ENVIADA`: la donación queda con `cancelacionPedida = true` y el broker guarda
     una entrada `CANCELACION` en el outbox, hacia el proveedor de esa solicitud, con el `traceId`
     de la baja;
   - con solicitud `PENDIENTE` (el pedido todavía no salió del outbox): igual, pero el relay no
     manda la cancelación hasta que la solicitud quede `ENVIADA` (D4);
   - sin solicitud activa (nunca se pidió o quedó `FALLIDA`): vuelve al depósito en el acto, porque
     ningún proveedor la tiene.
   Las `EN_TRASLADO` no se tocan: llegan (B1). La baja en cascada de una entidad
   (`EntidadBeneficiariaService`, que desactiva todas sus necesidades) hace lo mismo con cada una:
   las dos bajas usan el mismo componente (`CancelacionDeEntregas`). `[OBSERVED]` al implementar.
2. **Comando.** `entrega.cancelada.<proveedorId>.v1` en `donaciones.exchange`, con
   `donacionIndependienteId`, `motivo = NECESIDAD_DADA_DE_BAJA` y `fecha`; headers `X-Trace-Id` y
   `messageId` como el pedido. Mismas reglas de envío que el pedido (`mandatory` y acuse), salvo
   que un rechazo **no** pasa al siguiente proveedor: se reintenta con el mismo, porque la entrega
   está en ese.
3. **Proveedores HTTP.** El puerto `IProveedorLogistica` suma `cancelar`. El adapter HTTP no tiene
   cancelación: responde "no soportada", la marca se limpia, la donación sigue su camino y queda un
   log `warn` (punto 6 del ADR de la baja).
4. **Logística.** El comando llega a la **misma cola** que el pedido (segundo binding con la clave
   exacta), así que siempre se procesa después del pedido que cancela. `Entrega.cancelar(actor)`
   solo desde `PENDIENTE`: pasa a `CANCELADA` (valor nuevo de `EstadoEntrega`) y, si estaba en una
   ruta sin iniciar, sale de ella (`Ruta.quitarEntrega`). Publica `entrega.cancelada` en
   `logistica.exchange`, firmado como los otros cuatro eventos. Si la entrega ya salió
   (`EN_TRASLADO` o después), no hace nada: el `ruta.iniciada` dice que salió.
5. **Idempotencia (I1).** Un comando sobre una entrega ya `CANCELADA` vuelve a publicar la
   confirmación (cubre una confirmación perdida). Donaciones deduplica por
   `(EntregaCancelada, entregaId, donación)` con la tabla `evento_consumido` de hoy. Logística
   restaura el `X-Trace-Id` del comando en el MDC y lo publica en el evento; el header lo agrega
   la firma común de los eventos de vuelta, así que los otros cuatro también lo llevan cuando hay
   un traceId en el MDC (`[OBSERVED]` al implementar; avanza DTI-16, ítem 7).
6. **Vuelta (Donaciones).** Al recibir `entrega.cancelada`, verificado como cualquier evento de
   vuelta, la donación `ASIGNACION_REALIZADA` o `LISTA_PARA_ENTREGAR` vuelve a `EN_DEPOSITO` sin
   destino (limpia `necesidadId` y `periodoId`, como `EntregaFallida`), la marca se limpia y la
   solicitud pasa a `CANCELADA`. El algoritmo la vuelve a tomar en su próxima corrida.
7. **Una entrega activa por donación (R1).** Logística: el índice único pasa a parcial (`WHERE
   estado <> 'CANCELADA'` en Postgres; `partialFilter` en Mongo, con nombre nuevo) y la
   deduplicación del pedido mira solo las activas. Donaciones: `EstadoSolicitudEntrega.CANCELADA`
   no cuenta como activa.
8. **Carrera con la salida.** Si gana `ruta.iniciada`, la donación pasa a `EN_TRASLADO` y la marca
   se limpia al salir de los estados cancelables. Logística, al recibir después la cancelación, no
   hace nada (punto 4).
9. **Guarda.** El `PATCH` de estado que lleva una donación a `ASIGNACION_REALIZADA` rechaza una
   necesidad que no admite asignaciones (`NECESIDAD_INACTIVA`, `ERR-EST-521`), como la aprobación de
   propuestas.
10. **Contratos.** Aditivos: `cancelacionPedida` en la salida de la donación independiente; el
    comando y el evento nuevos con sus schemas. Incompatible: `CANCELADA` en el enum de estado que
    devuelve Logística, registrado en `cambios-incompatibles-aceptados.txt` (corte coordinado: los
    consumidores están en el repo).

## Consecuencias

### Positivas

- Ninguna donación queda en dos lugares: Donaciones la libera recién con la confirmación.
- El pedido y su cancelación no se cruzan, sin tabla extra en Logística.
- Una donación cancelada puede volver a entregarse, también por nuestra Logística.
- Sin canales nuevos: mismo exchange, mismo outbox, misma verificación de la vuelta.

### Negativas

- Un valor nuevo en un enum que se devuelve (`CANCELADA`): un consumidor estricto puede romperse.
- El índice único de Logística cambia. En una base Mongo persistente queda el índice viejo y hay que
  borrarlo a mano (`idDonacion`); en Postgres lo cambia la migración.
- Si la confirmación de Logística se pierde (se publica después del commit, sin outbox, como los
  otros cuatro eventos), la donación queda marcada "cancelación pedida" hasta que alguien repita
  la baja. Es la misma limitación de los otros eventos de vuelta (DTI-14, ítem 8).
- Si el proveedor no tiene la entrega (pedido descartado por inválido) o el pedido termina `FALLIDA`
  después de la baja, nadie confirma: la donación queda asignada y marcada hasta una revisión
  manual, como cualquier solicitud `FALLIDA` del broker (`[OBSERVED]` en la pre-auditoría).
- Una ruta que se queda sin paradas no se puede iniciar (`Ruta.iniciarRuta` lo rechaza) y queda
  `PENDIENTE` vacía.
- Un proveedor HTTP no cancela: la donación llega igual a la entidad que la dio de baja.

## Análisis por alternativa

- **A1.** Pro: consistencia sin transacción distribuida y por los canales decididos. Contra: estado
  intermedio. **A2.** Pro: respuesta inmediata a la entidad. Contra: canal sincrónico nuevo hacia
  nuestra Logística, saltea el broker y la baja falla si Logística está caída. **A3.** Pro:
  inmediata. Contra: si el camión ya salió, la donación figura en el depósito y en camino.
- **S1.** Pro: respeta B1. Contra: hay que sacar paradas de rutas. **S2.** Pro: más simple. Contra:
  contradice B1. **S3.** Pro: abarca más. Contra: se cruza con la decisión del administrador sobre
  la revisión.
- **O1.** Pro: reusa el outbox, el relay y la clasificación de resultados. Contra: una columna y un
  `switch` más. **O2.** Pro: reintenta hasta la confirmación. Contra: un segundo relay que duplica
  al outbox. **O3.** Pro: lo más barato. Contra: si RabbitMQ está caído, la cancelación se pierde.
- **Q1.** Pro: FIFO de una cola con un consumidor. Contra: el listener pasa a atender dos tipos.
  **Q2.** Pro: colas separadas. Contra: tabla de lápidas y su limpieza.
- **R1.** Pro: la historia queda y se puede volver a entregar. Contra: cambia un índice en dos
  motores. **R2.** Pro: sin índices nuevos. Contra: el mismo `entregaId` con otro destino choca con
  la deduplicación por `entregaId`. **R3.** Pro: nada que hacer. Contra: la donación reasignada
  queda trabada.
- **I1.** Pro: cubre la confirmación perdida sin estado extra. Contra: puede llegar una
  confirmación repetida, que Donaciones ya descarta. **I2.** Pro: deduplicación explícita. Contra:
  tabla nueva. **I3.** Pro: lo más simple. Contra: una confirmación perdida no se recupera.

## Trabajo futuro

- [DTI-31](./DEUDA_TECNICA.md#dti-31): se salda con esta implementación, salvo lo de abajo.
- Avisarle al donante que su donación va a otra entidad (no hay evento para eso).
- Liberar camión y chofer de una ruta que se quedó sin paradas.
- `[INFERRED]` Fuera de alcance, deuda previa: una donación `ENTREGA_FALLIDA` que vuelve al depósito
  y se reasigna choca con la solicitud `ENVIADA` y con la entrega única de Logística (mismo hueco
  que R1 resuelve para la cancelación; habría que cerrar la solicitud al fallar).
