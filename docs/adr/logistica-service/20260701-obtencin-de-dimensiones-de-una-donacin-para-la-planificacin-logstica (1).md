# Obtención de dimensiones de una donación para la planificación logística
- Status: accepted
- Date: 2026-07-01
- Deciders: Decisión Grupal

## Contexto y Problema
El módulo de Logística necesita conocer el peso y el volumen de una donación para verificar si puede ser transportada por un camión. Sin embargo, el dominio de Donaciones únicamente administra la información propia de los bienes (tipo, cantidad, unidad de medida y subcategoría), sin incluir datos físicos como peso o volumen.

Era necesario definir dónde debía realizarse la conversión entre los datos de la donación y las dimensiones utilizadas por Logística, procurando mantener una adecuada separación de responsabilidades entre ambos módulos.

## Alternativas Consideradas
* Incorporar peso y volumen unitario a Bien
* Calcular dimensiones en Logística a partir de una clasificación del bien
* Utilizar dimensiones estándar

## Resultado de la Decisión

Alternativa elegida: "Calcular dimensiones en Logística a partir de una clasificación del bien"

Justificación:
Se eligió esta alternativa porque preserva la separación entre los dominios de Donaciones y Logística, evitando incorporar información de transporte dentro del modelo de negocio de las donaciones.

Además, concentra toda la lógica de cálculo en el módulo que realmente la necesita, disminuye el impacto sobre el código existente y permite modificar las reglas de conversión sin afectar al resto del sistema. La utilización de una abstracción para intercambiar la información también evita dependencias fuertes entre ambos módulos y facilita su evolución independiente.

### Consecuencias Positivas
* El dominio de Donaciones permanece desacoplado de la lógica logística.
* Las reglas de conversión pueden evolucionar sin modificar el modelo de Donaciones.
* Se reduce el impacto sobre entidades, DTOs y servicios existentes.
* El diseño mantiene una clara separación de responsabilidades.

### Consecuencias Negativas
* Es necesario mantener actualizado el catálogo de conversiones utilizado por Logística.
* La precisión de los cálculos depende de la calidad de dichas conversiones.

## Análisis de Alternativas

### Incorporar peso y volumen unitario a Bien

Cada bien almacena su peso y volumen unitario. Donaciones calcula el peso y volumen total multiplicando dichas dimensiones por la cantidad de unidades y envía los valores resultantes a Logística.

#### Pros
* Logística recibe toda la información ya procesada.
* El cálculo se realiza una única vez.

#### Contras
* Introduce información logística dentro del dominio de Donaciones.
* Incrementa el acoplamiento entre ambos servicios.
* Requiere modificar múltiples componentes existentes (DTOs, mappers, constructores y tests).
* El impacto de implementación resultó considerable.

### Calcular dimensiones en Logística a partir de una clasificación del bien

Los bienes conservan únicamente la información correspondiente al dominio de Donaciones. El módulo de Logística utiliza un catálogo de conversiones asociado a la subcategoría del bien para estimar el peso y el volumen cuando necesita planificar una entrega.  La comunicación entre ambos módulos se realiza mediante una abstracción compartida, evitando dependencias directas entre sus implementaciones.

#### Pros
* Mantiene separadas las responsabilidades de ambos dominios.
* Toda la lógica de cálculo permanece en Logística.
* Reduce el impacto sobre el modelo existente.
* Permite modificar las reglas de conversión sin afectar Donaciones.
* Favorece un bajo acoplamiento entre módulos.

#### Contras
* Logística debe mantener un catálogo de conversiones para las distintas subcategorías.
* Las dimensiones son estimadas y no un atributo propio del bien.

### Utilizar dimensiones estándar

Asignar un peso y volumen fijo por unidad o por caja para todas las donaciones.

#### Pros
* Implementación muy sencilla.
* No requiere modificar el modelo existente.

#### Contras
* Baja precisión.
* Escasa flexibilidad para distintos tipos de bienes.
