# Baja de una necesidad con entregas en curso: lo que salió llega, lo que no vuelve al depósito

- Status: proposed
- Date: 2026-11-02
- Deciders: tsorren, con las respuestas del grupo (Nico, Anush y Camila) a las preguntas abiertas de Raíces
- Tags: donaciones, logistica, necesidades, amqp, broker, comandos, saga, idempotencia, entrega-6

> **Origen:** pregunta 13 del artifact de preguntas abiertas de Raíces (2026-11-01). El grupo votó C, B, C
> y el usuario eligió "según si salió" el 2026-11-02. La ficha que la planteó es
> [Necesidades de la entidad](../interfaz/flujos/raices-necesidades.md). Deuda asociada:
> [DTI-31](./DEUDA_TECNICA.md#dti-31). No se toca código en esta tarea.

## Contexto y Problema

Una entidad puede dar de baja una necesidad (RF-10) que ya tiene donaciones asignadas, algunas
todavía en el depósito y otras arriba del camión.

Qué pasa hoy:

- `[OBSERVED]` `DELETE /api/necesidades/{id}` hace una baja lógica (`NecesidadesService.eliminar`,
  `Necesidad.desactivar()`) y no toca las donaciones asignadas.
- `[OBSERVED]` La donación independiente pasa por `EN_DEPOSITO → ASIGNACION_REALIZADA →
  LISTA_PARA_ENTREGAR → EN_TRASLADO → ENTREGADA` (`TipoEstadoDonacion`). `EN_DEPOSITO` sin destino
  es "espera destino": el algoritmo de asignación la vuelve a tomar.
- `[OBSERVED]` Al asignarla, el broker de Donaciones manda el comando `entrega.solicitada.<proveedorId>.v1`
  al proveedor elegido ([broker de integración](./20261007-broker-de-integracion-con-logistica.md)).
  La vuelta llega solo por eventos firmados ([vuelta por mensajería](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md)).
- `[OBSERVED]` Logística no puede cancelar una entrega. `EstadoEntrega` no tiene un estado de
  cancelación y el controlador de entregas no tiene `DELETE`. El único camino de vuelta es
  `REVISION → PENDIENTE` (`Entrega.java:235-246`).
- `[INFERRED]` Una donación puede quedar asignada a una necesidad dada de baja: la asignación por
  `PATCH` no consulta `activa`. Solo lo hace la aprobación de propuestas (`NECESIDAD_INACTIVA`).

Problema: al dar de baja una necesidad, ¿qué pasa con cada donación asignada, según dónde esté?

## Atributos de Calidad y Drivers de Decisión

Que ninguna donación quede perdida o en dos lugares (consistencia) 35 · respetar el broker y la
vuelta por mensajería ya decididos 25 · que lo donado se aproveche (va a otra entidad) 20 · costo
15 · claridad para la entidad 5.

## Relaciones Arquitectónicas

1. **Extiende:** [broker de integración con Logística](./20261007-broker-de-integracion-con-logistica.md)
   (`proposed`). Suma un segundo comando, ruteado por destinatario como el primero.
2. **Extiende:** [vuelta por mensajería con identidad](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md)
   (`proposed`). Suma un quinto evento de vuelta, firmado igual que los otros cuatro.
3. **Respeta:** [ciclo de vida de la entrega](./logistica-service/20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md)
   (`accepted`). La cancelación es otro método explícito de `Entrega`.
4. **Respeta:** [idempotencia en el consumo de eventos](./20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md)
   y [outbox transaccional](./20260901-patron-transactional-outbox-para-consistencia-eventual.md).
5. **Se relaciona con:** [ciclo de vida de la necesidad](./donaciones-service/20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato.md)
   (`proposed`).

## Alternativas Consideradas

Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`).

### Qué pasa con las entregas

| Alternativa | Consistencia | Respeta broker | Aprovecha | Costo | Claridad | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **B1. Según si salió: lo que está en camino llega; lo que no, se cancela y vuelve a "espera destino"** | 4 | 5 | 5 | 2 | 4 | **4,1** |
| B2. Todo llega igual y la baja corre desde el próximo pedido | 5 | 5 | 2 | 5 | 3 | 4,1 |
| B3. No se puede dar de baja mientras haya entregas sin terminar | 5 | 5 | 3 | 4 | 2 | 4,2 |

Las tres quedan parejas en la matriz. El usuario eligió **B1**: es la única en la que lo que no
salió llega a otra entidad que lo necesita. El costo es el comando y el estado nuevos.

### Cómo se cancela lo que no salió

| Alternativa | Consistencia | Respeta broker | Aprovecha | Costo | Claridad | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **C1. Comando de cancelación y confirmación por evento; Donaciones devuelve la donación al depósito recién al confirmar** | 5 | 5 | 5 | 3 | 4 | **4,7** |
| C2. Donaciones devuelve la donación al depósito en el acto y manda el comando sin esperar | 2 | 4 | 5 | 4 | 4 | 3,5 |
| C3. Logística consulta a Donaciones por HTTP si la necesidad sigue activa antes de salir | 3 | 1 | 5 | 2 | 4 | 2,9 |

## Resultado de la Decisión

Se eligen **B1** (usuario) y **C1** (agente, `[PROPOSED]`).

1. **La baja sigue siendo lógica.** `Necesidad.desactivar()`, como hoy. Después, Donaciones recorre
   las donaciones con ese `necesidadId` que no están `ENTREGADA`.
2. **Lo que salió llega.** Una donación `EN_TRASLADO` sigue su camino y, al confirmarse, cuenta
   como recibida en el período donde estaba. No se toca.
3. **Lo que no salió se cancela.** Una donación `ASIGNACION_REALIZADA` o `LISTA_PARA_ENTREGAR` queda
   marcada con "cancelación pedida", sin cambiar de estado. El broker manda el comando
   `entrega.cancelada.<proveedorId>.v1` al proveedor de su `SolicitudEntrega`, por el outbox del
   broker, con `donacionIndependienteId`, `motivo = NECESIDAD_DADA_DE_BAJA` y `traceId`.
4. **Logística responde.** `Entrega.cancelar(actor)`: solo desde `PENDIENTE`. Pasa a un estado
   nuevo, `CANCELADA`, y si tenía ruta sin iniciar, sale de ella (las paradas que quedan se
   reordenan). Publica `entrega.cancelada` en `logistica.exchange`, firmado como los otros cuatro
   eventos. Si la entrega ya está `EN_TRASLADO`, no la cancela: el `ruta.iniciada` que ya mandó o
   va a mandar dice que salió, y vale el punto 2.
5. **Donaciones cierra.** Al recibir `entrega.cancelada`, verificado como cualquier evento de vuelta,
   devuelve la donación a `EN_DEPOSITO` sin destino. Limpia `necesidadId` y `periodoId`, como
   hace hoy `EntregaFallida`. El algoritmo de asignación la vuelve a tomar en su próxima corrida.
6. **Proveedores HTTP.** El adapter HTTP traduce el comando a la cancelación de ese proveedor. Si el
   proveedor no la tiene, la donación sigue su camino (punto 2) y queda un log `warn`.
7. **Idempotencia.** Logística deduplica el comando por `donacionIndependienteId`; Donaciones, el
   evento por `(tipo, businessId, donación)`, como hoy.
8. **La pantalla** dice antes de confirmar cuántas entregas ya salieron (llegan igual) y cuántas
   vuelven al depósito para otra entidad.
9. **Guarda que falta.** La asignación por `PATCH` rechaza una necesidad inactiva
   (`NECESIDAD_INACTIVA`), igual que la aprobación de propuestas.

## Consecuencias

### Positivas

- Ninguna donación queda en dos lugares: Donaciones no la libera hasta que Logística confirma.
- Lo que no salió llega a otra entidad, sin trabajo extra del administrador.
- Usa los mismos canales: comando por destinatario, vuelta firmada, outbox e idempotencia.

### Negativas

- Un comando, un evento y un estado nuevos (`CANCELADA`) en los contratos AMQP y en el enum de
  Logística. Un valor nuevo de un enum que se devuelve puede romper a un consumidor estricto: se
  registra en `cambios-incompatibles-aceptados.txt`.
- Mientras no llega la confirmación, la donación figura asignada a una necesidad dada de baja. La
  pantalla la muestra como "vuelve al depósito".
- Sacar una parada de una ruta planificada obliga a reordenar las paradas y a avisarle al
  planificador.

## Análisis por alternativa

- **B1.** Pro: aprovecha lo donado. Contra: es la más cara. **B2.** Pro: no hay que cancelar nada.
  Contra: la entidad recibe lo que ya no necesita. **B3.** Pro: lo más simple y consistente.
  Contra: la entidad no puede dar de baja algo que ya no necesita.
- **C1.** Pro: consistencia sin transacciones distribuidas (una saga corta, con confirmación).
  Contra: hay un estado intermedio. **C2.** Pro: inmediata. Contra: si el camión ya salió, la
  donación figura en el depósito y en camino a la vez. **C3.** Pro: Logística decide con el dato
  fresco. Contra: un canal HTTP de Logística a Donaciones, que la vuelta por mensajería descartó.

## Trabajo futuro

- [DTI-31](./DEUDA_TECNICA.md#dti-31): comando, evento, estado `CANCELADA`, schemas y la guarda del
  punto 9.
