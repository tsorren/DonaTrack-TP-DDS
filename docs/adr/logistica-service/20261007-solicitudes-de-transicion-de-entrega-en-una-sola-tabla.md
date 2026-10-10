# Solicitudes de transición de entrega en una sola tabla

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística; revisado en la reunión grupal del 06/10/2026
- Tags: logística, persistencia, herencia, orm, single-table

## Contexto y Problema

En el dominio, `SolicitudTransicionEntrega` es una clase abstracta con cuatro subclases: `ConfirmacionRecepcion` (puede traer una foto), `NoRecepcion` (trae justificación y si es replanificable), `RevisionEntrega` y `RegresoDeposito` (sin datos propios). Cada solicitud queda registrada como historial de la entrega. Hay que decidir cómo mapear esa herencia a tablas.

## Alternativas Consideradas

* **Una tabla por subclase (o JOINED):** cada tabla tiene solo sus columnas, sin nulos. Son cuatro tablas, o cinco con la base, para dos atributos propios en total, y leer el historial de una entrega obliga a unirlas.
* **Una sola tabla con discriminador:** todas las columnas en `solicitud_transicion_entrega`, con `tipo_transicion` para distinguir la subclase y nulos en las columnas que no aplican.

## Resultado de la Decisión

Alternativa elegida: **una sola tabla con discriminador**.

`[OBSERVED]` El código actual ya implementa esta decisión.

`solicitud_transicion_entrega` tiene `tipo_transicion` (enum `TRANSICION_ENTREGA`) y las columnas opcionales `foto_recepcion_url`, `justificacion` y `replanificable`. Como el modelo de persistencia está separado del dominio, no se usa `@Inheritance` de JPA: `SolicitudesTransicionEntregaRepositoryJpaAdapter` mira la subclase de la solicitud y completa `tipo_transicion` y las columnas que le corresponden.

Para que los nulos no sean arbitrarios, un `CHECK` exige qué columnas van completas en cada tipo (por ejemplo, `NO_RECEPCION` obliga a tener justificación y `replanificable`). Las reglas están anotadas en el DER. Es el mismo criterio que usó Notificaciones con `MedioDeContacto`.

## Consecuencias

* **Positivas:** una tabla en lugar de cuatro, y el historial de una entrega se lee sin joins.
* **Negativas:** la tabla queda desnormalizada y con columnas nulas. Agregar una subclase con datos nuevos implica sumar columnas y actualizar el `CHECK` con una migración.
