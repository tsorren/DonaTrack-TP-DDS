# Clasificación de Eventos Notificables como Políticas Transitorias no Persistibles

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: notificaciones, ddd, persistencia, jpa, politicas, orm

## Contexto y Problema

En `notificaciones-service`, existe una rica jerarquía polimórfica de 10 clases que representan eventos entrantes: la interfaz `EventoNotificable`, la clase abstracta `EventoDeDonacion` y 8 subclases concretas (`DonacionAsignada`, `DonacionEnTraslado`, `DonacionEntregada`, `DonacionFallida`, `DonacionRecibida`, etc.). Cada una implementa un Template Method (`armarMensajeDonante()`, `armarMensajeBeneficiario()`) y contiene validaciones estrictas en sus constructores. Al diseñar la persistencia física relacional (Fase 2 / PostgreSQL), surge la duda arquitectónica de si estas clases deben mapearse como entidades `@Entity` de JPA con tablas relacionales (`evento_notificable`, etc.).

## Atributos de Calidad y Drivers de Decisión

* **Simplicidad de Persistencia (YAGNI):** No mapear a base de datos conceptos que no requieren almacenamiento ni ciclo de vida persistente.
* **Claridad en los Límites de Agregados (DDD):** Distinguir entre Aggregate Roots duraderos y Políticas de Fábrica transitorias.
* **Rendimiento:** Evitar escrituras innecesarias en base de datos de objetos efímeros que solo sirven para formatear mensajes.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleadas 3 y 10 de `notificaciones-service` ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/notificaciones/decisiones_futuras_en_oleada_10.md) §3).
* **Hallazgo:** La auditoría final de notificaciones estableció formalmente: *"Las 8 subclases de EventoDeDonacion NO se persisten... Son políticas transitorias de fábrica con guardas de negocio en sus constructores... Se documenta explícitamente para que nadie las mapee por consistencia con el resto del dominio cuando llegue la migración real: no hace falta @Entity ni tabla propia; lo único que persiste es su efecto (las Notificacion que generan)"*.

## Alternativas Consideradas

* **Políticas de Dominio Transitorias (POJOs en Memoria Sin `@Entity`):** Tratar a `EventoNotificable` y sus subtipos estrictamente como objetos de dominio transitorios en memoria (patrón Strategy / Factory Policy). Reciben los datos del evento, aplican sus guardas de validación, generan los textos de los mensajes para la entidad `Notificacion`, y son recolectados por el Garbage Collector sin persistirse en base de datos.
* **Persistencia Relacional Completa con Herencia JPA (`SINGLE_TABLE`):** Crear una tabla `evento_notificable` y guardar una fila por cada evento recibido antes de despacharlo.
* **Almacenamiento como Documentos JSONB:** Guardar el payload crudo del evento en una columna JSONB dentro de la tabla de notificaciones.

## Resultado de la Decisión

Alternativa elegida: "Políticas de Dominio Transitorias (POJOs en Memoria Sin `@Entity`)"

Justificación:
`Notificacion` es el único Aggregate Root persistente en este microservicio (con su ID, persona destinataria, canal utilizado, estado `PENDIENTE/ENVIADA/FALLIDA`, fecha y texto final). El evento es meramente el disparador efímero que ayuda a construir la notificación. Persistir el evento crearía duplicación redundante de datos (guardar el evento y luego la notificación derivada de él) y saturaría la base de datos sin aportar valor de negocio ni trazabilidad adicional (la cual ya queda registrada en la propia notificación y en el `traceId`).

### Consecuencias Positivas

* Cero tablas y mapeos JPA artificiales en la base de datos de notificaciones.
* Dominio limpio y enfocado: las clases de eventos permanecen como POJOs puros sin anotaciones ORM.
* Ahorro de I/O y espacio en disco al procesar millones de notificaciones.

### Consecuencias Negativas

* Si en el futuro se requiriera auditar el evento original exactamente en su estructura cruda independiente de la notificación, no se contará con una tabla dedicada (aunque queda registrado en los logs estructurados).

### Validación

Se valida mediante:
1. Clases de `grupo5.notificaciones.models.eventos` sin ninguna anotación `@Entity`, `@Table` ni `@Id`.
2. El script DDL de notificaciones solo contiene las tablas `notificaciones`, `medios_de_contacto` y `eventos_procesados`.

## Análisis de Alternativas

### Políticas Transitorias sin Persistencia

#### Pros
* Arquitectura limpia, elegante y minimalista.
* Cumple estrictamente YAGNI y DDD.

#### Contras
* Requiere documentar explícitamente la decisión para evitar que desarrolladores intenten mapearlas en el futuro.

### Mapeo con Herencia JPA

#### Pros
* Registro de auditoría del objeto de evento en SQL.

#### Contras
* Tablas duplicadas y sobrecarga masiva de base de datos para almacenar datos redundantes.

### Columna JSONB

#### Pros
* Flexibilidad para guardar el payload sin crear tablas.

#### Contras
* Incrementa el tamaño de la fila de notificación sin necesidad real en la mayoría de las consultas.