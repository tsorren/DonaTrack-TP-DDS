# Centralización de validación de patente en ValidadorPatentes

- Status: accepted
- Date: 2026-07-03
- Deciders: Nico (completar apellido)
- Tags: validacion, camiones, logistica, dominio

## Contexto y Problema

El ABM de camiones requiere validar dos reglas sobre la patente:
formato argentino (viejo `ABC123` o Mercosur `AB123CD`) y unicidad
global (incluyendo camiones dados de baja). ¿Dónde ubicar estas
validaciones para que sean cohesivas, testeables y consistentes con
el resto del proyecto?

## Atributos de Calidad y Drivers de Decisión

* Cohesión: toda la lógica de validación de patente debe vivir en
  un único lugar
* Testeabilidad: cada regla debe poder probarse en aislamiento sin
  levantar Spring ni HTTP
* Consistencia: el patrón debe ser replicable para futuras
  validaciones de dominio en `logistica-service`
* Encapsulamiento: el service no debe conocer los detalles del
  formato ni de la consulta de unicidad

## Alternativas Consideradas

* Validar formato y unicidad inline en `CamionesService.crear()`
* Validar formato en el constructor de `Camion` y unicidad en
  `CamionesService`
* Centralizar ambas validaciones en una clase `ValidadorPatentes`
  inyectada en el service

## Resultado de la Decisión

Alternativa elegida: centralizar en `ValidadorPatentes`.

Justificación: el formato de patente es una invariante del dato en sí
(no depende del estado del sistema), pero la validación de unicidad
requiere acceder al repository. Colocar ambas validaciones en el
constructor de `Camion` violaría el principio de que el dominio no
debe tener dependencias de infraestructura. Validarlas inline en el
service dispersa la lógica y la hace difícil de testear en
aislamiento. `ValidadorPatentes` actúa como un objeto de política
reutilizable, inyectado en el service, que encapsula ambas reglas en
un único punto de responsabilidad — mismo patrón que
`ValidadorPersonaDuplicada` en `donaciones-service`.

### Validación

`ValidadorPatentes` puede testearse independientemente del service y
del controller. El service no conoce el regex ni la consulta de
unicidad — solo llama a `validar(patente)`.

## Análisis de Alternativas

### Validación inline en `CamionesService` [DESCARTADO]

#### Pros

* Sin clases extra
* Código directo

#### Contras

* Duplicación si aparece otro punto de entrada que cree camiones
* Difícil de testear la regla de unicidad sin instanciar el service
  completo
* Viola SRP: el service mezcla orquestación con lógica de validación

---

### Formato en `Camion`, unicidad en `CamionesService` [DESCARTADO]

#### Pros

* El formato como invariante del dominio es semánticamente correcto

#### Contras

* La regla de unicidad sigue dispersa en el service
* El constructor de `Camion` dependería de un mecanismo de validación
  que no puede probar sin instanciar el objeto completo
* Dos lugares distintos con reglas sobre el mismo dato (patente)

---

### `ValidadorPatentes` centralizado [ELEGIDO]

#### Pros

* Un único punto de responsabilidad para todas las reglas de patente
* Testeable en aislamiento con un mock del repository
* Consistente con el patrón de `ValidadorPersonaDuplicada` en
  `donaciones-service`
* Si se agregan nuevas reglas (ej. longitud máxima, caracteres
  permitidos), el service no cambia

#### Contras

* Una clase extra en la capa de service

## Links

- Issue #617: ABM de Camiones
- Patrón similar: `ValidadorPersonaDuplicada` en `donaciones-service`