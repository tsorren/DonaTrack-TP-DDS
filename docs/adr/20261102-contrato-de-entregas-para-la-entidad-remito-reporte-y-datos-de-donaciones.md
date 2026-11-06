# Contrato de entregas para la entidad: remito, reporte de problemas y datos que vienen de Donaciones

- Status: proposed
- Date: 2026-11-02
- Deciders: tsorren, con las respuestas del grupo (Nico, Anush y Camila) a las preguntas abiertas de Raíces
- Tags: logistica, donaciones, cliente-liviano, contratos, openapi, entregas, composicion, entrega-6

> **Origen:** preguntas 5 y 14 del artifact de preguntas abiertas de Raíces (2026-11-01), decididas por el
> usuario el 2026-11-02. Las fichas que las plantearon son
> [Confirmar recepción](../interfaz/flujos/raices-confirmar-recepcion.md) y
> [Seguir entregas](../interfaz/flujos/raices-seguir-entregas.md). Deuda asociada:
> [DTI-29](./DEUDA_TECNICA.md#dti-29). No se toca código en esta tarea.

## Contexto y Problema

La pantalla "Confirmar recepción" de la entidad (RF-12) muestra, junto a la entrega, el pedido que
cubre y quién donó, y pide fotos, el número de remito y la cantidad de bultos. Si la entrega llegó
incompleta o dañada, ofrece un enlace, "Llegó con un problema".

Qué hay hoy en el contrato:

- `[OBSERVED]` `EstadoEntrega` tiene `PENDIENTE, EN_TRASLADO, ENTREGADA, NO_RECIBIDA, REVISION`
  (`logistica-service/.../entregas/EstadoEntrega.java:3-9`). Las transiciones son métodos del agregado
  `Entrega`, como fija el [ADR del ciclo de vida de la entrega](./logistica-service/20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md)
  (`accepted`).
- `[OBSERVED]` `NO_RECIBIDA` sale de `negarEntrega(entidad, justificacion, replanificable)` y exige
  una justificación libre (`Entrega.java:212-223`). Publica `entrega.fallida`, y Donaciones
  devuelve la donación al depósito sin destino (`EntregaFallida`). `REVISION` es el paso del
  administrador entre `NO_RECIBIDA` y `PENDIENTE` (`Entrega.java:225-246`).
- `[OBSERVED]` Confirmar (`ENTREGADA`) exige al menos una foto y no acepta nada más: no hay remito,
  bultos, cantidad recibida ni motivo (`CambioEstadoEntregaRequestDTO.java:9-16`). En
  `logistica-service/src/main` no aparecen `remito` ni `bulto`.
- `[OBSERVED]` `EntregaResponseDTO` y `SeguimientoEntregaResponseDTO` solo llevan `idDonacion` e
  `idBeneficiaria`: ni la necesidad, ni el donante, ni la categoría. Esos datos viven en Donaciones
  (`DonacionIndependiente.necesidadId`, `DonacionOutputDTO.donante`).
- `[OBSERVED]` `cliente-liviano` todavía no consume ningún servicio: sus pantallas son maqueta
  (`PrototipoController`).

Problema: ¿qué agrega el contrato de entregas para la pantalla de la entidad, de dónde salen el
pedido y el donante, y cómo se confirma una entrega que llegó con un problema?

## Atributos de Calidad y Drivers de Decisión

Cohesión de cada servicio con su dato (GRASP Experto) 30 · cambios aditivos, sin romper contratos
(AGENTS.md §8.2) 25 · trazabilidad de lo recibido (RF-12, RF-01 y RF-02 usan la foto) 20 · costo de
implementarlo 15 · tráfico y latencia de la pantalla 10.

## Relaciones Arquitectónicas

1. **Respeta:** [ciclo de vida de la entrega](./logistica-service/20260630-manejo-de-transiciones-y-ciclo-de-vida-de-entrega.md)
   (`accepted`): las transiciones siguen siendo métodos explícitos de `Entrega`, con el estado en
   un enum. No se suma un estado.
2. **Respeta:** [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): los campos nuevos son aditivos.
3. **Respeta:** [comunicación asimétrica entre servicios](./20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md):
   no se suma una llamada síncrona de Logística a Donaciones.
4. **Se relaciona con:** [nombre visible del donante](./20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md)
   (`proposed`), que fija qué se ve de quien donó.

## Alternativas Consideradas

Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`).

### Pedido y donante en la pantalla de la entidad

| Alternativa | Cohesión | Aditivo | Trazab. | Costo | Tráfico | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **P1. El cliente compone: pide la entrega a Logística y la donación a Donaciones, por `idDonacion`** | 5 | 5 | 4 | 4 | 3 | **4,5** |
| P2. Logística replica pedido y donante desde los eventos de Donaciones y los devuelve en su DTO | 2 | 4 | 4 | 2 | 5 | 3,2 |
| P3. Donaciones expone una vista que ya junta entrega y donación (llama a Logística) | 3 | 5 | 4 | 2 | 4 | 3,6 |

### Remito y bultos

| Alternativa | Cohesión | Aditivo | Trazab. | Costo | Tráfico | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **R1. El remito entra al contrato de entregas; los bultos salen de la pantalla** | 5 | 5 | 4 | 5 | 5 | **4,8** |
| R2. Remito y bultos, los dos al contrato | 4 | 5 | 4 | 4 | 5 | 4,4 |
| R3. Ninguno al contrato: se piden en la pantalla y no se guardan | — | — | — | — | — | Descartada: pedir un dato que se tira no es honesto con la entidad |

### Entrega incompleta o dañada

| Alternativa | Cohesión | Aditivo | Trazab. | Costo | Tráfico | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **I1. Se confirma (`ENTREGADA`, con fotos) y se adjunta un reporte del problema** | 5 | 5 | 5 | 4 | 5 | **4,9** |
| I2. No se confirma: pasa a `NO_RECIBIDA` y el administrador la revisa (`REVISION`) | 3 | 5 | 2 | 5 | 5 | 3,7 |
| I3. Un estado nuevo, `ENTREGADA_CON_PROBLEMA` | 3 | 2 | 4 | 2 | 5 | 2,9 |

## Resultado de la Decisión

Se eligen **P1**, **R1** e **I1**: el grupo votó A, A, A en la 5 y A, A, A en la 14, y el usuario
lo decidió así el 2026-11-02. "Bultos fuera" lo decidió el usuario el 2026-11-02, ante la duda que
dejó su elección de "solo el remito".

1. **Remito.** `PATCH /api/entregas/{id}/estado` con `ENTREGADA` acepta `numeroRemito` (texto, de 1
   a 40 caracteres). Es obligatorio para la entidad, pero opcional en el schema durante la
   migración, como `urlMapa` en DTI-27. `EntregaResponseDTO` lo devuelve. No se publica: no va en
   `EntregaMapaPublicoResponseDTO` ni en `entrega.exitosa`.
2. **Bultos.** No entran al contrato. La pantalla los saca: la etiqueta ya dice cuánto llegó
   (cantidad y unidad de la donación, dato de Donaciones).
3. **Pedido y donante.** El cliente liviano los compone. Primero pide `GET /api/entregas/{id}` a
   Logística, después `GET /donaciones-independientes/{idDonacion}` a Donaciones. De esa respuesta
   saca dos datos:
   - la necesidad: `necesidadId` → `GET /api/necesidades/{id}`;
   - el donante: `donacionOriginalId` → `GET /api/donaciones/{id}`.

   `[OBSERVED]` `DonacionIndependienteResponseDTO` hoy no devuelve `necesidadId`, aunque la donación
   lo guarda (`DonacionIndependiente.java:36-38`): se suma como campo opcional. El nombre visible
   del donante lo fija el [ADR del nombre visible](./20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md).
   Logística no guarda datos de Donaciones.
4. **Confirma y reporta.** Una entrega que llegó incompleta o dañada se confirma como cualquier
   otra (`ENTREGADA`, con fotos y remito). La confirmación acepta además un `reporte` opcional:
   - `motivo` (enum `INCOMPLETA`, `DANADA`, `OTRO`);
   - `detalle` (texto);
   - `cantidadRecibida` (opcional, en la unidad de la donación).

   El reporte se guarda en la entrega y viaja en `entrega.exitosa` como campo opcional, para que
   Donaciones e Incentivos sepan que hubo un problema. `NO_RECIBIDA` y `REVISION` no cambian: siguen
   siendo "no llegó" y su revisión.
5. **El diseño del reporte va aparte.** Esta decisión fija solo dónde vive. La pantalla del reporte,
   sus textos y la regla de qué hace Donaciones con una cantidad menor se diseñan después (DTI-29).

## Consecuencias

### Positivas

- Cada dato vive en su dueño. Logística no replica la necesidad ni el donante, y no queda nada que
  sincronizar.
- Todos los cambios son aditivos: un cliente actual que no manda remito ni reporte sigue andando.
- La entidad no queda sin confirmar por un problema menor, y la foto llega igual a la portada y al
  mapa (RF-01, RF-02).

### Negativas

- La pantalla de la entidad hace cuatro llamadas en vez de una. Se acepta: es una pantalla por
  entrega, de uso ocasional.
- La entidad necesita permiso de lectura sobre la donación y la necesidad de su entrega. Hoy no
  hay control de pertenencia: depende de HB-13.
- `entrega.exitosa` suma un campo opcional, y su schema y sus consumidores lo tienen que tolerar.

## Análisis por alternativa

- **P1.** Pro: GRASP Experto, sin réplicas y sin acoplar los servicios entre sí. Contra: más
  llamadas desde el cliente.
- **P2.** Pro: una sola llamada. Contra: Logística pasa a conocer conceptos de Donaciones (necesidad,
  donante), una réplica que hay que mantener y que va contra el [ADR de referencias sin clave
  foránea](./logistica-service/20261007-referencias-a-donaciones-sin-clave-foranea.md).
- **P3.** Pro: una sola llamada para el cliente. Contra: Donaciones pasa a llamar a Logística por
  HTTP, un canal que hoy no existe y que la comunicación asimétrica evita.
- **R1.** Pro: el remito, que es el comprobante, queda guardado con la entrega, y la entidad carga
  un dato menos. Contra: los bultos, si alguien los necesitara, habría que sumarlos después como
  campo aditivo.
- **R2.** Pro: igual a Cozy. Contra: un dato que nadie usa después y que el usuario descartó.
- **R3.** Pro: no toca el contrato. Contra: pedir un dato que no se guarda.
- **I1.** Pro: la entrega cuenta como recibida y la foto se publica; el problema queda escrito con
  la entrega, como información (SRP: confirmar y reportar son dos datos, no dos estados). Contra:
  el reporte suma un campo opcional al evento y falta definir qué hace Donaciones con una cantidad
  menor.
- **I2.** Pro: no toca el contrato. Contra: una entrega que llegó con un faltante quedaría como "no
  llegó", la donación volvería al depósito sin destino y la foto no se publicaría.
- **I3.** Pro: el estado lo dice todo. Contra: un valor nuevo del enum rompe a los consumidores y
  obliga a sumar transiciones al ciclo de vida, que está `accepted`.

## Trabajo futuro

- [DTI-29](./DEUDA_TECNICA.md#dti-29): implementación en Logística, OpenAPI y schemas.
- Diseñar la pantalla "Llegó con un problema" (hoy, `sin-dibujar` en el prototipo de Raíces).
