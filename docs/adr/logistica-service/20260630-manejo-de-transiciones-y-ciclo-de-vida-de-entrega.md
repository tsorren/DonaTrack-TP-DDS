# Manejo de transiciones y ciclo de vida de Entrega
- Status: accepted
- Date: 2026-06-30
- Deciders: Decisión Grupal

## Contexto y Problema
Se requiere modelar el ciclo de vida de una entrega (que pasa por estados como PENDIENTE, EN_TRASLADO, ENTREGADA, NO_RECIBIDA, REVISION). El dilema principal radica en definir la estrategia de diseño para gestionar los cambios de estado del negocio y las reglas asociadas, garantizando además que quede un registro histórico preciso de cada transición.

## Atributos de Calidad y Drivers de Decisión
* Mantenibilidad
* Auditabilidad/Trazabilidad
* Consistencia de datos

## Alternativas Consideradas
* Métodos de Comportamiento Explícitos con Enum e Historial
* Implementación del Patrón State

## Resultado de la Decisión

Alternativa elegida: "Métodos de Comportamiento Explícitos con Enum e Historial"

Justificación:
Se optó por esta alternativa debido a que el ciclo de vida de la entrega se beneficia más de la simplicidad y la trazabilidad directa que de la flexibilidad polimórfica del patrón State. Al encapsular las transiciones en métodos semánticos claros (ej: negarEntrega()) y centralizar el registro del historial a través de registrarCambioEstado(), resolvemos el requerimiento de auditoría de forma nativa y limpia, manteniendo un modelo de datos plano y fácil de persistir.

### Consecuencias Positivas
* El modelo es sumamente intuitivo y fácil de testear mediante pruebas unitarias. La persistencia en la base de datos es directa.

### Consecuencias Negativas
* Si en el futuro aparecen comportamientos extremadamente complejos y diferenciados por cada estado, se tendrá que refactorizar hacia polimorfismo o delegados para evitar métodos gigantes en la entidad.

## Análisis de Alternativas

### Métodos de Comportamiento Explícitos con Enum e Historial

Modelar las transiciones directamente en la entidad raíz mediante métodos públicos dirigidos por el negocio (iniciarRuta(), confirmarEntrega(), etc.). El estado se representa con un Enum (EstadoEntrega) y cada cambio invoca un método privado (registrarCambioEstado) que apendiza un objeto CambioEstadoEntrega a una lista histórica.

#### Pros
* Alta legibilidad
* Simplicidad
* Facilidad para auditoria

#### Contras
* Si las reglas de negocio específicas de cada estado crecen exponencialmente, la clase principal podría volverse muy grande (violación potencial de Single Responsibility Principle).

### Implementación del Patrón State

Delegar el comportamiento dependiente del estado a clases separadas que implementen una interfaz común para cada estado (EstadoPendiente, EstadoEnTraslado, etc.).

#### Pros
* Cumple estrictamente con el principio de Abierto/Cerrado

#### Contras
* Complejidad innecesaria: Genera una explosión de clases para estados cuyas transiciones son lineales o lógicas simples.
* Dificultad de persistencia y tracking: Complejiza el mapeo con ORMs y el registro del historial de auditoría (CambioEstadoEntrega), dado que el estado se convierte en un objeto polimórfico dinámico.
