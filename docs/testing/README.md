# Testing y Aseguramiento de la Calidad (QA) — DonaTrack

> **Portal Canónico de Pruebas Automatizadas, Estrategia QA y Performance**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §11`](../../AGENTS.md) y [`docs/context-index.md`](../context-index.md)

---

## 1. Propósito y Filosofía de Calidad

Este directorio centraliza la infraestructura, arquitectura y lineamientos de pruebas automatizadas del proyecto DonaTrack. El sistema cuenta con más de 1.500 pruebas automáticas (recuento por `grep` al 2026-10-14; la clasificación detallada está pendiente de recuento) estructuradas bajo el modelo del **Testing Honeycomb** (Panal de Pruebas), priorizando pruebas unitarias rápidas de dominio y pruebas de slicing sobre costosos tests de integración distribuida.

```text
docs/testing/
├── README.md                              # Este índice de navegación
├── auditoria-arquitectura-testing.md      # 🏛️ Documento Maestro: Auditoría integral y Blueprint Target
├── integration-tests.md                   # 🧪 Pruebas de integración E2E distribuida
├── testing-performance.md                 # ⚡ Rendimiento, Surefire, WebMvc slicing y TIA local
├── plan-auditoria-y-blueprint-qa.md       # 📋 Plan de auditoría y arquitectura target
├── decisiones-diseno-auditoria-qa.md      # ⚖️ Decisiones y alternativas descartadas
├── preprod-fiel.md                        # 🧭 Preprod fiel: criterio, estado y brechas (fase 1)
├── preprod-fiel-arquitectura-objetivo.md  # 🏗️ Preprod fiel: arquitectura objetivo (fase 2)
├── preprod-fiel-roadmap.md                # 🗺️ Preprod fiel: roadmap, rediseño SPEC-05 y estado (fase 3)
├── diagnostico-integration-tests-e5.md    # 🩺 Diagnóstico de integration-tests en E5
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
└── postman/                               # 📮 LEGADO: copia anterior de colecciones; la fuente vigente es ../../postman/
```

---

## 2. Documentos Maestros y Guías Operativas

* [**Auditoría y Blueprint de Arquitectura de Testing y QA**](auditoria-arquitectura-testing.md): **Documento canónico maestro.** Informe integral de auditoría factual de 1.191 tests en 191 clases de test (cifras de la auditoría original; pendiente de recuento), matriz de patologías F-01 a F-12, evaluación adversarial y blueprint target.
* [**Guía de Pruebas de Integración (`integration-tests.md`)**](integration-tests.md): Arquitectura de pruebas de integración de extremo a extremo (E2E), clientes HTTP tipados, configuración de Docker, PostgreSQL y RabbitMQ.
* [**Testing Performance y Optimización Local (`testing-performance.md`)**](testing-performance.md): Optimización de tiempos de compilación, ejecución de Test Impact Analysis (TIA con `scripts/test-changed.ps1`), slices `@WebMvcTest` y paralelismo.
* [**Plan de Auditoría y Blueprint QA**](plan-auditoria-y-blueprint-qa.md): Metodología y especificación operativa de 5 fases para ejecución de auditorías automatizadas.
* [**Decisiones de Diseño y Alternativas Descartadas**](decisiones-diseno-auditoria-qa.md): Registro fundamentado de alternativas evaluadas y descartadas (`[REJECTED]`).
* [**Preprod fiel: criterio, estado y brechas (`preprod-fiel.md`)**](preprod-fiel.md): Criterio de 12 propiedades para un entorno preproductivo fiel, medición del pipeline y de `integration-tests` (9/24) y brechas B1–B12 priorizadas. Fase 1 de 3.
* [**Preprod fiel: arquitectura objetivo (`preprod-fiel-arquitectura-objetivo.md`)**](preprod-fiel-arquitectura-objetivo.md): Alternativas con matriz ponderada para B1–B12, entorno preprod, etapas del pipeline (PR, post-merge, promoción, despliegue), estratos S0–S7 y evaluación de ADR. Fase 2 de 3.
* [**Preprod fiel: roadmap (`preprod-fiel-roadmap.md`)**](preprod-fiel-roadmap.md): Roadmap RM-01 a RM-20 por entrega (E5, E6, E7 y después), con brechas, propiedades, dependencias, tamaño y criterio de hecho; rediseño de la [SPEC-05](../specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md) (base por servicio, despliegue sin Render, política de contratos) y estado de implementación de RM-02 a RM-08; proyección del puntaje, aprobaciones y confirmación Gate A + Gate B, con 3 ADRs `proposed`. Fase 3 de 3.
* [**Diagnóstico de integration-tests en E5 (`diagnostico-integration-tests-e5.md`)**](diagnostico-integration-tests-e5.md): Línea base medida (3 corridas del script canónico), hallazgos de cobertura, robustez, calidad y costo cruzados con F-xx, QA-GAP-xx y RM-xx, y propuestas P1–P5 con matriz de alternativas para la fase 2.

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
