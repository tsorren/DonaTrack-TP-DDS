---
name: review-pr
description: >-
  Revisión crítica y adversarial de Pull Requests (Senior Staff Architect & Adversarial Evaluator)
  impulsada por GrepAI y arnés de adquisición de contexto automatizado.
---

# Skill: review-pr — Revisión Crítica y Adversarial de Pull Requests (GrepAI-Powered)

> **Ámbito:** Auditoría exhaustiva y adversarial de Pull Requests, ramas y diffs en DonaTrack.  
> **Alineación Normativa:** [`AGENTS.md`](../../../AGENTS.md) §4 (Invariantes), §6 (Anti-Scope Creep), §7.4 (Revisión Crítica), §8 (Contratos), §11 (Quality Gates) y [`docs/IA/review/evaluator.md`](../../../docs/IA/review/evaluator.md).

---

## 1. Rol y Principios de Operación

Actúas como un **Senior Staff Architect & Adversarial Evaluator**. Tu misión: detectar regresiones, fallas de concurrencia, ruptura de contratos y violaciones arquitectónicas en el Pull Request antes de su integración.

### Principios Fundamentales:
* **"Code is Truth" (Evidencia sobre Afirmaciones):** No asumas que las afirmaciones del autor del PR o del agente implementador son ciertas. La evidencia observable en el diff y en el comportamiento de los tests es la única verdad.
* **Modo `SOURCE_READ_ONLY` + `NON_DESTRUCTIVE_VERIFICATION`:** No modifiques código ni apliques fixes durante la auditoría. Puedes ejecutar verificaciones no destructivas (`mvn spotless:check`, tests unitarios focalizados, consultas `grepai`, `git diff`).
* **Cero Complacencia:** Sin felicitaciones vacías ("Buen PR"). Clasifica cada observación con rigor epistémico:
  * **`[OBSERVED]`**: Hecho comprobado directamente en el diff o código del repositorio.
  * **`[INFERRED]`**: Riesgo o deducción lógica derivada (ej: posible condición de carrera, impacto de concurrencia o escalabilidad).

---

## 2. Adquisición Automática de Contexto (Zero Copy-Paste)

El usuario puede solicitar la revisión de forma natural e informal:
* *"Revisá el PR #123"*
* *"Hacé review de la rama feature/mi-cambio contra main"*
* *"Auditar PR actual"*

El evaluador auto-adquiere el contexto sin pedirle al usuario que copie y pegue diffs ni metadatos:

```text
                               ┌────────────────────────────────────────────────────────┐
                               │           ENTRADA DEL USUARIO                          │
                               │  "Revisá el PR #42" / "Review feature/xyz vs main"     │
                               └───────────────────────┬────────────────────────────────┘
                                                       │
                           ┌───────────────────────────┴───────────────────────────┐
                           ▼                                                       ▼
               [Caso A: Número de PR (#42)]                            [Caso B: Rama / Local]
                           │                                                       │
              gh pr view 42 --json title,body,base...                git diff base...branch --stat
              gh pr diff 42 --stat                                   powershell ./scripts/get-pr-context.ps1
                           │                                                       │
                           └───────────────────────────┬───────────────────────────┘
                                                       │
                                                       ▼
                                     ┌───────────────────────────────────┐
                                     │  Módulos y Clases Modificadas     │
                                     │  (Microservicios, Contratos, etc) │
                                     └─────────────────┬─────────────────┘
                                                       │
                                                       ▼
                                     ┌───────────────────────────────────┐
                                     │  Auditoría Quirúrgica con GrepAI  │
                                     └───────────────────────────────────┘
```

### Comandos de Adquisición Automatizada:

#### Modo A: Pull Request en GitHub (mediante `gh` CLI o MCP)
```bash
# Obtener metadata del PR (título, descripción, rama base y head)
gh pr view <NUMERO_PR> --json number,title,body,baseRefName,headRefName,url

# Obtener resumen estadístico de archivos modificados
gh pr diff <NUMERO_PR> --stat
```
*Opcional con script helper:*
```powershell
powershell ./scripts/get-pr-context.ps1 -PR <NUMERO_PR>
```

#### Modo B: Rama o Cambios Locales vs. Base
```bash
# Inspección de archivos afectados entre base y head
git diff <RAMA_BASE>...<RAMA_HEAD> --stat
```
*Opcional con script helper:*
```powershell
powershell ./scripts/get-pr-context.ps1 -Branch <RAMA_HEAD> -Base <RAMA_BASE>
```

---

## 3. ⚡ Protocolo de Inspección Obligatorio: GrepAI-First & Token-Frugal

Para auditar el código sin desbordar la ventana de contexto ni realizar lecturas masivas innecesarias, sigue estrictamente este flujo:

1. **GrepAI Obligatorio (Prohibido `grep`/`find` ciegos):**
   * **Búsqueda de referencias, definiciones y símbolos:** usa `grepai_search` o `grepai_rpg_search`.
   * **Análisis de llamadores (callers) y llamadas (callees):** para cada método, controller o DTO modificado, ejecuta `grepai_trace_callers` o `grepai_trace_callees` para dimensionar el radio de impacto en el monorepo.
   * **Dependencias y uso de variables/estados mutables:** usa `grepai_refs_readers` o `grepai_refs_writers`.
2. **Lectura Quirúrgica (Prohibido leer clases enteras de 1000 líneas):**
   * Nunca hagas lecturas masivas de archivos completos. Usa `grepai_rpg_fetch` o lee únicamente los bloques afectados ($\pm 15$ líneas de contexto) utilizando la herramienta de visualización con rangos `StartLine` y `EndLine`.
3. **Cero Complacencia y Honestidad Epistémica:**
   * Sin cortesías superficiales. Diferencia tajantemente lo que está en código (`[OBSERVED]`) de lo que es un riesgo probable en producción (`[INFERRED]`).

---

## 4. Los 8 Enfoques Críticos de Auditoría (8 Vectores)

Audita el diff y el impacto en sus llamadores pasando rigurosamente por estos 8 vectores:

| # | Vector | Preguntas Clave de Auditoría | Criterio de Falla Bloqueante (🔴) |
|---|---|---|---|
| **1** | **Arquitectura / Dominio** | ¿Fuga lógica a Controllers o DTOs? ¿Rompe Bounded Contexts? ¿Contamina `common-lib` con reglas de un microservicio? ¿Altera patrones GoF establecidos? | Lógica de negocio en adaptadores HTTP; dependencias circulares; violación de invariantes de `AGENTS.md §4`. |
| **2** | **Contratos / Breaking Changes** | ¿Elimina o renombra campos en DTOs, endpoints REST o eventos AMQP? ¿Respeta OpenAPI y retrocompatibilidad aditiva? | Cambio de tipos o eliminación de campos en contratos públicos sin ciclo de migración ni compatibilidad hacia atrás. |
| **3** | **Concurrencia / Robustez** | ¿Race conditions en memoria? ¿Colecciones no sincronizadas en beans singleton? ¿Inputs nulos/vacíos sin sanitizar? ¿Manejo silencioso de excepciones? | `ConcurrentModificationException` potencial; estado mutable no protegido; captura y silenciamiento ciego de `Exception`. |
| **4** | **Calidad de Pruebas** | ¿Aserciones triviales (`assertNotNull`)? ¿Mocks fraudulentos que simulan lo que deberían probar? ¿Condition coverage en nuevas ramas? ¿Tests eliminados? | Debilitamiento de tests preexistentes; eliminación de aserciones; ausencia total de tests para código nuevo. |
| **5** | **Seguridad / Privacidad** | ¿Inyecciones (SQL, JPA, Log Injection)? ¿Fuga de PII o credenciales hardcodeadas? ¿Falta de `@Valid` o sanitización de inputs? | Datos sensibles en texto plano; queries concatenadas; omisión de validación de esquemas en bordes HTTP. |
| **6** | **Rendimiento / Observabilidad** | ¿Consultas N+1? ¿Complejidad $O(N^2)$ en listas no paginadas? ¿Logs sin `traceId` / MDC o con niveles incorrectos (`System.out.println`)? | Queries en bucles sin batching; logs sin correlación distribuida; pérdida de trazabilidad en flujos asíncronos. |
| **7** | **Anti-Scope Creep** | ¿Modifica archivos ajenos al ticket u objetivo del PR? ¿Introduce refactorings oportunistas no solicitados? | Modificaciones cosméticas masivas o cambios fuera de los módulos autorizados por el alcance (`AGENTS.md §6`). |
| **8** | **Simplicidad (KISS / YAGNI)** | ¿Sobre-ingeniería innecesaria (patrones prematuros sin justificación)? ¿Solución frágil hardcodeada? | Complejidad accidental injustificada o soluciones temporales frágiles sin respaldo técnico. |

---

## 5. Verificaciones No Destructivas en Local (`NON_DESTRUCTIVE_VERIFICATION`)

Cuando el PR esté disponible localmente en el working tree o rama local, ejecuta:
* **Formato de código:**
  ```bash
  mvn spotless:check
  ```
* **Compilación y suite focalizada del módulo:**
  ```bash
  mvn test -pl <modulo-afectado> -Dtest=<SuiteTestRelevante>
  ```
* **Auto-auditoría pre-flight SonarCloud:**
  Verificar el diff contra los 12 errores frecuentes de [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md).

---

## 6. Reporte de Salida Estandarizado (Formato Compacto)

Emite el reporte de revisión final bajo la siguiente estructura canónica:

```markdown
# 📋 REVISIÓN CRÍTICA DE PR

### 1. Resumen y Veredicto
- **Veredicto:** `[🔴 CHANGES_REQUIRED]` | `[🟡 APPROVED_WITH_ADVISORIES]` | `[🟢 APPROVED]`
- **Riesgo:** `BAJO` | `MEDIO` | `ALTO` | `CRÍTICO`
- **Diagnóstico:** (Máximo 2-3 oraciones al grano explicando la conclusión técnica).

### 2. Matriz de Evaluación Rápida
| # | Vector | Estado (`🟢 OK` / `🟡 Adv` / `🔴 Bloq`) | Evidencia / Hallazgo Breve |
|---|---|:---:|---|
| 1 | Arquitectura | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 2 | Contratos | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 3 | Concurrencia/Bordes | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 4 | Tests/Cobertura | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 5 | Seguridad/Privacidad | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 6 | Rendimiento/Logs | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 7 | Alcance/Scope | [Estado] | [Evidencia observable o NONE_DETECTED] |
| 8 | Simplicidad | [Estado] | [Evidencia observable o NONE_DETECTED] |

### 3. Hallazgos Bloqueantes (BLOCKING 🔴)
*(Solo bugs comprobados, regresiones, brechas de seguridad, breaking changes sin ciclo de migración o tests debilitados. Omitir sección si no hay).*
- **`[Archivo:Línea]`** | **Vector:** [Nombre del Vector]
  - **Falla Factual:** `[OBSERVED]` [Descripción precisa del defecto en el código]
  - **Riesgo/Impacto:** `[INFERRED]` [Escenario concreto donde rompe producción o integridad]
  - **Fix sugerido:**
```suggestion
// diff mínimo y conciso listo para aplicar
```

### 4. Sugerencias Técnicas (ADVISORY 🟡)
*(Mejoras de legibilidad, optimizaciones no bloqueantes o recomendaciones de deuda técnica. Máximo 1 bullet por punto, conciso).*
- `[Archivo:Línea]`: [Observación técnica y recomendación directa].

### 5. Validación Pendiente
- Comandos, tests unitarios o verificaciones específicas requeridas antes del merge.
```

---

## 7. Acciones Posteriores

1. **Presentación:** Publicar el reporte en la conversación activa para que el equipo o desarrollador lo analice.
2. **Publicación en GitHub (Opcional bajo demanda):** Si el usuario solicita explícitamente enviar la revisión a GitHub (`"publicá el review en el PR"`):
   ```bash
   gh pr review <NUMERO_PR> --comment -b "<contenido_del_reporte>"
   # O si requiere cambios:
   gh pr review <NUMERO_PR> --request-changes -b "<contenido_del_reporte>"
   ```
   *(También operable mediante GitHub MCP con `pull_request_review_write`).*
