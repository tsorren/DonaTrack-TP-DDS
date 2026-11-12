# `fecha_alta` del donante y los últimos importados guardados en una columna del archivo

- Status: proposed
- Date: 2026-11-11
- Deciders: `fecha_alta`, el diseñador de la spec `R11-p0-api-listados` (agente) con la aprobación del usuario, que aceptó que los donantes existentes tomen la hora de la migración (§0.4); `ultimos`, el usuario pidió mostrarlos (§0.3) y el constructor del lote eligió cómo guardarlos (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P0
- Tags: donaciones, persistencia, modelo-de-datos, importacion, csv, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL: la corrida R11 construyó
> el lote con su [spec aprobada](../../specs/completed/R11-p0-api-listados-spec.md) y dejó los ADRs
> para el cierre documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión
> (`V21__indices_listados.sql`, `Donante`, `Archivo`, `UltimoImportado`,
> `ArchivosPersistenciaMapper`, commit `b3f8fc179`).

## Contexto y Problema

Dos datos que las pantallas del admin necesitaban y el modelo no guardaba:

1. **Cuándo se dio de alta un donante.** La pantalla ofrece "Más nuevos primero", pero `donante`
   no tenía fecha y su id es un UUID al azar.
2. **Los últimos importados.** El avance en vivo de la importación (R11-02) muestra los últimos
   donantes procesados del CSV con su resultado. El archivo guardaba solo los contadores.

**Gate A:** dos cambios nuevos del modelo de datos. **Gate B:** cambian el modelo relacional y el
contrato (campos `fechaAlta` y `ultimos`), con una elección deliberada de cómo guardar una lista.

## Alternativas Consideradas

**Fecha de alta**

1. Ordenar por id, o sacar el orden de la pantalla.
2. Tomar la fecha de la primera donación del donante.
3. **Columna `fecha_alta`** con valor por defecto (elegida).

**Últimos importados**

4. **Tabla aparte** (`archivo_ultimo`, una fila por donante, con clave al archivo).
5. **Columna `JSONB`** en `archivo`.
6. **Columna `TEXT`** en `archivo`, una línea por donante (elegida).
7. Deducirlos de los donantes con `fecha_alta` más reciente.

## Resultado de la Decisión

- **`fecha_alta`:** `ALTER TABLE donante ADD COLUMN fecha_alta TIMESTAMP NOT NULL DEFAULT now()` y el
  índice `(fecha_alta DESC, id)`. Es la del primer registro como donante: una reactivación no la
  cambia. Los donantes que ya existían toman la hora de la migración. Sale como `fechaAlta` en
  `DonanteOutputDTO`.
- **`ultimos`:** `ALTER TABLE archivo ADD COLUMN ultimos TEXT NULL`. Hasta 5 líneas
  `RESULTADO<tab>nombre visible`, la más nueva primero, guardadas junto con el avance cada 500 filas.
  Al escribir se quitan tabuladores y saltos del nombre. Sale como el campo opcional `ultimos`
  (`nombre`, `resultado`: `NUEVO` o `ACTUALIZADO`) de `GET /api/donantes/archivos/{id}`.

**Por qué:**

- El orden por alta es lo que pide la pantalla; el id no lo da y la primera donación no existe para
  quien todavía no donó.
- Los últimos importados son una lista chica y acotada (5) que solo se lee con su archivo y se
  reescribe entera con cada avance: una columna evita un join y una tabla que crece y hay que
  podar. `TEXT` alcanza para dos datos por línea sin sumar el mapeo de `JSONB`.
- Deducirlos de `fecha_alta` no distingue nuevo de actualizado y mezcla altas de otros caminos.

## Consecuencias

**Positivas**

- "Más nuevos primero" y "Más antiguos primero" ordenan en la base con índice.
- El avance muestra los últimos importados sin una consulta más (en el ensayo de 20.000 filas, una
  cifra nueva cada ~3 s con sus últimos).

**Negativas**

- En una base con historia, "Más nuevos" no distingue entre sí a los donantes anteriores a la
  migración (desempatan por id). Aceptado: la base de la demo se siembra después.
- El formato de `ultimos` es propio y lo interpreta solo `ArchivosPersistenciaMapper`: no se puede
  consultar desde SQL.
- `ultimos` guarda el nombre visible tal como lo calcula `PersonaEnListado.nombreVisible`; lo ve
  solo el admin, que ya ve el listado de donantes.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Por id o sin orden | Sin migración | El id es al azar; sacar el orden contradice la ficha aprobada |
| 2. Primera donación | Dato que ya existe | No hay fecha para quien no donó; mezcla dos conceptos |
| **3. `fecha_alta`** | Ordena con índice; dato propio del donante | Los existentes comparten la hora de la migración |
| 4. Tabla aparte | Normalizada; consultable | Join y tabla que crece y se poda, para 5 filas |
| 5. `JSONB` | Consultable y estructurado | Mapeo extra en JPA para dos campos |
| **6. `TEXT`** | Se lee con el archivo; sin join; tope fijo | Formato propio, no consultable |
| 7. Deducir de `fecha_alta` | Sin dato nuevo | No sabe si fue nuevo o actualizado; mezcla altas de otros caminos |

## Relaciones

- **Respeta:** [desnormalizaciones del modelo relacional de Donaciones](./20261022-desnormalizaciones-del-modelo-relacional-de-donaciones.md)
  (criterio de cuándo guardar un dato derivado) y la
  [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
  (campos aditivos).
- **Va con:** [listados paginados sin lista completa](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md).
