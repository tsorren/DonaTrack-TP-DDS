# Cambios al DER de Donaciones

> Qué hay que cambiar en el DER de Lucid (versión del 19/10/2026) para que coincida con el schema `donaciones` que crean las migraciones Flyway V1 a V12. El diagrama completo está en [`donaciones-DER.puml`](donaciones-DER.puml), con estos cambios en rojo.
>
> Los códigos (P-n, D-n) remiten a las decisiones de la [bitácora de persistencia](../../../entrega-4/donaciones/bitacora-persistencia.md). Las desnormalizaciones (D1 a D6) están en el [ADR de desnormalización](../../../adr/donaciones-service/20261022-desnormalizaciones-del-modelo-relacional-de-donaciones.md) y no cambian: ya estaban en el DER.

## Pendientes de discusión con el equipo

| Tabla | Qué | Estado |
|---|---|---|
| `solicitud_entrega` | `proveedores_descartados TEXT[]`: proveedores que rechazaron el pedido en la ronda actual. Sin este dato, el broker alterna entre proveedores sin terminar nunca | Columna **provisoria** (D-37). La alternativa es una tabla hija `(id_solicitud, proveedor_id)` |
| Transacciones | Hoy cubren solo el broker y cada evento de Logística | La opción por caso de uso está documentada en la bitácora (D-38) |

## Cambios por tabla

Salvo que se indique, son **aditivos** (columnas que el dominio usa y el DER no tiene) o de **tipo**.

### Personas

| Tabla | Cambio | Motivo |
|---|---|---|
| `direccion` | `altura` y `piso` `INTEGER` (DER: `SMALLINT`) | P-11 |
| `direccion` | Suma `provincia` y `pais` (`TEXT NOT NULL`) y una restricción única sobre todas las columnas | D1: la dirección es plana, inmutable y compartida (D-9) |
| `persona` | Suma `nro_documento TEXT`, único junto con `tipo_documento` | La unicidad que evita personas duplicadas (D-13) |
| `humana` | `fecha_nacimiento` `DATE` (DER: `TIMESTAMPTZ`) | D-10: es una fecha, sin hora |
| `correo` | Suma `direccion_correo TEXT` | El DER no tenía dónde guardar el correo |
| `telefono` | Suma `codigo_area`, `numero` y `tipo_telefono` (`WHATSAPP`, `ESTANDAR`) | Ídem para el teléfono |
| `juridica` | Valores de `tipo_juridico`: `GUBERNAMENTAL`, `ONG`, `EMPRESA`, `INSTITUCION` | Paso 3 |
| `donante`, `entidad_beneficiaria` | Suman `activo BOOLEAN NOT NULL` | Baja lógica de los roles (P-16) |
| `archivo` | `id` en vez de `id_archivo`; suma `estado_archivo` | P-8 |

### Catálogo y necesidades

| Tabla | Cambio | Motivo |
|---|---|---|
| `categoria` | Suma `tipo_unidad` | P-8 |
| `alias` | Suma `id` como PK y una restricción única `(id_subcategoria, alias)` | El DER no le daba clave; el alias repetido se ignora (P-9) |
| `necesidad` | Suma `descripcion` y `version` (bloqueo optimista) | `descripcion` ya estaba en V1; D-17 |
| `necesidad` | `periodo` `VARCHAR(20)` en formato ISO-8601 (`P1M`, `P7D`) (DER: `BIGINT`) | D-18: un período de meses no se puede guardar en días |
| `necesidad`, `periodo_necesidad` | Fechas como `TIMESTAMP` (DER: `TIMESTAMPTZ`) | Mismo criterio en todo el schema: el dominio usa `LocalDateTime` |
| `periodo_necesidad` | La FK se llama `necesidad_id` (DER: `id_necesidad`) | Nombre de V1 (#897) |

### Donaciones

| Tabla | Cambio | Motivo |
|---|---|---|
| `deposito` | `nombre NOT NULL`, único junto con la dirección | El depósito se comparte y se reutiliza por nombre y dirección (P-2) |
| `donacion` | `fecha` `TIMESTAMP` | Criterio general de fechas |
| `item_donacion` | `peso_unitario` y `volumen_unitario` `NUMERIC(12,3)` con `CHECK > 0` (DER: `BIGINT`) | D-31 |
| `item_donacion` | `fecha_vencimiento` `DATE`; valores de `estado`: `NUEVO`, `USADO`, `DESGASTADO` | D-10 |
| `item_donacion_normalizado` | Suma la copia del bien completo (`foto_url`, `fecha_vencimiento`, `estado`, `peso_unitario`, `volumen_unitario`) y `estado_normalizacion` (`PENDIENTE_REVISION`, `ACEPTADO`, `RECHAZADO`). `bien_nombre` queda sin uso | D-30 (foto de lo donado, D4) y P-3 |

### Donaciones independientes

| Tabla | Cambio | Motivo |
|---|---|---|
| `donacion_independiente` | `fecha_registro NOT NULL`, `TIMESTAMP`; `CHECK`: no puede haber período sin necesidad | El dominio siempre la registra |
| `item_donacion_independiente` | Suma `id_item_normalizado (FK) NOT NULL` | **D-24**: sin eso no se reconstituye el bien (subcategoría, confianza). La relación `Subcategoria` → `Item_donacion_independiente` del DER no es una columna: la subcategoría sale del ítem normalizado |
| `estado_donacion_independiente` | Suma `tipo NOT NULL` (los 7 estados) | **D-21**: sin el tipo, el historial no dice qué estados eran |
| `estado_donacion_independiente` | `patente_camion` se completa en `ENTREGADA`; `replanificable_hasta` queda sin uso | D-34 |
| `cambio_estado_donacion_independiente` | Suma `justificacion TEXT`; `id_estado_nuevo NOT NULL` | **D-22**: la justificación de una entrega fallida |

### Propuestas

| Tabla | Cambio | Motivo |
|---|---|---|
| `propuesta` | Suma `estado` (`PENDIENTE`, `APROBADA`, `DESCARTADA`) y `fecha_creacion` | P-20: sin el estado no se sabe si se puede aprobar |
| `posible_fragmentacion` | Suma `id_donacion_independiente (FK) NOT NULL`; `id` pasa a ser un UUID propio | **D-23**: el DER la conectaba por su PK y una donación en dos propuestas chocaba |
| `posible_fragmentacion` | `cantidad` `INTEGER` (DER: `SMALLINT`) | D-35 |

### Logística e integración

| Tabla | Cambio | Motivo |
|---|---|---|
| `solicitud_entrega` | Suma `fecha_creacion` y `proveedores_descartados` (provisoria, ver arriba); `estado` `VARCHAR` con `CHECK` (DER: `TEXT`); una sola solicitud no `FALLIDA` por donación | P-21 |
| `outbox_logistica` | Suma `max_intentos`; `proximo_intento` `TIMESTAMP` (DER: `TIMESTAMPZ`, errata de `TIMESTAMPTZ`) | P-21 |
| `evento_consumido` | Suma `fecha_procesamiento`; restricción única `(event_type, business_id, id_donacion_independiente)` | P-21: la restricción garantiza la idempotencia |

## Tablas que no están en el DER

Ninguna. El historial de corridas del algoritmo (`EjecucionAsignacion`) sigue en memoria (D-36) y los reintentos hacia Notificaciones (`OutboxStore`) también, porque guardan código y no datos.
