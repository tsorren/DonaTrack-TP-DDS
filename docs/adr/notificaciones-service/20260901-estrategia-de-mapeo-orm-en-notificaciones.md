# Estrategia de Mapeo ORM en Notificaciones

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: persistencia, jpa, hibernate, notificaciones, single-table, orm

## Contexto y Problema

En `notificaciones-service`, el dominio gira en torno a dos agregados fundamentales:
1. `MedioDeContacto`: Representa los canales registrados por una persona para ser notificada (con la jerarquía polimórfica `Correo` y `Telefono`, este último cubriendo canales SMS estándar y WhatsApp mediante `TipoTelefono.WHATSAPP`).
2. `Notificacion`: Representa el mensaje concreto despachado a un destinatario, con su estado de entrega (`PENDIENTE`, `ENVIADA`, `FALLIDA`), timestamps de reintentos y canal asignado.
Al diseñar el esquema relacional con JPA/Hibernate 6 para Fase 2, se requiere definir la estrategia de herencia para los medios de contacto y la forma en que `Notificacion` se vincula con la persona y el canal, respetando los límites de microservicios.

## Atributos de Calidad y Drivers de Decisión

* **Simplicidad y Eficiencia de Consultas:** Minimizar joins al momento de recuperar el medio predeterminado de un usuario para despachar una alerta.
* **Aislamiento de Bounded Context:** `notificaciones-service` no debe asumir que las tablas de personas de `donaciones-service` residen en la misma base de datos.
* **Integridad Relacional:** Asegurar restricciones claras sobre direcciones de correo y números de teléfono.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `notificaciones-service` ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/notificaciones/decisiones_futuras_en_oleada_10.md) §1, §2, §7).
* **Hallazgo:** Se estableció que la jerarquía de medios de contacto se unifica en una sola tabla mediante `SINGLE_TABLE` por ser extremadamente homogénea (ambos subtipos comparten `persona_id`, `es_predeterminado`, `valor` y solo difieren en la columna opcional `tipo_telefono`), y que `Notificacion` debe referenciar a la persona mediante `persona_id UUID` sin foreign key forzada.

## Alternativas Consideradas

* **Mapeo con SINGLE_TABLE y Desacoplamiento por UUID:**
  1. *Jerarquía MedioDeContacto:* `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` con columna discriminadora `tipo_medio VARCHAR(20)` (`CORREO` / `TELEFONO`) y columna auxiliar `tipo_telefono VARCHAR(20)` (`ESTANDAR` / `WHATSAPP`).
  2. *Desacoplamiento de Agregados en Notificacion:* La tabla `notificacion` contiene `persona_id UUID NOT NULL` como valor escalar (sin `@ManyToOne` hacia entidades de persona), respetando el desacoplamiento de microservicios.
  3. *Estado de Notificación:* Persistido como `VARCHAR(20)` mediante `@Enumerated(EnumType.STRING)`.
* **Herencia JOINED para Medios de Contacto:** Tablas separadas `medio_de_contacto`, `correo` y `telefono`.
* **Relación Forzada @ManyToOne hacia Persona:** Exigir que `Notificacion` contenga un puntero JPA a una tabla de personas.

## Resultado de la Decisión

Alternativa elegida: "Mapeo con SINGLE_TABLE y Desacoplamiento por UUID"

Justificación:
Es la opción de mayor rendimiento y menor complejidad. `SINGLE_TABLE` permite que la consulta para obtener los medios de contacto de un destinatario (`SELECT * FROM medio_de_contacto WHERE persona_id = :id`) se resuelva en una sola lectura indexada ultra-rápida sin joins polimórficos. Guardar `persona_id` como UUID puro garantiza la independencia del microservicio frente a cualquier cambio en el esquema de personas de otros módulos.

### Consecuencias Positivas

* Consultas y despachos de notificaciones de altísima velocidad.
* Esquema SQL simple, fácil de migrar y auditar.
* Microservicio 100% autónomo y desacoplado de las tablas de datos personales centrales.

### Consecuencias Negativas

* La columna `tipo_telefono` queda en `NULL` para los registros donde `tipo_medio = 'CORREO'` (despreciable).

### Validación

Se valida mediante:
1. Esquema DDL de notificaciones en `decisiones_futuras_en_oleada_10.md`.
2. Tests unitarios con Testcontainers comprobando la persistencia y recuperación polimórfica de correos y teléfonos WhatsApp desde una única tabla.

## Análisis de Alternativas

### SINGLE_TABLE y UUID

#### Pros
* Máxima velocidad de consulta.
* Desacoplamiento absoluto entre bounded contexts.

#### Contras
* Una columna nulable en la tabla relacional.

### Herencia JOINED

#### Pros
* Pureza relacional sin columnas nulas.

#### Contras
* Penalización de joins innecesarios para una tabla con solo dos subtipos elementales.

### Relación JPA Cruzada

#### Pros
* Navegación fluida de entidades en el código.

#### Contras
* Violación crítica de microservicios: acopla las bases de datos de diferentes bounded contexts.