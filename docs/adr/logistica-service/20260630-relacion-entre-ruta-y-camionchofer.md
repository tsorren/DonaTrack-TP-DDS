# Relacion entre Ruta y Camion/Chofer
- Status: accepted
- Date: 2026-06-30
- Deciders: Decisión Grupal
- Tags: logística

## Alternativas Consideradas
* Referencia Unidireccional
* Referencia Bidireccional por ID

## Resultado de la Decisión

Alternativa elegida: "Referencia Unidireccional"

## Análisis de Alternativas

### Referencia Unidireccional

Solo la Ruta conoce los IDs del Camion y del Chofer. Las clases Camion y Chofer no tienen ningún campo rutaActualId.

#### Pros
* Es un enfoque DDD, por lo que no habrá inconsistencias. La información de la asignación solo está en la ruta.
* Desacoplamiento de Chofer y Camion de Ruta.
* Se elimina de raíz cualquier posibilidad de bucles infinitos al convertir los objetos a JSON.

#### Contras
* Consultas más complejas.
* Menos intuitivo.

### Referencia Bidireccional por ID

En este modelo, la Ruta conoce el camionId, y el Camion también tiene un campo rutaActualId.

#### Pros
* La clase Camion puede tener un método asignarARuta() que valide su propio estado.

#### Contras
* Doble fuente de verdad: si una transacción falla a la mitad, puede ocurrir que la Ruta apunte al Camion, pero que el Camion no apunte a la Ruta, dejando los datos en un estado inconsistente.
