# Réplicas por evento: fotos de recepción en Donaciones y donante en la entrega

- Status: proposed
- Date: 2026-11-06
- Deciders: tsorren (2026-11-06), con el plan de trabajo sin autenticación: réplica de las fotos para HB-01 y `donanteId` en el comando y en la entrega para HB-16; la forma de la referencia a la foto, propuesta por el agente y aprobada por tsorren el mismo día
- Tags: logistica, donaciones, amqp, contratos, replicas, consistencia-eventual, entregas, entrega-6

> **Origen:** HB-01 y HB-16 de [`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md) §8 y
> tareas 5 y 7 del [plan E6 sin autenticación](../requerimientos/plan-e6-trabajo-sin-auth.md). La
> implementación queda en [DTI-36](./DEUDA_TECNICA.md#dti-36). En esta tarea solo cambian los schemas
> de los dos mensajes y el [catálogo de eventos](../arquitectura/eventos-amqp.md) (contrato primero).

## Contexto y Problema

Dos pantallas de E6 necesitan, en una sola lectura, un dato que vive en otro servicio:

- **Destacadas de la landing (RF-01, HB-01).** `GET /donaciones/destacadas` lista las donaciones
  entregadas en los últimos 30 días con foto (D28). La categoría y el estado son de Donaciones; la
  foto de recepción es de Logística.
- **Entregas activas de un donante (RF-09, HB-16).** El mapa del donante muestra todas sus entregas
  en curso. Las entregas son de Logística; quién donó es de Donaciones.

Qué hay hoy:

- `[OBSERVED]` `EventoEntregaExitosa` (Logística → Donaciones, `entrega.exitosa`) lleva `entregaId`,
  `donacionIndependienteId`, `camionId`, `patenteCamion` y `fechaEntrega`; no lleva fotos. Donaciones
  solo pasa la donación a `ENTREGADA` (`ProcesadorEventosLogistica.procesarEntregaExitosa`).
- `[OBSERVED]` `Entrega` guarda `fotosRecepcion` (`List<String>`), y confirmar la entrega exige al
  menos una (`Entrega.confirmarEntrega`).
- `[OBSERVED]` `EventoEntregaSolicitadaV1` (Donaciones → Logística, `entrega.solicitada.<proveedorId>.v1`)
  lleva `personaBeneficiariaId`, pero no el donante. `Entrega` y `EntregaDocument` tienen
  `idBeneficiaria` (indexado), pero no `donanteId`. En Donaciones, el donante está en `Donacion.donanteId`,
  y la donación independiente llega a él por `donacionOriginalId`.
- `[OBSERVED]` Los dos records del comando tienen `@JsonIgnoreProperties(ignoreUnknown = true)`. El
  `EventoEntregaExitosa` de Donaciones no lo tiene, pero su conversor usa `tools.jackson`
  `JsonMapper.builder().build()` (Jackson 3, `RabbitMQConfig.messageConverter`), que no falla ante
  campos desconocidos por defecto `[INFERRED]`. Los dos schemas tienen `"additionalProperties": true`.
- `[OBSERVED]` El seguimiento es por donación (`GET /api/entregas/seguimiento/donaciones/{idDonacion}`)
  o por beneficiaria; no hay listado por donante.

Problema: ¿cómo se sirven estas dos lecturas sin que un servicio llame a otro en el momento de leer,
y qué se replica exactamente?

## Atributos de Calidad y Drivers de Decisión

Pesos `[INFERRED]`: lectura en una sola llamada, sin cruzar servicios (latencia y disponibilidad) 25 ·
cohesión de cada servicio con su dato (GRASP Experto) 20 · cambio aditivo (AGENTS.md §8.2) 20 ·
costo antes del 13/11 15 · consistencia y vigencia del dato 10 · privacidad y autorización por
pertenencia 10.

## Relaciones Arquitectónicas

1. **Respeta:** [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): contrato primero, campos opcionales, schemas abiertos y *Tolerant Reader*.
2. **Respeta:** [topología pub/sub AMQP](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md):
   las réplicas viajan por los mensajes que ya existen; no se suma un canal.
3. **Afina:** [contrato de entregas para la entidad](./20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md)
   (`proposed`), que dice que "Logística no guarda datos de Donaciones" y descarta replicar el pedido
   y el donante (P2). `donanteId` es un identificador opaco, igual que `idDonacion` e
   `idBeneficiaria`: Logística no guarda nombre, tipo ni datos personales del donante. Los dos campos
   opcionales de `entrega.exitosa` (`reporte`, de ese ADR, y `fotosRecepcion`, de este) conviven.
4. **Respeta:** [referencias a donaciones sin clave foránea](./logistica-service/20261007-referencias-a-donaciones-sin-clave-foranea.md):
   `donanteId` es otra referencia sin integridad referencial.
5. **Depende de:** ADR-E6-08, fotos de recepción en almacenamiento de objetos (D27 de
   `requerimientos-e6.md`; `proposed` en el PR #932, sin integrar todavía), que define la URL estable de cada foto.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`). Columnas: lectura · cohesión ·
aditivo · costo · consistencia · privacidad.

### Destacadas (HB-01)

| Alternativa | Lect. | Cohes. | Adit. | Costo | Consist. | Priv. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **R1. Donaciones replica las fotos que trae `entrega.exitosa`** | 5 | 3 | 5 | 4 | 3 | 4 | **4,15** |
| R2. El cliente compone: Logística da las entregas con foto y Donaciones, la categoría de cada una | 2 | 5 | 5 | 4 | 5 | 3 | 3,9 |
| R4. Donaciones pide las fotos a Logística por HTTP al leer | 2 | 4 | 5 | 3 | 5 | 4 | 3,65 |
| R3. Logística replica la categoría desde Donaciones y sirve las destacadas | 5 | 2 | 4 | 2 | 3 | 4 | 3,45 |

### Qué se replica de cada foto

| Alternativa | Lect. | Cohes. | Adit. | Costo | Consist. | Priv. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **U1. Una URL estable de Logística que redirige a una URL firmada de vida corta** | 4 | 5 | 5 | 4 | 5 | 4 | **4,5** |
| U4. Copia del archivo al almacenamiento de Donaciones | 5 | 3 | 5 | 1 | 4 | 3 | 3,7 |
| U3. La clave del objeto, con credenciales del bucket de Logística en Donaciones | 5 | 1 | 5 | 3 | 5 | 2 | 3,6 |
| U2. La URL firmada | — | — | — | — | — | — | Descartada: vence en minutos y la portada quedaría sin foto |

### Entregas activas de un donante (HB-16)

| Alternativa | Lect. | Cohes. | Adit. | Costo | Consist. | Priv. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **H1. `donanteId` en el comando y en `Entrega`; Logística filtra por donante** | 5 | 3 | 5 | 4 | 3 | 4 | **4,15** |
| H4. El cliente pide a Donaciones los ids de las donaciones en curso y a Logística un lote por ids | 3 | 5 | 5 | 4 | 5 | 2 | 4,05 |
| H2. El cliente compone: una llamada de seguimiento por donación (lo que hay hoy) | 1 | 5 | 5 | 5 | 5 | 2 | 3,7 |
| H3. Logística le pide a Donaciones las donaciones del donante por HTTP al leer | 2 | 4 | 5 | 2 | 5 | 4 | 3,5 |

## Resultado de la Decisión

Se eligen **R1**, **U1** y **H1**.

1. **Fotos en `entrega.exitosa`.**
   - Campo opcional `fotosRecepcion`: lista de URLs estables (`format: uri`), una por foto, en el
     orden de la entrega.
   - La URL estable de una foto subida a MinIO es `GET /api/entregas/{id}/fotos/{fotoId}` de
     Logística, armada sobre una URL base configurable, como hace `GeneradorDeURLSeguimiento` con la
     del seguimiento de rutas (hoy no existe una URL de fotos). Responde `302`
     hacia una URL firmada de vida corta (ADR-E6-08). Una foto cargada por URL con el
     `PATCH /api/entregas/{id}/fotos` actual viaja con su URL tal cual.
   - Donaciones guarda la lista en la donación independiente al aplicar `ENTREGADA` y sirve
     `GET /donaciones/destacadas` sin llamar a Logística. El navegador pide la imagen a la URL
     estable, a través del gateway.
2. **`donanteId` en el comando y en la entrega.**
   - Campo opcional `donanteId` (`uuid`) en `EventoEntregaSolicitadaV1`. El broker lo saca de
     `Donacion.donanteId`, por el `donacionOriginalId` de la donación independiente.
   - Logística lo guarda en `Entrega` y en `EntregaDocument`, con índice, y lo devuelve en el
     listado de entregas activas de un donante (HB-16).
   - Para que el dato no dependa del canal, el `POST /api/entregas` de los proveedores HTTP también
     suma `donanteId` opcional en `CrearEntregaRequestDTO`. Ese cambio de OpenAPI va con la
     implementación (DTI-36).
3. **Contrato primero.** Este PR suma los dos campos opcionales a
   `evento-entrega-exitosa.schema.json` y a `evento-entrega-solicitada-v1.schema.json`, y los
   ejemplos del catálogo. Ninguno pasa a `required`.
4. **Datos viejos.** No se reconstruyen. Las entregas creadas antes del cambio no tienen
   `donanteId` y no salen en el listado por donante. Las donaciones entregadas antes no tienen fotos
   replicadas y no salen en destacadas.

Justificación: cada lectura queda en el servicio que ya es Experto en el dato que filtra (Donaciones
en categoría y estado; Logística en la entrega), con una copia mínima del otro. Lo que viaja son
identificadores y URLs, nunca datos personales. Es lo que el *Tolerant Reader* de la política de
contratos permite sin coordinar despliegues. H1 le gana a H4 porque deja que Logística compruebe la
pertenencia de una entrega sin preguntar a Donaciones cuando llegue la autorización (HB-13); con H4,
Logística recibe ids que no puede verificar.

## Consecuencias

### Positivas

- `GET /donaciones/destacadas` y el listado de entregas de un donante responden en una llamada,
  aunque el otro servicio esté caído.
- Los dos cambios son aditivos: un emisor viejo sigue andando y un consumidor viejo ignora el campo.
- El bucket de fotos sigue privado y con un solo dueño; Donaciones no tiene credenciales de
  almacenamiento de Logística.

### Negativas

- **Consistencia eventual:** una donación aparece en destacadas recién cuando Donaciones procesa
  `entrega.exitosa`. Las fotos que se suman a una entrega ya confirmada (`PATCH .../fotos`) no se
  replican: Donaciones se queda con las de la confirmación.
- **Datos viejos sin el campo:** las entregas y donaciones anteriores al cambio no aparecen en
  esas dos lecturas. Para la demo se regeneran los datos; en un entorno real haría falta un
  backfill.
- **Proveedores externos:** un proveedor que no mande `fotosRecepcion` en `entrega.exitosa` no
  aporta destacadas. Si Donaciones no manda `donanteId` (un broker viejo), la entrega no sale en el
  listado por donante.
- **Réplica de identificador:** si una donación cambiara de donante, Logística quedaría con el
  viejo. Hoy no pasa `[OBSERVED]`: `Donacion.donanteId` es obligatorio desde el alta y lo asignan los dos
  constructores de `Donacion`; tiene un setter de paquete (`@Setter(AccessLevel.PACKAGE)`) sin
  llamadas en el código.
- La URL estable de una foto es pública mientras la foto esté publicada; quien la tenga puede
  verla. Se acepta porque RF-01 y RF-02 publican esas fotos.
- `EventoEntregaExitosa` de Donaciones no declara `@JsonIgnoreProperties(ignoreUnknown = true)` y
  depende del valor por defecto de Jackson 3. La implementación suma la anotación para no depender
  de esa configuración.

## Análisis por alternativa

- **R1.** Pro: una lectura local en Donaciones, que ya filtra por estado y categoría. Contra:
  Donaciones guarda un dato de Logística y queda atrasado hasta procesar el evento.
- **R2.** Pro: sin réplica. Contra: 1 + N llamadas desde el cliente en la portada, la pantalla más
  visitada, y Logística expone un listado público con fotos.
- **R3.** Pro: Logística ya tiene las fotos. Contra: Logística pasa a conocer categorías de
  Donaciones, justo lo que el ADR del contrato de entregas evita.
- **R4.** Pro: dato siempre fresco. Contra: una llamada HTTP de Donaciones a Logística, un canal que
  hoy no existe, y la portada cae si Logística no responde.
- **U1.** Pro: no vence, el bucket sigue privado y el dueño sigue siendo Logística. Contra: la
  imagen se pide a Logística (vía gateway), y una URL estable expone la foto a quien la tenga.
- **U2.** Pro: trivial. Contra: deja de funcionar a los pocos minutos.
- **U3.** Pro: Donaciones firma sus propias URLs. Contra: comparte credenciales de almacenamiento
  entre servicios y acopla Donaciones al bucket de Logística.
- **U4.** Pro: Donaciones no depende de Logística ni para la imagen. Contra: duplica cada archivo,
  y la copia necesita su propio EXIF, tope y limpieza.
- **H1.** Pro: una llamada, y Logística puede autorizar por pertenencia. Contra: Logística guarda un
  identificador de Donaciones y las entregas viejas no lo tienen.
- **H2.** Pro: ya existe. Contra: una llamada por donación y Logística no puede saber si las
  donaciones pedidas son del donante.
- **H3.** Pro: sin réplica. Contra: un canal HTTP nuevo entre servicios en cada lectura.
- **H4.** Pro: sin réplica y con dos llamadas. Contra: Logística recibe ids que no puede verificar,
  y el cliente arma la lista.

## Validación

- `node scripts/agent-check.js` y la validación de contratos siguen en verde con los schemas abiertos.
- En la implementación: un test del consumidor de Donaciones con y sin `fotosRecepcion` (un mensaje
  viejo sigue pasando a `ENTREGADA`), y un test del listener de Logística con y sin `donanteId`.
- En el E2E: los mensajes capturados validan contra los schemas nuevos (RM-08).

## Trabajo futuro

- [DTI-36](./DEUDA_TECNICA.md#dti-36): implementación en Logística, Donaciones y OpenAPI.
- HB-13: el listado por donante filtra por la persona del token, no por un parámetro.
