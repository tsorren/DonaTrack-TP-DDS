# Identificación del estado actual de una DonacionIndependiente sin instanceof

- Status: proposed
- Date: 2026-06-16
- Deciders: Tadeo Sorrentino, Sofia Deane
- Tags: patrones-de-diseño, estado, donaciones-independientes

## Contexto y Problema

`DonacionIndependienteRepository` necesita filtrar donaciones que estén en un estado particular (por ejemplo, "en depósito"). La implementación original usaba `instanceof EnDeposito`, que es una construcción específica de Java que no se puede trasladar a otro lenguaje y rompe el encapsulamiento: el que filtra necesita conocer la clase concreta del estado. ¿Cómo identificar el estado actual de una `DonacionIndependiente` sin depender de la clase concreta?

## Atributos de Calidad y Drivers de Decisión

* Independencia del lenguaje: la lógica no debe depender de mecanismos de reflexión propios de Java
* Encapsulamiento: quien filtra no debería conocer los estados concretos
* Mantenibilidad: agregar un estado nuevo no debería requerir cambios en múltiples lugares ni permitir inconsistencias silenciosas
* Cohesión: la pregunta "¿en qué estado estoy?" debería responderse desde la propia entidad o estado

## Alternativas Consideradas

* `instanceof` sobre el estado concreto
* Enum `TipoEstadoDonacion` con `getTipo()` en la interfaz `EstadoDonacion`
* Método booleano en `DonacionIndependiente` respaldado por el enum (`estaEnDeposito()`)
* Método booleano en la interfaz `EstadoDonacion` con default `false`, override en cada estado concreto

## Resultado de la Decisión

Alternativa elegida: pendiente de discusión grupal.

### Validación

La implementación elegida debe permitir que `DonacionIndependienteRepository.findEnDeposito()` filtre correctamente sin importar `EnDeposito` ni ninguna otra clase de estado concreta. Se valida que agregar un nuevo estado no requiera cambios fuera del propio estado y que el compilador detecte implementaciones incompletas.

## Análisis de Alternativas

### `instanceof` sobre el estado concreto [DESCARTADO]

```java
.filter(d -> d.getEstadoActual() instanceof EnDeposito)
```

#### Pros

* Sin cambios en el modelo de estados
* Código directo y facil de leer

#### Contras

* Depende de reflexión propia de Java: no es trasladable a otro lenguaje
* El repository conoce la clase concreta `EnDeposito`, rompiendo el encapsulamiento
* Si se renombra o reemplaza `EnDeposito`, el filtro falla silenciosamente en tiempo de ejecución

---

### Enum `TipoEstadoDonacion` con `getTipo()` en la interfaz

```java
// EstadoDonacion
TipoEstadoDonacion getTipo(); //metodo abstracto en la clase padre

// EnDeposito
public TipoEstadoDonacion getTipo() { return TipoEstadoDonacion.EN_DEPOSITO; } //definido por cada hijo

// Repository
.filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.EN_DEPOSITO)
```

#### Pros

* Sin reflexión de Java: el estado se identifica a través de un contrato explícito
* El compilador obliga a cada estado nuevo a implementar `getTipo()`, evitando inconsistencias silenciosas
* El enum sirve como inventario explícito de todos los estados posibles

#### Contras

* El repository conoce `TipoEstadoDonacion`, es decir, conoce el vocabulario interno de los estados
* Agregar un estado nuevo requiere dos cambios coordinados: la clase del estado y el enum — si el enum lo mantiene otra persona, puede haber desincronización
* El enum puede crecer y convertirse en un punto de acoplamiento si se usa desde muchos lugares

---

### Método booleano en `DonacionIndependiente` respaldado por el enum

```java
// DonacionIndependiente
public boolean estaEnDeposito() {
    return getEstadoActual == TipoEstadoDonacion.EN_DEPOSITO;
}

// Repository
.filter(DonacionIndependiente::estaEnDeposito)
```

#### Pros

* El repository no conoce ni el enum ni los estados: solo le pregunta a la entidad
* Semántica clara y alineada con Tell, Don't Ask
* El enum queda encapsulado dentro de `DonacionIndependiente`

#### Contras

* Hereda el problema del enum: agregar un estado nuevo requiere agregar el valor al enum Y el método booleano a `DonacionIndependiente`
* Si despues hay que implementar algoritmos que requieran filtrar por muchos/todos los estados, `DonacionIndependiente` acumula muchos métodos `estaEnX()`

---

### Método booleano en la interfaz `EstadoDonacion` con default `false`

```java
// EstadoDonacion
default boolean estaEnDeposito() { return false; }

// EnDeposito
@Override
public boolean estaEnDeposito() { return true; }

// DonacionIndependiente
public boolean estaEnDeposito() { return estadoActual.estaEnDeposito(); }

// Repository
.filter(DonacionIndependiente::estaEnDeposito)
```

#### Pros

* Sin enum, sin reflexión: cada estado responde por sí mismo
* El acoplamiento es mínimo: nadie fuera de `DonacionIndependiente` conoce los estados concretos
* Agregar un estado nuevo no rompe nada: el `default false` actúa como valor seguro

#### Contras

* El `default false` puede ocultar un bug: si se agrega un estado que debería considerarse "en depósito" y se olvida el override, el compilador no avisa
* Agregar un nuevo tipo de consulta (por ejemplo `estaEnTraslado()`) requiere modificar la interfaz `EstadoDonacion` y potencialmente todos sus implementadores
* La interfaz `EstadoDonacion` mezcla responsabilidades: comportamiento (transiciones) e identidad (consultas booleanas)

## Links

- Refinement de [herencia-repositories-en-memoria](20260616-herencia-repositories-en-memoria.md)
