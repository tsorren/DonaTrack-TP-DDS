# Historial de importaciones: nombre y fecha de subida guardados en el archivo, listado paginado y descarga armada por el cliente

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p1-importaciones` y de su ficha, 2026-11-12, con las opciones recomendadas); los detalles que la spec dejaba abiertos, el constructor del lote `p1-importaciones` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: donaciones, contrato, rest, persistencia, importacion, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-importaciones-spec.md) y dejó los ADRs para el cierre
> documental de la oleada. La spec decía "sin ADR nuevo, salvo que el revisor lo pida"; el cierre de
> la oleada lo escribe porque el lote cambia el modelo de `archivo` y el contrato de Donaciones.
> `[OBSERVED]` El código ya implementa esta decisión (commit `41e7c9902`, migración renumerada a
> `V23__archivo_nombre_y_fecha_de_subida.sql` en `6fba26549`, OpenAPI de Donaciones 1.10.0).
> Código existente no es aprobación: si el revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-20, el admin solo veía la última importación que subía en su sesión: el
nombre del archivo vivía en la sesión del cliente y la tabla `archivo` guardaba la clave interna del
almacenamiento (`path`), el estado y los números, pero ni el nombre con que se subió ni cuándo. No
había un listado de importaciones, y las filas con error se veían de a 20 sin descarga
([DTI-39](../DEUDA_TECNICA.md#dti-39), ítem 4).

Problema: ¿dónde vive el dato que permite buscar y filtrar las importaciones, qué se hace con las
viejas que no lo tienen y quién arma el archivo de descarga de las filas con error?

**Gate A:** decisión nueva sobre el modelo de `archivo` (dos columnas nuevas y su regla para las
filas viejas) y sobre dónde se arma la descarga. El listado aplica el patrón ya decidido en el
[ADR de listados paginados](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md).
**Gate B:** cambia el modelo de datos de Donaciones y el contrato REST (ruta nueva y campos nuevos).

## Alternativas Consideradas

1. **Nombre y fecha de subida en `archivo`** (elegida), `NULL` en las importaciones viejas, con un
   listado `GET /api/donantes/archivos` paginado; la descarga la arma el cliente de a tandas.
2. **El nombre sigue en la sesión del cliente**, y el listado muestra solo el `path`.
3. **Columnas nuevas y, a las filas viejas, la hora de la migración** (como se hizo con
   `donante.fecha_alta`).
4. **La descarga como endpoint de Donaciones** (`GET .../filas-con-error.csv`).

## Resultado de la Decisión

Alternativa 1:

| Regla | Valor |
|---|---|
| Base | `V23`: `archivo.nombre_original VARCHAR(255) NULL` (sin la ruta del navegador, recortado a 255) y `archivo.subido_en TIMESTAMP NULL` (del `Clock` del servicio, hora de Buenos Aires); índice `ix_archivo_subido_en (subido_en DESC NULLS LAST, id)` |
| Filas viejas | Quedan en `NULL`. La pantalla dice "Archivo sin nombre guardado" y "Sin fecha guardada"; van al final del orden y quedan fuera de todo filtro de fecha. No se inventa un dato |
| Listado | `GET /api/donantes/archivos` (solo `ADMIN`): `page`, `size` (por defecto 0 y 20, tope 100), `q` sin tildes sobre el nombre, `estado` repetible, `desde` y `hasta` (días de `subido_en` en hora argentina), `orden` (`recientes` o `antiguas`); arreglo de `ArchivoOutputDTO` y total en `X-Total-Count`; 400 `ERR-CSR-003` con un valor inválido o `desde > hasta` |
| DTO | `ArchivoOutputDTO` suma `nombre` y `subidoEn`, opcionales. `path` sigue siendo interno: no se muestra ni se busca |
| En curso | El número sale de `estado=PENDIENTE&estado=PROCESANDO&size=1` y `X-Total-Count`: un conteo, no una lista |
| Descarga | La arma el cliente liviano en `GET /admin-donantes-importado/filas-con-error.csv`, pidiendo `filas-con-error` de a 100 y escribiendo cada tanda en la respuesta, en el mismo hilo del pedido (así sigue a mano el token de la sesión). UTF-8 con BOM y `;` para que la planilla la abra en español; el texto de "Qué pasó" es el mismo de la pantalla |
| Gateway | Sin cambios: `/api/donantes/**` ya enruta la ruta nueva |

**Por qué:**

- Buscar y filtrar en el servidor (regla general 1 del backlog) necesita el dato en la base; en la
  sesión, otro admin no lo ve y se pierde al salir.
- A `fecha_alta` se le dio un valor a las filas viejas porque el orden de donantes lo necesitaba en
  todas; acá alcanza con mandarlas al final, y un dato falso confundiría al leer el historial.
- El texto en español de cada motivo de error vive en el cliente (regla 15 de los textos): un CSV
  armado en Donaciones obligaba a duplicar esos textos o a mandar los códigos internos. Con tandas
  de 100, ni el cliente ni el servicio cargan todas las filas con error en memoria.

## Consecuencias

**Positivas**

- R11-20 hecho: las importaciones en curso e históricas se ven de a una página, con búsqueda y
  filtros, y cada una lleva a su avance o a su resultado.
- El listado usa el mismo contrato que el resto de los listados de Donaciones.

**Negativas**

- Las importaciones anteriores a `V23` no se pueden encontrar por nombre ni por fecha.
- La descarga pasa por el cliente liviano: un archivo con muchas filas con error hace una llamada
  a Donaciones por cada 100. Se aceptó porque son pocas (en el ensayo, 200 de 20.000).
- `StreamingResponseBody` habría liberado el hilo, pero corre en otro hilo y pierde el token de la
  sesión: se descartó. La descarga de 20.000 filas reales con el stack queda para probar
  ([DTI-42](../DEUDA_TECNICA.md#dti-42)).
- El DER de Donaciones no muestra todavía las dos columnas.
- Quién subió cada archivo no se guarda (Administración era una sola cuenta al diseñar el lote):
  queda para `p2-actividad`.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. Columnas en `archivo`, `NULL` en las viejas** | Se busca y se filtra en la base; ningún dato inventado | Las viejas quedan sin nombre ni fecha |
| 2. Nombre en la sesión | Sin migración | No se busca; otro admin no lo ve; se pierde al salir |
| 3. Hora de la migración a las viejas | Todas tienen fecha | Un dato falso en un historial |
| 4. CSV en Donaciones | Una sola llamada | Duplica los textos en español o expone códigos internos |

## Relaciones

- **Aplica:** [listados de Donaciones siempre paginados](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  y la [búsqueda sin tildes con `unaccent`](./20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md).
- **Sigue a:** [fecha de alta del donante y últimos importados en el archivo](./20261111-fecha-de-alta-del-donante-y-ultimos-importados-en-el-archivo.md),
  que también suma datos propios al archivo; acá con otra regla para las filas viejas.
- **Spec y ficha:** [R11-p1-importaciones](../../specs/completed/R11-p1-importaciones-spec.md) ·
  [ficha de flujo](../../interfaz/flujos/r11-p1-importaciones.md).
- **Deuda:** [DTI-39](../DEUDA_TECNICA.md#dti-39) (ítem 4, la descarga, saldada) y
  [DTI-42](../DEUDA_TECNICA.md#dti-42).
