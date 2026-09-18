# Arquitectura y Modelo de Dominio — DonaTrack

> **Portal Canónico de Arquitectura, DDD y Contratos del Sistema**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §4`](../../AGENTS.md) y [`docs/context-index.md`](../context-index.md)

---

## 1. Propósito y Estructura del Bounded Context

Este directorio contiene las especificaciones técnicas y de diseño del dominio de DonaTrack. La plataforma se organiza bajo una arquitectura distribuida orientada al dominio (Domain-Driven Design), complementada con un Shared Kernel (`common-lib`) y desacoplamiento asíncrono mediante RabbitMQ.

```text
docs/arquitectura/
├── README.md                              # Este índice de navegación
├── principios-diseno-arquitectura.md      # 🏛️ Documento Maestro: Atributos de calidad, SOLID, GRASP, DDD
├── shared-kernel.md                       # 🏗️ Núcleo compartido (common-lib): CrudRepository, DTOs neutros
├── logging-trazabilidad.md                # 📊 Observabilidad distribuida: traceId, MDC, NDJSON
├── catalogo-errores.md                    # ⚠️ Catálogo unificado de 109 códigos de error
├── guia-patrones-diseno.md                # 🧩 Catálogo de patrones de diseño aplicados
├── analisis-arquitectonico.md             # 🔍 Diagnóstico estructural del monorepo
│
├── aggregates-donaciones.md               # 📦 Dominio: Donaciones, 7 estados y Asignación
├── aggregates-incentivos.md               # 🏆 Dominio: Gamificación, Insignias y Rankings
├── aggregates-logistica.md                # 🚚 Dominio: Rutas, Camiones y Entregas
├── aggregates-notificaciones.md           # 🔔 Dominio: Réplicas de personas y despacho
│
├── contratos-rest.md                      # 🌐 Contratos REST consolidados y OpenAPI 3.0
├── eventos-amqp.md                        # 🐇 Topología RabbitMQ y contratos de eventos
│
├── contratos/                             # 📜 Archivos OpenAPI YAML y JSON Schemas
│   └── schemas/                           # 11 Schemas JSON validados mecánicamente
│
└── diseno/                                # 📐 Bitácoras de refactor, diagramas PUML y anexos
```

---

## 2. Documentos Fundacionales

* [**Principios de Diseño y Arquitectura**](principios-diseno-arquitectura.md): Fundamentación teórica de los 8 atributos de calidad (Disponibilidad, Mantenibilidad, Modificabilidad, Seguridad, Rendimiento, Testeabilidad, Escalabilidad, Auditabilidad), principios SOLID, GRASP, patrones GoF y fitness checks operacionales.
* [**Shared Kernel (`common-lib`)**](shared-kernel.md): Reglas de gobierno para componentes transversales, contratos de repositorio (`CrudRepository`), excepciones unificadas y trazabilidad.
* [**Logging y Trazabilidad**](logging-trazabilidad.md): Propagación de `traceId`, MDC en Spring Boot, interceptores Feign y observabilidad estructurada.
* [**Catálogo Unificado de Errores**](catalogo-errores.md): Normalización exhaustiva de los 109 códigos de error estructurados del sistema (`ERR-INF`, `ERR-CSR`, `ERR-VAL`, `ERR-EST`).
* [**Guía de Patrones de Diseño**](guia-patrones-diseno.md): Catálogo de patrones GoF aplicados (State Machine, Strategy, Template Method, Observer, Factory, Adapter).
* [**Análisis Arquitectónico**](analisis-arquitectonico.md): Diagnóstico estructural de acoplamiento, cohesión y modularidad del monorepo.

---

## 3. Especificaciones de Aggregates por Microservicio

| Microservicio | Puerto | Documento de Aggregates | Patrones de Dominio Clave |
|---|:---:|---|---|
| `donaciones-service` | `:8080` | [aggregates-donaciones.md](aggregates-donaciones.md) | State Pattern (7 estados DI), Template Method (Algoritmo Asignación) |
| `notificaciones-service` | `:8081` | [aggregates-notificaciones.md](aggregates-notificaciones.md) | Strategy (Medios de contacto), Adapter (Twilio/SendGrid), Person Replica |
| `incentivos-service` | `:8082` | [aggregates-incentivos.md](aggregates-incentivos.md) | Template Method (Misiones), Strategy (Inactividad), Observer (Logros) |
| `logistica-service` | `:8083` | [aggregates-logistica.md](aggregates-logistica.md) | State Pattern (Ciclo de vida entrega), Routing Optimization, AMQP Publisher |

---

## 4. Contratos de Comunicación e Integración

* [**Contratos REST Consolidados**](contratos-rest.md): Mapeo completo de 94 endpoints REST, especificaciones OpenAPI 3.0 y Swagger UI interactivo por microservicio.
* [**Eventos AMQP y Topología RabbitMQ**](eventos-amqp.md): Arquitectura de intercambio asíncrono, TopicExchanges, colas, routing keys, políticas de DLQ e idempotencia en consumidores.
* [**Especificaciones y Schemas (`contratos/`)**](contratos/):
  - OpenAPI 3.0 YAMLs por microservicio (`openapi-donaciones.yaml`, `openapi-logistica.yaml`, etc.).
  - 11 Schemas JSON validados en CI por `scripts/validate-contracts.js`.

---

## 5. Bitácoras de Diseño y Refactoring

El subdirectorio [`diseno/`](diseno/README.md) contiene la memoria histórica y técnica de las oleadas de refactorización progresiva, planes de refactorización por servicio, auditorías de diseño y anexos técnicos autogenerados por Maven.

---

## 6. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
