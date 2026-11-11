# Estado EN_PUERTA de la entrega: el camión llegó y la entidad todavía no confirmó

- Status: proposed
- Date: 2026-11-11
- Deciders: tsorren (pendiente de aprobación); redactado por el agente en HB-22
- Tags: logistica, donaciones, entregas, maquina-de-estados, amqp, contrato, cliente-liviano, entrega-6

> **Origen:** HB-22 de la corrida de cierre de E6
> ([SPEC-09](../../specs/active/SPEC-09-cierre-e6-demo-punta-a-punta.md), §3.1 "D1"). El usuario
> eligió el estado nuevo (alternativa b) contra la recomendación de la matriz (2026-11-11). Este ADR
> baja esa elección al diseño: quién pone el estado, qué transiciones abre, cómo lo replica
> Donaciones y qué cambia en los contratos. Extiende la máquina de
> [manejo de transiciones y ciclo de vida de Entrega](./20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md),
> sin reemplazarla.

## Contexto y Problema

La entidad necesita ver, en "Esperan tu confirmación", las entregas que ya llegaron a su puerta. Hoy
no se puede saber:

- `[OBSERVED]` `EstadoEntrega` (`logistica-service/.../entregas/EstadoEntrega.java`) tiene
  `PENDIENTE`, `EN_TRASLADO`, `ENTREGADA`, `NO_RECIBIDA`, `REVISION` y `CANCELADA`. Una entrega que
  viene en camino y una que espera en la puerta están las dos `EN_TRASLADO`.
- `[OBSERVED]` `horaArribo` se completa recién al confirmar (`Entrega.confirmarEntrega`): no sirve
  para saber que el camión llegó.
- `[OBSERVED]` El camión lo mueve la simulación (`FuentePosicionCamionSimulada`, ADR-E6-07), que
  calcula la posición por el tiempo transcurrido y recorre las paradas en el orden de visita.
  `ActualizadorPosicionCamiones` guarda esa posición en el camión cada intervalo. Nadie usa ese dato
  para la entrega.
- `[OBSERVED]` Donaciones replica el estado de la entrega en su propia máquina
  (`TipoEstadoDonacion`, patrón State en `DonacionIndependiente`) a partir de los eventos de
  `logistica.exchange` (`ruta.iniciada` → `EN_TRASLADO`, `entrega.exitosa` → `ENTREGADA`). Las tres
  pantallas de la entidad (Inicio, Donaciones recibidas y Seguir entregas) leen el estado de
  Donaciones (`RecibidasRest`), que hoy manda todo lo que está en traslado a "En camino".
- `[OBSERVED]` `RecibidasRest` ya tiene el grupo `puerta` en su orden, pero ningún estado lo llena.

Problema: ¿cómo se agrega a la máquina de la entrega un estado "llegó a la puerta" sin romper a
quien hoy confirma desde `EN_TRASLADO`, y cómo llega ese dato hasta la pantalla de la entidad?

## Atributos de Calidad y Drivers de Decisión

- **Dato confiable:** lo pone quien mueve el camión, no una deducción de la pantalla.
- **Compatibilidad:** los consumidores que confirman desde `EN_TRASLADO` (integration-tests, flujos
  de Postman, proveedores de logística que no conocen el estado nuevo) siguen funcionando.
- **Trazabilidad:** el cambio queda en el historial de la entrega y de la donación, con su hora.
- **Mantenibilidad:** se respetan los patrones existentes (métodos de comportamiento con historial
  en `Entrega`, State en `DonacionIndependiente`, eventos de vuelta firmados en
  `logistica.exchange`).

## Relaciones Arquitectónicas

- Extiende [manejo de transiciones y ciclo de vida de Entrega](./20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md)
  (métodos de comportamiento explícitos con enum e historial).
- Sigue la mensajería de [uso de RabbitMQ entre Logística y los demás servicios](./20260703-uso-de-rabbitmq-para-la-comunicacion-entre-logistica-y-los-demas-servicios.md):
  un evento de vuelta más en `logistica.exchange`, con su cola en Donaciones.
- Toma el precedente de [cancelación de entregas que no salieron](../20261109-cancelacion-de-entregas-que-no-salieron.md)
  (HB-19) para el valor nuevo del enum: minor de la OpenAPI, línea en
  `cambios-incompatibles-aceptados.txt` y "una entrega activa por donación".

## Alternativas Consideradas

### D1. Cómo se sabe que la entrega llegó (decidida por el usuario, SPEC-09 §3.1)

| Criterio (peso) | a. Marca `llegoEn` en `Entrega` | **b. Estado nuevo `EN_PUERTA`** | c. Lo deduce el cliente por la distancia |
|---|:-:|:-:|:-:|
| Contrato aditivo (0,30) | 5 | 2 | 5 |
| No toca la máquina de estados (0,25) | 5 | 1 | 5 |
| Dato confiable (0,25) | 4 | 5 | 2 |
| Tamaño (0,20) | 4 | 2 | 5 |
| **Total** | 4,55 | 2,50 | 4,25 |

- **a. Marca `llegoEn`:** un campo opcional en `Entrega`, replicado en Donaciones. Aditivo, pero la
  pantalla tiene que combinar estado y marca para decidir el grupo. Descartada por el usuario: el
  dato más claro para la entidad es un estado de la máquina, no una marca al costado.
- **b. Estado `EN_PUERTA`:** elegida.
- **c. Deducción en el cliente:** depende de la posición simulada y de un umbral de distancia; el
  cliente decidiría algo que es del dominio de Logística. `[REJECTED]`.

### D2. Quién pone el estado

| Opción | Pros | Contras |
|---|---|---|
| **a. La fuente de posición del camión (simulación) y el `PATCH` de estado** | El dato lo pone quien mueve el camión. En la demo pasa solo; un chofer o un proveedor real lo informa por el endpoint que ya existe | El planificador de posiciones pasa a escribir entregas |
| b. Solo el `PATCH` | Mínimo | En la demo nadie lo llama: la entidad nunca vería "Esperan tu confirmación" |
| c. La entidad, al abrir la pantalla | — | No es un dato confiable: la entidad confirmaría que llegó sin haberlo visto |

### D3. Transiciones

| Opción | Pros | Contras |
|---|---|---|
| **a. `EN_PUERTA` intermedio y opcional; `EN_TRASLADO → ENTREGADA / NO_RECIBIDA` sigue** | Nadie que confirme desde `EN_TRASLADO` se rompe. Una parada sin coordenadas o un proveedor que no informa la llegada se pueden confirmar igual | La máquina tiene dos caminos al mismo final |
| b. `EN_PUERTA` obligatorio antes de confirmar | Un solo camino | Rompe integration-tests, Postman y a todo proveedor que no lo mande; con el planificador apagado nada se podría confirmar |

### D4. Cómo llega a Donaciones

| Opción | Pros | Contras |
|---|---|---|
| **a. Evento de vuelta `entrega.en-puerta` y estado `EN_PUERTA` en `TipoEstadoDonacion`** | Mismo canal, firma e idempotencia que los otros eventos de vuelta. Las tres pantallas ya leen Donaciones | Un estado más en el State de Donaciones y una migración |
| b. El cliente lee el estado en Logística | Sin cambios en Donaciones | Recibidas e Inicio hacen una segunda llamada por entidad; Donaciones queda desfasada respecto de Logística |

## Resultado de la Decisión

**D1 = b, D2 = a, D3 = a, D4 = a.**

**Máquina de `Entrega` (Logística):**

```text
PENDIENTE ──iniciarRuta──> EN_TRASLADO ──llegarAPuerta──> EN_PUERTA
                               │                            │
                               ├──confirmarEntrega──────────┼──> ENTREGADA
                               └──negarEntrega──────────────┴──> NO_RECIBIDA ──> REVISION ──> PENDIENTE
PENDIENTE ──cancelar──> CANCELADA (final)
```

- `Entrega.llegarAPuerta(actor)`: solo desde `EN_TRASLADO`; registra el cambio en el historial y el
  evento de dominio `EntregaEnPuerta`. No toca `horaArribo`, que sigue siendo la hora de la
  confirmación: la hora de llegada es la del cambio a `EN_PUERTA` en el historial.
- `confirmarEntrega` y `negarEntrega` aceptan `EN_TRASLADO` y `EN_PUERTA`.
- Sin `EN_PUERTA → REVISION` directo: la revisión es el paso del administrador después de una falla,
  que necesita la justificación de `NO_RECIBIDA`.
- `EN_PUERTA` es un estado activo: cuenta para "una entrega activa por donación" (índice único
  parcial), para las entregas activas del donante (HB-16) y bloquea completar la ruta, como
  `EN_TRASLADO`.

**Quién lo pone:**

- La fuente de posición dice qué paradas alcanzó el camión
  (`FuentePosicionCamion.entregasAlcanzadas(Ruta)`, con un `default` vacío para otras fuentes). La
  simulación devuelve las paradas con coordenadas cuyo índice es menor o igual al tramo en curso:
  si se pierde un tick, la parada se marca en el siguiente. El camión arranca en la primera parada,
  así que esa entrega pasa a `EN_PUERTA` en el primer tick.
- `ActualizadorPosicionCamiones` llama a `EntregasService.marcarEnPuerta` por cada una que siga
  `EN_TRASLADO`, con el actor `SIMULADOR`. Si pierde la carrera de versión con la confirmación de
  la entidad, la saltea.
- `PATCH /api/entregas/{id}/estado` con `estado=EN_PUERTA` y el `actor` (chofer o proveedor) hace lo
  mismo. Es idempotente: si la entrega ya está `EN_PUERTA`, no cambia nada.

**Donaciones:** `TipoEstadoDonacion.EN_PUERTA` y el estado `EnPuerta`, al que se llega solo desde
`EnTraslado` (`llegarAPuerta`). Desde `EnPuerta` se confirma la entrega o se registra la falla, igual
que desde `EnTraslado`, que conserva esas dos salidas. El consumidor de `entrega.en-puerta` aplica el
cambio solo si la donación está `EN_TRASLADO`; si ya está en otro estado (el evento llegó después de
`entrega.exitosa`, o se repitió), lo registra como consumido y no lo aplica.

**Contratos:**

- REST (cambio de contrato, AGENTS.md §8.2): `EN_PUERTA` se suma al enum del estado de la entrega
  (`openapi-logistica.yaml` 1.9.0: respuestas, seguimiento, historial y el request del `PATCH`) y
  al de la donación independiente (`openapi-donaciones.yaml` 1.8.0). Un consumidor estricto del enum
  puede romperse; se anota en `cambios-incompatibles-aceptados.txt`.
- AMQP: evento de vuelta `entrega.en-puerta` (alias `__TypeId__` igual a la routing key) con
  `entregaId`, `donacionIndependienteId` y `fechaLlegada`, firmado con `X-Proveedor-Id` y
  `X-Proveedor-Token`. Esquema: `schemas/evento-entrega-en-puerta.schema.json`.
- Persistencia: Logística V9 (`ALTER TYPE estado_entrega ADD VALUE 'EN_PUERTA'` y el índice único
  parcial pasa a `estado <> 'CANCELADA'`, que no usa el valor recién agregado). Mongo suma
  `EN_PUERTA` al filtro del índice parcial, con un nombre nuevo. Donaciones V20 reemplaza los dos
  `CHECK` de estado.

## Consecuencias

### Positivas

- La entidad ve en "Esperan tu confirmación" lo que de verdad llegó, en Inicio, Recibidas y Seguir
  entregas, con la hora de llegada del historial.
- Nada que hoy confirme desde `EN_TRASLADO` se rompe.
- El historial de la entrega y de la donación distingue el viaje de la espera en la puerta.

### Negativas

- No es aditivo: un consumidor externo que valide el enum de forma estricta se rompe (mitigado con
  la versión minor y la línea en `cambios-incompatibles-aceptados.txt`).
- Dos caminos a `ENTREGADA`: los reportes que midan la espera en la puerta solo la tienen cuando
  alguien informó la llegada.
- En la simulación la primera parada pasa a `EN_PUERTA` apenas sale el camión.
- El planificador de posiciones escribe entregas y publica eventos, además de mover el camión.
- El `PATCH` de estado conserva su permiso (`CONFIRMAR_SU_ENTREGA`): la entidad podría marcar su
  propia entrega en la puerta. El cliente no lo hace; restringirlo por estado queda para la
  seguridad del grupo (SPEC-06).
- Una base Mongo persistente conserva el índice viejo `uq_entrega_donacion_activa`; es redundante
  (cubre un subconjunto) y se puede borrar a mano.

## Análisis por alternativa

- **D1.a (marca `llegoEn`):** aditiva y chica, pero deja la decisión del grupo repartida entre estado y
  marca, y la máquina no refleja la espera. **D1.c:** responsabilidad del dominio en la pantalla
  (viola el principio de experto en información, GRASP).
- **D2.b:** correcta pero incompleta para la demo. **D2.c:** el dato no lo da quien lo sabe.
- **D3.b:** más pura, pero rompe a consumidores fuera del alcance del frente y deja a la entidad sin
  poder confirmar cuando nadie informa la llegada.
- **D4.b:** acopla tres pantallas a dos servicios y deja a Donaciones sin el dato que replica
  para todos los demás estados.

## Trabajo futuro

- Notificar a la entidad que la entrega llegó a su puerta (evento notificable desde Donaciones):
  fuera de HB-22.
- Un proveedor externo de logística (n8n) tendría que publicar `entrega.en-puerta` para que sus
  entregas aparezcan en "Esperan tu confirmación"; mientras no lo haga, siguen en "En camino" y se
  confirman igual.
