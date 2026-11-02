# Ciclo de vida de la necesidad en el contrato: estado, versión, fecha límite, períodos y unidad

- Status: proposed
- Date: 2026-11-02
- Deciders: tsorren, con las respuestas del grupo (Nico, Anush y Camila) a las preguntas abiertas de Raíces
- Tags: donaciones, necesidades, contratos, openapi, periodos, bloqueo-optimista, entrega-6

> **Origen:** preguntas 6 a 12 del artifact de preguntas abiertas de Raíces (2026-11-01), decididas por
> el usuario el 2026-11-02. La ficha que las planteó es
> [Necesidades de la entidad](../../interfaz/flujos/raices-necesidades.md), RF-10. Deuda asociada:
> [DTI-30](../DEUDA_TECNICA.md#dti-30). No se toca código en esta tarea.

## Contexto y Problema

La pantalla "Necesidades" de la entidad muestra los pedidos vigentes y aparte los cumplidos, los
terminados y los dados de baja. Para cada uno muestra la unidad ("40 kg"), la fecha límite y, si se
repite, la historia de sus períodos con lo que llegó en cada uno y las entregas que lo cubren. Deja
cambiar la cantidad, con aviso si otra persona la cambió antes.

Qué hay hoy en el contrato:

- `[OBSERVED]` `NecesidadDTO` (`donaciones-service/.../dto/NecesidadDTO.java:9-20`) es el mismo record
  para entrada y salida: `id, tipo, idEntidad, idSubcategoria, cantidadNecesitada, descripcion,
  estaSatisfecha, fechaInicio, fechaFin`. No tiene estado, versión, unidad ni fecha límite.
- `[OBSERVED]` El dominio sí tiene `version` (`Necesidad.java:21-22`, `@Version` en
  `NecesidadEntity`) y `activa`. Ninguno de los dos llega al DTO.
- `[OBSERVED]` `GET /api/necesidades` filtra solo por `entidadId` y `tipo`
  (`NecesidadesService.java:59-67`) y devuelve también las dadas de baja. La OpenAPI dice "Listar
  necesidades activas" (`openapi-donaciones.yaml:183`).
- `[OBSERVED]` Una necesidad extraordinaria no tiene fecha de fin: queda abierta hasta que se cumple
  o se da de baja. Una recurrente toma su período de `Period.between(fechaInicio, fechaFin)`
  (`NecesidadesService.java:102`), así que acepta cualquier largo.
- `[OBSERVED]` La historia de períodos existe en la base (`periodo_necesidad`, cargada por
  `NecesidadRecurrenteEntity`), pero ningún endpoint la devuelve. "Satisfecha" cuenta lo asignado,
  no lo recibido (`AcumuladosDeAsignacion`).
- `[OBSERVED]` Cada período guarda su objetivo al crearse (`NecesidadRecurrente.java:41,129`). Por
  eso cambiar la cantidad hoy recién vale desde el período siguiente.
- `[OBSERVED]` La unidad vive en la categoría (`Categoria.tipoUnidad`, enum `Unidad`: unidades,
  kilogramo, litros y metros), no en la subcategoría (`Subcategoria.java`, sin unidad). Una
  categoría tiene una sola unidad: si "Alimentos" está en kilos, el aceite no se puede pedir en
  litros.
- `[OBSERVED]` Las donaciones guardan su `necesidadId` y su `periodoId` (`DonacionIndependiente.java:35-37`).

Problema: ¿qué expone el contrato de la necesidad para que la entidad vea y cuide sus pedidos sin
datos inventados en la pantalla?

## Atributos de Calidad y Drivers de Decisión

Fidelidad a lo que la entidad necesita ver (RF-10, RF-22) 30 · evolución compatible del contrato
(AGENTS.md §8.2) 25 · coherencia con el modelo de períodos aceptado 20 · costo 15 · protección
frente a ediciones simultáneas 10.

## Relaciones Arquitectónicas

1. **Refina:** [gestión de necesidades y períodos](./20260609-gestion-de-necesidades-y-periodos.md)
   (`accepted`). Las subclases, los períodos independientes y el planificador diario no cambian. Se
   cierran los largos de período a tres y se expone la historia.
2. **Aplica:** [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`). Las dos restricciones nuevas (fecha límite obligatoria y largos cerrados) son
   incompatibles y se registran en `docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt`.
3. **Se relaciona con:** [baja de una necesidad con entregas en curso](../20261102-baja-de-una-necesidad-con-entregas-en-curso.md)
   (`proposed`), que define qué pasa al dar de baja.

## Alternativas Consideradas

Puntajes de 1 a 5, estimados por el agente (`[INFERRED]`). Columnas: Fidelidad · Compatible ·
Coherente · Costo · Simultaneidad.

### Fecha límite de una necesidad de una sola vez (pregunta 7)

| Alternativa | Fid. | Comp. | Coh. | Costo | Sim. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **F1. Campo `fechaLimite`, obligatorio en las extraordinarias** | 5 | 2 | 4 | 4 | — | **3,9** |
| F2. Campo `fechaLimite` opcional | 4 | 5 | 4 | 4 | — | 4,3 |
| F3. Sin fecha: la pantalla deja de pedirla (voto del grupo: B, B, B) | 2 | 5 | 5 | 5 | — | 3,9 |

La matriz favorece F2. El usuario eligió **F1** contra el voto del grupo y contra la matriz: una
necesidad sin fecha no le dice al donante cuándo deja de servir, y el campo opcional deja pedidos
abiertos para siempre. El costo es el cambio incompatible.

### Historia de períodos y entregas por necesidad (pregunta 8)

| Alternativa | Fid. | Comp. | Coh. | Costo | Sim. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **H1. Otra llamada: `GET /api/necesidades/{id}/periodos` y `GET /donaciones-independientes?necesidadId=`** | 5 | 5 | 5 | 4 | — | **4,8** |
| H2. Todo dentro de `NecesidadDTO` (lista de períodos y de entregas) | 5 | 4 | 4 | 3 | — | 4,1 |

### Estado y filtro (pregunta 9)

| Alternativa | Fid. | Comp. | Coh. | Costo | Sim. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **E1. Campo `estado` calculado y filtro `?estado=`; sin filtro, devuelve todas, como hoy** | 5 | 5 | 4 | 4 | — | **4,6** |
| E2. Igual, pero sin filtro devuelve solo las vigentes, como dice la OpenAPI | 5 | 2 | 4 | 4 | — | 3,8 |
| E3. Solo el booleano `activa` | 3 | 5 | 4 | 5 | — | 4,1 |

### Unidad de la cantidad (pregunta 6)

| Alternativa | Fid. | Comp. | Coh. | Costo | Sim. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **U1. Campo `unidad` en la subcategoría, que hereda la de su categoría si no tiene una propia** | 5 | 5 | 5 | 3 | — | **4,6** |
| U2. Solo la unidad de la categoría, leída por la subcategoría | 3 | 5 | 4 | 5 | — | 4,1 |
| U3. Campo `unidad` en cada necesidad | 4 | 5 | 2 | 4 | — | 3,8 |

### Largos de período (pregunta 11)

| Alternativa | Fid. | Comp. | Coh. | Costo | Sim. | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **L1. Enum `periodicidad`: `SEMANAL`, `QUINCENAL`, `MENSUAL`** | 5 | 2 | 5 | 4 | — | **4,1** |
| L2. Cualquier largo (lo de hoy), la pantalla ofrece tres | 3 | 5 | 3 | 5 | — | 3,9 |

## Resultado de la Decisión

Se eligen **F1, H1, E1, L1 y U1**, más las dos decisiones sin alternativas fuertes de abajo, decididas
por el usuario el 2026-11-02.

1. **Estado (9).** `NecesidadDTO` de salida suma `estado`, calculado y de solo lectura:

   | Estado | Cuándo |
   |---|---|
   | `VIGENTE` | Está activa y, si es de una sola vez, todavía no se cumplió ni venció |
   | `CUMPLIDA` | De una sola vez: lo recibido llegó a la cantidad |
   | `TERMINADA` | De una sola vez: pasó la fecha límite sin cumplirse |
   | `DADA_DE_BAJA` | La entidad la dio de baja (`activa = false`) |

   Una recurrente está `VIGENTE` o `DADA_DE_BAJA`. Cada período tiene su propio resultado (H1).
   `GET /api/necesidades` acepta `estado` (uno o varios, separados por coma). Sin el parámetro
   devuelve todas, como el código hoy, y se corrige la descripción de la OpenAPI.
2. **Versión (10).** La salida suma `version`. `PUT /api/necesidades/{id}` la acepta. Si llega y
   no coincide con la guardada, responde `409` con el error de concurrencia existente. Mientras
   dure la migración es opcional.
3. **Fecha límite (7).** Campo `fechaLimite` (fecha), obligatorio cuando `tipo = EXTRAORDINARIA` y
   posterior a hoy. Las recurrentes no lo llevan: su período sale de `periodicidad`. Pasada la
   fecha sin cumplirse, la necesidad queda `TERMINADA`.
4. **Largos de período (11).** Campo `periodicidad` (`SEMANAL` = 7 días, `QUINCENAL` = 15 días,
   `MENSUAL` = un mes), obligatorio cuando `tipo = RECURRENTE`. El primer período empieza el día de
   la publicación, como hoy. `fechaFin` deja de usarse como entrada: queda deprecado y se sigue
   devolviendo como fin del período en curso.
5. **Cambio de cantidad (12).** `PUT` con otra `cantidadNecesitada`, en una recurrente, cambia
   también el objetivo del período en curso. Si lo recibido en ese período ya alcanza el nuevo
   objetivo, el período queda cumplido. Los períodos cerrados no cambian.
6. **Historia (8).** `GET /api/necesidades/{id}/periodos` devuelve, por período: inicio, fin,
   objetivo, asignado, recibido y resultado (`EN_CURSO`, `CUMPLIDO`, `INCOMPLETO`).
   `GET /donaciones-independientes` acepta `necesidadId` para listar las donaciones que la cubren,
   con su estado (en camino, entregada). Lo recibido cuenta las donaciones `ENTREGADA`; lo
   asignado, las que tienen destino, como hoy.
7. **Unidad (6).** `Subcategoria` suma `unidad` (el mismo enum `Unidad`), opcional: si falta, vale
   la de su categoría. `SubcategoriaOutputDTO` y la salida de `NecesidadDTO` la devuelven, de solo
   lectura. La necesidad no guarda unidad: la cantidad se pide en la de su subcategoría.

## Consecuencias

### Positivas

- La pantalla deja de inventar datos: estado, unidad, fecha límite e historia salen del contrato.
- La versión evita pisar el cambio de otra persona aunque haya abierto el formulario minutos antes.
- Los períodos de tres largos se leen fácil y el planificador diario sigue igual.

### Negativas

- Dos cambios incompatibles: el alta de una extraordinaria sin `fechaLimite`, y una recurrente con
  un largo que no es de los tres, pasan a responder `400`. Hay que registrarlos y avisar a los
  clientes (`integration-tests`, la colección de Postman, el cliente liviano).
- `estado` se calcula en cada lectura: para `TERMINADA` y `CUMPLIDA` hace falta lo recibido, que
  sale de las donaciones. Es la misma consulta agregada que ya usa `AcumuladosDeAsignacion`.
- Cambiar el objetivo del período en curso altera la historia de un período abierto. Se acepta: el
  período cerrado no cambia.

## Análisis por alternativa

- **F1.** Pro: el donante sabe hasta cuándo sirve. Contra: rompe el alta actual. **F2.** Pro:
  compatible. Contra: deja pedidos sin fin. **F3.** Pro: el modelo no cambia. Contra: la entidad
  no puede decir "lo necesito antes del viernes".
- **H1.** Pro: la lista sigue liviana y cada llamada es de un experto (GRASP). Contra: una llamada
  más al abrir el detalle. **H2.** Pro: una sola llamada. Contra: el listado carga historias que
  casi nadie abre.
- **E1.** Pro: no cambia lo que devuelve hoy. Contra: la OpenAPI se corrige en vez de cumplirse.
  **E2.** Pro: cumple la OpenAPI. Contra: cambia el comportamiento para los clientes actuales.
  **E3.** Pro: lo mínimo. Contra: no distingue cumplida de terminada.
- **L1.** Pro: los mismos tres largos en la pantalla y en el contrato. Contra: incompatible con
  quien mande otros largos. **L2.** Pro: compatible. Contra: el contrato acepta períodos que la
  pantalla no puede mostrar.
- **Quincena de 15 días o del calendario (1 al 15 y 16 a fin de mes).** Se eligen 15 días: el
  período empieza el día de la publicación, como fija el ADR de períodos. La quincena del
  calendario obligaría a que cada período empiece en una fecha fija.
- **U1.** Pro: cada cosa se pide en su unidad (aceite en litros, fideos en kilos) sin partir las
  categorías, y sin migración, porque hereda la de la categoría. Contra: un campo nuevo en el
  catálogo. **U2.** Pro: sin cambios en el modelo. Contra: obliga a partir las categorías por
  unidad. **U3.** Pro: lo más flexible. Contra: dos pedidos de lo mismo pueden quedar en unidades
  distintas y el algoritmo de asignación ya no puede sumarlos.

## Trabajo futuro

- [DTI-30](../DEUDA_TECNICA.md#dti-30): implementación en Donaciones, OpenAPI y registro de los
  cambios incompatibles.
