# Política Generator / Evaluator — DonaTrack

> **Fuente canónica de la política de revisión de implementación para agentes de IA.**  
> Define roles, modos de independencia, Review Contract y vectores de revisión.  
> Complementa `AGENTS.md` §7.4 — que define la *profundidad* (QUICK / STANDARD / ARCHITECTURAL).  
> Auditoría documental (fuente separada): [`../auditoria/plan-revisor-critico.md`](../auditoria/plan-revisor-critico.md)

---

## 1. Roles

### Generator

El agente o contexto responsable de:

- comprender el scope y los constraints de la tarea;
- cargar contexto proporcional al nivel ([`docs/context-index.md`](../../context-index.md));
- establecer baseline antes de modificar;
- diseñar cuando el nivel lo requiere (STANDARD: mínimo si no es trivial; ARCHITECTURAL: obligatorio);
- implementar el cambio mínimo suficiente;
- producir artefactos observables: diff, tests ejecutados, reporte de validación;
- ejecutar los Quality Gates del nivel (`AGENTS.md` §11).

### Evaluator

El rol o contexto que, sobre los artefactos del Generator:

- busca regresiones activamente (V1);
- cuestiona supuestos del Generator;
- verifica invariantes arquitectónicas (V2);
- revisa contratos: REST, AMQP, Feign, eventos (V3);
- revisa cobertura y calidad de tests (V4);
- detecta scope creep (V5);
- contrasta evidencia: ¿lo que el Generator declara coincide con el diff real? (V6);
- aplica pre-flight SonarCloud si hay código Java (V7);
- verifica seguridad y privacidad cuando aplica (V8);
- audita integridad del grafo documental (V9);
- emite un **Review Contract** observable (§5 / §6).

El Evaluator **no necesita ni debe recibir** el chain-of-thought del Generator. Opera sobre artefactos.

---

## 2. Modos de independencia

### `INDEPENDENT_REVIEW`

Disponible cuando existe un contexto o agente secundario que:

- es distinto del contexto donde operó el Generator;
- no depende del razonamiento interno previo del Generator (recibe solo artefactos explícitos);
- puede emitir output separado e independiente.

`INDEPENDENT_REVIEW` es **strongly preferred** para ARCHITECTURAL y preferido para STANDARD.

### `SELF_REVIEW`

Mismo contexto realiza la revisión después de implementar.

**Obligaciones:**
- declarar explícitamente `Mode: SELF_REVIEW` en el Review Contract;
- nunca etiquetar `SELF_REVIEW` como `INDEPENDENT_REVIEW`;
- emitir Review Contract completo con los mismos campos;
- para ARCHITECTURAL: incluir `[SELF_REVIEW_FALLBACK]` en el Reporte de Fase 7.

### `LIGHTWEIGHT_CLOSING_CHECK`

Chequeo proporcional para tareas QUICK. No es un Review Contract. Ver §5.

---

## 3. Capability detection (vendor-neutral)

La política no asume la existencia de ningún comando, nombre de herramienta ni API específica.

```text
CAPABILITY: tool supports isolated secondary agent/context

  Condición — las tres deben cumplirse:
    (a) el Evaluator opera en un contexto de ejecución diferente al Generator
    (b) no hereda el razonamiento interno previo del Generator
    (c) recibe solo los artefactos explícitamente pasados

  Si CAPABILITY presente → INDEPENDENT_REVIEW disponible
  Si CAPABILITY ausente  → SELF_REVIEW obligatorio
```

**Regla de oro:** si el Evaluator tuvo acceso al razonamiento del Generator (por cualquier mecanismo), el modo es `SELF_REVIEW`, no `INDEPENDENT_REVIEW`.

---

## 4. Evaluator: SOURCE_READ_ONLY + NON_DESTRUCTIVE_VERIFICATION

El Evaluator opera en modo `SOURCE_READ_ONLY`:

- **no modifica** código fuente;
- **no modifica** documentación;
- **no modifica** configuración;
- **no aplica fixes** durante la evaluación.

El Evaluator **puede ejecutar** verificaciones de tipo `NON_DESTRUCTIVE_VERIFICATION` cuando la herramienta lo permita:

- tests focalizados sobre el código modificado;
- Maven checks (`spotless:check`, compilación, test unitario puntual);
- `grep` / búsqueda en el repositorio;
- `git diff` para contrastar el diff declarado vs. el real;
- inspección de archivos;
- linters;
- validación de links markdown;
- otros checks que no modifiquen artefactos versionados.

Esto permite que el Evaluator contraste independientemente la evidencia producida por el Generator.

**Si no puede ejecutar un check:**

```text
[TESTS_NOT_EXECUTED_BY_EVALUATOR]
Razón: <descripción de la limitación>
Se revisó la evidencia producida por el Generator para este vector.
```

Nunca afirmar validación independiente de algo que no se verificó.

---

## 5. LIGHTWEIGHT_CLOSING_CHECK (QUICK)

Chequeo de cierre proporcional para tareas QUICK. No requiere Review Contract.

```text
=== LIGHTWEIGHT CLOSING CHECK ===

Scope/diff check:
  ¿El diff toca solo lo declarado en el objetivo?  [OK | UNEXPECTED_SCOPE: <detalle>]

Validation performed:
  <qué se ejecutó: spotless, git diff, link check, etc.>

Evidence limitations:
  <qué no se pudo verificar y por qué>

Result:  PASS | ESCALATE_TO_STANDARD
```

Si `Result: ESCALATE_TO_STANDARD` → reescalar nivel de la tarea y ejecutar `REVIEW_REQUIRED`.

---

## 6. Review Contract (STANDARD y ARCHITECTURAL)

### Template

```text
=== REVIEW CONTRACT ===

Mode:        INDEPENDENT_REVIEW | SELF_REVIEW
Task Level:  STANDARD | ARCHITECTURAL

V1 Regression risks:
  NONE_DETECTED | NOT_VERIFIED | <descripción y evidencia>

V2 Architecture/invariant violations:
  NONE_DETECTED | <invariante + evidencia de violación>

V3 Contracts/integration:
  NONE_DETECTED | <contrato afectado + tipo de cambio>

V4 Tests/coverage:
  NONE_DETECTED | NOT_VERIFIED [TESTS_NOT_EXECUTED_BY_EVALUATOR] | <ramas sin cobertura>

V5 Scope violations:
  NONE_DETECTED | <qué fue modificado fuera del scope declarado>

V6 Evidence integrity:
  NONE_DETECTED | <afirmaciones del Generator sin evidencia en diff/tests>

V7 SonarCloud pre-flight:
  NOT_APPLICABLE | NONE_DETECTED | <smells detectados vía 07-errores-frecuentes-sonarcloud-ia.md>

V8 Security/privacy:
  NOT_APPLICABLE | NONE_DETECTED | <PII, credenciales o vulnerabilidades detectadas>

V9 Documentation graph integrity:
  NONE_DETECTED | <docs tocados sin sync en docs/README.md o ESTADO_DOCUMENTACION.md>

ADR review (si ADR proposed):
  NOT_APPLICABLE | Puntaje: X.X/5.0 — ver docs/adr/README.md para rúbrica
  (ADR Review es independiente del Implementation Review — ver §11)

Verdict:  PASS | CHANGES_REQUIRED

Findings:
  BLOCKING:
    - <finding que impide cerrar; Generator debe corregir>
    (vacío si no hay)
  ADVISORY:
    - <finding que el equipo humano evalúa; no cierra automáticamente como deuda>
    (vacío si no hay)

[SELF_REVIEW_FALLBACK]
  ← incluir solo si Mode: SELF_REVIEW en tarea ARCHITECTURAL
```

### Valores válidos de campo

| Valor | Significado |
|---|---|
| `NONE_DETECTED` | Evaluator revisó el vector y no encontró hallazgo |
| `NOT_APPLICABLE` | El vector no aplica a este cambio (ej. V7 sin código Java) |
| `NOT_VERIFIED` | El Evaluator no pudo ejecutar la verificación — complementar con `[TESTS_NOT_EXECUTED_BY_EVALUATOR]` |

No rellenar campos con texto artificial cuando no existe un finding.

### Ampliación para ARCHITECTURAL

Para tareas ARCHITECTURAL, además del template base:

- V1 con análisis de impacto cross-service explícito;
- V3 con revisión completa de contratos públicos (REST / AMQP / Feign / eventos);
- ADR review obligatorio si hay ADR `proposed` (ver §11);
- `[SELF_REVIEW_FALLBACK]` si `Mode: SELF_REVIEW`.

---

## 7. Vectores de revisión V1–V9

```text
V1. REGRESSION_RISK
    ¿El cambio puede romper comportamiento existente?
    Señales: tests eliminados, mocks que ocultan comportamiento real,
    aserciones debilitadas, lógica de dominio alterada sin escalar nivel.

V2. ARCHITECTURAL_INVARIANTS
    ¿El cambio viola las invariantes de AGENTS.md §4?
    Señales: lógica de dominio en controllers, common-lib contaminado,
    traceId no propagado, pureza del dominio comprometida.

V3. CONTRACTS_AND_INTEGRATION
    ¿El cambio afecta contratos públicos (REST, AMQP, Feign, eventos)?
    Señales: campos eliminados o renombrados, status codes cambiados,
    payload de eventos alterado, DTO compartido modificado.

V4. TESTS_AND_COVERAGE
    ¿El cambio tiene cobertura adecuada?
    Señales: ramas condicionales sin test, casos borde no cubiertos,
    condition coverage < 80% en nuevas bifurcaciones.

V5. SCOPE_CREEP
    ¿El diff toca más de lo declarado en el objetivo?
    Señales: archivos no mencionados en el reporte, refactors oportunistas
    no autorizados, cambios en módulos no relacionados.

V6. EVIDENCE_INTEGRITY
    ¿El reporte del Generator es fiel al diff real?
    Señales: afirmaciones de validación sin Gate ejecutado, tests declarados
    como [VERIFIED] sin evidencia de ejecución, diff real > diff declarado.

V7. SONARCLOUD_PREFLIGHT  [si hay código Java]
    ¿El código introduce smells catalogados en docs/IA/07-errores-frecuentes-sonarcloud-ia.md?
    Señales: métodos privados no estáticos, clases utilitarias sin constructor private,
    literales duplicados, falta de @Override, condition coverage insuficiente.

V8. SECURITY_AND_PRIVACY  [condicional]
    ¿El cambio introduce PII, credenciales hardcodeadas o vulnerabilidades?
    Señales: datos reales en fixtures o logs, queries sin parámetros, tokens expuestos.
    Aplicar solo cuando el cambio toca seguridad, privacidad o datos de usuarios.

V9. DOC_GRAPH_INTEGRITY
    ¿Se actualizaron docs/README.md y docs/ESTADO_DOCUMENTACION.md si correspondía?
    Señales: archivos movidos/creados sin reflejo en el índice; links rotos post-cambio.
```

---

## 8. Contexto del Evaluator

### Recibe (mínimo necesario)

```text
EVALUATOR RECEIVES:
  - task/spec:             objetivo original y scope declarado
  - Task Level:            QUICK | STANDARD | ARCHITECTURAL
  - relevant AGENTS rules: §4 invariantes, §6 anti-scope-creep, §7.3 profundidad
  - relevant context docs: entry del servicio en docs/context-index.md;
                           shared-kernel si el cambio toca common-lib
  - diff:                  git diff de la rama
  - validation results:    output de Quality Gates ejecutados por el Generator
  - ADR si applicable:     el ADR propuesto (no toda la carpeta /adr)
```

### No necesita

```text
EVALUATOR DOES NOT REQUIRE:
  - cadena de razonamiento interna del Generator
  - conversación completa del Generator
  - documentos no relacionados con el cambio
  - resultados de Gates no ejecutados
```

### Si necesita más contexto

```text
[CONTEXT_REQUESTED: <path o descripción>]
```

El Evaluator puede solicitar contexto adicional explícitamente antes de emitir el Verdict.

---

## 9. Ciclo Generator → Evaluator → re-check

```text
[1] Generator produce artefactos (diff, tests, reporte de validación)
      ↓
[2] Evaluator emite Review Contract
      ↓
[3] Verdict?
      PASS ───────────────────────────────────────── [6] Cerrar
      CHANGES_REQUIRED
          ↓
[4] Generator aplica correcciones de findings BLOCKING
    Re-ejecuta Quality Gates afectados
    Produce nuevos artefactos
          ↓
[5] Evaluator re-check focalizado
    (verifica findings BLOCKING resueltos; no re-evalúa desde cero)
          ↓
    PASS → [6] Cerrar
    CHANGES_REQUIRED → iterar, con condición de parada
```

### Condición de parada

| Condición | Acción |
|---|---|
| `PASS` | Cerrar normalmente |
| Solo findings ADVISORY | `PASS` posible; reportar en Fase 7; equipo humano decide |
| Varias iteraciones sin convergencia (orientativamente 2–3 ciclos) | Escalar: `[ESCALATED_TO_HUMAN]` |
| Findings BLOCKING nuevos en cada ciclo | Escalar: scope o spec probablemente mal definido |
| Correcciones requieren decisión arquitectónica nueva | Escalar: posible ADR necesario |
| Generator y Evaluator no convergen sobre evidencia observable | Escalar |

`[ESCALATED_TO_HUMAN]` en el Reporte de Fase 7 indica que la iteración fue detenida. La decisión de continuar es del equipo.

### Hallazgos ADVISORY

Un finding ADVISORY queda documentado en el Review Contract y/o en el Reporte de Fase 7.

El Evaluator **no decide automáticamente**:

- crear deuda técnica en `docs/adr/DEUDA_TECNICA.md`;
- crear issue;
- crear ADR;
- ampliar scope.

El equipo humano decide si el advisory se resuelve, se descarta, se convierte en issue o se registra como deuda.

---

## 10. Responsabilidad humana

| Decisión | El Evaluator puede... |
|---|---|
| Aceptar o rechazar un ADR | Aplicar rúbrica y emitir puntaje |
| Aprobar PR para integrar a main | Emitir Review Contract con Verdict |
| Aceptar riesgo residual | Identificar riesgo, probabilidad e impacto |
| Decidir entre alternativas arquitectónicas cuando el spec no las resuelve | Presentar alternativas con trade-offs |
| Promover ADR a `superseded` | Proponer la relación |
| Decidir que findings BLOCKING pueden ignorarse | Documentar el finding; el equipo decide |
| Registrar deuda en `DEUDA_TECNICA.md` | Señalar el candidate finding en el Reporte |

**El Evaluator identifica, cuestiona, verifica y recomienda. El humano decide, acepta riesgo y aprueba.**

---

## 11. Implementation Review vs. ADR Review

Son actividades distintas que pueden coexistir en tareas ARCHITECTURAL.

### Implementation Review

**Pregunta central:** ¿el cambio está correctamente implementado?  
**Vectores:** V1–V9.  
**Output:** Review Contract (`=== REVIEW CONTRACT ===`).

### ADR Review

**Pregunta central:** ¿la decisión arquitectónica está bien fundamentada?  
**Rúbrica:** escala 1–5 en 4 dimensiones — ver [`docs/adr/README.md`](../adr/README.md).  
**Threshold mínimo:** ≥ 4.0/5.0 para que el ADR sea apto para review humana.  
**Output:** puntaje X.X/5.0 + observaciones sobre el ADR propuesto.

Un `PASS` de Implementation Review no implica `PASS` de ADR Review, y viceversa. Cuando existe ADR `proposed`, ambos reviews se ejecutan y se reportan por separado.
