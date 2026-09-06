# Estrategia de Mapeo ORM y Herencia Relacional en Donaciones

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: persistencia, jpa, hibernate, state-pattern, orm, donaciones

## Contexto y Problema

`donaciones-service` es el microservicio con mayor complejidad de dominio en DonaTrack: posee jerarquías polimórficas (`Persona` implementada por `Humana` y `Juridica`; `Necesidad` implementada por `NecesidadExtraordinaria` y `NecesidadRecurrente`), objetos de valor inmutables complejos (`Bien`, `Direccion`), y un agregado central (`DonacionIndependiente`) gobernado por el **State Pattern con 7 clases concretas**. Mapear este modelo a PostgreSQL con JPA/Hibernate 6 requiere decisiones de ingeniería cuidadosas: un mapeo ingenuo que cree tablas artificiales para cada clase de estado o que elija la estrategia de herencia incorrecta generará tablas llenas de valores nulos o consultas con decenas de `JOIN`s que destruirán el rendimiento del algoritmo de matching.

## Atributos de Calidad y Drivers de Decisión

* **Rendimiento de Consultas:** Optimizar las búsquedas masivas de stock y necesidades activas ejecutadas por los algoritmos de asignación.
* **Integridad y Normalización:** Aplicar restricciones `NOT NULL` e índices únicos a nivel de tabla relacional donde corresponda.
* **Aislamiento del Dominio:** Preservar el State Pattern y los records inmutables de Java sin obligar a las clases de estado a convertirse en entidades de base de datos.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `donaciones-service` ([decisiones_futuras_en_oleada_10.md](../../arquitectura/diseno/donaciones/decisiones_futuras_en_oleada_10.md) §1).
* **Hallazgo:** Se constató que para el State Pattern de `DonacionIndependiente` no se deben crear 7 tablas relacionales polimórficas; la solución óptima es persistir el enum `TipoEstadoDonacion` mediante un `AttributeConverter` que rehidrata la instancia correcta del estado a través de un factory.

## Alternativas Consideradas

* **Mapeo Híbrido Calibrado (JOINED + SINGLE_TABLE + AttributeConverter):**
  1. *Jerarquía Persona:* `@Inheritance(strategy = InheritanceType.JOINED)`. Justificación: subtipos muy disímiles (`persona_humana` con `apellido NOT NULL`, `persona_juridica` con `cuit UNIQUE`). Evita columnas dispersas con valores nulos.
  2. *Jerarquía Necesidad:* `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` con discriminador. Justificación: ambos subtipos comparten el 80% de columnas; optimiza las consultas del algoritmo de matching ejecutando un `SELECT` directo sin `JOIN`.
  3. *State Pattern de DonacionIndependiente:* Mapear mediante `EstadoDonacionIndependienteConverter` (`AttributeConverter<EstadoDonacionIndependiente, String>`). La columna `estado_actual VARCHAR(30)` almacena el nombre del tipo y el convertidor utiliza `EstadoDonacionIndependienteFactory.crear(tipo)`.
  4. *Value Objects:* `Direccion`, `Localidad` y `Bien` mapeados como `@Embeddable` en las tablas maestras.
* **Estrategia Homogénea TABLE_PER_CLASS:** Crear una tabla independiente por cada clase concreta del modelo.
* **Single Table para Todo:** Colocar a todas las personas en una sola tabla llena de columnas nulables.

## Resultado de la Decisión

Alternativa elegida: "Mapeo Híbrido Calibrado (JOINED + SINGLE_TABLE + AttributeConverter)"

Justificación:
Es la combinación que mejor equilibra normalización relacional, integridad de restricciones y rendimiento. `JOINED` protege la integridad estricta de los datos fiscales y demográficos de las personas. `SINGLE_TABLE` acelera al máximo el cruce de necesidades. El `AttributeConverter` permite que el State Pattern siga siendo un diseño orientado a objetos puro y polimórfico en memoria, sin trasladar complejidad accidental al esquema relacional de PostgreSQL.

### Consecuencias Positivas

* Consultas de matching de alta velocidad sin joins innecesarios en la tabla `necesidad`.
* Tablas de personas normalizadas con restricciones relacionales estrictas.
* Preservación absoluta del State Pattern de 7 estados en Java sin ensuciar la base de datos con tablas de estado artificiales.

### Consecuencias Negativas

* Consultas polimórficas sobre `Persona` requieren un `JOIN` entre `persona` y `persona_humana` / `persona_juridica`.

### Validación

Se valida mediante:
1. Esquema DDL en `decisiones_futuras_en_oleada_10.md` ejecutado en Testcontainers PostgreSQL.
2. Test unitario de `EstadoDonacionIndependienteConverter` comprobando bidireccionalidad entre string y clase de estado concreta.

## Análisis de Alternativas

### Mapeo Híbrido Calibrado

#### Pros
* Arquitectura ORM óptima según las directrices de Hibernate 6.
* Protección de invariantes tanto en Java como en SQL.

#### Contras
* Dos estrategias de herencia distintas en el mismo microservicio.

### Single Table Universal

#### Pros
* Consultas simples sin joins para cualquier jerarquía.

#### Contras
* Imposible colocar restricciones `NOT NULL` en campos de subtipos (ej: apellido en humana o razón social en jurídica).
* Esquemas dispersos y vulnerables a corrupción de datos.

### Table Per Class

#### Pros
* Tablas independientes por cada subtipo.

#### Contras
* Consultas polimórficas sobre la raíz generan `UNION`s costosas que destruyen el plan de ejecución de la base de datos.