---
name: corrida-creativa
description: >-
  Corre sin el usuario un diseño creativo desde cero en el cliente liviano de DonaTrack a partir
  de un encargo creativo aprobado: iteraciones que escalan (concepto de una pantalla, sistema y
  flujo completo) con constructores en paralelo, verificación del piso, un panel de críticos que no
  diluye la audacia, decisión con matriz, arreglos, evidencia, bitácora y un commit de control por
  iteración; cierra con la página de antes y después, la auditoría y el PR si el encargo lo
  autoriza. Usar después de relevamiento-creativo, o cuando el usuario pide correr un encargo
  creativo o el workflow corrida-creativa.
---

# Skill: corrida-creativa — Diseño creativo desatendido

> **Ámbito:** `cliente-liviano/` y `docs/interfaz/` (E6).
> **Alineación:** [`AGENTS.md`](../../../AGENTS.md) (§6 alcance, §7 niveles, §11 gates, §12 cierre),
> [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md),
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md) ("Libertad creativa" y
> "Oficio") y las plantillas de [`docs/interfaz/plantillas/`](../../../docs/interfaz/plantillas/)
> (`encargo-constructor.md` y `encargo-critico.md`).
> **Rama = gate:** durante la corrida no se le pregunta nada al usuario; decide con el merge.

## 1. Qué necesita

- Un **encargo creativo aprobado** en `docs/interfaz/encargos/<nombre>.md`, sin "a definir". Si
  falta, primero [`relevamiento-creativo`](../relevamiento-creativo/SKILL.md).
- **Árbol limpio, en una rama propia.** Con PR hacia `ENTREGA_6`, la rama tiene que empezar con
  `E6_` (lo exige el CI): si no, la corrida la renombra antes de subirla.
- Una **carpeta de trabajo fuera del repo** (el scratchpad o la carpeta de notas de la sesión) para
  los encargos llenos, las capturas, las críticas, las decisiones y las notas de traspaso.
- La sesión en Opus. Cada rol declara su modelo y su esfuerzo:

  | Rol | Modelo y esfuerzo | Por qué |
  |---|---|---|
  | Director (encargo de la iteración), decisor, constructores | Opus, alto | Diseño |
  | Arregladores | Opus, medio | Implementación sobre una idea ya decidida |
  | Críticos (arte, piso, flujo) | Opus, medio | Lectura con citas |
  | Preparar, integrar, cerrar, cierre, PR | Sonnet, alto | Mecánico |
  | Auditoría de cierre (`qa-gatekeeper`) | Sonnet, alto | Nivel STANDARD |

## 2. Cómo se lanza

Con el workflow guardado [`.claude/workflows/corrida-creativa.js`](../../../.claude/workflows/corrida-creativa.js),
por nombre (invocarlo es el pedido explícito del usuario de correrlo):

```text
Workflow({ name: "corrida-creativa", args: {
  encargo: "docs/interfaz/encargos/<nombre>.md",   // obligatorio
  carpeta: "<ruta absoluta fuera del repo>",       // obligatorio
  iteraciones: 1, conceptos: 2, pr: false,         // opcionales: pisan el encargo
  inicio: 2,                                       // opcional: retomar desde esa iteración
  auditoria: true                                  // opcional; por defecto, sí
}})
```

Sin la herramienta Workflow, los mismos pasos se hacen a mano con la herramienta Agent (los
constructores con `isolation: "worktree"`), siguiendo la sección 3. Al terminar, mandarle al usuario
la página de antes y después y el enlace del PR.

## 3. Qué hace cada iteración

Las iteraciones escalan: cada una es más ambiciosa que la anterior y parte del commit de control de
la anterior.

| Iteración | Qué se construye | Dónde | Partes en paralelo |
|---|---|---|---|
| 1. Concepto | Una pantalla (la ancla del encargo), con su navegación y su voz | Variantes de estructura `templates/variantes/concepto-<letra>.html` y su CSS; se ven con `?variante=` también sobre `/prototipo/<pantalla>` | Un concepto por constructor (de 2 a 4), opuestos en composición y navegación |
| 2. Sistema | El ganador en el juego real de plantillas: navegación, layouts, pantallas y catálogo `/estilos` | `templates/alternativas/<alt>/` y `static/css/alternativas/<alt>/`, sin plantillas nuevas (gemelas) | Zonas de propiedad con archivos disjuntos (por ejemplo, estructura y componentes) |
| 3. Flujo | El recorrido completo como prototipo navegable | `/prototipo/<pantalla>` (hoy `PrototipoController` sirve solo a Cozy / Medieval: para otra alternativa, la corrida para en el sistema y lo anota) | Un grupo de pantallas por rol |

Pasos de cada iteración (cada uno es un agente; los de "paralelo" respetan un tope de 5 por paso):

1. **Encargo (director).** Elige las partes (conceptos divergentes o zonas), hace el commit de
   preparación si hace falta (hojas de cada zona enlazadas, tabla de voz) y llena las dos plantillas
   en la carpeta de la iteración: `encargo-constructor.md` y `encargo-critico.md`.
2. **Construir (en paralelo, cada uno en su worktree).** Cada constructor parte de la base, construye
   su parte en serio, comprueba el piso, mira sus capturas y se critica al menos dos vueltas, y
   comitea solo sus archivos.
3. **Integrar.** Trae cada commit a la rama de la corrida, corre `InterfazFitnessTest` y una sola
   verificación visual conjunta, y copia capturas, `reporte.json` y el visor a la carpeta.
4. **Criticar (en paralelo, solo lectura).** Tres críticos con lentes distintas (arte, piso y flujo)
   y la regla "no diluyas la audacia": cada hallazgo con un arreglo que conserva la idea; `BLOQUEA`
   solo para piso, seguridad o tarea.
5. **Decidir.** Matriz con criterios ponderados (mensaje del encargo, audacia, oficio, piso y si da
   para todo el sitio), el ganador, qué se toma de los otros y la lista de arreglos por parte.
6. **Arreglar (en paralelo, por parte).** Cada arreglo conserva la idea; lo que no se puede hacer
   sin traicionarla se explica.
7. **Cerrar la iteración.** Gates (`mvn test -pl cliente-liviano`, el piso con `reporte.json` en
   `[]` y la comparación píxel a píxel de las identidades que no se diseñan), evidencia en JPG de
   400 KB o menos en `docs/interfaz/evidencia/<alt>/<tema>/`, la entrada de la bitácora (con lo que
   reemplaza), el commit de control y la nota de traspaso en la carpeta.

Con una sola iteración, los conceptos quedan como variantes en la rama para que el usuario los
compare (`comparacion.html` o `?variante=` en la maqueta); desde la segunda, la zona "estructura"
los borra al llevar el ganador al juego real.

## 4. Cierre de la corrida

1. **Página de antes y después**, con `java scripts/visual/Imagenes.java resumen` (ver
   [`scripts/visual/README.md`](../../../scripts/visual/README.md)), en la carpeta de trabajo: no
   entra al repo.
2. **Documentación al día:** README de interfaz, `docs/ESTADO_DOCUMENTACION.md` y, en el flujo, la
   ficha `docs/interfaz/flujo-completo-<alt>.md`; `node scripts/agent-check.js`.
3. **Auditoría de cierre:** la autorrevisión (una fila por criterio), un pre-auditor
   (`lector-auditoria`, si está) y `qa-gatekeeper` en Sonnet alto; hasta dos rondas con arreglos.
4. **PR**, solo si el encargo lo autoriza y la auditoría dio PASS: rama `E6_*`, PR en borrador hacia
   la rama base con la plantilla de [`docs/IA/04-checklist-antes-de-pr.md`](../../../docs/IA/04-checklist-antes-de-pr.md).
   Antes del push, comprobar la cuenta de GitHub activa.

## 5. Los guardarraíles que habilitan la libertad

- **Piso automático:** `VerificacionVisualTest` (axe AA, 48 px, desbordes en 3 anchos y en los
  intermedios, y la primera pantalla del celular en 360 × 640).
- **Gemelas y voz:** `InterfazFitnessTest` (mismos datos y acciones; la voz solo en títulos y
  rótulos; solo tokens de uso; los quiebres de `tokens.css`).
- **Lo que no se diseña no cambia:** captura antes de empezar y `Imagenes.java comparar` en cada
  cierre.
- **Libertad y oficio:** la dirección de arte. Nunca "cambio mínimo" en una exploración.

## 6. Fricciones previstas

| Fricción | Qué hace la corrida |
|---|---|
| Se acaba el límite de uso a mitad | En la misma sesión, `Workflow({ scriptPath, resumeFromRunId })`: los agentes terminados vuelven de la memoria. En otra sesión, `inicio` = la iteración siguiente a la última nota de traspaso |
| El contexto del orquestador crece | El workflow no acumula lo que leen los agentes: cada uno lee archivos y deja su resultado en la carpeta |
| Una edición de un solo comando rompe un archivo | Leer la línea antes de editarla; nada de `sed` con números de línea calculados |
| Las barras fijas salen a mitad de la captura completa | Mirar también la primera pantalla (360 × 640); si hay duda, el navegador con un puerto propio |
| `paginas.txt` editado a mano para acotar | `-Dverificacion.paginas=a,b` (sin la barra inicial en Git Bash) |
| Las herramientas de imagen vivían en carpetas temporales | `scripts/visual/Imagenes.java` |

## 7. Criterio de salida

- [ ] Cada iteración con su commit de control, su entrada en la bitácora y su evidencia.
- [ ] `mvn test -pl cliente-liviano` en verde, `reporte.json` en `[]` y las identidades que no se
      diseñan sin cambios (comparación píxel a píxel).
- [ ] Página de antes y después en la carpeta de trabajo, mandada al usuario.
- [ ] Documentación al día y `node scripts/agent-check.js` en verde.
- [ ] Auditoría de cierre con PASS (o lo que quedó abierto, declarado).
- [ ] PR en borrador si el encargo lo autoriza; si no, la rama lista y sin push.
