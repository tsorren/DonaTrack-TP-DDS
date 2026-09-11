# Testing y Aseguramiento de la Calidad (QA) — DonaTrack

> **Portal Canónico de Pruebas Automatizadas, Estrategia QA y Performance**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §11`](../../AGENTS.md) y [`docs/context-index.md`](../context-index.md)

---

## 1. Propósito y Filosofía de Calidad

Este directorio centraliza la infraestructura, arquitectura y lineamientos de pruebas automatizadas del proyecto DonaTrack. El sistema cuenta con más de 1.190 pruebas automáticas estructuradas bajo el modelo del **Testing Honeycomb** (Panal de Pruebas), priorizando pruebas unitarias rápidas de dominio y pruebas de slicing sobre costosos tests de integración distribuida.

```text
docs/testing/
├── README.md                              # Este índice de navegación
├── auditoria-arquitectura-testing.md      # 🏛️ Documento Maestro: Auditoría integral y Blueprint Target
├── integration-tests.md                   # 🧪 Pruebas de integración E2E distribuida
├── testing-performance.md                 # ⚡ Rendimiento, Surefire, WebMvc slicing y TIA local
├── plan-auditoria-y-blueprint-qa.md       # 📋 Plan de auditoría y arquitectura target
├── decisiones-diseno-auditoria-qa.md      # ⚖️ Decisiones y alternativas descartadas
│
├── auditoria/                             # 🔬 Compendio hiper-granular de auditoría QA
│   ├── README.md                          # Portal de auditoría, síntesis ejecutiva y glosario
│   ├── 01-diagnostico-ejecutivo.md        # Radiografía factual y evaluación teórica
│   ├── 02-matriz-antipatrones.md          # Matriz de smells AP-01 a AP-10 y fortalezas
│   ├── 03-estudio-comparativo.md          # Comparativa multidimensional en 4 ejes
│   ├── 04-blueprint-target.md             # Blueprint target: Honeycomb, SLAs y paridad DDL
│   ├── 05-roadmap-migracion.md            # Roadmap en 4 fases y compatibilidad docente
│   └── 06-cobertura-critica-qa-backlog.md # Backlog de cobertura crítica para futura iteración
│
└── postman/                               # 📮 Colecciones API y flujos distribuídos
```

---

## 2. Documentos Maestros y Guías Operativas

* [**Auditoría y Blueprint de Arquitectura de Testing y QA**](auditoria-arquitectura-testing.md): **Documento canónico maestro.** Informe integral de auditoría factual de 1.191 tests en 191 clases de test, matriz de patologías F-01 a F-12, evaluación adversarial y blueprint target.
* [**Guía de Pruebas de Integración (`integration-tests.md`)**](integration-tests.md): Arquitectura de pruebas de integración de extremo a extremo (E2E), clientes HTTP tipados, configuración de Docker, PostgreSQL y RabbitMQ.
* [**Testing Performance y Optimización Local (`testing-performance.md`)**](testing-performance.md): Optimización de tiempos de compilación, ejecución de Test Impact Analysis (TIA con `scripts/test-changed.ps1`), slices `@WebMvcTest` y paralelismo.
* [**Plan de Auditoría y Blueprint QA**](plan-auditoria-y-blueprint-qa.md): Metodología y especificación operativa de 5 fases para ejecución de auditorías automatizadas.
* [**Decisiones de Diseño y Alternativas Descartadas**](decisiones-diseno-auditoria-qa.md): Registro fundamentado de alternativas evaluadas y descartadas (`[REJECTED]`).

---

## 3. Compendio Especializado de Auditoría QA (`auditoria/`)

El subdirectorio [`auditoria/`](auditoria/README.md) desglosa la auditoría en 6 documentos especializados:
1. [`01-diagnostico-ejecutivo.md`](auditoria/01-diagnostico-ejecutivo.md): Radiografía cuantitativa y evaluación teórica (Khorikov, Fowler, Google SWE).
2. [`02-matriz-antipatrones.md`](auditoria/02-matriz-antipatrones.md): Catálogo empírico de anti-patrones detectados y erradicados.
3. [`03-estudio-comparativo.md`](auditoria/03-estudio-comparativo.md): Comparativa multidimensional de herramientas y frameworks.
4. [`04-blueprint-target.md`](auditoria/04-blueprint-target.md): Arquitectura objetivo, Testing Honeycomb y diagramas Mermaid.
5. [`05-roadmap-migracion.md`](auditoria/05-roadmap-migracion.md): Fases de modernización preservando scripts docentes.
6. [`06-cobertura-critica-qa-backlog.md`](auditoria/06-cobertura-critica-qa-backlog.md): Backlog priorizado de cobertura de riesgo.

---

## 4. Comandos de Ejecución de Pruebas

| Objetivo | Comando |
|---|---|
| Formato y estilo de código | `mvn spotless:check` / `mvn spotless:apply` |
| Test unitario puntual | `mvn test -pl <modulo> -Dtest=<Test>` |
| Test unitario rápido (sin spotless) | `mvn test -pl <modulo> -Dtest=<Test> -Dspotless.check.skip=true` |
| Módulo completo + dependencias | `mvn clean test -pl <modulo> -am` |
| Suite completa del monorepo | `mvn test` |
| Suite de integración E2E | `mvn verify -pl integration-tests -DskipTests=false` |
| Stack Docker completo interactivo | `./run-preprod-tests-stay.sh` |
| Test Impact Analysis (TIA local) | `./scripts/test-changed.ps1 -Fast` *(PowerShell)* |

---

## 5. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
