# AGENTS.md — Engineering & Architecture Rules

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Versión:** 3.3.0 (Gobernanza Calibrada, Integridad Operativa y Política de Congelamiento)  
> **Ámbito:** Obligatorio e inmutable para agentes de IA y desarrolladores.

---

## 1. Propósito y Alcance Operativo

Este documento establece las **reglas de gobierno, invariantes arquitectónicas, protocolo de razonamiento y criterios de validación** que rigen la interacción con el repositorio DonaTrack.

`AGENTS.md` **no es un inventario ni una wiki documental**; actúa como el **núcleo de políticas operativas**. Los detalles de diseño, requerimientos de cátedra y decisiones históricas residen en sus respectivos documentos en `docs/`.

```
                    ┌───────────────────────────────────────────────┐
                    │                   AGENTS.md                   │
                    │      (Gobernanza, Invariantes y Workflow)     │
                    └───────────────────────┬───────────────────────┘
                                            │
               ┌────────────────────────────┼───────────────────────────┐
               ▼                            ▼                           ▼
        [docs/README.md]            [docs/adr/ & DEUDA]         [Código & Tests]
       Índice y Arquitectura        Decisiones y Deuda         Verdad Ejecutable
```

---

## 2. Jerarquía de Fuentes de Verdad y Memoria Histórica

La autoridad de una fuente **se determina según la naturaleza de la pregunta o consulta**. La siguiente matriz no constituye una jerarquía lineal universal de precedencia:

| Dimensión de la Consulta | Fuente con Autoridad Primaria | Criterio de Resolución |
|---|---|---|
| **Requerimientos y Alcance Académico** | Enunciados de Cátedra (`docs/entregas/`) | Mandatorio. Ningún código ni ADR puede violar el enunciado base. |
| **Decisiones y Justificaciones de Diseño** | ADRs Aprobados (`docs/adr/`) | Explican el *porqué*. Si el código contradice un ADR, constituye una violación arquitectónica o deuda no registrada. |
| **Arquitectura y Modelo de Dominio** | `docs/arquitectura/` | Define límites de agregados, contratos de interfaz y flujos conceptuales. |
| **Contratos Públicos y Comunicación Vigente** | Especificaciones OpenAPI / DTOs / Tests de Contrato | La compatibilidad hacia atrás es prioritaria. |
| **Comportamiento en Ejecución Real** | Código Fuente + Tests Automatizados | Refleja el estado presente del sistema, incluyendo deuda técnica catalogada. |
| **Deuda Técnica Aceptada** | `docs/adr/DEUDA_TECNICA.md` | Lista excepciones temporales reconocidas y autorizadas (DTI-01 a DTI-06). |
| **Metodología y Prompts de IA** | `docs/IA/` | Reglas de interacción y checklists de Pull Request. |

* **`[INVARIANT]` Inmutabilidad de Registros Históricos:** Los enunciados en `docs/entregas/` y los ADRs aprobados son inmutables. **Está terminantemente prohibido editar documentos históricos para hacerlos coincidir con el estado actual del código**. Las divergencias deben documentarse formalmente como nueva evolución, ADR sucesor o deuda técnica.

---

## 3. Protocolo Epistémico y Manejo de Evidencia

Para evitar alucinaciones, conclusiones precipitadas y re-evaluaciones redundantes, todo análisis técnico, diagnóstico o reporte emitido por un agente debe clasificar sus afirmaciones bajo la siguiente taxonomía:

* **`[OBSERVED]`**: Hecho comprobado directamente en el código fuente, archivos de configuración o historial de Git del repositorio local.
* **`[DOCUMENTED]`**: Regla, requerimiento o decisión formalizada en un ADR, especificación o documento de cátedra.
* **`[INFERRED]`**: Deducción lógica o hipótesis derivada a partir de evidencia observable o documentada.
* **`[PROPOSED]`**: Solución, abstracción o cambio de diseño sugerido que **aún no existe** en el repositorio.
* **`[REJECTED]`**: Alternativa evaluada y formalmente descartada en ADRs o minutas por costo, complejidad o desvío académico (evita ciclos de razonamiento repetitivos).
* **`[VERIFIED]`**: Comportamiento confirmado mediante la ejecución exitosa de pruebas automatizadas o comandos de validación.

> [!IMPORTANT]
> **Regla de Honestidad Intelectual:** Nunca presentar una propuesta (`PROPOSED`) o inferencia (`INFERRED`) como si fuera una capacidad ya implementada (`OBSERVED`).

---

## 4. Invariantes Arquitectónicas y Reglas de Integridad

Las siguientes directivas normativas delimitan el espacio de soluciones válidas en el proyecto:

### 4.1 Dominio y Encapsulamiento
* **`[INVARIANT]` State Pattern de Donaciones:** Las transiciones de estado de `DonacionIndependiente` deben preservar rigurosamente:
  1. Validación de precondiciones de negocio antes de transicionar.
  2. Imposibilidad de ejecutar transiciones inválidas o saltarse estados intermedios.
  3. Mantenimiento consistente e inmutable del historial de cambios de estado.
  4. Encapsulamiento de la lógica de transición dentro del dominio (evitar mutaciones directas desde servicios externos).
  * *Nota:* No sustituir el mecanismo actual por una implementación alternativa sustancialmente diferente sin demostrar mediante análisis y tests que preserva estrictamente estas invariantes y sin la correspondiente decisión arquitectónica (ADR).
* **`[CONSTRAINT]` Dependencias del Dominio:** Las entidades y agregados de dominio no deben acoplarse a DTOs de transporte, clientes HTTP ni dependencias de persistencia JPA/Hibernate durante Fase 1.

### 4.2 Arquitectura de Capas y Microservicios
* **`[INVARIANT]` Controladores como Adaptadores Puros:** Los `Controllers` son adaptadores de entrada HTTP. Su única responsabilidad es recibir requests, validar DTOs de frontera, delegar en Application Services y mapear respuestas. No deben contener lógica de dominio ni orquestación compleja.
* **`[INVARIANT]` Criterio de Pertenencia a `common-lib` (Shared Kernel):** Una abstracción solo pertenece a `common-lib` si es utilizada o razonablemente compartible por múltiples bounded contexts **y su semántica no depende de ningún dominio específico** (ej. `CrudRepository<T>`, jerarquía base de excepciones, helpers de logging y tracing). Queda prohibido introducir entidades de dominio, DTOs específicos o reglas de negocio.
* **`[PHASE-1 CONSTRAINT]` Persistencia en Fase 1:** La persistencia actual se resuelve en memoria mediante `CrudRepositoryEnMemoria<T>` (`ConcurrentHashMap`). No se debe incorporar JPA/Hibernate ni bases de datos SQL en esta fase salvo indicación explícita.
* **`[TARGET ARCHITECTURE]` Persistencia Futura:** El diseño del dominio y los repositorios debe permitir migrar la persistencia a SQL en fases posteriores sin contaminar el modelo con detalles de infraestructura ni requerir reescrituras de la lógica de negocio.
* **`[CONSTRAINT]` Comunicación Inter-Servicios:**
  * Interacciones HTTP sincrónicas $\rightarrow$ Spring Cloud OpenFeign, siempre que sean compatibles con el modelo de integración arquitectónico vigente.
  * Eventos de dominio y desacoplamiento temporal $\rightarrow$ Mensajería AMQP vía RabbitMQ (`logistica-service` $\rightarrow$ `donaciones-service`).
* **`[INVARIANT]` Trazabilidad e Idempotencia:** Todo flujo distribuido debe conservar la correlación mediante `traceId`, propagándolo explícitamente en los metadatos/headers apropiados de Feign y mensajes RabbitMQ y restaurándolo en el contexto de logging (MDC) del consumidor. Los consumidores AMQP deben diseñarse tolerantes a reintentos (idempotencia en capa de aplicación).

### 4.3 Seguridad Operacional e Integridad de Pruebas
* **`[CONSTRAINT]` Seguridad y Datos Sensibles:** Queda terminantemente prohibido introducir credenciales, tokens, contraseñas hardcodeadas o datos personales reales (PII) en código, tests, fixtures, logs o reportes. Toda prueba debe utilizar datos sintéticos.
* **`[INVARIANT]` Integridad de Quality Gates y Pruebas:** Queda terminantemente prohibido deshabilitar tests, eliminar o debilitar aserciones (`assertTrue(true)`), aumentar tolerancias arbitrariamente o alterar configuraciones de linters/CI con el fin de enmascarar regresiones o forzar un build exitoso.

---

## 5. Criterios de Evaluación y Atributos de Calidad (Fitness Checks)

Antes de proponer o implementar un cambio, someter el diseño a las siguientes preguntas operacionales:

| Atributo de Calidad | Pregunta de Verificación (Fitness Check) |
|---|---|
| **Mantenibilidad (OCP / KISS)** | ¿El cambio permite extender comportamiento variable sin modificar responsabilidades estables ni introducir abstracciones innecesarias? |
| **Bajo Acoplamiento** | ¿Se evitaron referencias directas a objetos en memoria entre agregados/microservicios, utilizando identificadores estables (ej. UUID)? |
| **Alta Cohesión (SRP)** | ¿La responsabilidad asignada pertenece naturalmente al componente que posee la información necesaria para resolverla? |
| **Simplicidad (KISS / YAGNI)** | ¿El cambio resuelve el problema real sin añadir niveles de indirección o patrones innecesarios? |
| **Tolerancia a Fallos** | ¿El fallo de un servicio externo degrada de forma controlada el flujo sin generar bloqueos en cascada? |
| **Manejo de Estado** | ¿La solución evita asumir que el estado en memoria de proceso constituye una solución de persistencia distribuida? |
| **Reversibilidad** | ¿El cambio puede revertirse de forma limpia sin dejar estados inconsistentes ni obligar a reescrituras masivas? |
| **Interoperabilidad** | ¿Se preserva la compatibilidad hacia atrás de los contratos REST y los esquemas de eventos publicados? |
| **Control de Dependencias** | ¿Se evitó incorporar dependencias externas innecesarias para resolver problemas que pueden solucionarse con código estándar? |
| **Testeabilidad** | ¿La nueva lógica puede validarse mediante tests unitarios aislados sin requerir el levantamiento de toda la infraestructura distribuida? |

---

## 6. Control de Alcance (Anti-Scope Creep)

* **`[CONSTRAINT]` Alcance Estrictamente Delimitado:** Una tarea autoriza cambios únicamente sobre los módulos y clases directamente vinculados a su objetivo.
* **`[CONSTRAINT]` Prohibición de Refactorings Oportunistas No Autorizados:** Si durante la inspección se descubren errores secundarios, antipatrones o deuda técnica no relacionada con la tarea en curso:
  1. **No corregirlos en la misma iteración**, salvo que el hallazgo impida completar correctamente la tarea actual o represente un riesgo crítico de integridad.
  2. Documentarlos formalmente en el reporte de salida como hallazgo o deuda técnica a tratar.
* **`[CONSTRAINT]` Control de Dependencias Externas:** No incorporar nuevas librerías en `pom.xml` ni alterar imágenes base de Docker sin justificación técnica explícita, requiriendo aprobación previa cuando implique una decisión arquitectónica, nuevo framework, cambio de persistencia/seguridad o modificación de infraestructura.
* **`[CONSTRAINT]` Minimalismo Suficiente:** Aplicar el **cambio mínimo suficiente** para alcanzar el objetivo arquitectónico o funcional definido, sin abstenerse de realizar extracciones o refactors locales cuando la cohesión del diseño lo exija.

---

## 7. Protocolo de Trabajo del Agente en 6 Fases

Cuando el agente aborde tareas de análisis, diseño, refactorización o implementación, debe estructurar su ejecución en las siguientes fases secuenciales:

```
  FASE 1          FASE 2          FASE 3          FASE 4          FASE 5          FASE 6
Descubrimiento ➔  Análisis    ➔   Diseño      ➔  Implementación ➔ Validación ➔   Reporte
(Git & Baseline) (OBS/INF/DOC)  (PROP / Trade-offs) (Mínimo suficiente) (Quality Gates) (Evidencia)
```

### Fase 1: Descubrimiento y Baseline
* Inspeccionar el estado real del repositorio:
  ```bash
  git status
  git branch --show-current
  git log -n 5 --oneline
  ```
* **Protocolo de Baseline (`BASELINE_GREEN` / `BASELINE_RED`):**
  * Ejecutar los tests de los módulos afectados antes de editar código.
  * Si el baseline es `BASELINE_RED` (hay tests preexistentes rotos), **listar explícitamente los fallos previos en el reporte** para no atribuirlos a la modificación en curso.

### Fase 2: Análisis Estructural
* Separar hechos observados de inferencias y alternativas descartadas (`[REJECTED]`).
* Identificar restricciones, dependencias y riesgos potenciales.
* **No modificar ningún archivo de código durante esta fase.**

### Fase 3: Propuesta de Diseño
* Presentar la solución técnica justificando responsabilidades y trade-offs.
* Verificar compatibilidad con los ADRs e invariantes arquitectónicas.
* **Interacción Humana Calibrada:** Solicitar confirmación del usuario **únicamente si el cambio modifica o supera el alcance originalmente autorizado** (evitar pausas innecesarias en tareas donde el refactor arquitectónico ya fue encomendado).

### Fase 4: Implementación Quirúrgica
* Aplicar el cambio mínimo suficiente respetando la encapsulación y los contratos.
* Mantener consistencia de nombres, estilo y convenciones del proyecto.

### Fase 5: Validación Gradual (Quality Gates)
* Ejecutar los Quality Gates correspondientes según el nivel de impacto (ver Sección 11).
* Comprobar formato y estilo de código (`mvn spotless:check`).

### Fase 6: Reporte y Entrega
* Describir exactamente qué se observó, propuso y verificó.
* Explicitar si quedó deuda técnica pendiente o hallazgos secundarios.

---

## 8. Política de Refactoring y Evolución de Contratos

### 8.1 Criterio de Refactor Válido
Todo refactor estructural debe regirse por el principio de preservación:

$$\text{Refactor Válido} = \text{Intención Preservada} \land \text{Invariantes Preservadas} \land \text{Contratos Preservados/Migrados} \land \text{Sin Regresiones}$$

1. **Establecer Baseline:** Identificar el estado inicial (`GREEN` o `RED`).
2. **Preservar Contratos:** Mantener la firma de endpoints, DTOs y payloads de eventos AMQP.
3. **Validación Empírica:** No considerar exitoso un refactor únicamente porque compile; se requiere validación objetiva mediante tests.

### 8.2 Tipificación de Cambios en Contratos y Eventos
* **Refactoring:** Preserva estrictamente los contratos públicos y payloads existentes.
* **Feature:** Permite evolución retrocompatible de contratos REST y eventos AMQP (cambios estrictamente **aditivos**: campos nuevos opcionales; nunca renombrar ni eliminar campos sin ciclo de migración).
* **Breaking Change:** Requiere justificación formal mediante ADR, actualización de suites de integración y aprobación explícita.

---

## 9. Gestión de ADRs y Registro de Deuda Técnica

* **¿Cuándo redactar un nuevo ADR?**
  * Introducción de un nuevo patrón de diseño o framework.
  * Modificación de límites (*boundaries*) entre Bounded Contexts.
  * **Cambio significativo en el modelo de dominio** (agregados, entidades raíz, ownership de datos).
  * Cambio en la estrategia de integración o transporte de datos.
  * Alteración o evolución del modelo de persistencia.
* **Ubicación:** `docs/adr/` (utilizando la convención de numeración existente).
* **Deuda Técnica:** Registrar discrepancias intencionales en `docs/adr/DEUDA_TECNICA.md` asignando un código identificador (ej. `DTI-07`).

---

## 10. Enrutador Documental (Router de Navegación)

Para consultar especificaciones detalladas, dirigirse a las carpetas especializadas en `docs/`:

```
docs/
├── ESTADO_DOCUMENTACION.md        ← Matriz de vigencia y auditoría documental
├── README.md                      ← Mapa general y navegación del proyecto
├── arquitectura/                  ← Especificaciones de dominio, agregados y Shared Kernel
├── adr/                           ← Decisiones de arquitectura y catálogo de DEUDA_TECNICA.md
├── auditoria/                     ← Marco metodológico y rúbricas de revisión crítica
├── testing/                       ← Arquitectura de integración y colecciones Postman
├── cicd/                          ← Flujos de CI/CD y políticas de Pull Request
├── IA/                            ← Contexto base para LLMs, antipatrones y checklists
└── entregas/                      ← Enunciados oficiales y requerimientos de cátedra (INMUTABLES)
```

---

## 11. Pirámide de Validación y Quality Gates

Ejecutar las validaciones de menor a mayor costo computacional según el alcance del cambio:

```
                     ▲
                    / \       Gate 4: Validación Distribuida E2E
                   /   \      ./run-preprod-tests.sh (Docker + n8n + 4 Servicios)
                  /─────\
                 /       \    Gate 3: Integración y Contratos
                /         \   mvn test -pl integration-tests -DskipTests=false
               /───────────\
              /             \ Gate 2: Módulo Completo
             /               \ mvn test -pl <modulo> -am
            /─────────────────\
           /                   \ Gate 1: Compilación, Formato y Test Unitario
          /                     \ mvn test -pl <modulo> -Dtest=MiTest | mvn spotless:check
         /───────────────────────\
```

### 11.1 Comandos Universales Maven (Java 21)

```bash
# Gate 1: Test unitario específico
mvn test -pl <modulo-service> -Dtest=<NombreTest>

# Gate 1: Verificación y corrección de formato con Spotless
mvn spotless:check
mvn spotless:apply

# Gate 2: Compilación y tests del módulo con sus dependencias
mvn clean test -pl <modulo-service> -am

# Gate 2: Compilación rápida de todo el reactor (omitiendo tests)
mvn clean package -DskipTests

# Gate 2: Suite completa de tests unitarios del monorepo
mvn test

# Gate 3: Tests de integración y contratos (requiere servicios levantados o preprod)
mvn test -pl integration-tests -DskipTests=false
```

### 11.2 Entornos Docker y Tests Distribuidos E2E (Gate 4)

* **En entornos Bash (Linux / macOS / Git Bash / WSL):**
  ```bash
  # Ejecución integral E2E con ciclo de vida automático
  ./run-preprod-tests.sh

  # Ejecución manteniendo la infraestructura activa para depuración
  ./run-preprod-tests-stay.sh
  ```
* **En Windows (PowerShell nativo):**
  ```powershell
  # Levantar entorno preproducción con JARs precompilados
  docker compose -f docker-compose.preprod.yml up --build -d

  # Ejecutar integración con maven apuntando a integration-tests
  mvn test -pl integration-tests -DskipTests=false

  # Detener y limpiar recursos
  docker compose -f docker-compose.preprod.yml down -v
  ```

---

## 12. Checklist de Cierre y Criterio de Parada

Antes de dar por concluida cualquier interacción o propuesta técnica, verificar:

- [ ] **Fuentes y memoria respetadas:** ¿La solución es coherente con los ADRs y se mantuvo intacta la documentación histórica?
- [ ] **Baseline verificado:** ¿Se identificaron y aislaron claramente los fallos preexistentes (`BASELINE_RED`)?
- [ ] **Invariantes protegidas:** ¿Se preservaron las guardas de estado y la pureza del dominio sin dependencias JPA prematuras?
- [ ] **Límites de bounded context intactos:** ¿Se respetó el criterio de pertenencia de `common-lib` y el desacoplamiento por UUIDs?
- [ ] **Seguridad e integridad preservada:** ¿Se evitaron secretos/PII en código y se mantuvieron intactas las aserciones de tests sin degradar umbrales?
- [ ] **Alcance respetado:** ¿Se aplicó el cambio mínimo suficiente sin introducir dependencias no autorizadas ni refactors colaterales?
- [ ] **Contratos y eventos verificados:** ¿Se categorizó el impacto en contratos y se garantizó compatibilidad aditiva en eventos AMQP?
- [ ] **Quality Gates superados:** ¿Pasaron los tests correspondientes al nivel de impacto y se verificó `mvn spotless:check`?
- [ ] **Evidencia presentada:** ¿El reporte clasifica claramente lo observado, documentado, propuesto, rechazado y verificado?

---

## 13. Política de Modificación y Gobernanza de `AGENTS.md`

`AGENTS.md` es un artefacto de gobernanza controlada y congelada.

Los agentes y desarrolladores **no deben modificarlo como parte incidental de una tarea de desarrollo, refactorización, corrección de bugs o actualización documental**.

Toda modificación de `AGENTS.md` debe cumplir obligatoriamente:

1. **Aislamiento de Tarea:** Realizarse exclusivamente mediante un commit/PR dedicado e independiente.
2. **Justificación Normativa:** Explicitar qué regla se altera, el motivo técnico y el impacto en los workflows.
3. **Versionado Semántico (SemVer):** Actualizar el encabezado de versión (`MAJOR` para cambios estructurales de gobierno, `MINOR` para nuevas directivas, `PATCH` para correcciones tipográficas).
4. **Preservación de Coherencia:** Mantener compatibilidad con los ADRs, requisitos de cátedra e invariantes vigentes.
5. **Revisión Humana Mandatoria:** Recibir aprobación humana explícita antes de integrarse a la rama principal.

* **`[INVARIANT]` Prohibición de Elusión Normativa:** Un agente nunca debe modificar `AGENTS.md` para eludir una restricción, enmascarar un fallo de Quality Gate o ampliar artificialmente el alcance autorizado de una tarea.

