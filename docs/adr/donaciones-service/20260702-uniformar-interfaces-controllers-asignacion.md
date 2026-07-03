# Uniformar el patrón interfaz + implementación en todos los controllers
- Status: accepted
- Date: 2026-07-02
- Deciders: Sofia Deane
- Tags: donaciones, api-rest, consistencia, mantenibilidad

## Contexto y Problema
Al auditar la exposición REST de `donaciones-service` para esta issue se detectó que todos los controllers del servicio siguen el patrón `I<Nombre>Controller` (interfaz) + `impl/<Nombre>Controller` (implementación) — confirmado en `IDonacionesController`, `IPersonasController`, `IDonantesController`, `IEntidadBeneficiariaController` y `INecesidadesController` — excepto `AsignacionController` y `PropuestasController`, que están implementados directamente sin una interfaz que declare su contrato. ¿Se agregan las interfaces faltantes para uniformar el criterio, o se documenta la excepción como aceptable?

## Atributos de Calidad y Drivers de Decisión
* Mantenibilidad: un patrón uniforme en todo el paquete `controllers` reduce la carga cognitiva de quien lee o extiende el código
* Consistencia arquitectónica: dos controllers con un criterio distinto al resto, sin justificación funcional, es una inconsistencia detectable en cualquier revisión de diseño
* Costo de la solución: el cambio debe ser proporcional al beneficio obtenido

## Alternativas Consideradas
* Dejar `AsignacionController` y `PropuestasController` sin interfaz (statu quo)
* Agregar `IAsignacionController` e `IPropuestasController`, siguiendo el mismo criterio que el resto del servicio

## Resultado de la Decisión

Alternativa elegida: "Agregar `IAsignacionController` e `IPropuestasController`"

Justificación:
La issue pide explícitamente verificar que la exposición REST esté "correctamente implementada", y una inconsistencia de patrón sin justificación de negocio cuenta como parte de eso. El costo de agregar la interfaz es mínimo (una declaración de método por cada uno ya existente en la implementación, sin lógica nueva), mientras que el beneficio de mantener un único criterio en todo el paquete es permanente para cualquiera que trabaje en el servicio de ahí en adelante.

### Consecuencias Positivas
* Uniformidad total del paquete `controllers`: cualquier controller nuevo tiene un único patrón a seguir, sin ambigüedad
* Facilita mockear el contrato de estos controllers en tests, igual que ya se hace con el resto

### Consecuencias Negativas
* Dos archivos adicionales sin lógica propia (las interfaces), que no aportan comportamiento nuevo por sí solos

### Validación

Se valida confirmando que `AsignacionController implements IAsignacionController` y `PropuestasController implements IPropuestasController`, que las interfaces declaran exactamente los métodos ya expuestos (`POST /api/asignaciones/ejecuciones`, `GET /api/asignaciones/ejecuciones`, `GET /api/asignaciones/propuestas`, `PUT /api/asignaciones/propuestas/{id}/estado`), y que la compilación y los tests existentes de ambos controllers siguen pasando sin modificaciones funcionales.

## Análisis de Alternativas

### Dejar sin interfaz (statu quo)

No modificar `AsignacionController` ni `PropuestasController`.

#### Pros
* Cero trabajo adicional

#### Contras
* Inconsistencia visible para cualquiera que lea el árbol de paquetes `controllers/` e `controllers/impl/`
* Rompe la predictibilidad del patrón que el resto del servicio sí respeta

### Agregar las interfaces faltantes

Crear `IAsignacionController` e `IPropuestasController` con la firma de los métodos ya implementados, y hacer que las clases `impl` las implementen.

#### Pros
* Consistencia total con el resto del servicio
* Costo de implementación mínimo, sin cambios de comportamiento

#### Contras
* Ninguno funcional; solo el mínimo overhead de mantener dos archivos adicionales
