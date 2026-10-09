# Referencias a Donaciones sin clave foránea

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística
- Tags: logística, persistencia, integración, ddd

## Contexto y Problema

Una entrega conoce la donación que traslada y la entidad beneficiaria que la recibe (`id_donacion`, `id_beneficiario`). Esos datos viven en Donaciones. La primera versión del DER los marcaba como claves foráneas.

## Resultado de la Decisión

Se guardan como `UUID NOT NULL` sin `REFERENCES`: son referencias por identidad.

`[OBSERVED]` El código actual ya implementa esta decisión.

Una FK no es posible: cada servicio tiene su propio schema y el rol de Logística no puede leer el de Donaciones (ADR `20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql`). Aunque lo fuera, uniría dos servicios a través de la base. Es la regla de referenciar otros agregados por id (`donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid`), la misma que aplicó Notificaciones con `persona_id`.

La existencia del id la garantiza el flujo: las entregas nacen de mensajes de Donaciones, que trae el id, y Logística lo devuelve en sus eventos para que Donaciones busque la donación en su propia base. `UNIQUE (id_donacion)` impide crear dos entregas para la misma donación; si llega un duplicado por la API, se responde 409.

`evento_entrega` repite `id_donacion` a propósito: cada evento tiene que poder publicarse sin consultar otra tabla.

## Consecuencias

* **Positivas:** los servicios quedan desacoplados en la base y cada uno puede cambiar su esquema sin afectar al otro.
* **Negativas:** la base no valida que la donación exista; la consistencia entre servicios es eventual.
