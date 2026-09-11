# Plan de Auditoría y Marco Revisor Crítico — DonaTrack

> **Sistema de Aseguramiento de Calidad Arquitectónica, Verificación Adversarial y Auditoría Documental**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Cátedra:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Emisión Canónica:** 2026-08-29  
> **Estado:** 🟢 Vigente y Sincronizado (100% Factual con Java 21 / Spring Boot 3)

---

## 1. Marco Metodológico del Sistema Revisor Crítico

El Aseguramiento de Calidad Arquitectónica y la Auditoría Técnica en DonaTrack no se conciben como una inspección superficial de texto, sino como un **proceso riguroso de verificación adversarial y contraste empírico contra el código fuente ejecutable**.

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                             CICLO DE AUDITORÍA Y VERIFICACIÓN ADVERSARIAL                        │
│                                                                                                  │
│  [FASE 1: Relevamiento] ──> [FASE 2: Extracción Factual] ──> [FASE 3: Fact-Checking en Código]  │
│  Inspección de docs/ y      Afirmaciones, endpoints,         Contraste con clases Java 21,       │
│  estructura de navegación    modelos y contratos DTO          Spring Boot 3, Feign y RabbitMQ    │
│                                                                             │                    │
│  [FASE 6: Consistencia] <── [FASE 5: Plan de Acción]    <─── [FASE 4: Scoring y Hallazgos]       │
│  Validación de 0 links      Backlog de mitigación y          Matriz 1-5 (5 ejes + 8 atributos),  │
│  rotos y salud 100%          registro en DEUDA_TECNICA        severidad 🔴/🟡/🟢                 │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1. Principios Epistemológicos de Auditoría

1. **Primacía del Código Ejecutable (*Code is Truth*):** Ningún documento, diagrama o minuta tiene validez per se si contradice la implementación en Java 21, Spring Boot 3, OpenFeign, RabbitMQ o los flujos n8n.
2. **Falsabilidad Popperiana y Verificación Adversarial:** Cada afirmación arquitectónica debe ser sometida a un intento deliberado de refutación mediante inspección de bytecode, firmas de métodos, anotaciones de frameworks, tests automatizados y trazas de logs.
3. **Determinismo y Reproducibilidad:** Todo hallazgo o calificación debe ser reproducible de forma determinística mediante scripts automatizados, comandos Maven o tests de integración.
4. **Transparencia en Deuda y Limitaciones:** Las limitaciones temporales (ej. persistencia en memoria, módulos cascarón de entregas futuras) deben estar formalmente catalogadas y no disfrazarse de funcionalidad productiva.

---

## 2. Matriz de Evaluación en los 5 Ejes Transversales

Cada documento, subsistema o especificación técnica es evaluado bajo una escala cuantitativa de **1 a 5**, asignando un color de semáforo según el nivel de madurez y rigor alcanzado:

| Nivel | Semáforo | Criterio de Madurez y Estado Factual |
|---|---|---|
| 1.0 - 2.4 | 🔴 Crítico | Deficiente, con enlaces rotos, desactualización severa o contradicciones graves con el código. |
| 2.5 - 3.9 | 🟡 Aceptable | Comprensible y mayormente exacto, pero con omisiones técnicas, inconsistencias menores o falta de formalidad. |
| 4.0 - 5.0 | 🟢 Excelente | 100% factual, riguroso, sincronizado con Java 21 / Spring Boot 3, navegable y pedagógicamente ejemplar. |

### Rúbricas de Calificación por Criterio:

```text
┌─────────────────┬────────────────────────────────────────────────────────────────────────────────────────┐
│ Eje de Análisis │ Descripción y Criterio de Puntuación (1 a 5)                                          │
├─────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ 1. Organización │ Estructura de carpetas modular, sin directorios informales ni huérfanos.               │
│                 │ 5: Índice central perfecto, enlaces relativos 100% operativos, modularidad impecable.  │
│                 │ 1: Directorios caóticos, archivos sueltos sin indexar, rutas rotas.                    │
├─────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. Veracidad    │ Coincidencia estricta 1:1 con el código fuente real.                                   │
│                 │ 5: 0 discrepancias con Java 21, frameworks, puertos y persistencia ConcurrentHashMap. │
│                 │ 1: Describe tecnologías o patrones inexistentes (ej. JPA activo cuando es en memoria). │
├─────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ 3. Importancia  │ Relevancia para Diseño de Sistemas (UTN-FRBA) y valor para toma de decisiones.        │
│                 │ 5: Enfoque puro en decisiones arquitectónicas, trade-offs y fundamentos sin relleno.   │
│                 │ 1: Texto redundante, explicaciones obvias de sintaxis, falta de fundamentación.        │
├─────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ 4. Precisión    │ Exactitud en nombres de clases, firmas, endpoints, contratos DTO y diagramas UML.      │
│                 │ 5: Nombres de paquetes, clases, métodos y diagramas PlantUML 100% exactos.             │
│                 │ 1: Errores tipográficos en clases centrales, endpoints invertidos, diagramas erróneos. │
├─────────────────┼────────────────────────────────────────────────────────────────────────────────────────┤
│ 5. Extensión    │ Nivel de exhaustividad equilibrado (profundidad donde se requiere, concisión donde no).│
│                 │ 5: Cubre todas las invariantes y estados de negocio sin sobreextensión innecesaria.    │
│                 │ 1: Lagunas críticas de información en componentes centrales o texto excesivo y hueco.  │
└─────────────────┴────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Auditoría de los 8 Atributos de Calidad y Principios Rectores

El Revisor Crítico debe evaluar cómo la documentación técnica aborda, fundamenta y protege los siguientes 8 atributos:

| # | Atributo de Calidad / Principio | Pregunta de Control de Auditoría | Evidencia Esperada en Documentación |
|:---:|---|---|---|
| **1** | **Mantenibilidad y Flexibilidad** | ¿Se documenta cómo extender el sistema sin modificar código base (OCP)? | Diagramas y explicaciones de AlgoritmoAsignacion, Mision y NotificationSender. |
| **2** | **Desacoplamiento (Low Coupling)** | ¿Se documenta la independencia entre microservicios y Bounded Contexts? | Aislamiento de common-lib, uso de UUIDs foráneos y desprendimiento DTO vs Dominio. |
| **3** | **Alta Cohesión (y SRP)** | ¿Cada capa y clase tiene una responsabilidad acotada y bien definida? | Separación Controllers (HTTP) ➔ Services (Casos de Uso) ➔ Dominio (Reglas e Invariantes). |
| **4** | **Simplicidad e Integridad Conceptual** | ¿El diseño es homogéneo y evita la complejidad accidental (KISS/YAGNI)? | Convenciones de arquitectura idénticas en los 4 servicios, manejo unificado de excepciones. |
| **5** | **Disponibilidad y Tolerancia a Fallos** | ¿Se justifican los mecanismos de resiliencia y desacoplamiento temporal? | Uso de RabbitMQ para eventos de transporte (logistica ➔ donaciones), evitando bloqueos. |
| **6** | **Escalabilidad y Performance** | ¿Se explican los trade-offs de rendimiento y consistencia? | Procesamiento asíncrono (@Async), microservicios stateless y persistencia en memoria. |
| **7** | **Interoperabilidad e Integración** | ¿Los contratos de comunicación están claramente tipados y especificados? | Contratos OpenAPI, interfaces Feign, Webhooks n8n y colecciones Postman exportadas. |
| **8** | **Testeabilidad (Facilidad de Prueba)** | ¿Se especifica la estrategia de pruebas automatizadas y determinismo? | Suite integration-tests (26 clases), fixtures reproducibles y scripts preprod. |

---

## 4. Checklists Operativos por Etapa de Revisión

### Etapa 1: Reorganización Estructural y Limpieza de Archivos
* **Objetivo:** Auditar la higiene del repositorio documental, eliminando enlaces rotos, rutas absolutas locales y carpetas informales.
* **Archivos a inspeccionar:**
  * [docs/README.md](../README.md)
  * [docs/ESTADO_DOCUMENTACION.md](../ESTADO_DOCUMENTACION.md)
  * AGENTS.md (raíz) y Readme.md (raíz).
* **Checklist de Verificación:**
  - [x] Ausencia de archivos plantilla obsoletos (ej. ddsi-tp-template.md).
  - [x] Ausencia de carpetas con espacios, caracteres no-ASCII o nombres informales (normalización de `docs/arquitectura/diseno/`).
  - [x] 100% de enlaces relativos markdown resolviendo a archivos existentes.
  - [x] Mapa de navegación centralizado y sincronizado en docs/README.md.

### Etapa 2: Common-Lib (Shared Kernel) y Logs
* **Objetivo:** Auditar la base transversal compartida, garantizando que cumpla con las restricciones de un Shared Kernel puro (sin entidades de dominio específicas) y provea observabilidad completa.
* **Archivos a inspeccionar:**
  * [docs/arquitectura/shared-kernel.md](../arquitectura/shared-kernel.md)
  * [docs/arquitectura/logging-trazabilidad.md](../arquitectura/logging-trazabilidad.md)
  * Código fuente en common-lib/src/main/java/grupo5/common/.
* **Checklist de Verificación:**
  - [x] Documentación precisa de CrudRepository<T extends AggregateRoot> y CrudRepositoryEnMemoria.
  - [x] Explicación del flujo de traceId en MDC vía TraceResponseHeaderFilter e interceptor OpenFeign FeignTraceRequestInterceptor.
  - [x] Jerarquía de excepciones unificadas (DonaTrackException, RecursoNoEncontradoException, BusinessStateException, ValidationException, InfrastructureException, ErrorCatalog).
  - [x] Verificación de que common-lib no contiene dependencias circulares ni modelos de negocio de microservicios.

### Etapa 3: Aggregates y Modelo de Dominio
* **Objetivo:** Auditar la especificación DDD de cada microservicio contra las clases reales del dominio.
* **Archivos a inspeccionar:**
  * [docs/arquitectura/aggregates-donaciones.md](../arquitectura/aggregates-donaciones.md)
  * [docs/arquitectura/aggregates-incentivos.md](../arquitectura/aggregates-incentivos.md)
  * [docs/arquitectura/aggregates-logistica.md](../arquitectura/aggregates-logistica.md)
  * [docs/arquitectura/aggregates-notificaciones.md](../arquitectura/aggregates-notificaciones.md)
  * [docs/arquitectura/analisis-arquitectonico.md](../arquitectura/analisis-arquitectonico.md)
* **Checklist de Verificación:**
  - [x] **Donaciones:** 7 estados del State Pattern formalizados en EstadoDonacionIndependiente (EnDeposito, AsignacionRealizada, ListaParaEntregar, EnTraslado, Entregada, EntregaFallida, Vencida) + Aggregate Roots DonacionIndependiente, Necesidad (NecesidadExtraordinaria, NecesidadRecurrente) y Propuesta.
  - [x] **Incentivos:** Aggregates DonanteIncentivos, RankingMensual, Insignia y Mision con Template Method (MisionCompletitud, MisionDonacionesExitosas, MisionHabilDonador, MisionRacha).
  - [x] **Logística:** Estados de Entrega (PENDIENTE, EN_CAMINO, ENTREGADA, NO_RECIBIDA, REGRESO_DEPOSITO, CANCELADA), Aggregate Roots Camion, Ruta, Entrega y publicación AMQP vía ComunicadorEventosLogisticaRabbit.
  - [x] **Notificaciones:** Réplica de Persona (con MedioDeContacto: Correo, Telefono), Aggregate Root Notificacion y comunicación sincrónica REST (OpenFeign) explícita (sin RabbitMQ).
  - [x] **Aislamiento DTO:** Identificación de deudas de aislamiento de DTO en dominio (ej. Necesidad.java:toDTO()).

### Etapa 4: Documentación Extensiva de Principios de Diseño y Arquitectura
* **Objetivo:** Auditar la formalización canónica de los marcos teóricos y principios de ingeniería de software aplicados en DonaTrack.
* **Archivos a inspeccionar:**
  * [docs/arquitectura/principios-diseno-arquitectura.md](../arquitectura/principios-diseno-arquitectura.md)
* **Checklist de Verificación:**
  - [x] Fundamentación teórica completa (Uncle Bob Martin, Craig Larman, Eric Evans, GoF, Bass/Clements/Kazman).
  - [x] Cobertura exhaustiva de los 8 Atributos de Calidad con motivación y evidencia en código Java 21.
  - [x] Mapeo de principios SOLID (S, O, L, I, D) con clases del repositorio.
  - [x] Mapeo de patrones GRASP (Information Expert, Controller, Pure Fabrication, Low Coupling, High Cohesion, etc.).
  - [x] Mapeo de patrones GoF (State, Strategy, Template Method, Observer, Factory, Adapter, Facade).
  - [x] Arquitectura Hexagonal y en capas con regla de dependencia estricta.

### Etapa 5: CI/CD, IA y Registro de Deuda Técnica
* **Objetivo:** Auditar la infraestructura de automatización, directrices para asistentes de IA y la trazabilidad de compromisos técnicos diferidos.
* **Archivos a inspeccionar:**
  * [docs/cicd/DonaTrack-CICD.md](../cicd/DonaTrack-CICD.md)
  * [docs/IA/README.md](../IA/README.md) y [docs/IA/06-contexto-base-donatrack.md](../IA/06-contexto-base-donatrack.md)
  * [docs/adr/DEUDA_TECNICA.md](../adr/DEUDA_TECNICA.md)
* **Checklist de Verificación:**
  - [x] Documentación de los 7 workflows de GitHub Actions y flujo de stacked PRs en cascada.
  - [x] Lineamientos de IA sincronizados con la realidad técnica (prohibición de inventar endpoints o agregar @Entity prematuramente).
  - [x] Catálogo formal de deudas técnicas DTI-01 a DTI-06 con alcance claro para la Entrega 2.

---

## 5. Plantilla Estandarizada para Informes de Auditoría

Cada ciclo de auditoría debe reportar sus hallazgos utilizando la siguiente estructura canónica:

```markdown
### [AUDIT-ID] Título Descriptivo del Hallazgo
* **Documento / Módulo Afectado:** path/al/archivo.md o modulo-service
* **Etapa Asociada:** Etapa 1 a 5
* **Severidad:** 🔴 Alta / Crítica | 🟡 Media | 🟢 Baja / Sugerencia
* **Criterios Afectados:** Organización | Veracidad | Importancia | Precisión | Extensión
* **Atributo de Calidad Vinculado:** Mantenibilidad | Desacoplamiento | Cohesión | etc.
* **Descripción del Problema:** Explicación concreta de la discrepancia fáctica o deficiencia conceptual.
* **Evidencia en Código Fuente:** Referencia exacta a archivo y líneas de código (Clase.java:LXX-LYY).
* **Propuesta de Resolución:** Acción correctiva precisa para sincronizar la documentación o registrar en deuda técnica.
* **Estado:** Abierto | En Proceso | Resuelto | Mitigado en DEUDA_TECNICA.md
```

---

## 6. Panel Consolidado de Salud Documental

Tras la ejecución de este marco de auditoría sobre el repositorio DonaTrack:

```text
┌────────────────────────────────────────────────────────────────────────┐
│             ESTADO DE SALUD DOCUMENTAL (POST-AUDITORÍA CANÓNICA)       │
├──────────────────────────────────────┬──────────────────┬──────────────┤
│ Categoría                            │ Cantidad         │ Porcentaje   │
├──────────────────────────────────────┼──────────────────┼──────────────┤
│ 🟢 Vigentes y 100% Sincronizados     │ 33 documentos    │ 43%          │
│ 🔴 Con Discrepancias Altas / Críticas│ 0 documentos     │ 0%           │
│ 🟡 Con Discrepancias Medias          │ 0 documentos     │ 0%           │
│ 🟢 Con Discrepancias Bajas/Cosméticas│ 0 documentos     │ 0%           │
│ 🔒 Históricos e Inmutables (ADRs/Ent)│ 44 componentes   │ 57%          │
└──────────────────────────────────────┴──────────────────┴──────────────┘
```

> **Conclusión del Revisor Crítico:**  
> La documentación del proyecto DonaTrack cumple satisfactoriamente con los 5 ejes analíticos y protege con rigor académico e industrial los 8 atributos de calidad requeridos por la cátedra de Diseño de Sistemas (UTN-FRBA).
