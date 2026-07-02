# Gestión de Donaciones a la clase abstracta Necesidad para habilitar Polimorfismo
- Status: rejected
- Date: 2026-06-12
- Deciders: Decisión Grupal

## Contexto y Problema
la gestión de donaciones independientes está fragmentada y acoplada de forma inconsistente: NecesidadExtraordinaria las maneja de forma directa en una lista local, mientras que NecesidadRecurrente delega esa lista dentro de cada ciclo en PeriodoNecesidad. Esto impide que las capas superiores (Servicios y Controladores) puedan operar de forma agnóstica con cualquier tipo de necesidad, rompiendo la transparencia y obligando a realizar preguntas de tipo (instanceof o casteos) para interactuar con las donaciones.

## Alternativas Consideradas
* Mantener la lógica distribuida
* Subir la lista y los métodos de asignación a la clase abstracta Necesidad

## Resultado de la Decisión

Alternativa elegida: "Subir la lista y los métodos de asignación a la clase abstracta Necesidad"

Justificación:
- Polimorfismo mediante Asignable: la introducción de la interfaz Asignable desacopla por completo la lógica de negocio de las capas externas. El NecesidadesService ya no necesita preguntar mediante condicionales  de qué tipo es cada necesidad para operar con ella; simplemente interactúa con el contrato de la interfaz de forma homogénea.
- Eliminación de la lista intermedia en la Entidad: al tener el atributo entidad dentro de Necesidad, cada necesidad conoce a quién pertenece, lo que simplifica el filtrado en la base de datos en memoria y limpia a la clase EntidadBeneficiaria de colecciones pesadas.
- Reutilización y Consistencia.

## Análisis de Alternativas

### Mantener la lógica distribuida

(Estado anterior)

#### Pros
* No requería refactorizar el código de la Entrega 1 ni migrar datos en memoria.

#### Contras
* Rompe el polimorfismo. Fuerza al NecesidadesService a conocer detalles internos de las subclases, generando alto acoplamiento y vulnerando el principio de Abierto/Cerrado .

### Subir la lista y los métodos de asignación a la clase abstracta Necesidad

Agregar entidad a Necesidad. Agregar interfaz "Asignable" para que cada Necesidad pueda reconocer de qué tipo es.

#### Pros
* Habilita el manejo polimórfico real de la jerarquía.
* El servicio interactúa exclusivamente con la interfaz pública de Necesidad.
* Elimina la duplicación de lógica de validación de asignación y cálculo de cantidades acumuladas.

#### Contras
* Requiere agregar los métodos correspondientes en NecesidadRecurrente, NecesidadExtraordinaria y en DonacionIndependiente.
