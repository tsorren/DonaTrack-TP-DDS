# Estrategia de modelado para los estados operativos y administrativos del Camión
- Status: accepted
- Date: 2026-06-30
- Deciders: Decisión Grupal
- Tags: DDD, Patrones de Estado

## Contexto y Problema
El sistema requiere gestionar el flujo de las entregas y las rutas. Un camión no puede ser eliminado físicamente de la base de datos (borrado lógico/baja blanda) para preservar la trazabilidad histórica de los recorridos y entregas que realizó.

Además, se necesita saber si el camión está apto para iniciar una ruta, si actualmente está realizando una, o si el administrador lo dejó fuera de servicio (ya sea por mantenimiento o baja definitiva).

## Atributos de Calidad y Drivers de Decisión
* Consistencia del dominio 
* Trazabilidad
* Extensibilidad

## Alternativas Consideradas
* Enum de estados
* Separar en Atributos Distintos (Enum Operativo + Booleano Administrativo)
* Enum + referencia a Ruta activa

## Resultado de la Decisión

Alternativa elegida: "Enum + referencia a Ruta activa"

Justificación:
Se eligió esta alternativa porque permite modelar el ciclo de vida del camión utilizando un único estado, evitando inconsistencias derivadas de múltiples atributos de estado. Además, la referencia por identificador (UUID) a la ruta activa brinda la información necesaria para validar las operaciones del dominio sin generar un acoplamiento fuerte entre los agregados Camion y Ruta.

Esta solución preserva la trazabilidad histórica, mantiene encapsuladas las transiciones de estado dentro del agregado Camion y resulta consistente con un enfoque de DDD, donde los agregados se relacionan mediante identidades en lugar de referencias directas.

### Consecuencias Positivas
* Las reglas de transición quedan centralizadas en el agregado Camion, mientras que la planificación y coordinación de rutas permanece en los servicios de aplicación.

### Consecuencias Negativas
* Es necesario garantizar la consistencia entre el estado del camión y la referencia a la ruta activa durante las operaciones de asignación y finalización de recorridos.

## Análisis de Alternativas

### Enum de estados

Representar el estado del camión mediante un único enum.  Ejemplo:  DISPONIBLE EN_RUTA EN_MANTENIMIENTO DESHABILITADO  Las transiciones válidas se controlan desde el dominio o desde los services.

#### Pros
* Los estados son mutuamente excluyentes.
* Evita combinaciones inconsistentes.
* Centraliza la lógica de transición.
* Facilita agregar nuevos estados.
* No requiere atributos adicionales para representar disponibilidad.

#### Contras
* Requiere definir correctamente las transiciones.
* Algunas validaciones dependen del contexto (por ejemplo, existencia de una ruta activa).

### Separar en Atributos Distintos (Enum Operativo + Booleano Administrativo)

Tener un enum para el flujo del día a día (DISPONIBLE, EN_RUTA, EN_MANTENIMIENTO) y un booleano estaActivo (o estaHabilitado) para la baja lógica del administrador.

#### Pros
* Separa claramente el estado operativo del ciclo de vida administrativo.
* Facilita la implementación de la baja lógica sin perder la trazabilidad histórica.
* Permite realizar consultas diferenciadas sobre camiones activos e inactivos.

#### Contras
* Introduce dos fuentes de verdad para el estado del camión.
* Requiere validar que ambos atributos sean consistentes (por ejemplo, impedir un camión inactivo en estado EN_RUTA).
* Incrementa la complejidad de las reglas de transición y validación del dominio.

### Enum + referencia a Ruta activa

Representar el estado del camión mediante un único enum (DISPONIBLE, EN_RUTA, EN_MANTENIMIENTO, DESHABILITADO) y mantener una referencia opcional al identificador (UUID) de la ruta que está realizando.  El estado continúa siendo la única fuente de verdad para las transiciones, mientras que la referencia a la ruta permite conocer el recorrido activo y realizar validaciones sin establecer una dependencia directa entre los agregados Camion y Ruta.

#### Pros
* Mantiene una única fuente de verdad para el estado del camión.
* Preserva un bajo acoplamiento entre agregados al almacenar únicamente el identificador de la ruta.
* Facilita validar la asignación y liberación de rutas desde el dominio.
* Permite conocer rápidamente la ruta activa del camión.
* Conserva la trazabilidad sin requerir referencias bidireccionales.

#### Contras
* Requiere mantener sincronizados el estado del camión y el identificador de la ruta.
* Agrega un atributo adicional cuya consistencia debe garantizarse mediante las transiciones del dominio.
* Algunas validaciones continúan dependiendo de la coordinación entre los servicios y el dominio.
