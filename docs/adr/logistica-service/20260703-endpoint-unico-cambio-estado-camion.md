# Endpoint único PATCH para cambio de estado de camión

- Status: accepted
- Date: 2026-07-03
- Deciders: Nico (completar apellido)
- Tags: rest, camiones, logistica, api

## Contexto y Problema

El dominio de `Camion` expone múltiples transiciones de estado:
`habilitar()`, `deshabilitar()`, y en el futuro las que correspondan
a planificación de rutas. ¿Cómo exponer estas transiciones vía REST
— un endpoint por transición, o un endpoint único que reciba el
estado destino?

## Atributos de Calidad y Drivers de Decisión

* Consistencia: el patrón debe ser coherente con la API ya definida
  en `donaciones-service`
* Mantenibilidad: agregar una nueva transición no debería requerir
  un nuevo endpoint
* Claridad: el contrato de la API debe ser predecible para quien
  la consuma
* Separación de responsabilidades: el controller no debe conocer
  las reglas de transición — esa lógica vive en el dominio

## Alternativas Consideradas

* Un endpoint por transición:
  `PATCH /api/camiones/{id}/habilitar`,
  `PATCH /api/camiones/{id}/deshabilitar`
* Un endpoint único con el estado destino en el body:
  `PATCH /api/camiones/{id}/estado`

## Resultado de la Decisión

Alternativa elegida: endpoint único `PATCH /api/camiones/{id}/estado`
con `CambioEstadoCamionRequestDTO` en el body.

Justificación: es el mismo patrón que usa
`DonacionesIndependientesController` con
`PATCH /donaciones-independientes/{id}/estado`. El controller no
necesita conocer qué transiciones son válidas desde cada estado —
esa responsabilidad es del dominio, que lanza
`ValidationException(ESTADO_CAMION_TRANSICION_INVALIDA)` si la
transición no es válida. Agregar una nueva transición futura (ej.
`EN_MANTENIMIENTO` si se decide incorporar ese estado) no requiere
un nuevo endpoint — solo un nuevo case en el switch del service.

### Validación

El controller no tiene ningún `if` de validación de estado. El
switch en `CamionesService.cambiarEstado()` mapea cada estado
destino al método de dominio correspondiente y rechaza `EN_RUTA`
explícitamente (esa transición es responsabilidad de `RutasService`
en #615).

## Análisis de Alternativas

### Endpoints por transición [DESCARTADO]
PATCH /api/camiones/{id}/habilitar
PATCH /api/camiones/{id}/deshabilitar
#### Pros

* Nombre de endpoint expresa la intención de la operación
* No requiere un DTO para el body

#### Contras

* Cada nueva transición requiere un nuevo endpoint
* Inconsistente con el patrón ya establecido en `donaciones-service`
* El controller conoce implícitamente qué operaciones existen,
  duplicando la lógica que ya está en el dominio

---

### Endpoint único `PATCH /estado` [ELEGIDO]

#### Pros

* Consistente con `donaciones-service`
* El controller es agnóstico a las reglas de transición
* Extensible sin cambios en el contrato de la API

#### Contras

* El body puede recibir valores inválidos para el ABM (ej. `EN_RUTA`)
  que se rechazan en el service — no se puede prevenir a nivel de tipo

## Links

- Issue #617: ABM de Camiones
- Issue #618: Exposición REST (Swagger sobre estos endpoints)
- Patrón similar: `DonacionesIndependientesController` en
  `donaciones-service`