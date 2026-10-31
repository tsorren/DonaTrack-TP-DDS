# Desnormalizaciones de lectura en entregas y rutas

- Status: proposed
- Date: 2026-10-25
- Deciders: Equipo de Logística; propuesta de Martín Zajdenberg aprobada por Camila Belén y Tadeo Sorrentino en Discord (20/10/2026)
- Tags: logística, persistencia, mongodb, desnormalización, lectura, entrega-5

## Contexto y Problema

La consigna de E5 pide contemplar desnormalizaciones cuando hagan falta para optimizar lecturas. Con el [modelo documental](20261025-modelo-documental-de-logistica-embebidos-y-referencias.md) la única que había era el destino embebido en la entrega, y hay dos lecturas que todavía necesitaban datos de otros documentos:

1. **Una entrega con su ruta y su camión.** El comprobante que pide la consigna de E3 lleva fecha, hora y camión responsable, y en E6 la entidad beneficiaria tiene que ver cuándo llega su entrega y en qué camión. Con la entrega guardando solo `idRuta`, eso son tres lecturas: la entrega, la ruta y el camión.
2. **Una ruta con sus paradas.** El mapa de seguimiento (la URL que se arma con el id de la ruta) muestra cada parada con su destino. Con la ruta guardando solo `entregaIds`, había que leer la ruta y después todas sus entregas.

En PostgreSQL esto se resolvía con un JOIN en cada lectura. MongoDB no tiene JOIN, así que esas uniones las tendría que hacer la aplicación, consulta por consulta, cada vez que alguien abre la pantalla.

## Atributos de Calidad y Drivers de Decisión

* **Rendimiento de lectura:** que las pantallas de seguimiento y el comprobante salgan de un solo documento.
* **Consistencia:** no copiar nada que obligue a mantener copias sincronizadas cuando el original cambia.
* **Fidelidad histórica:** el comprobante tiene que mostrar el camión con el que se hizo la entrega, aunque después cambie.
* **Mínimo impacto:** sin tocar el dominio ni los contratos con otros servicios.

## Alternativas Consideradas

* **No desnormalizar:** resolver las uniones en la aplicación en cada lectura.
* **Copiar en la entrega los datos de su asignación y en la ruta los de sus paradas**, en el momento en que se asigna la entrega.
* Copiar también datos que cambian: el estado de cada entrega en la ruta, contadores de avance, el nombre del chofer.
* Agregar `idDonante` a la entrega para listar "mis donaciones en camino".

## Resultado de la Decisión

Alternativa elegida: **copiar la asignación en la entrega y las paradas en la ruta**, solo con datos que no cambian después de copiarse.

`[OBSERVED]` El código actual ya implementa esta decisión.

La idea es pagar el JOIN una sola vez, al escribir. Cuando la planificación asigna una entrega a una ruta (corrida diaria de las 2:00 o planificación manual), el adaptador de Mongo lee la ruta y el camión y copia lo que hace falta dentro de la entrega. Desde ahí, cada lectura de esa entrega ya trae todo. En relacional el JOIN se repite en cada lectura; acá se hace una vez por entrega planificada, con dos lecturas extra que no disparan otras.

**Asignación embebida en la entrega.** `asignacion: { idRuta, fecha, idCamion, patente }`, en `null` mientras la entrega no tiene ruta. Reemplaza al `idRuta` suelto y el índice pasa a `asignacion.idRuta`.

* `EntregaMongoMapper` pone solo `idRuta` cuando la ruta de la entrega cambia. `EntregasRepositoryMongoAdapter.completarAntesDeGuardar` lee la ruta y el camión y completa `fecha`, `idCamion` y `patente` en el mismo guardado.
* Una vez completa no se vuelve a escribir. Si la entrega pasa a otra ruta, se arma una asignación nueva; si vuelve al depósito, queda en `null` y vuelve a aparecer entre las entregas sin ruta.
* Si más adelante cambia la patente del camión, la entrega conserva la del día en que viajó. Para un comprobante es el dato correcto: registra lo que pasó.

**Paradas embebidas en la ruta.** `entregaIds` pasa a `paradas: [{ orden, idEntrega, idDonacion, destino }]`, que es la tabla `Parada_Ruta` del DER dentro de la ruta.

* `RutaMongoMapper` numera las paradas en el orden del dominio y conserva las que ya estaban completas. `RutasRepositoryMongoAdapter.completarAntesDeGuardar` lee en una sola consulta las entregas de las paradas nuevas y copia `idDonacion` y `destino`. Los dos datos son inmutables.
* Al reconstruir la ruta, los ids de las entregas salen de las paradas ordenadas por `orden`, que reemplaza a la columna `orden_visita` del [ADR de orden de visita](20261007-orden-de-visita-de-las-paradas-de-una-ruta.md).
* Si una entrega vuelve al depósito, su parada se queda en la ruta: la ruta conserva lo que recorrió, que es lo que pedía ese ADR.
* Es el mismo criterio que usó E4 con `evento_entrega`, que repite `id_donacion` para publicarse sin consultar otra tabla.

Las dos copias las hace la capa de persistencia, sin cambiar `Entrega` ni `Ruta`, y se escriben dentro de la misma transacción que la planificación ([ADR de transacciones](20261025-transacciones-multi-documento-con-replica-set-en-logistica.md)).

**Descartadas:**

* **Estado de la entrega en la parada, contadores de avance o nombre del chofer en la ruta.** El estado y el avance cambian en cada transición de una entrega y obligarían a reescribir la ruta cada vez, con el riesgo de que la copia quede atrasada. El avance se calcula desde las entregas. El nombre del chofer no lo pide ninguna pantalla ni evento junto con la ruta.
* **`idDonante` en la entrega.** El evento que manda Donaciones no lo trae, y según el [ADR de granularidad de eventos](../20260702-granularidad-de-los-eventos-de-logistica-notificacion-por-donacion-en-vez-de-por-ruta-completa.md) el que sabe de quién es cada donación es Donaciones. Para "mis donaciones en camino", el front le pide a Donaciones las donaciones del donante y a Logística las entregas por `idDonacion`, que tiene índice único.
* **No desnormalizar.** Funciona, pero cada apertura del seguimiento o del comprobante repite tres lecturas, y la consigna pide justamente evitar eso cuando la lectura es la operación frecuente.

Referencia: Kleppmann, *Designing Data-Intensive Applications*, cap. 2, sobre la localidad de los datos en el modelo documental y cómo, sin soporte de joins, el trabajo de unir pasa a la aplicación.

## Consecuencias

### Positivas

* El comprobante y la pantalla de la entidad salen de un documento de `entregas`; el mapa de seguimiento, de un documento de `rutas`.
* Como lo copiado es inmutable o es un registro del día, no hay copias que actualizar después.
* El dominio y los services no cambiaron.

### Negativas

* Guardar una entrega asignada o una ruta nueva cuesta lecturas extra (ruta y camión por entrega; una consulta por las entregas de las paradas nuevas). Se paga una vez por planificación.
* Los datos copiados ocupan espacio repetido: el destino queda en la entrega y en la parada.
* Si alguna vez hace falta corregir un dato copiado (por ejemplo, una patente mal cargada antes de planificar), hay que actualizar también las entregas que ya la copiaron.
* `EntregasService` todavía lee la ruta y el camión para publicar `EventoEntregaExitosa`. Podría usar `asignacion`, pero implica tocar el service y se dejó para más adelante.

## Validación

`DesnormalizacionesMongoTest` (MongoDB real con Testcontainers):

* Al asignar una entrega, la asignación queda completa en el mismo documento, y conserva la patente del día aunque después cambie.
* Una entrega que cambia de ruta toma la asignación de la ruta nueva; una que vuelve al depósito queda sin asignación, vuelve a estar entre las sin ruta y su parada sigue en la ruta.
* Las paradas se numeran en el orden de la ruta, copian la donación y el destino de su entrega, y la ruta leída devuelve las entregas en ese orden.
* Guardar una asignación o una parada que apunta a una ruta, un camión o una entrega inexistente lanza `RecursoNoEncontradoException`.

## Links

* Depende de [Modelo documental de Logística](20261025-modelo-documental-de-logistica-embebidos-y-referencias.md).
* Complementa, para el perfil `mongo`, a [Orden de visita de las paradas de una ruta](20261007-orden-de-visita-de-las-paradas-de-una-ruta.md).
