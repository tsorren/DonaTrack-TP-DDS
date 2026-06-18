# Strategy en GestorAlgoritmos

- Status: proposed
- Date: 2026-06-15
- Deciders: Sofia Deane
- Tags: matchmaking, patrones-de-diseño, algoritmos

## Contexto y Problema

`GestorAlgoritmos` necesita ejecutar dos algoritmos de asignación en secuencia y consolidar sus resultados. Ambos comparten la interfaz de `AlgoritmoAsignacion`. La pregunta es cómo almacenar y acceder a ellos dentro del gestor de forma que el código sea legible y el sistema sea extensible si en el futuro se agregan más algoritmos.

## Atributos de Calidad y Drivers de Decisión

* Legibilidad: el código que invoca a cada algoritmo debe dejar claro qué algoritmo se está ejecutando
* Extensibilidad: agregar un tercer algoritmo no debería requerir cambios fuera de `GestorAlgoritmos`
* Cohesión: la responsabilidad de conocer qué algoritmos existen y en qué orden correr debe estar en un único lugar

## Alternativas Consideradas

* Hardcodear las llamadas a las clases concretas en `ejecutar()`
* `List<AlgoritmoAsignacion>` con acceso por índice (`get(0)`, `get(1)`)
* `List<AlgoritmoAsignacion>` con métodos privados con nombre (`algoritmoPorCompatibilidad()`, `algoritmoPorPrioridad()`)

## Resultado de la Decisión

Alternativa elegida: "`List<AlgoritmoAsignacion>` con métodos privados con nombre"

Justificación:
Mantener los algoritmos en una lista respeta el patrón Strategy y permite iterar sobre ellos si en el futuro se agrega un tercer algoritmo. El acceso por índice crudo (`get(0)`) no es legible, así que se encapsula en métodos privados con nombre declarativo que revelan la intención (`algoritmoPorCompatibilidad()`, `algoritmoPorPrioridad()`). De esta forma se combina la flexibilidad de la lista con la claridad de nombres explícitos.

### Consecuencias Positivas

* `ejecutar()` lee de forma declarativa qué hace cada llamada
* La lista permite en el futuro iterar sobre todos los algoritmos sin cambiar `ejecutar()`
* Agregar un algoritmo solo requiere extender la lista en el constructor y agregar su método de acceso

### Consecuencias Negativas

* Los métodos de acceso nombrados quedan acoplados a los índices de la lista; si se reordena la lista hay que actualizar los métodos

### Validación

Se verifica que `GestorAlgoritmos` pueda instanciarse y ejecutarse correctamente en los tests de integración. El comportamiento de consolidación se testea con los escenarios de intersección y unión.

## Análisis de Alternativas

### Hardcodear las llamadas a las clases concretas en `ejecutar()`

```java
new AlgoritmoCompatibilidadSemantica(comparadorTexto).ejecutar(...)
new AlgoritmoPrioridadSubAtendidos().ejecutar(...)
```

#### Pros

* Código directo y sin indirección

#### Contras

* Viola el principio Open/Closed: agregar un algoritmo requiere modificar `ejecutar()`
* No permite tratar los algoritmos de forma polimórfica
* Las instancias no se pueden inyectar ni reemplazar fácilmente para testing

### `List<AlgoritmoAsignacion>` con acceso por índice

La lista se construye en el constructor y se accede con `algoritmos.get(0)` y `algoritmos.get(1)`.

#### Pros

* Código compacto

#### Contras

* `get(0)` y `get(1)` no comunican nada sobre qué algoritmo es cada uno
* Un error de índice no se detecta en compilación

### `List<AlgoritmoAsignacion>` con métodos privados con nombre

La lista se construye en el constructor. Se agregan métodos privados `algoritmoPorCompatibilidad()` y `algoritmoPorPrioridad()` que encapsulan el `get(i)`.

#### Pros

* El sitio de llamada es legible y declarativo
* La lista como estructura subyacente queda como detalle de implementación encapsulado

#### Contras

* Requiere mantener sincronizados los métodos de acceso con el orden de la lista
