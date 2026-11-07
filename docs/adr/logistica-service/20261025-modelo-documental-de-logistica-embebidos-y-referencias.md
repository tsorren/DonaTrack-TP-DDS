# Modelo documental de Logística: qué se embebe y qué se referencia

- Status: proposed
- Date: 2026-10-25
- Deciders: Equipo de Logística (Camila Belén, Martín Zajdenberg); campos y nombres revisados con Tadeo Sorrentino y Camila Aylén en Discord (20/10/2026)
- Tags: logística, persistencia, mongodb, modelo-documental, ddd, entrega-5

## Contexto y Problema

En PostgreSQL, Logística usaba 16 tablas: una por agregado, cuatro para la geografía normalizada (`pais`, `provincia`, `localidad`, `direccion`), cuatro historiales de estado, la tabla de paradas, la tabla intermedia de solicitudes y dos de auditoría. Al pasar a MongoDB ([ADR de persistencia documental](20261025-persistencia-documental-de-logistica-en-mongodb.md)) hay que decidir cómo se agrupan esos datos en documentos: qué va dentro de otro documento y qué queda en su propia colección, referenciado por id.

En un modelo documental esa decisión define qué se puede leer y escribir de una sola vez. Mongo garantiza atomicidad dentro de un documento y no tiene JOIN ni claves foráneas, así que lo que se separa en colecciones distintas se lee con más de una consulta y su coherencia queda a cargo de la aplicación.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia del agregado:** lo que el dominio modifica junto tiene que escribirse junto.
* **Rendimiento de lectura:** las consultas habituales (una entrega, una ruta, el estado de un camión) tienen que salir de un documento.
* **Inmutabilidad del historial:** el destino de una entrega y los cambios de estado no se reescriben (acuerdo del 06/10 en la reunión de Logística).
* **Ciclo de vida independiente:** lo que existe por su cuenta no puede quedar dentro de otro documento.
* **Crecimiento acotado:** Mongo limita cada documento a 16 MB y cada lectura trae el documento entero.

## Alternativas Consideradas

* **Un documento por agregado**, con sus value objects y su historial embebidos, y referencias por id entre agregados.
* Copiar el DER: una colección por tabla y referencias por id para todo (incluidos país, provincia, localidad y los historiales).
* Embeber más: las entregas dentro de la ruta y las auditorías dentro de la entrega.

## Resultado de la Decisión

Alternativa elegida: **un documento por agregado**.

`[OBSERVED]` El código actual ya implementa esta decisión.

En DDD el agregado es la unidad de consistencia y en Mongo el documento es la unidad de escritura atómica, así que hacerlos coincidir permite guardar cada agregado de una sola vez. Copiar el DER hubiera traído a Mongo los joins de Postgres, ahora resueltos con varias consultas desde la aplicación. Embeber más no es posible sin romper el dominio, como se explica abajo.

Quedan 5 colecciones de agregados (`camiones`, `choferes`, `rutas`, `entregas`, `solicitudes_planificacion`) y 2 de auditoría (`solicitudes_transicion_entrega`, `eventos_entrega`).

**Se embebe:**

* **`destino` en la entrega**, con calle, altura, piso, departamento, código postal, localidad, provincia, país y `ubicacion`. Localidad, provincia y país quedan como texto. Se pierde el catálogo geográfico que eligió el [ADR de direcciones normalizadas](20261007-direcciones-normalizadas-e-inmutables-en-logistica.md) para E4, y con él el "buscar o crear" de localidades en cada guardado. A cambio, la entrega se lee completa sin joins. La inmutabilidad se mantiene: `EntregaMongoMapper` no reemplaza un `destino` ya guardado; solo actualiza `ubicacion` cuando llegan las coordenadas. Agrupar por zona, que era el motivo del catálogo, ahora se puede hacer con el índice geoespacial sobre `destino.ubicacion`.
* **`historialEstado` en camiones, choferes, rutas y entregas**, como un array de `{estadoAnterior, estadoNuevo, timestamp, actor}`. Se escribe con el agregado y es append-only: `HistorialEstadoMongo.conCambiosNuevos` conserva lo guardado y agrega solo los cambios nuevos.
* **`fotosRecepcion` en la entrega**, como un array de `{clave, subidaEn}` con la clave de cada objeto en MinIO. En Postgres, varias fotos necesitaban una tabla aparte.

**Se referencia por id:**

* **Entrega y ruta.** La entrega no se embebe en la ruta porque existe antes (llega por RabbitMQ y espera a ser planificada), puede volver al depósito y entrar en otra ruta, y tiene sus propias transiciones. La ruta referencia sus entregas desde `paradas` y la entrega a su ruta desde `asignacion.idRuta`; las dos copias de datos que llevan esas referencias se justifican en el [ADR de desnormalizaciones](20261025-desnormalizaciones-de-lectura-en-entregas-y-rutas.md).
* **Ruta, camión y chofer** por `idCamion` e `idChofer`, igual que en E4 ([ADR de relación entre ruta y camión](20260630-relacion-entre-ruta-y-camion.md)).
* **`rutaId` de camión y chofer no se guarda.** El adaptador lo deriva buscando la ruta `EN_TRASLADO` del camión o del chofer, con los índices compuestos `{idCamion, estado}` e `{idChofer, estado}`. Guardarlo en el camión ahorraba esa consulta, pero agregaba otra escritura en otro documento que tendría que coincidir con la ruta.
* **Auditorías en colecciones propias.** Las solicitudes de transición y los eventos de entrega se podrían embeber en la entrega, porque son pocos por entrega. Se dejan aparte porque solo se insertan, se consultan por `idEntrega` y en E4 se pensaron como base de un futuro outbox. Siguen en una sola colección por tipo, como decidió el [ADR de solicitudes de transición](20261007-solicitudes-de-transicion-de-entrega-en-una-sola-tabla.md).

**Sin claves foráneas.** Mongo no las tiene, así que el criterio que el [ADR de referencias a Donaciones](20261007-referencias-a-donaciones-sin-clave-foranea.md) aplicaba solo a `idDonacion` pasa a valer para todas las referencias. La coherencia queda a cargo del dominio, los services y los adaptadores: por ejemplo, guardar una entrega con una ruta inexistente lanza `RecursoNoEncontradoException`.

## Consecuencias

### Positivas

* Cada agregado se guarda y se lee con una operación, y su escritura es atómica sin transacción.
* El historial y el destino quedan fijos junto al agregado al que pertenecen.
* Varias fotos por entrega salen sin cambios de esquema.

### Negativas

* Sin claves foráneas, una referencia rota solo se detecta en la aplicación.
* Sin catálogo geográfico, la misma localidad puede quedar escrita de distintas formas ("CABA", "Capital Federal").
* El historial de camiones y choferes crece durante toda su vida (al menos dos cambios por cada día de ruta) y cada lectura lo trae entero. Para el volumen del TP no se acerca a los 16 MB, así que queda embebido. Si creciera, la salida es guardar en el documento solo los últimos cambios y mover el resto a otra colección.
* Para derivar `rutaId`, leer un camión en ruta cuesta una consulta extra a `rutas`.

## Validación

* `RepositoriosLogisticaMongoTest` (MongoDB real con Testcontainers): los 5 agregados se guardan y se leen completos, la entrega con su destino; el historial del camión y de la entrega crece sin perder cambios anteriores; un camión y un chofer en una ruta en traslado se leen con el id de esa ruta; cada transición y cada evento agregan un documento de auditoría.
* `EntregaUbicacionYFotosMongoTest`: una entrega con tres fotos se guarda y se lee completa, y agregar una foto después conserva la hora de las anteriores.

## Links

* Depende de [Persistencia documental de Logística en MongoDB](20261025-persistencia-documental-de-logistica-en-mongodb.md).
* Complementa, para el perfil `mongo`, a [Direcciones normalizadas e inmutables](20261007-direcciones-normalizadas-e-inmutables-en-logistica.md), que sigue valiendo para el perfil `postgres`.
