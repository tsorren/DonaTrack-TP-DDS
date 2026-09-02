# Checklist antes de abrir PR con ayuda de IA

## Objetivo

Este checklist debe usarse antes de abrir una Pull Request cuando se haya usado IA para analizar, diseñar, implementar, testear, debuggear o revisar cambios.

La IA puede acelerar el trabajo, pero no reemplaza la responsabilidad del owner.

---

# 1. Comprensión del cambio

Antes de abrir PR confirmo que:

- [ ] Entiendo cada archivo que modifiqué.
- [ ] Entiendo cada bloque de código agregado.
- [ ] Puedo explicar por qué el cambio resuelve la issue.
- [ ] Puedo explicar qué partes del sistema se ven afectadas.
- [ ] Puedo explicar qué alternativas descarté, si las hubo.
- [ ] No acepté código que no entiendo.

---

# 2. Alcance de la issue

Confirmo que:

- [ ] El cambio respeta el alcance de la issue.
- [ ] No agregué funcionalidades que no fueron pedidas.
- [ ] No hice refactors grandes innecesarios.
- [ ] No cambié contratos públicos sin avisar.
- [ ] No modifiqué arquitectura sin aprobación.
- [ ] No mezclé varias issues en una misma PR.

---

# 3. Diseño técnico y Gobernanza de ADRs

Confirmo que:

- [ ] La issue no requiere diseño previo, o el diseño ya fue aprobado.
- [ ] Si había PlantUML asociado, lo revisé.
- [ ] La implementación respeta el diseño aprobado.
- [ ] Las responsabilidades quedaron en las clases correctas.
- [ ] No sobrecargué innecesariamente la capa de services.
- [ ] No dupliqué lógica de negocio existente.
- [ ] No introduje acoplamiento innecesario entre módulos.
- [ ] Evalué si la tarea introdujo una nueva decisión arquitectónicamente significativa (Gate A + Gate B en [`docs/adr/README.md`](../adr/README.md)). Si aplica: formalicé el ADR en `Status: proposed` bajo formato MADR.
- [ ] Si el ADR corresponde a una entrega futura, lo formalicé sin introducir código especulativo (ver Bifurcación Temporal en [`docs/adr/README.md`](../adr/README.md)).
- [ ] Ningún ADR nuevo fue auto-promovido a `Status: accepted`; se delegó la aprobación formal al revisor humano en la PR.

---

# 4. Backend

Si la PR toca backend, confirmo que:

- [ ] Revisé reglas de negocio afectadas.
- [ ] Revisé controllers, services, repositories y DTOs involucrados.
- [ ] Validé errores esperados.
- [ ] Validé status codes si aplica.
- [ ] Revisé impacto en entidades JPA.
- [ ] Revisé relaciones, cascades, lazy loading o queries si aplica.
- [ ] Revisé transacciones si aplica.
- [ ] No introduje queries innecesariamente costosas.
- [ ] No expuse datos que no deberían exponerse.

---

# 5. Frontend

Si la PR toca frontend, confirmo que:

- [ ] Probé el flujo principal en pantalla.
- [ ] Probé estados de carga.
- [ ] Probé estados de error.
- [ ] Probé estados vacíos.
- [ ] Validé que no se rompieron componentes compartidos.
- [ ] Validé que los datos enviados al backend respetan el contrato esperado.
- [ ] Revisé mensajes visibles para el usuario.

---

# 6. Testing

Confirmo que:

- [ ] Agregué o actualicé tests relevantes.
- [ ] Corrí los tests del módulo afectado.
- [ ] Probé manualmente el caso principal.
- [ ] Probé al menos un caso borde si corresponde.
- [ ] Probé al menos un caso de error si corresponde.
- [ ] Los tests no solo prueban mocks sin validar comportamiento.
- [ ] Los nombres de tests son claros.
- [ ] Los datos de prueba son entendibles.

---

# 7. Debugging y errores

Si usé IA para resolver un error, confirmo que:

- [ ] Entendí la causa del error.
- [ ] No apliqué cambios al azar.
- [ ] Validé que la solución corrige el problema.
- [ ] Corrí nuevamente el comando o test que fallaba.
- [ ] Dejé comentario en la issue si el bloqueo fue relevante.
- [ ] Escalé al equipo si el error implicaba diseño, contratos o arquitectura.

---

# 8. Uso de IA

Confirmo que:

- [ ] Usé IA con contexto suficiente.
- [ ] Revisé críticamente la respuesta de la IA.
- [ ] No acepté código inventado contra clases inexistentes.
- [ ] No delegué decisiones arquitectónicas.
- [ ] Usé IA también para revisar riesgos, no solo para generar código.
- [ ] Puedo defender la solución sin mencionar “lo hizo la IA”.
- [ ] Si la IA propuso algo dudoso, lo validé con el equipo o lo descarté.

---

# 9. Revisión Crítica (Fase 6 de AGENTS.md)

Antes de pedir review humana, es mandatorio ejecutar la **Fase 6 de AGENTS.md** según el nivel de la tarea.

Fuente canónica de política: [`review/evaluator.md`](./review/evaluator.md) · Prompt operativo: [`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

Confirmo que:

- [ ] **QUICK:** se completó el `LIGHTWEIGHT_CLOSING_CHECK` (scope/diff check, validaciones ejecutadas, evidencia, result `PASS` o `ESCALATE_TO_STANDARD`).
- [ ] **STANDARD / ARCHITECTURAL:** se emitió el Review Contract con Verdict `PASS` o con findings documentados.
- [ ] El modo de revisión fue declarado: `INDEPENDENT_REVIEW` | `SELF_REVIEW`.
- [ ] Si la PR introduce ADRs `proposed`: se aplicó la Rúbrica de ADR Review (escala 1 a 5 en 4 dimensiones, puntaje ≥ 4.0/5.0) — separado del Review Contract de implementación.
- [ ] Se auditó la integridad del grafo documental (V9): `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md` sincronizados si correspondía.
- [ ] Findings BLOCKING resueltos y Gate 1 re-ejecutado antes de cerrar.
- [ ] Findings ADVISORY documentados en el Review Contract; el equipo humano decide si requieren acción.
- [ ] Si ARCHITECTURAL con `SELF_REVIEW`: reportado como `[SELF_REVIEW_FALLBACK]` en Reporte de Fase 7.

---

# 10. Calidad Estática y SonarCloud Quality Gate

Antes de abrir PR confirmo que:

- [ ] Verifiqué el diff contra [`07-errores-frecuentes-sonarcloud-ia.md`](./07-errores-frecuentes-sonarcloud-ia.md) para prevenir los *smells* y vulnerabilidades recurrentes de IA.
- [ ] Todo método privado auxiliar o de validación que no accede al estado de la instancia fue declarado `static` (`java:S2325`).
- [ ] Las clases utilitarias o factories tienen un constructor `private` explícito para prevenir instanciación (`java:S1118`).
- [ ] Las clases y métodos de test JUnit 5 tienen visibilidad de paquete (sin modificador `public` redundante `java:S5786`).
- [ ] Los métodos que implementan interfaces o sobreescriben comportamiento llevan explícitamente la anotación `@Override` (`java:S1161`).
- [ ] No existen cadenas literales duplicadas 3 o más veces; se extrajeron a constantes (`java:S1192`).
- [ ] Todo nuevo bloque condicional (`if`, `switch`, ternarios, `Optional`) cuenta con cobertura de pruebas en todas sus bifurcaciones (*Condition Coverage $\ge 80\%$*).
- [ ] Se ejecutó `mvn spotless:check` con resultado exitoso para asegurar formato estándar.

---

# 11. GitHub y comunicación

Confirmo que:

- [ ] La issue está actualizada.
- [ ] La PR referencia la issue correspondiente.
- [ ] El grafo documental está íntegro: no hay documentos huérfanos ni desfasajes en `docs/README.md` o `docs/ESTADO_DOCUMENTACION.md`.
- [ ] La descripción de la PR incluye el **Reporte Operativo Estructurado** (Fase 7 de `AGENTS.md`) documentando baseline, cambios, correcciones de la Fase 6 y Quality Gates superados.
- [ ] Si no hubo acceso al daemon de Docker en local, se declaró formalmente `[DEFERRED_NO_DOCKER]`.
- [ ] La PR es revisable y no demasiado grande.
- [ ] Si hubo decisiones relevantes, quedaron documentadas en un ADR o en la issue.
- [ ] Si hubo dudas, quedaron comentadas en canales públicos o en GitHub.

---

# 12. Declaración final

Antes de abrir PR, debería poder afirmar:

> Entiendo el cambio, validé su comportamiento, revisé los riesgos contra el Quality Gate de SonarCloud, emití el Review Contract (o `LIGHTWEIGHT_CLOSING_CHECK` si QUICK) con Verdict, y puedo hacerme responsable técnicamente por esta Pull Request.

Si no puedo afirmar eso, todavía no debería pedir review.

---

## Apéndice: Plantilla de Reporte Operativo (Fase 7 de AGENTS.md)

Incluir en la descripción de la PR:

```markdown
### 📋 Reporte Operativo — DonaTrack

#### 1. Resumen Ejecutivo y Alcance
* **Objetivo:** [Breve descripción de la tarea solicitada]
* **Estado de Baseline:** `[BASELINE_GREEN]` | `[BASELINE_RED (Detallar fallos preexistentes aislados)]`
* **Archivos Modificados:** [Listado de rutas relativas intervenidas]

#### 2. Matriz Epistémica de Cambios y Hallazgos
* `[OBSERVED]`: [Evidencias constatadas en código/git/tests antes de intervenir]
* `[DOCUMENTED]`: [ADRs, consignas de cátedra o contratos que respaldan el cambio]
* `[INFERRED]`: [Deducciones lógicas o hipótesis tomadas durante el análisis]
* `[PROPOSED]`: [Modificaciones arquitectónicas o de código implementadas]
* `[REJECTED]`: [Alternativas evaluadas y descartadas con justificación técnica]
* `[VERIFIED]`: [Comandos ejecutados, tests superados y validaciones de formato]

#### 3. Revisión Crítica (Fase 6)
* **Modo:** `[INDEPENDENT_REVIEW]` | `[SELF_REVIEW]` | `[LIGHTWEIGHT_CLOSING_CHECK]`
* **Fallback declarado:** `[SELF_REVIEW_FALLBACK]` si ARCHITECTURAL sin independencia real
* **Findings BLOCKING resueltos:** [Correcciones aplicadas antes del cierre]
* **Findings ADVISORY:** [Listado; decisión pendiente del equipo humano]
* **ADR Review (si aplica):** [Puntaje X.X/5.0 — separado del Review Contract de implementación]

#### 4. Validación y Quality Gates
* **Gate 1 (Unitario + Formato):** [✅ Aprobado]
* **Gate 2 (Módulo Completo):** [✅ Aprobado | ⏭️ Omitido por alcance]
* **Gate 3/4 (Integración / E2E):** [✅ Aprobado | ⚠️ `[DEFERRED_NO_DOCKER]`]

#### 5. Deuda Técnica y Hallazgos Colaterales
* [Hallazgos secundarios detectados pero NO modificados en esta iteración]
```