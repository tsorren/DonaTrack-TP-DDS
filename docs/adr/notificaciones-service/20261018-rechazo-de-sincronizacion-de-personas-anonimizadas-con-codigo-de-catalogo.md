# Rechazo de Sincronización de Personas Anonimizadas con Código de Catálogo en lugar de Excepción Dedicada

- Status: proposed
- Date: 2026-10-18
- Deciders: Decisión Grupal
- Tags: notificaciones, excepciones, error-catalog, anonimizacion, sonarcloud

## Contexto y Problema

El eje 3 del ADR aceptado [`20260902-adapters-de-notificacion-y-observabilidad`](./20260902-adapters-de-notificacion-y-observabilidad.md) decidió que, si `PUT /api/notificaciones/personas` recibe una persona cuya réplica ya está anonimizada (`denominacion = "ANONIMIZADO"`), se lance `PersonaYaAnonimizadaException` (subclase de `BusinessStateException`) y se responda `HTTP 409 Conflict`. La regla protege el derecho al olvido (Ley 25.326).

`[OBSERVED]` La subclase se implementó con el código genérico `ERR-CSR-003` (`ARGUMENTO_INVALIDO`), que en el catálogo está documentado como error de validación `400`. El cuerpo de error respondía `409` con un código que el catálogo atribuye a `400`.

`[OBSERVED]` SonarCloud reporta `java:S110` (MAJOR) sobre la subclase en la PR de la Entrega 4: tiene 6 ancestros (`BusinessStateException` → `DonaTrackException` → `RuntimeException` → `Exception` → `Throwable` → `Object`) y el máximo permitido es 5. Cualquier subclase de las excepciones de `common-lib` supera ese límite. El resto de los servicios no crea subclases: lanza `BusinessStateException` o `ValidationException` con una constante propia de `ErrorCatalog`.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia del contrato de errores:** el código del cuerpo de error tiene que corresponder al status HTTP documentado en [`catalogo-errores.md`](../../arquitectura/catalogo-errores.md).
* **Mantenibilidad:** seguir el mismo mecanismo de errores que el resto de los servicios y superar el Quality Gate de SonarCloud sin excepciones de configuración.
* **Privacidad:** conservar intacta la regla de negocio que impide sobrescribir una persona anonimizada.

## Alternativas Consideradas

### Alternativa 1 (Elegida): `BusinessStateException` con la constante `PERSONA_ANONIMIZADA` (`ERR-EST-902`)

Eliminar la subclase. `PersonasService.sincronizar()` lanza `new BusinessStateException(ErrorCatalog.PERSONA_ANONIMIZADA)`. El `GlobalExceptionHandler` ya traduce `BusinessStateException` a `409 Conflict`, así que no hace falta un handler nuevo.

* **Pros:** el código de error queda alineado con el status (`ERR-EST` → `409`); es el mecanismo que usan los demás servicios; resuelve `java:S110` sin tocar la configuración de SonarCloud.
* **Contras:** el cuerpo de error ya no incluye el mensaje con el ID de la persona (`details` queda vacío, como en el resto de las `BusinessStateException`); agrega una constante a `common-lib`.

### Alternativa 2 (Descartada): Mantener la subclase dedicada (status quo)

Conservar `PersonaYaAnonimizadaException` y marcar `java:S110` como *Won't Fix* en SonarCloud.

*Motivo de descarte:* deja abierta la inconsistencia entre `ERR-CSR-003` (`400`) y la respuesta `409`. Además, es la única subclase de excepción de `common-lib` del repositorio y requiere una excepción manual al Quality Gate.

### Alternativa 3 (Descartada): Heredar directamente de `DonaTrackException`

Baja a 5 ancestros, pero la excepción deja de ser `BusinessStateException` y el `GlobalExceptionHandler` la trataría como un error genérico (`500`). Habría que agregar un handler específico en `common-lib` para un caso propio de Notificaciones, lo que contradice el criterio del Shared Kernel ([`20260901-limites-y-responsabilidades-del-shared-kernel-common-lib`](../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md)).

## Resultado de la Decisión

Se elige la Alternativa 1. La regla del eje 3 de `20260902` se mantiene sin cambios (rechazar con `409` la re-sincronización de una persona anonimizada). Cambian el mecanismo (sin subclase) y el código de error (`ERR-CSR-003` → `ERR-EST-902`).

`[PROPOSED]` Relación con `20260902-adapters-de-notificacion-y-observabilidad`: este ADR reemplaza solo el eje 3. Los ejes 1 y 2 siguen vigentes. Marcar el ADR original como `superseded` (total o parcial) queda a cargo del revisor humano (AGENTS.md §9.3).

## Consecuencias Positivas

* El cuerpo de error de `PUT /api/notificaciones/personas` informa un código coherente con su status `409`.
* Se elimina una clase y se unifica el manejo de errores con el resto de los servicios.
* Se resuelve `java:S110` sin excepciones de configuración en SonarCloud.

## Consecuencias Negativas

* Un cliente que dependiera del valor `ERR-CSR-003` en esta respuesta debe pasar a `ERR-EST-902`. El campo `type` del cuerpo de error también cambia, de `PersonaYaAnonimizadaException` a `BusinessStateException` (`ErrorResponse` usa el nombre simple de la clase). `[OBSERVED]` En el repositorio ningún cliente ni test de integración valida ese código.
* El log del handler ya no muestra el ID de la persona en el mensaje de la excepción. Se puede correlacionar por `traceId`.

## Validación

* `PersonasServiceTest` verifica que se lance `BusinessStateException` con `ErrorCatalog.PERSONA_ANONIMIZADA`.
* `PersonasControllerTest` verifica `409 Conflict` y `code = ERR-EST-902` en el cuerpo.

## Links

* ADR afectado: [`20260902-adapters-de-notificacion-y-observabilidad`](./20260902-adapters-de-notificacion-y-observabilidad.md) (eje 3).
* Catálogo: [`catalogo-errores.md`](../../arquitectura/catalogo-errores.md) (`ERR-EST-902`).
* Origen: issues de SonarCloud de la PR [tsorren/DonaTrack-TP-DDS#823](https://github.com/tsorren/DonaTrack-TP-DDS/pull/823).
