# Orden de visita de las paradas de una ruta

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística
- Tags: logística, persistencia, rutas, orm

## Contexto y Problema

El planificador externo devuelve, por cada camión, "una lista ordenada de destinos" (consigna, Entrega 3), y el chofer recorre las paradas en ese orden. En la primera versión de la base, la ruta guardaba qué entregas tenía (`entrega.id_ruta`) pero no en qué posición iba cada una. Al releer una ruta, las paradas se ordenaban por el UUID de la entrega, que es un valor al azar, y Java y PostgreSQL ni siquiera ordenan los UUID de la misma forma. Además, cuando una entrega vuelve al depósito pierde su `id_ruta` y desaparecía de la ruta que ya había recorrido.

## Alternativas Consideradas

* **Columna `orden_visita` en `entrega`:** una sola columna, pero el orden pasa a ser un dato de la entrega cuando en el dominio pertenece a la ruta (`Ruta.entregas`). El repositorio de rutas tendría que escribir filas de otro agregado, y se sigue perdiendo la parada de una entrega devuelta.
* **Tabla `parada_ruta (id_ruta, orden_visita, id_entrega)`:** la ruta guarda su propia lista ordenada, separada de la asignación actual de cada entrega.

Las dos alternativas son las que proponía la revisión de diseño de logística del 30/08 (`docs/arquitectura/diseno/logistica/lucid/analisis.md`).

## Resultado de la Decisión

Alternativa elegida: **tabla `parada_ruta`**, con clave primaria `(id_ruta, orden_visita)`.

`[OBSERVED]` El código actual ya implementa esta decisión.

`RutaEntity` la mapea como colección ordenada (`@ElementCollection` + `@OrderColumn(name = "orden_visita")`), el mismo recurso que usa Incentivos para sus listas. Quedan dos datos con funciones distintas: `entrega.id_ruta` indica en qué ruta está hoy la entrega (nulo si está pendiente) y `parada_ruta` conserva cómo se armó cada ruta.

La tabla se agregó con una migración Flyway nueva. Cada cambio de esquema es un script versionado que se aplica al arrancar el servicio, y uno ya aplicado no se modifica. La misma migración cargó las paradas de las rutas existentes.

## Consecuencias

* **Positivas:** el orden del planificador sobrevive a un reinicio, y una ruta completada sigue mostrando todas sus paradas.
* **Negativas:** la relación entre ruta y entrega queda guardada en dos lugares con significados distintos, y eso tiene que estar aclarado en el DER para que no se lea como redundancia.
