# Relacion entre Ruta y Camion
- Status: accepted
- Date: 2026-06-30
- Deciders: Decisión Grupal
- Tags: logística

## Alternativas Consideradas
* Referencia Unidireccional
* Referencia Bidireccional por ID (Modelo Híbrido)

## Resultado de la Decisión

Alternativa elegida: "Referencia Bidireccional por ID (Modelo Híbrido)"

Justificación:
Equilibra el uso del modelo DDD con la necesidad de tener un dominio rico, permitiendo al camión validar su propio estado, a costa de un acoplamiento de datos mínimo (la referencia por ID).

### Consecuencias Positivas
* Mejora mantenibilidad y lógica de consulta.

### Consecuencias Negativas
* La responsabilidad de la consistencia recae en el service.

## Análisis de Alternativas

### Referencia Unidireccional

Referencia Unidireccional (Modelo Puro DDD): La entidad Camion no tiene ninguna referencia a Ruta. La disponibilidad del camión se deduce externamente consultando el RutaRepository.

#### Pros
* Es un enfoque DDD, por lo que no habrá inconsistencias. La información de la asignación solo está en la ruta.
* Desacoplamiento de Chofer y Camion de Ruta.
* Se elimina de raíz cualquier posibilidad de bucles infinitos al convertir los objetos a JSON.
* Cumple rigurosamente con la regla de que un agregado no debe tener referencias a otro.

#### Contras
* Consultas más complejas.
* Menos intuitivo.
* Toda la lógica de validación de estados del camión se "fuga" a la capa de Service, haciendo que esta sea más compleja y el dominio más pobre.

### Referencia Bidireccional por ID (Modelo Híbrido)

La entidad Camion mantiene un enum de estado (EstadoCamion) y un campo rutaActualId para saber a qué ruta está asignado, gestionando sus propias transiciones de estado.

#### Pros
* La lógica de negocio y las reglas de validación del ciclo de vida del camión residen en la propia entidad Camion. La entidad protege su propia consistencia.
* Consultas de disponibilidad simples.
* Al usar un UUID en lugar de una referencia de objeto completa, se evita el acoplamiento fuerte y los problemas de serialización JSON (bucles infinitos).

#### Contras
* Existe un riesgo, aunque bajo, de inconsistencia al introducir una segunda fuente de datos.
