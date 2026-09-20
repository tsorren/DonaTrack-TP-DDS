# Evaluación de la Interfaz Asignable frente a Identificador Directo de Entidad Beneficiaria

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: ddd, arquitectura, agregados, persistencia, donaciones

## Contexto y Problema

En el modelo actual de `donaciones-service`, cuando una `DonacionIndependiente` es asignada a una demanda, mantiene una referencia polimórfica en memoria:

```java
@JsonIgnore private Asignable asignadaA;
```

donde `Asignable` es una interfaz con un único método:

```java
public interface Asignable {
  public Necesidad obtenerNecesidad();
}
```

Esta interfaz es implementada simultáneamente por:
1. `Necesidad` (y su especialización `NecesidadExtraordinaria`).
2. `PeriodoNecesidad` (un `record` que encapsula las donaciones de un ciclo específico dentro de `NecesidadRecurrente`).

Para resolver la entidad beneficiaria destinataria de la donación, `DonacionIndependiente` debe navegar en memoria a través de la cadena `this.asignadaA.obtenerNecesidad().getEntidadId()`.

Este diseño presenta varias tensiones arquitectónicas:
1. **Violación de Límites de Agregados (DDD):** Tanto `DonacionIndependiente` como `Necesidad` son Aggregate Roots independientes. El principio fundamental de agregados en DDD establece que una raíz de agregado no debe sostener referencias directas a objetos de otra raíz de agregado; la relación debe resolverse estrictamente por identidad (`UUID`).
2. **Síntoma de Acoplamiento (`@JsonIgnore` en Dominio):** La presencia de anotaciones de serialización como `@JsonIgnore` dentro de la entidad de dominio es una señal de que el modelo sostiene referencias bidireccionales o circulares en memoria que deben ser ocultadas artificialmente para evitar bucles en las respuestas JSON.
3. **Complejidad Artificial para Persistencia ORM (Fase 2 / Entrega 4):** Mapear con JPA/Hibernate una interfaz polimórfica (`Asignable`) que apunta indistintamente a una entidad de tabla propia (`Necesidad`) o a un value object/registro secundario (`PeriodoNecesidad`) requiere configuraciones complejas y frágiles (e.g. `@Any`, `@AnyMetaDef` o discriminadores manuales). Por el contrario, almacenar `UUID entidadBeneficiariaId` y `UUID necesidadId` se traduce en columnas relacionales limpias e indexables.
4. **Indirección Prescindible:** En el 100% de los flujos del sistema (trazabilidad, generación de eventos AMQP, ruteo logístico de entregas y notificaciones), el único dato que los consumidores requieren es el identificador de la entidad beneficiaria (`entidadBeneficiariaId`) y la necesidad satisfecha. La navegación hacia el `PeriodoNecesidad` solo es relevante para la rotación interna del ciclo, no para la donación física en sí.

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Simplicidad (KISS / SRP):** Eliminar interfaces de un solo método cuyo único propósito es servir de pasamanos hacia un identificador.
* **Consistencia Arquitectónica (DDD):** Respetar la regla de que los agregados solo se referencian por identificadores estables (`UUID`).
* **Preparación para Persistencia Relacional:** Facilitar el mapeo ORM nativo en PostgreSQL (Entrega 4) sin ensuciar el esquema de base de datos.

## Alternativas Consideradas

* **1. Mantener la interfaz `Asignable` (Statu quo en memoria):**
  Preserva el polimorfismo actual donde `DonacionIndependiente` almacena una referencia a `Asignable`.
  * *Pros:* No requiere refactorizaciones inmediatas en los tests ni en la capa de servicios de Fase 1.
  * *Contras:* Mantiene la anotación `@JsonIgnore` en la entidad, viola las invariantes de DDD y complica severamente el mapeo ORM con JPA.

* **2. Reemplazar `Asignable` por Identificadores Directos (`UUID entidadBeneficiariaId`, `UUID necesidadId`):**
  Desacoplar la relación haciendo que `DonacionIndependiente` almacene directamente el `UUID entidadBeneficiariaId` (obligatorio al asignar) y opcionalmente `UUID necesidadId`.
  * *Pros:* Enfoque DDD puro, elimina dependencias a clases de necesidad dentro de la donación, erradica `@JsonIgnore`, y mapea de forma trivial a columnas `@Column(name = "entidad_beneficiaria_id")` en PostgreSQL.
  * *Contras:* Requiere que el método `asignar(...)` reciba los UUIDs o que el servicio de aplicación resuelva los identificadores antes de invocar la transición de dominio.

* **3. Value Object `DestinoAsignacion` (`record DestinoAsignacion(UUID entidadId, UUID necesidadId, UUID periodoId)`):**
  Encapsular los identificadores destino en un Value Object inmutable embebible (`@Embeddable`).
  * *Pros:* Permite trazabilidad granular (incluso a nivel de período histórico) sin sostener referencias a entidades vivas en memoria.
  * *Contras:* Agrega un tipo de dato adicional cuando dos atributos `UUID` directos suelen ser suficientes para el dominio.

## Resultado de la Decisión

Alternativa elegida: **"Reemplazar `Asignable` por Identificadores Directos (`UUID entidadBeneficiariaId`, `UUID necesidadId`)"** (pendiente de aprobación e implementación para la migración relacional de la Entrega 4).

Justificación:
El desacoplamiento por `UUID` es la solución canónica en DDD y resuelve de raíz las complicaciones de persistencia ORM detectadas para la Entrega 4. La interfaz `Asignable` fue un mecanismo transitorio de la Entrega 1 que ya no se justifica en un esquema distribuido multi-servicio.

### Consecuencias Positivas

* `DonacionIndependiente` queda completamente desacoplada de la jerarquía de `Necesidad` y `PeriodoNecesidad`.
* Se elimina `@JsonIgnore` del dominio.
* El mapeo relacional JPA en PostgreSQL queda normalizado y optimizado.
* Mayor coherencia con la resolución de [20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md](./20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md).

### Consecuencias Negativas

* `DonacionIndependiente` no puede navegar directamente al objeto `Necesidad` en memoria sin consultar previamente al repositorio correspondiente en la capa de aplicación.

## Origen y Lecciones de las Oleadas de Refactor

* **DTI-06 y Refactor de Agregados:** En las oleadas de refactor del Shared Kernel y Donaciones, se aisló cada aggregate root con su propio ciclo de vida. `Asignable` permaneció como una excepción documentada que debe ser deprecada en la migración relacional.
* **Preparación Entrega 4:** El enunciado oficial de Entrega 4 (`Enunciado-4.pdf`) exige persistencia con ORM y PostgreSQL. Desechar relaciones polimórficas de interfaz en favor de foreign keys estándar simplifica radicalmente las entidades JPA.

## Links

* [ADR DTI-06 Desacoplamiento por UUID](./20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
* [ADR Rechazado de Gestión de Donaciones en Necesidad](./20260612-gestion-donaciones-en-necesidad.md)
* [ADR Mapeo ORM en Donaciones](./20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md)