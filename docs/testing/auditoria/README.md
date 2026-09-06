# Portal de Auditoría y Arquitectura Target de Testing y QA (DonaTrack)

> **Módulo:** Documentación de Calidad, Testing y Arquitectura de Pruebas  
> **Ubicación:** `docs/testing/auditoria/`  
> **Institución:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Responsable:** Principal Systems Engineer & Lead QA Architect  
> **Ámbito de Ejecución:** Analítico y Documental (`SOURCE_READ_ONLY` en `src/`)  
> **Fecha de Publicación:** 2026-09-06  
> **Estado:** Completo y Vigente (`[DOCUMENTED]`)  

---

## 1. Síntesis Ejecutiva

El presente compendio documental recoge los resultados de la **auditoría profunda, adversarial, empírica y comparativa del ecosistema de pruebas automatizadas y aseguramiento de la calidad (QA) de la plataforma DonaTrack**.

La evaluación contrastó el estado real del código fuente (`[OBSERVED]`) frente a la literatura seminal de la disciplina (**Vladimir Khorikov**, **Gerard Meszaros**, **Martin Fowler**, **Google SWE**, **ISO/IEC/IEEE 29119**), identificando las fortalezas operativas del sistema y los vicios de diseño a corregir:

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        BALANCE DE LA AUDITORÍA DE TESTING                              │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  🟢 Fortalezas:                                                                        │
│     • ~2020 ejecuciones de tests unitarios de dominio (1118 métodos en 198 archivos)   │
│       con 0 fallos, alta velocidad (~15-20s) y seguimiento de la Escuela Clásica.      │
│     • Uso sistemático de Test Data Builders (Persona, Donacion, Necesidad) y Mothers.  │
│     • Infraestructura previa de contratos: 4 OpenAPI 3.0 YAML y 11 JSON Schemas.       │
│     • Trazabilidad distribuida activa con header X-Trace-Id y propagación MDC.         │
│                                                                                        │
│  🔴 Anti-patrones Críticos a Erradicar:                                                │
│     • AP-01 (Green Smoke Contract): ContractIT solo aserta paths, no esquemas/tipos.   │
│     • AP-02 (Sequential Load Loop): PerformanceStressIT corre for síncrono sin carga.  │
│     • AP-03 (Standalone Setup Blindspot): 17 controllers ignoran interceptores/filtros.│
│                                                                                        │
│  🎯 Arquitectura Target Adoptada:                                                      │
│     • Panal de Pruebas (Testing Honeycomb) con Testcontainers (@ServiceConnection).    │
│     • Contratos vivos bidireccionales en Java con WireMock y swagger-request-validator.│
│     • Fitness Functions universales con ArchUnit y Pitest acotado a lógica crítica.    │
│     • Pruebas de rendimiento modernas con k6 en contenedor (VUs, percentiles, SLA).   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Mapa de Navegación y Estructura de Entregables

La auditoría se organiza de forma hiper-granular en 5 documentos especializados e independientes:

| Documento | Título y Enlace | Propósito y Contenido Principal |
|:---:|---|---|
| **01** | [`01-diagnostico-ejecutivo.md`](01-diagnostico-ejecutivo.md) | **Radiografía Factual y Evaluación Teórica:** Conteo de tests por módulo, evaluación contra los 4 pilares de Khorikov, taxonomía Google SWE (Small/Medium/Large), análisis de la Pirámide Invertida y estado del hallazgo INC-01 en CI. |
| **02** | [`02-matriz-antipatrones.md`](02-matriz-antipatrones.md) | **Matriz de Hallazgos Empíricos y Smells:** Tabla exhaustiva con evidencias en código de 10 anti-patrones (AP-01 a AP-10) y 5 fortalezas arquitectónicas (FT-01 a FT-05), con análisis en profundidad de los 3 casos críticos. |
| **03** | [`03-estudio-comparativo.md`](03-estudio-comparativo.md) | **Estudio Comparativo Multidimensional:** Tablas de trade-offs en 4 dimensiones (Ambientes, Contratos, Fitness Functions, Rendimiento) con análisis profundo de alternativas descartadas (`[REJECTED]`). |
| **04** | [`04-blueprint-target.md`](04-blueprint-target.md) | **Blueprint de la Arquitectura Target:** Topología formal del Panal de Pruebas (*Testing Honeycomb*), SLAs por capa, paridad DDL multi-schema, modo degradado y 3 diagramas Mermaid. |
| **05** | [`05-roadmap-migracion.md`](05-roadmap-migracion.md) | **Roadmap de Migración No Disruptivo:** Plan en 4 fases, preservación de compatibilidad con scripts de entrega docente (`run-preprod-tests.sh`), Definitions of Done y matriz de contingencias. |

---

## 3. Documentos Complementarios y Gobernanza de ADRs

### 3.1 Especificaciones de Origen
* [`plan-auditoria-y-blueprint-qa.md`](../plan-auditoria-y-blueprint-qa.md) — Plan maestro operativo y metodología de 5 fases para ejecución con `/goal`.
* [`decisiones-diseno-auditoria-qa.md`](../decisiones-diseno-auditoria-qa.md) — Registro de deliberaciones técnicas, fundamentación y memorial de alternativas descartadas (`[REJECTED]`).

### 3.2 Propuestas de ADR Canónicos Generados (`docs/adr/`)
En cumplimiento de la regla Two-Gate de §9.1 de [`AGENTS.md`](../../../AGENTS.md), esta auditoría generó 4 propuestas formales de Decisiones de Arquitectura (en estado `proposed`):

1. [`20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md`](../../adr/20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md) — Adopción de Testcontainers en slicing de componentes (`@ServiceConnection`), reteniendo Docker Compose para E2E distribuido.
2. [`20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md`](../../adr/20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md) — Validación viva de contratos REST con OpenAPI 3.0, stubs WireMock y serialización RabbitMQ con JSON Schema.
3. [`20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md`](../../adr/20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md) — Custodia automática de invariantes de arquitectura con ArchUnit en compilación y perfil acotado de mutación con Pitest.
4. [`20260906-migracion-pruebas-rendimiento-a-k6.md`](../../adr/20260906-migracion-pruebas-rendimiento-a-k6.md) — Migración total de pruebas de rendimiento a k6, deprecación de `PerformanceStressIT` y wrapper docente en scripts.

---

## 4. Glosario Canónico de Términos de QA y Testing

* **Testing Honeycomb (Panal de Pruebas):** Modelo de pruebas para arquitecturas distribuidas de microservicios propuesto por Martin Fowler y Spotify, que prioriza las pruebas de componentes y slicing sobre una base de pruebas unitarias, minimizando los costosos tests E2E distribuidos.
* **Escuela Clásica de Detroit vs. Escuela de Londres (Mockista):** La Escuela Clásica evalúa el comportamiento y estado final de los agregados interactuando con colaboradores reales o fakes simples en memoria. La Escuela de Londres aísla la unidad bajo prueba reemplazando a todos sus colaboradores con mocks y verificando las interacciones entre ellos. DonaTrack adhiere prioritariamente a la Escuela Clásica en su capa de dominio.
* **Shift-Left Testing:** Práctica de ingeniería de software orientada a adelantar la detección de defectos a las fases más tempranas posibles del ciclo de desarrollo (compilación y pruebas unitarias), donde el costo de corrección es órdenes de magnitud inferior al de producción.
* **Green Smoke Contract:** Anti-patrón de pruebas de contrato donde el test se limita a consultar la presencia sintáctica de una ruta HTTP en un documento JSON, pasando siempre en verde sin validar el esquema de datos, parámetros obligatorios ni tipos de retorno.
* **Sequential Load Loop:** Anti-patrón de pruebas de rendimiento donde se ejecutan peticiones HTTP una tras otra en un bucle sincrónico de un único hilo de ejecución, midiendo tiempos sin someter al sistema a contención de recursos ni concurrencia real.
* **Fitness Functions:** Funciones objetivas y automatizadas (como las provistas por ArchUnit) que evalúan si la estructura de clases, paquetes y dependencias del código respeta las directrices e invariantes de arquitectura del proyecto.
* **Mutation Testing:** Técnica de evaluación de la calidad de las pruebas que introduce mutaciones sintácticas deliberadas en el bytecode compilado (ej. cambiar `>` por `>=`) para verificar si al menos un test de la suite falla (*mata al mutante*).

---

## 5. Guías de Lectura Recomendadas por Rol

```text
┌──────────────────────────────┬────────────────────────────────────────────────────────┐
│ Rol / Perfil del Lector      │ Ruta de Lectura Óptima Recomendada                     │
├──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Desarrollador de Microservice│ 04-blueprint-target.md §3 y §5  →  02-matriz-antipatrones.md│
│                              │ (Aprender a usar Test Data Builders, Slicing y Mocks)  │
├──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Arquitecto de Software       │ 01-diagnostico-ejecutivo.md  →  03-estudio-comparativo.md │
│                              │ → 04-blueprint-target.md  →  ADRs en docs/adr/          │
├──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Ingeniero DevOps / CI-CD     │ 01-diagnostico-ejecutivo.md §6  →  04-blueprint §6     │
│                              │ → 05-roadmap-migracion.md §3 (Compatibilidad Scripts)  │
├──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Evaluador Docente / Cátedra  │ Este README.md  →  05-roadmap-migracion.md §3           │
│                              │ → 01-diagnostico-ejecutivo.md §1 y §2                  │
└──────────────────────────────┴────────────────────────────────────────────────────────┘
```
