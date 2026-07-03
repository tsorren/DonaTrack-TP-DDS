# Uso de AliasSubcategoria en los Normalizadores Semánticos

- Status: accepted
- Date: 2026-06-15
- Deciders: Sofia Deane
- Tags: matchmaking, normalizacion, dominio

## Contexto y Problema

`NormalizadorSemantico` y `NormalizadorSemanticoBien` necesitan traducir términos coloquiales a nombres canónicos de subcategorías (ej: "celu" → "Celular", "spaghetti" → "Pasta"). La pregunta es dónde definir ese mapeo: ¿hardcodeado dentro del normalizador, o a partir de `AliasSubcategoria`, que ya existe en el dominio con exactamente esa información?

## Atributos de Calidad y Drivers de Decisión

* Consistencia: los alias deben tener una única fuente de verdad en todo el sistema
* Mantenibilidad: agregar un nuevo alias no debería requerir modificar código de infraestructura
* Cohesión: el concepto de "alias de una subcategoría" pertenece al dominio, no a la capa de infraestructura

## Alternativas Consideradas

* Hardcodear el mapa de alias como constantes dentro del normalizador
* Construir el mapa a partir de una lista de `AliasSubcategoria` recibida por constructor

## Resultado de la Decisión

Alternativa elegida: "Construir el mapa a partir de una lista de `AliasSubcategoria` recibida por constructor"

Justificación:
`AliasSubcategoria` ya modela exactamente la relación entre un término alternativo y su subcategoría canónica. Duplicar esa información como strings hardcodeados en el normalizador rompe la regla de única fuente de verdad: si se agrega un nuevo alias en el dominio, también habría que acordarse de actualizarlo en el normalizador. Al inyectar la lista por constructor, el normalizador queda desacoplado de los datos concretos y la responsabilidad de qué aliases existen queda exclusivamente en el dominio.

### Consecuencias Positivas

* Agregar o modificar un alias solo requiere cambiar los datos del dominio, no el código del normalizador
* El normalizador es más fácil de testear: se le pasan los aliases que necesita el test sin depender de una configuración fija
* Se elimina la duplicación entre el dominio y la infraestructura

### Consecuencias Negativas

* El normalizador ya no puede ser un `@Component` de Spring sin un mecanismo que provea la lista de `AliasSubcategoria` como bean; requiere wiring explícito

### Dónde viven los aliases: `Subcategoria` como dueña

Una vez decidido que los aliases pertenecen al dominio, surgió la pregunta de dónde almacenarlos concretamente. Las opciones consideradas fueron un `AliasSubcategoriaRepository` separado o una lista dentro de `Subcategoria` misma.

Se eligió que `Subcategoria` sea la dueña de sus aliases, siguiendo el principio de responsabilidad única: `Subcategoria` es la entidad que mejor conoce cómo se la puede nombrar. Un repositorio separado solo existiría para guardar datos que conceptualmente le pertenecen a otra entidad, lo que dispersa la responsabilidad.

`Subcategoria` expone los métodos `agregarAlias(String)`, `removerAlias(String)` y `tieneAlias(String)` para gestionar su lista de aliases. Para construir el normalizador se recolectan los aliases de todas las subcategorías relevantes y se le pasan por constructor.

### Validación

El normalizador se instancia en tests pasando una lista de `AliasSubcategoria` construida a mano desde subcategorías de prueba, verificando que traduce correctamente los alias al nombre canónico de la subcategoría. Los métodos de alias de `Subcategoria` se validan en `SubcategoriaTest`.

## Análisis de Alternativas

### Hardcodear el mapa de alias como constantes dentro del normalizador

Los términos alternativos se definen directamente como `String` en el constructor del normalizador.

```java
semanticMap.put("celu", "celular");
semanticMap.put("movil", "celular");
```

#### Pros

* Simple, sin dependencias externas
* No requiere wiring de Spring

#### Contras

* Duplica información que ya existe en `AliasSubcategoria`
* Agregar un alias obliga a modificar código de infraestructura
* Si el dominio evoluciona (ej: se renombra una subcategoría), el normalizador queda desincronizado silenciosamente

### Construir el mapa a partir de `AliasSubcategoria` recibida por constructor

El normalizador recibe una `List<AliasSubcategoria>` y construye el mapa iterando sobre ella.

```java
for (AliasSubcategoria aliasSubcategoria : aliases) {
    semanticMap.put(aliasSubcategoria.getAlias(), aliasSubcategoria.getSubcategoria().getNombre());
}
```

#### Pros

* Los alias tienen una única fuente de verdad: el dominio
* El normalizador es agnóstico a los datos concretos
* Facilita el testing con alias arbitrarios

#### Contras

* Requiere que quien construya el normalizador provea la lista de aliases (repositorio o configuración)
