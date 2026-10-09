# Comentarios sobre el mapeo ORM de Logística

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística
- Tags: logística, persistencia, jpa, hibernate, orm

Decisiones de mapeo que no justifican un ADR propio, pero que conviene dejar escritas porque en algunos casos difieren de lo que hicieron otros servicios. `[OBSERVED]` El código actual ya implementa todas.

* **Modelo de persistencia separado del dominio.** Las entidades JPA viven en `infrastructure/persistencia` y se traducen al dominio con mappers. Una regla de ArchUnit impide que el dominio importe `jakarta.persistence`. Es el mismo esquema de puertos y adaptadores que usa Notificaciones.
* **Enums nativos de PostgreSQL.** Los estados y tipos son `CREATE TYPE ... AS ENUM` y las tablas tienen `CHECK` para las reglas entre columnas. Notificaciones e Incentivos guardan los enums como `VARCHAR`. La ventaja es que la base rechaza un valor inválido; el costo es que agregar un estado exige un `ALTER TYPE` en una migración.
* **Historiales en cuatro tablas.** `cambio_estado_camion`, `cambio_estado_chofer`, `cambio_estado_ruta` y `cambio_estado_entrega`, cada una con FK a su dueño. Unificarlas en una sola tabla haría que el id apuntara a cuatro tablas distintas, y eso no se puede expresar con una FK.
* **Historial con id propio y solo inserción.** Cada cambio de estado es una fila con UUID que se inserta una vez y no se modifica. Notificaciones mapea su historial como `@ElementCollection` sin id. Con id propio, Hibernate agrega la fila nueva en lugar de borrar y reinsertar el historial completo en cada cambio.
* **Bloqueo optimista.** `camion`, `chofer`, `ruta`, `entrega` y `solicitud_planificacion` tienen `version BIGINT`. Si el callback del planificador y una llamada a la API modifican la misma fila al mismo tiempo, el segundo en guardar falla en lugar de pisar en silencio el cambio del primero.
* **Carga de colecciones.** Las colecciones (historiales, paradas de una ruta) son `EAGER` con `FetchMode.SUBSELECT`: al leer varias rutas, Hibernate trae todas sus colecciones con una consulta adicional en lugar de una por ruta (problema N+1). `open-in-view` está desactivado, así que nada se carga fuera de la transacción.
* **Esquema versionado.** El esquema lo crean migraciones de Flyway y Hibernate corre con `ddl-auto=validate`: no crea ni modifica tablas, solo verifica al arrancar que el mapeo coincida con la base.
* **`solicitud_planificacion_ruta` como tabla intermedia.** Las rutas que generó cada solicitud se guardan aparte, y así `Ruta` no conoce la solicitud de planificación que la originó.
