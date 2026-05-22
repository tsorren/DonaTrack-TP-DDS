# Modelado de Categorías y Subcategorías

- Status: proposed
- Date: 2026-05-21
- Deciders: Decisión Grupal

## Contexto y Problema

Debemos decidir cómo clasificar los bienes donados y las necesidades registradas por las entidades beneficiarias.

La consigna plantea que los bienes pertenecen a una categoría general y que para cada categoría existen múltiples
subcategorías. Además, indica que la subcategoría constituye la unidad mínima de asignación dentro del sistema.

## Atributos de Calidad y Drivers de Decisión

* Claridad del modelo de dominio
* Mantenibilidad
* Simplicidad

## Alternativas Consideradas

* Modelar categoría y subcategoría como atributos simples dentro de `Bien` y `Necesidad`, por ejemplo mediante `String`
  o `Enum`, evitando crear clases propias.

* Usar `Categoria` y `Subcategoria`, dejando a `Subcategoria` como menor nivel del modelo.

## Resultado de la Decisión

Alternativa elegida: "Usar `Categoria` y `Subcategoria`, dejando a `Subcategoria` como menor nivel del modelo".

Justificación:

Se decidió modelar `Categoria` como agrupador general y `Subcategoria` como la unidad concreta sobre la que se donan,
necesitan y asignan bienes.

Por ejemplo, una categoría puede ser `Alimentos` y una subcategoría puede ser `Fideos secos`; o una categoría puede ser
`Vestimenta` y una subcategoría puede ser `Remeras`. No se modelan niveles más específicos como `fideos codito`,
`fideos moñito` o `remera en V`, porque eso haría crecer el modelo con detalles demasiado particulares. Si ese dato hace
falta, puede quedar en la descripción del bien.

Además, se descartó modelarlas como atributos simples porque `Categoria` concentra información propia del dominio que
luego reutilizan sus subcategorías, como si requiere estado de uso, si admite vencimiento y qué unidad de medida
corresponde.

## Consecuencias

* El modelo queda simple y alineado con la consigna.
* La asignación de donaciones contra necesidades se hace usando subcategorías.
* Se evita una jerarquía demasiado específica de bienes.
* Algunos detalles particulares del producto no quedan estructurados como clases, sino como descripción del bien.

## Validación

Se valida revisando que `Bien` y `Necesidad` se asocien a `Subcategoria`, y que `Subcategoria` mantenga una referencia a
su `Categoria`, como figura en el diagrama de clases.