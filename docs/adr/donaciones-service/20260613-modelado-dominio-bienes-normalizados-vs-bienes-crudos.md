# Modelado del Dominio: Bienes Normalizados vs. Bienes Crudos

- Status: accepted
- Date: 2026-06-13
- Deciders: Decisión Grupal
- Tags: dominio, normalizacion, segmentacion

## Contexto y Problema

En el subsistema de normalización y segmentación de DonaTrack, los bienes cargados por los usuarios (`Bien`) deben ser procesados para asignarles una categoría y subcategoría estándar. Durante el proceso de segmentación, se generan donaciones independientes para cada ítem de donación normalizado.

El problema radica en cómo representar la información de normalización del bien en el modelo de dominio. Un `Bien` original cargado por el usuario es inmutable en cuanto a su descripción y estado inicial. Sin embargo, para realizar la segmentación, se requiere asociar el bien con su subcategoría asignada en la etapa de normalización. Se debe decidir si se modifica directamente la clase de dominio `Bien` agregando la información de normalización o si se introduce una entidad separada que actúe como envoltura o wrapper.

## Atributos de Calidad y Drivers de Decisión

* Inmutabilidad
* Mantenibilidad
* Trazabilidad

## Alternativas Consideradas

* Modificación Directa de la Entidad de Dominio (Bienes Crudos Mutables)
* Entidad Separada / Bienes Normalizados Desacoplados (BienNormalizado como Wrapper)

## Resultado de la Decisión

Alternativa elegida: "Entidad Separada / Bienes Normalizados Desacoplados (BienNormalizado como Wrapper)"

Justificación:
Esta opción preserva la inmutabilidad de la información original ingresada por el donante, la cual representa fielmente lo que se recibió físicamente en el sistema. Al modelar `BienNormalizado` como una entidad separada que contiene una referencia al `Bien` crudo, se evita contaminar el registro original con datos deducidos por algoritmos automáticos o decisiones de operadores. Además, esta separación facilita el reprocesamiento histórico (volver a normalizar si cambian las reglas de negocio o los algoritmos) y mantiene un historial claro del ciclo de vida de la donación (Carga -> Normalización -> Segmentación).

### Consecuencias Positivas

* Se respeta el principio de inmutabilidad para los registros de donaciones crudas y sus bienes asociados.
* Permite volver a ejecutar el proceso de normalización sobre un mismo `Bien` original si los criterios o algoritmos cambian en el futuro.
* Facilita la auditoría al poder comparar directamente la descripción ingresada por el usuario con la categorización asignada por el sistema.

### Consecuencias Negativas

* Introduce mayor complejidad y cantidad de clases en el modelo de dominio (`BienNormalizado`, `ItemDonacionNormalizado`).
* Requiere de un mapeo intermedio adicional al momento de segmentar y transformar los ítems normalizados a las donaciones independientes resultantes.

### Validación

Se verificará la correcta creación y persistencia de las entidades `BienNormalizado` e `ItemDonacionNormalizado` sin alterar el estado de los objetos `Bien` e `ItemDonacion` originales a través de pruebas unitarias y de integración que cubran todo el ciclo de normalización y segmentación.

## Análisis de Alternativas

### Modificación Directa de la Entidad de Dominio (Bienes Crudos Mutables)

Consiste en incorporar los campos `subcategoria`, `estadoNormalizacion` y `confianza` directamente dentro de la clase `Bien` existente, mutando el objeto original a medida que avanza en su ciclo de vida.

#### Pros

* Mayor simplicidad inicial en el modelo, al evitar la creación de clases intermedias.
* Menor número de mapeos y transformaciones de datos en los flujos de normalización y segmentación.

#### Contras

* Violación de la inmutabilidad del registro histórico original: se pisa la información inicial con el resultado del procesamiento.
* Dificulta el reprocesamiento: si se necesita volver a clasificar un bien, se destruye la categorización anterior sin dejar registro del cambio de estado.
* Acopla la definición básica del bien con la lógica y el estado del proceso de normalización.

### Entidad Separada / Bienes Normalizados Desacoplados (BienNormalizado como Wrapper)

Consiste en mantener la clase `Bien` intacta e inmutable, representando el estado físico reportado en la carga. El proceso de normalización genera una nueva entidad `BienNormalizado` que envuelve al `Bien` original y le adjunta los metadatos de clasificación.

#### Pros

* Desacoplamiento total entre el registro de carga física y la lógica de clasificación del negocio.
* Posibilidad de realizar auditorías completas comparando la entrada del usuario vs. el resultado del sistema.
* Flexibilidad para cambiar la subcategoría asignada o reprocesar la normalización sin alterar la donación de origen.

#### Contras

* Aumento en el número de clases en el dominio y en la complejidad de las consultas para recuperar la información unificada del bien.
