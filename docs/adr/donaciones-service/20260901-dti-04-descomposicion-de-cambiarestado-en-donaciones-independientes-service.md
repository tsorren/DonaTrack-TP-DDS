# [DTI-04] Descomposición de cambiarEstado() en DonacionesIndependientesService

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-04, casos-de-uso, mantenibilidad, srp, refactor

## Contexto y Problema

El método `cambiarEstado()` en `DonacionesIndependientesService` concentra en un único método un `switch` extenso con 6 transiciones de negocio heterogéneas:
1. Asignar donación a necesidad
2. Preparar lote (Lista para entregar)
3. Iniciar traslado
4. Confirmar entrega
5. Registrar entrega fallida
6. Marcar vencimiento
Además de violar SRP, este método mezcla la mutación interna del agregado con llamadas condicionadas a OpenFeign hacia `notificaciones-service` y lógica de inventario, volviendo el código frágil, difícil de extender y complejo de auditar.

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Legibilidad:** Reemplazar switches anémicos gigantes por métodos de intención semántica clara.
* **Alta Cohesión:** Cada caso de uso del ciclo de vida debe estar aislado para no introducir efectos colaterales en otras transiciones.
* **Open/Closed Principle:** Facilitar la incorporación de nuevas reglas o transiciones sin editar un método monolítico.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-04**; Diagnóstico Arquitectónico §1.5.
* **Hallazgo:** La auditoría final contrastó este método contra `logistica-service`, observando que en logística las transiciones están limpiamente segregadas por comando o intención de negocio, mientras que en donaciones un único switch gobierna todo el ciclo de vida.

## Alternativas Consideradas

* **Descomposición en Métodos Semánticos Especializados de Caso de Uso:** Dividir `cambiarEstado()` en métodos explícitos en `IDonacionesIndependientesService`: `asignarDonacion(...)`, `iniciarTraslado(...)`, `confirmarEntrega(...)`, `marcarEntregaFallida(...)`, etc. Cada método orquesta exclusivamente las validaciones y comunicaciones específicas de ese evento.
* **Patrón Command / Handler por Transición:** Crear clases de comando independientes (`AsignarDonacionCommand`, `ConfirmarEntregaCommand`).
* **Mantener Switch Monolítico:** Dejar el switch actual y solo extraer funciones auxiliares privadas.

## Resultado de la Decisión

Alternativa elegida: "Descomposición en Métodos Semánticos Especializados de Caso de Uso"

Justificación:
Provee el balance perfecto entre simplicidad suficiente (KISS) y alta cohesión. Al exponer métodos semánticos explícitos, los controladores REST y listeners AMQP invocan exactamente la operación que necesitan, eliminando la necesidad de fabricar DTOs genéricos con strings de estado para forzar la entrada a un switch.

### Consecuencias Positivas

* Eliminación del switch monolítico de control en la capa de aplicación.
* Claridad en contratos de interfaz: cada método exige únicamente los parámetros pertinentes a su transición.
* Aislamiento de fallos: un cambio en la lógica de fallo de entrega no afecta la asignación inicial.

### Consecuencias Negativas

* Requiere actualizar la interfaz `IDonacionesIndependientesService` y los controladores que la consumen.

### Validación

Se valida mediante:
1. `IDonacionesIndependientesService` expone métodos semánticos individuales.
2. Cobertura unitaria de cada método en `DonacionesIndependientesServiceTest` con verificación de llamadas Mockito a Feign específicas por caso.

## Análisis de Alternativas

### Métodos Semánticos Especializados

#### Pros
* Código limpio, autodocumentado y testeable.
* Facilidad de mantenimiento a largo plazo.

#### Contras
* Mayor cantidad de métodos en la interfaz del servicio.

### Patrón Command / Handler

#### Pros
* Desacoplamiento extremo a nivel de clase.

#### Contras
* Sobrecarga de indirección y boilerplate innecesario para el tamaño actual del servicio.

### Mantener Switch

#### Pros
* Mantiene un único punto de entrada en la interfaz.

#### Contras
* Código propenso a regresiones y antipatrón clásico de mantenimiento.