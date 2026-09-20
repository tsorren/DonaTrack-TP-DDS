# ADR Governance — DonaTrack

> Fuente canónica de política de decisiones arquitectónicas.  
> Referenciada desde [`AGENTS.md §9`](../../AGENTS.md).

---

## Propósito

Un ADR (Architecture Decision Record) documenta una **decisión arquitectónica**: qué se eligió, qué alternativas se evaluaron y por qué. No es un how-to de implementación ni una spec de tarea. Los ADRs son permanentes; cuando una decisión cambia, se crea uno nuevo que la reemplaza.

---

## Cuándo crear un ADR — Two-Gate Rule

Crear un ADR `proposed` solo cuando se cumplen **ambos**:

### Gate A — Decision novelty

¿La tarea introduce una decisión arquitectónica **nueva**?

No hay decisión nueva cuando:
- la tarea implementa un ADR ya aceptado;
- se agrega un endpoint dentro de un contrato o patrón ya decidido;
- se agrega un consumer siguiendo la arquitectura de mensajería adoptada;
- se aplica un patrón de diseño ya existente en el proyecto a un nuevo caso.

Si NO → no crear ADR. Si SÍ → Gate B.

### Gate B — Architectural significance

¿La nueva decisión es arquitectónicamente significativa?

Señales a evaluar (impacto real; no son triggers mecánicos):
- afecta la comunicación o integración entre servicios
- cambia estrategia de persistencia o modelo de datos
- introduce tecnología, framework o dependencia estructural nueva
- modifica límites entre bounded contexts, módulos o capas
- implica trade-off deliberado entre atributos de calidad
- tiene alto costo de reversión
- reemplaza un ADR aceptado o un patrón arquitectónico central al dominio
- tiene implicancias de privacidad, seguridad o compliance

`NEW DECISION + ARCHITECTURAL SIGNIFICANCE = ADR`

---

## Cuándo NO crear un ADR

- Bugs que no alteran contratos ni arquitectura.
- Cambios cosméticos, de formateo o comentarios.
- DTOs internos sin impacto externo.
- Tests con herramientas ya adoptadas en el proyecto.
- Endpoints o consumers que siguen patrones establecidos.
- Refactors locales sin cambio de límites ni contratos.
- Implementación de una decisión ya documentada y aceptada.
- Aplicar un patrón de diseño que el proyecto ya usa a un nuevo caso.

---

## Lifecycle y autoridad de estados

| Estado | Quién autoriza | Significado |
|---|---|---|
| `proposed` | Agente o desarrollador | Decisión formulada; pendiente de aprobación humana |
| `accepted` | Solo revisor humano en PR | Decisión aprobada formalmente. `accepted` ≠ `implemented` |
| `rejected` | Solo revisor humano en PR | Alternativa descartada. El archivo se preserva |
| `superseded` | Propuesto por agente; aprobado por humano | Nueva decisión reemplaza a la anterior |

**Prohibición absoluta:** Ningún agente puede auto-promover un ADR a `accepted` o `rejected` por ningún motivo — incluyendo código ya presente en main o decisión preexistente. Código existente ≠ aprobación arquitectónica.

Si el agente descubre una decisión implementada pero no documentada: crear ADR `proposed` indicando `[OBSERVED] El código actual ya implementa esta decisión`. La aprobación es exclusivamente humana.

---

## Spec vs ADR

**Spec** responde: qué queremos construir, alcance, constraints, validación.  
**ADR** responde: qué decisión arquitectónica tomamos, alternativas, consecuencias, por qué.

| Situación | Spec | ADR |
|---|---|---|
| Implementar ADR ya aceptado | Sí (define scope) | No (la decisión ya existe) |
| Nueva decisión en tarea ARCHITECTURAL | Sí | Sí (`proposed`) |
| Decisión emergida de auditoría o discusión | No necesariamente | Sí (`proposed`) |

El spec no toma decisiones arquitectónicas; las referencia. El ADR no describe cómo implementar; documenta por qué se eligió. El ADR puede referenciar la spec sin copiar su contenido.

---

## ADR status ≠ implementation status

Son ejes independientes. La aceptación depende de autoridad humana, no del estado del código.

| ADR status | Implementation | Significado |
|---|---|---|
| `proposed` | not-started | Decisión formulada; implementación pendiente |
| `proposed` | in-progress | Implementación adelantada; riesgo si el ADR es rechazado |
| `accepted` | not-started | Decisión aprobada; implementación futura (entrega posterior) |
| `accepted` | in-progress | Implementación en curso — estado válido |
| `accepted` | implemented | Estado ideal |
| `superseded` | legacy-present | Deuda técnica; documentar en `DEUDA_TECNICA.md` |

Si el código contradice un ADR `accepted`: reportar en Fase 7 sin resolver unilateralmente. Es una violación arquitectónica o el ADR requiere un sucesor.

---

## Bifurcación temporal

Una decisión para una entrega futura puede documentarse en un ADR `proposed` sin introducir código especulativo. El ADR existe para reservar la decisión. El código solo cambia cuando el scope de la entrega correspondiente lo autoriza. `accepted` no significa `implemented`.

---

## Formato MADR

Todo ADR debe incluir:

| Campo | Obligatorio | Nota |
|---|---|---|
| `Status:` | Sí | `proposed` / `accepted` / `rejected` / `superseded` |
| `Date:` | Sí | YYYY-MM-DD |
| `Deciders:` | Sí | Quién participó en la decisión |
| `Tags:` | Recomendado | Facilita navegación en Log4brains |
| Contexto y Problema | Sí | Por qué existe esta decisión |
| Alternativas Consideradas | Sí | Las alternativas materialmente razonables evaluadas, incluyendo el status quo si es una opción real. Si no existe alternativa genuina, explicar por qué. |
| Resultado de la Decisión | Sí | Alternativa elegida + justificación |
| Consecuencias Positivas/Negativas | Sí | Trade-offs de la elección |
| Análisis por alternativa | Sí | Pros/contras de cada alternativa evaluada |
| Origen / contexto de entrega | Opcional | Solo para ADRs de refactor o deuda técnica |
| Trabajo Futuro / Links | Opcional | Referencias a issues o ADRs relacionados |

Los ADRs históricos (pre-Oleada 5) mantienen su formato original. No reescribir documentos históricos.

---

## Benchmark de calidad (Fase 6)

Todo ADR `proposed` incluido en una PR es auditado en Fase 6 de Revisión Crítica en 4 dimensiones:

1. **Profundidad Técnica y Alternativas** — alternativas materialmente razonables con pros/contras sólidos.
2. **Adherencia a Principios Académicos** — justificación en SOLID, GRASP, GoF y DDD.
3. **Factibilidad Operativa** — realismo técnico compatible con Java 21 / PostgreSQL / Docker.
4. **Factualidad y Enlaces** — citas de clases, métodos y links 100% operativos sin alucinaciones.

Umbral mínimo: promedio ≥ 4.0 / 5.0.

Marco de auditoría completo: [`docs/auditoria/plan-revisor-critico.md`](../auditoria/plan-revisor-critico.md).

---

## Tooling — Log4brains

```bash
npm install -g log4brains
log4brains preview        # Previsualización local con hot reload
log4brains adr new        # Crear nuevo ADR interactivo
```

* Portal local de Log4brains: [`index.md`](index.md)
* Base de conocimientos publicada: https://tsorren.github.io/DonaTrack-TP-DDS/adr-preview

---

## Reglas de compatibilidad con Log4brains

> [!IMPORTANT]
> El build de GitHub Pages (job `Build Log4brains`) falla silenciosamente si el campo `Status:` no respeta estas reglas. Los errores `"You forgot to pass the superseder"` y `"No entry in the configuration for this package"` son señales directas de estas violaciones.

### Mecanismo real de resolución

Log4brains usa el **texto del link** en `superseded by TEXTO(href)` como **slug** para llamar internamente a `getAdrBySlug(TEXTO)`. El href se usa solo para renderizar el hipervínculo, no para resolver el ADR sucesor en el índice.

El slug sigue el formato `package/filename-sin-extension` para ADRs de paquete, y `filename-sin-extension` para ADRs globales, según los paquetes definidos en [`.log4brains.yml`](../../.log4brains.yml).

### Regla 1 — Texto del link = slug canónico del ADR sucesor

| Tipo de sucesor | Formato del texto del link |
|---|---|
| ADR en el mismo paquete | `paquete/YYYYMMDD-nombre-del-adr` |
| ADR en paquete diferente | `otro-paquete/YYYYMMDD-nombre-del-adr` |
| ADR global (carpeta raíz `docs/adr/`) | `YYYYMMDD-nombre-del-adr` |

Los paquetes disponibles son: `donaciones`, `notificaciones`, `logistica`, `incentivos`, `cliente-liviano`, `auth`.

**Ejemplo — ADR de paquete superado por ADR del mismo paquete:**

```
- Status: superseded by donaciones/20260609-gestion-de-necesidades-y-periodos (href: ./20260609-gestion-de-necesidades-y-periodos.md)
```

**Ejemplo — ADR de paquete superado por ADR global:**

```
- Status: superseded by 20260901-limites-y-responsabilidades-del-shared-kernel-common-lib (href: ../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md)
```

**Ejemplo — ADR global superado por otro ADR global:**

```
- Status: superseded by 20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones (href: ./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md)
```

### Regla 2 — El texto del link NO debe contener `/` si el sucesor es global

Cuando el texto contiene `/`, Log4brains lo interpreta como `package/adr-id`. Si el texto fuera `Topología Pub/Sub AMQP...`, intentaría `PackageRepository.find("Topología Pub")` y fallaría con:

```
Error: No entry in the configuration for this package (Topología Pub)
```

Los ADRs globales no tienen prefijo de paquete: su slug es solo el filename sin `.md`.

### Regla 3 — El href refleja la ruta real del archivo (para el hipervínculo)

El href sí puede usar `./` para el mismo directorio o `../` para navegar a la raíz global. Esto no afecta la resolución del sucesor (que depende del texto), pero sí el enlace navegable en la UI.

### Señales de error y su causa

| Error en el build | Causa |
|---|---|
| `"You forgot to pass the superseder"` | El texto del link no corresponde a ningún slug en el índice de Log4brains |
| `"No entry in the configuration for this package (X)"` | El texto contiene `/` y la parte antes de `/` no coincide con ningún paquete en `.log4brains.yml` |

### Checklist de verificación pre-PR

Antes de hacer push de un ADR con `Status: superseded`, verificar:

- [ ] El texto del link usa el slug canónico: `paquete/YYYYMMDD-filename` para ADRs de paquete, o `YYYYMMDD-filename` para globales.
- [ ] El texto del link **no** contiene `/` si el sucesor es un ADR global.
- [ ] El archivo sucesor existe en el path indicado por el href.
- [ ] Previsualizar localmente con `log4brains preview` antes de la PR para detectar errores de pre-render.

