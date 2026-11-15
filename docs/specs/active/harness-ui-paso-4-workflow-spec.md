# Spec: harness-ui-paso-4-workflow — Workflow de oleada de interfaz con decisiones trazables

> **Estado:** implementada, sin correr de punta a punta (2026-11-15, rama `E6_harness_frontend_workflow`).
> Sale del plan del paso 4 que aprobó el usuario el 2026-11-15, con tres decisiones: un sucesor
> genérico de `backlog-r11.js` con su plantilla de manifiesto, `backlog-r11.js` al archivo (su prompt
> queda en `docs/IA/prompts/` con una nota de historia), y el manifiesto de R11 como registro. Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL. Cambia cómo los agentes producen la interfaz en corridas desatendidas
> (paso 4 del plan de migración del harness de frontend).
> **ADR:** ninguno nuevo. Implementa el paso 4 que ya describe el [ADR del harness de interfaz](../../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md)
> (`proposed`): no hay decisión nueva (§9.1, Two-Gate Rule, Gate A).
> **Depende de:** el [paso 3](harness-ui-paso-3-skills-spec.md) (PR #984) y el kit del paso 2 (PR #983).
> **Deuda:** [DTI-44](../../adr/DEUDA_TECNICA.md#dti-44), ítem 16.

## 1. Objetivo Funcional (Goal)

Que ninguna decisión de una ronda con el usuario se pierda en una corrida desatendida, como pasó con
R11-B04. Cada una tiene un ID y va a un lote con un criterio que el revisor comprueba, o queda
diferida con quién la difirió y dónde consta el OK. Que la revisión de interfaz bloquee de verdad y
que el usuario vea las pantallas de la oleada juntas antes de mergear.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope
- `.claude/workflows/oleada-ui.js`, con las fases de `backlog-r11.js` más "Revisar pantallas":
  - arranca solo con un manifiesto `oleada-ui/1` válido (`throw` con la lista de problemas);
  - las decisiones de cada lote entran en su ficha, y el revisor las rinde por ID con CUMPLE o NO
    CUMPLE; un NO CUMPLE es bloqueante;
  - los lotes con `rutasVisuales` van con Opus high en diseño, construcción, revisión y arreglos, y
    sin "Lo más simple que funcione";
  - en los lotes visuales, todo lo que falla en U1–U7 de `revisar-ui` es bloqueante; solo un PASS
    aprueba, y un revisor que no responde se vuelve a revisar;
  - cada decisión se rinde en una línea "<ID>: CUMPLE — evidencia" o "<ID>: NO CUMPLE — evidencia":
    solo un CUMPLE con su ID la da por cumplida, un NO CUMPLE es bloqueante aunque el revisor no lo
    liste, y una decisión sin rendir deja la revisión sin PASS;
  - "Revisar pantallas": un `revisar-ui` independiente (Opus high) sobre todas las pantallas, con
    las capturas de 360 y 1280 px lado a lado: hasta `RONDAS` revisiones, con un arreglo entre una y
    otra (`RONDAS` − 1 arreglos), como en los lotes; sin PASS no se abre el PR, y el resultado dice
    por qué;
  - la hoja de contacto en `docs/interfaz/evidencia/<oleada>/hoja/` y en el cuerpo del PR, con la
    casilla sin marcar "Hoja de contacto aprobada por el usuario";
  - las reglas propias de una oleada salen de `COMUN` y pasan a `M.reglas[]`.
- La plantilla `docs/interfaz/plantillas/manifiesto-oleada.ejemplo.json`.
- El check `UI_WAVE_MANIFEST` (`scripts/agent-check/checks/manifiestos-ui.js`) y sus tests (sección
  19 de `scripts/tests/run-tests.js`).
- `backlog-r11.js` a `docs/IA/history/harness-ui-2026-11/workflows/`; nota de historia en
  `docs/IA/prompts/orquestacion-backlog-r11.md`; la guía `docs/IA/prompts/orquestacion-oleada-ui.md`.
- Índices: `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md`, `docs/IA/README.md`,
  `docs/interfaz/README.md`, `docs/specs/README.md`, el README del archivo, la bitácora y DTI-44.

### Out-of-Scope
- Correr el workflow: necesita el opt-in del usuario. Queda sin verificar de punta a punta.
- Cambiar `scripts/visual/Imagenes.java`: `lado` deforma una captura de 1280 junto a una de 360, así
  que la hoja usa `jpg` y `resumen`.
- El gate de `auth-service`, que no está en el reactor raíz ([DTI-42](../../adr/DEUDA_TECNICA.md#dti-42),
  ítem 15): `gatesDe` sigue igual que en `backlog-r11.js`; queda como hallazgo.
- Editar registros: el manifiesto de R11, el §11 del backlog de R11 y las specs cerradas siguen
  nombrando `.claude/workflows/backlog-r11.js`.

## 3. Decisiones Acordadas y Trade-offs

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Sucesor genérico con plantilla (usuario) | Un workflow por backlog | Cada oleada nueva sería una copia de 400 líneas |
| La regla copiada en el workflow y en el check, con un test que compara las dos | Un módulo compartido | El sandbox del workflow no hace `require`; el test evita que se separen |
| "Criterio comprobable" se exige como texto presente | Validar su forma | Que sea comprobable lo juzga el revisor; una máquina solo ve si está |
| Sin PASS de pantallas, o con un lote visual o con decisiones sin PASS (por bloqueantes, o porque no se construyó o no se integró), no hay PR | Abrir el PR con pendientes, como R11 | La revisión de interfaz es bloqueante (ADR, paso 4) |
| Hoja con `jpg` + `resumen` y una tabla en el PR | `lado` de `scripts/visual` | `lado` usa el ancho menor para los dos lados y deforma la de 1280 (probado el 2026-11-15) |
| Opus high fijo en el código para las vistas | Configurable en el manifiesto | Es decisión del ADR; un manifiesto no la cambia |
| Las diferidas se informan en cada PR de la oleada | Atarlas a una ola | Una diferida no tiene lote; el usuario la ve donde decide |

## 4. Invariantes

- Con una decisión sin asignar, el workflow no llama a ningún agente y `agent-check` falla.
- Cada decisión de los lotes de la ola vuelve en el resultado y en el PR por ID: cumplida, no
  cumplida con el motivo, o pendiente; las diferidas, con quién y dónde consta el OK.
- Solo un PASS aprueba una revisión; un revisor mudo no cuenta como PASS, y una decisión sin su línea
  CUMPLE tampoco.
- La documentación de cierre se escribe después de la revisión de pantallas, con el estado final de
  cada decisión.
- Un lote visual o con decisiones que termina sin PASS de su revisión, por la razón que sea, frena
  el PR. Un lote condicional cuya condición no se pasó no corre y no frena: sus decisiones vuelven
  como no cumplidas, con la condición como motivo.
- Ningún registro histórico ni ADR se edita.

## 5. Criterios de Aceptación

- **Dado** un manifiesto `oleada-ui/1` con una decisión sin `lote` ni `diferida`, **cuando** corre
  `node scripts/agent-check.js`, **entonces** falla `UI_WAVE_MANIFEST`; **y cuando** se lanza el
  workflow, **entonces** lanza un error que dice "sin asignar" antes de llamar a un agente.
- **Dado** una decisión con un lote que no existe, sin `criterio`, diferida sin `ok`, con un id
  repetido o con lote y diferida a la vez, **cuando** corre `agent-check`, **entonces** falla.
- **Dado** un JSON de `docs/` sin `formato` (el manifiesto de R11) o un manifiesto bajo
  `docs/IA/history/`, **cuando** corre `agent-check`, **entonces** no lo evalúa.
- **Dado** el workflow con agentes falsos que aprueban todo, **cuando** corre la plantilla,
  **entonces** pasa por la revisión de pantallas, la hoja y el PR, y rinde D1 y D2 como cumplidas
  y D3 como diferida.
- **Dado** que la revisión de pantallas no llega a PASS, **cuando** termina, **entonces** no arma la
  hoja ni abre el PR, y las decisiones de los lotes visuales quedan como no cumplidas.
- **Dado** una revisión de lote con PASS que rinde una decisión como NO CUMPLE, que omite una, o un
  lote con decisiones cuyo constructor no responde, **cuando** termina la oleada, **entonces** no se
  abre el PR y esa decisión no vuelve como cumplida.

## 6. Riesgos

- El workflow no se corrió con agentes reales: las pruebas usan agentes falsos que contestan con el
  esquema. Lo verifica la primera oleada real.
- Un revisor que no respeta el formato de las decisiones deja el lote sin PASS: es más caro, pero no
  deja pasar una decisión sin comprobar.

## 7. Especificación Técnica de Diseño (Technical Design)

Regla `oleada-ui/1` (`problemasDelManifiesto`): `formato`, `nombre` (minúsculas, números y guiones),
`backlog`, `prefijoCommit`, `ramas` (base, destino, prefijoOla), `olas`, `reglas` y `lotes`
(id único, ola existente, tanda entera, listas `items`, `modulos` y `rutasVisuales`, `ruta`,
`disenio` y `rol` de una lista cerrada). `ensayo` es opcional, con `guia` y `lotes` existentes.
Cada decisión: `id` único, `texto`, `quien` y `ok`, y exactamente uno de `lote` (existente, con
`criterio`) o `diferida` (`quien` y `ok`).

Diferencias con `backlog-r11.js`, además de lo de §2:
- `COMUN` sin las reglas propias de R11: el renombre de Organización (`p1-nombres`) y "nunca una
  lista completa de un servicio". Una oleada nueva pone las suyas en `M.reglas`; la plantilla trae
  la segunda como ejemplo. Fichas y specs con `<nombre>-<lote>`, stack `donatrack-<nombre>` y
  ensayo por `M.ensayo`, en lugar de los ítems R11-03 y R11-B08.
- El PR se abre como borrador (`gh pr create --draft`).

Plan TDD: los tests 19.1 a 19.22 de `run-tests.js` cubren la regla, que las dos copias coincidan,
el arranque del workflow (`AsyncFunction` sin el `export`) y corridas con agentes falsos: todo
PASS, pantallas sin PASS, NO CUMPLE, decisión omitida, constructor mudo, modelos de las vistas,
"Lo más simple", PR en borrador, la casilla de la hoja y líneas contradictorias por ID.

## 8. Validación

- `node scripts/agent-check.js` sin fallas, con `UI_WAVE_MANIFEST` en PASS sobre la plantilla.
- `node scripts/tests/run-tests.js` en verde (129 casos).
- Sin Java tocado: no corre Maven. El workflow no se corrió con agentes reales.
