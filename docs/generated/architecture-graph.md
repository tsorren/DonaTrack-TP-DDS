# Grafo de Arquitectura y Topología de Integración — DonaTrack

> **Representación Visual y Mecánica de la Arquitectura Distribuida**

<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->

## 1. Topología de Comunicación Inter-Servicios

```mermaid
graph TD
    subgraph Clientes["Canales de Entrada"]
        Web["Frontend / Web SPA"]
        N8N["Orquestador n8n (Webhooks)"]
    end

    subgraph Microservicios["Core Platform (Java 21 / Spring Boot 3)"]
        DON["donaciones-service<br/>(Port 8081)"]
        LOG["logistica-service<br/>(Port 8082)"]
        INC["incentivos-service<br/>(Port 8083)"]
        NOT["notificaciones-service<br/>(Port 8084)"]
    end

    subgraph Mensajeria["Message Broker"]
        RABBIT[("RabbitMQ<br/>donatrack.events")]
    end

    Web -->|HTTP REST| DON
    Web -->|HTTP REST| LOG
    Web -->|HTTP REST| INC
    N8N -->|Webhooks| NOT

    DON -->|Feign Client (Sync)| LOG
    LOG -->|Feign Client (Sync)| DON
    DON -->|Feign Client (Sync)| INC

    DON -.->|Publish: donante.registrado| RABBIT
    LOG -.->|Publish: ruta.*, entrega.*| RABBIT
    INC -.->|Publish: donante.inactivo, mision.*| RABBIT

    RABBIT -.->|Consume| NOT
    RABBIT -.->|Consume| DON
    RABBIT -.->|Consume| INC
    RABBIT -.->|Consume| LOG
```

## 2. Catálogo de Microservicios y Bounded Contexts

| Servicio | Puerto | Bounded Context | Estrategia de Persistencia | Shared Kernel |
|---|:---:|---|---|:---:|
| `donaciones-service` | 8081 | Donaciones, Necesidades y Asignación | Repositorios Concurrente / JPA | [`common-lib`](../../common-lib/AGENTS.md) |
| `logistica-service` | 8082 | Rutas, Envíos, Entregas y Trazabilidad | Repositorios Concurrente / JPA | [`common-lib`](../../common-lib/AGENTS.md) |
| `incentivos-service` | 8083 | Misiones, Rachas y Puntos | Repositorios Concurrente / JPA | [`common-lib`](../../common-lib/AGENTS.md) |
| `notificaciones-service` | 8084 | Despacho de Mensajes y Alertas | Stateless / Event-Driven | [`common-lib`](../../common-lib/AGENTS.md) |

---
*Generado mecánicamente por DonaTrack Knowledge Engine.*
