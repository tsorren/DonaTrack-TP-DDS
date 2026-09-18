# Conocimiento Generado del Repositorio (Generated Knowledge)

> **Ámbito:** Catálogo dinámico y mechanically extracted de contratos, endpoints y eventos.
> **Alineación Normativa:** [`AGENTS.md`](../../AGENTS.md), [`docs/auditoria/auditoria-directivas-agentes.md`](../auditoria/auditoria-directivas-agentes.md) y arquitectura Nivel 4 (Agent-First).
> **Generador:** `node scripts/generate-repo-knowledge.js`

<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->

## 1. Propósito y Criterio de Automatización

Este directorio contiene artefactos documentales extraídos de manera 100% determinista a partir de las fuentes de verdad canónicas del repositorio (OpenAPI 3.0, JSON Schemas y configuraciones RabbitMQ).

Provee un inventario consolidado que erradica la duplicación manual y acelera el *Progressive Disclosure* para agentes de IA y desarrolladores humanos.

## 2. Métricas de Contratos del Sistema

| Dimensión | Cantidad Identificada |
|---|---:|
| **Microservicios Documentados** | 4 |
| **Endpoints REST Públicos** | 98 |
| **Schemas JSON Canónicos** | 21 |
| **Eventos RabbitMQ Topológicos** | 9 |

## 3. Catálogos Disponibles

* [`endpoints-catalog.md`](./endpoints-catalog.md) — Inventario completo de endpoints REST agrupados por microservicio.
* [`events-catalog.md`](./events-catalog.md) — Catálogo de mensajería asíncrona, exchanges, routing keys y contratos de eventos.
* [`architecture-graph.md`](./architecture-graph.md) — Diagramas Mermaid de topología, arquitectura de datos y dependencias inter-servicio.
* [`contracts-summary.json`](./contracts-summary.json) — Formato estructurado JSON para consumo programático por linters y agentes.

---
*Generado mecánicamente por DonaTrack Knowledge Engine.*
