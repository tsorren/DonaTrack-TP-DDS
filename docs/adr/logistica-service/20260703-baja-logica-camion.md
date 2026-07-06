# Baja lógica de camiones mediante estado DESHABILITADO

- Status: accepted
- Date: 2026-07-03
- Deciders: Nico (completar apellido), Tadeo Sorrentino
- Tags: camiones, logistica, estado, trazabilidad

## Contexto y Problema

El ABM de camiones requiere una operación de "baja". ¿Debe eliminarse
el registro físicamente del storage, o debe marcarse como inactivo
conservando el registro? Además, ¿qué sucede con la patente de un
camión dado de baja — puede reutilizarse en un alta futura?

## Atributos de Calidad y Drivers de Decisión

* Trazabilidad: un camión dado de baja puede tener historial de rutas
  y entregas asociadas — eliminarlo físicamente rompe esa trazabilidad
* Consistencia: el patrón debe ser coherente con el enfoque de baja
  lógica ya aplicado en el resto del proyecto
* Integridad de datos: una patente usada alguna vez no debería poder
  ser reutilizada para evitar ambigüedad en registros históricos

## Alternativas Consideradas

* Baja física: `repository.delete(camion)`
* Baja lógica con flag `activo: boolean` separado
* Baja lógica mediante transición al estado `DESHABILITADO` del enum
  `EstadoCamion`

## Resultado de la Decisión

Alternativa elegida: baja lógica mediante `EstadoCamion.DESHABILITADO`.

Justificación: `EstadoCamion` ya existe como parte del modelo de
dominio de `Camion`. Agregar un campo `activo` separado sería
redundante — `DESHABILITADO` cumple exactamente esa semántica sin
agregar un atributo extra. La baja es irreversible desde la API del
ABM: un camión `DESHABILITADO` no puede transicionar a ningún otro
estado vía el controller de camiones. La patente de un camión
deshabilitado sigue bloqueada para altas futuras — la validación de
unicidad en `ValidadorPatentes` consulta todos los camiones
independientemente de su estado.

### Validación

`consultarTodos()` filtra `DESHABILITADO`. `buscarCamionActivo()`
trata un camión deshabilitado igual que uno inexistente (404).
La validación de unicidad de patente no filtra por estado —
incluye deshabilitados.

## Análisis de Alternativas

### Baja física [DESCARTADO]

#### Pros

* Implementación directa: `repository.delete(camion)`
* Storage más liviano

#### Contras

* Rompe trazabilidad: el camión desaparece aunque haya tenido rutas
  y entregas asociadas
* Libera la patente para reutilización, generando posible ambigüedad
  en registros históricos
* Inconsistente con el enfoque del resto del proyecto

---

### Baja lógica con flag `activo: boolean` separado [DESCARTADO]

#### Pros

* Semántica explícita de "activo/inactivo"

#### Contras

* Redundante con `EstadoCamion.DESHABILITADO` — dos mecanismos para
  el mismo concepto
* Agrega un atributo al dominio sin valor adicional
* Requiere coordinar dos campos en lugar de uno

---

### Baja lógica mediante `DESHABILITADO` [ELEGIDO]

#### Pros

* Reutiliza el modelo de estados ya existente sin campos extra
* La irreversibilidad está protegida por el dominio
* Consistente con el enfoque de baja lógica del resto del proyecto
* La patente queda bloqueada permanentemente

#### Contras

* `consultarTodos()` debe filtrar explícitamente por estado
* La validación de unicidad debe incluir camiones deshabilitados

## Links

- Issue #617: ABM de Camiones
- Issue #615: Planificación de rutas (consume camiones DISPONIBLES)