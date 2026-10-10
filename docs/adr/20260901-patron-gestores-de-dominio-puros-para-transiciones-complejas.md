# Patrón Gestores de Dominio Puros para Transiciones Complejas

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: ddd, dominio, gestores, patrones, logistica

## Contexto y Problema

A medida que las entidades de dominio crecen en complejidad (por ejemplo, coordinar el inicio de una ruta que involucra validar el estado de un camión, la disponibilidad de un chofer y la consistencia de múltiples entregas), surgen dos extremos no deseados:
1. **Entidades Dios (God Entities):** Acoplar la entidad `Ruta` para que conozca y mute directamente el estado interno de `Camion` y `Chofer`, rompiendo los límites de los agregados.
2. **Servicios de Aplicación Obesos (Fat Services):** Mover la lógica de validación y transiciones a la capa `@Service`, llenándola de `switch` y condicionales procedurales, convirtiendo las entidades en modelos anémicos.
Se requiere un patrón que orqueste transiciones complejas entre agregados preservando la pureza del modelo sin acoplar entidades entre sí ni inflar los servicios de aplicación.

## Atributos de Calidad y Drivers de Decisión

* **Alta Cohesión:** Mantener las reglas de negocio encapsuladas en la capa de dominio.
* **Separación de Responsabilidades (SRP):** Los Application Services solo deben orquestar operaciones externas (I/O, persistencia, publicación); el dominio debe ejecutar las transiciones.
* **Testeabilidad:** Permitir probar algoritmos de transición de estados sin levantar el contexto de Spring Boot ni usar mocks.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 4 y Auditoría Final de `logistica-service`.
* **Hallazgo:** En `logistica-service`, los servicios `CamionesService`, `EntregasService` y `RutasService` contenían switches extensos y condicionales de validación cruzada. La solución adoptada fue crear **Gestores de Dominio Puros** (`GestorDeEntregas`, `GestorDeRutas`, `GestorDeCamiones`) como clases `final` con constructor privado, métodos estáticos de dominio y tipos de entrada sellados (*sealed interfaces* como `SolicitudTransicionEntrega`), extrayendo toda la complejidad de los services hacia el dominio.

## Alternativas Consideradas

* **Gestores de Dominio Puros (Domain Services utilitarios sin Spring):** Clases finales en el paquete de dominio, sin anotaciones `@Component` ni `@Service`, que implementan métodos puros que reciben agregados, validan invariantes de interacción y coordinan transiciones.
* **Domain Services con Spring (`@Service` de Dominio):** Instanciar los gestores como Beans de Spring inyectados en los Application Services.
* **Encapsulación Forzada dentro de un Solo Agregado:** Forzar a `Ruta` a absorber `Camion` y `Chofer` como partes internas de un macro-agregado.

## Resultado de la Decisión

Alternativa elegida: "Gestores de Dominio Puros (Domain Services utilitarios sin Spring)"

Justificación:
Los Gestores Puros respetan la definición canónica de Eric Evans de un Domain Service cuando una operación involucra múltiples agregados y no pertenece naturalmente a ninguno de ellos. Al implementarse sin Spring, se garantiza que el dominio permanezca 100% libre de frameworks, instantáneamente testeable con pruebas unitarias determinísticas en milisegundos y reutilizable.

### Consecuencias Positivas

* Application Services extraordinariamente delgados y legibles: reciben el DTO, buscan entidades, invocan al Gestor, persisten y publican eventos.
* Erradicación de switches extensos en capas de aplicación.
* Cohesión máxima en transiciones complejas mediante contratos explícitos (*sealed interfaces* y polimorfismo).

### Consecuencias Negativas

* Incremento en la cantidad de clases dentro de los paquetes de dominio.

### Validación

Se valida mediante:
1. `GestorDeEntregas.java`, `GestorDeRutas.java` y `GestorDeCamiones.java` no contienen anotaciones de Spring (`@Component`, `@Autowired`).
2. Constructores privados para impedir instanciación innecesaria.
3. Tests unitarios dedicados que validan todas las bifurcaciones de transición sin dependencias de base de datos ni Spring Context.

## Análisis de Alternativas

### Gestores de Dominio Puros

#### Pros
* Dominio 100% puro y portable.
* Tests unitarios ultrarrápidos y sin mocks de infraestructura.
* Responsabilidades estrictamente delimitadas.

#### Contras
* Requiere comprender la diferencia entre Application Service (orquestador con I/O) y Domain Service (lógica pura sin I/O).

### Domain Services con Spring

#### Pros
* Familiaridad para programadores acostumbrados a inyectar todo como Beans.

#### Contras
* Contamina el modelo de dominio con anotaciones del framework Spring.
* Tiende a tentar a los desarrolladores a inyectar repositorios dentro del gestor, desdibujando los límites de capas.

### Macro-Agregado

#### Pros
* Toda la lógica vive dentro de una sola clase raíz.

#### Contras
* Violación grave de DDD: crea cuellos de botella de concurrencia y relaciones masivas en memoria.