# Orquestación de una oleada de interfaz

> **Versión 1.0.0 · 2026-11-15**
> Guía de la sesión que corre el workflow `oleada-ui` (`.claude/workflows/oleada-ui.js`). Sucede a
> la [orquestación del backlog R11](orquestacion-backlog-r11.md), que queda como historia. Decisión:
> [ADR del harness de interfaz](../../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md),
> paso 4 (`proposed`); detalle en la [spec del paso 4](../../specs/active/harness-ui-paso-4-workflow-spec.md).

## Antes de lanzar

1. **Hay un backlog** en `docs/interfaz/` con los ítems y su evidencia (archivo y línea).
2. **Hubo ronda de decisiones con el usuario.** Las reglas de cómo preguntar están en
   [`docs/interfaz/README.md`](../../interfaz/README.md). Cada respuesta es una decisión con ID.
3. **El manifiesto** parte de la
   [plantilla](../../interfaz/plantillas/manifiesto-oleada.ejemplo.json) y vive junto al backlog,
   con `formato: "oleada-ui/1"`. Cada decisión lleva `id`, `texto`, `quien` y `ok` (dónde consta el
   OK), y uno de estos dos destinos:
   - `lote` con su `criterio`: lo que el revisor mira para decir CUMPLE;
   - `diferida: {quien, ok}`: quién la difirió y dónde consta.
4. **`node scripts/agent-check.js` pasa** (`UI_WAVE_MANIFEST`). Con una decisión sin asignar falla,
   y el workflow tampoco arranca.
5. El manifiesto, el backlog y las fichas están comiteados en `ramas.base`. `docker` y `gh`
   responden. La cuenta de `gh` es la del dueño del repo.
6. **El usuario dio el opt-in** para correr un workflow: lanza agentes en paralelo.

## Lanzar

```
Workflow({ name: "oleada-ui",
           args: { manifiesto: <el JSON como objeto>, ola: "P0", fase: "disenar",
                   carpeta: "<carpeta de trabajo, fuera del repo>" } })
```

| `args` | Valor |
|---|---|
| `manifiesto` | El JSON como objeto, no como texto |
| `ola` | Una de `olas` |
| `fase` | `"disenar"`: fichas y specs para aprobar juntas. `"construir"`: el resto |
| `carpeta` | Fuera del repo; ahí quedan cierre, decisiones y cuerpo del PR |
| `condiciones` | Opcional: las condiciones cumplidas, para los lotes con `condicion` |
| `pr` | Opcional: `false` para no hacer push ni abrir PR |

Por cada ola: `disenar`, el usuario aprueba los diseños de una vez y después `construir`.

## Qué hace `construir`

1. Por tanda: constructores en paralelo (cada uno en su worktree), integración y revisión de cada
   lote. En los lotes con pantallas, diseño, construcción, revisión y arreglos van con Opus high (el
   integrador y el cierre siguen con el modelo mecánico), y pasan `revisar-ui` (U1–U7): toda
   inconsistencia con el sistema visual es bloqueante. Cada decisión del lote se rinde en una línea
   "<ID>: CUMPLE" o "<ID>: NO CUMPLE": un NO CUMPLE bloquea y una sin rendir no aprueba. Solo un
   PASS aprueba; hasta `topes.rondasDeReparacion` revisiones, con un arreglo entre una y otra.
2. Cierre: gates completos, stack propio `donatrack-<nombre>` con seed y recorrido con login real,
   y ensayo si `M.ensayo` lo pide.
3. **Revisar pantallas:** un `revisar-ui` independiente sobre todas las pantallas de la oleada, con
   las capturas de 360 y 1280 px lado a lado, con el mismo tope de revisiones. Después, la
   documentación de cierre, con el estado final de cada decisión.
4. **Hoja de contacto** en `docs/interfaz/evidencia/<nombre>-<ola>/hoja/`: los JPG y `hoja.html`.
5. PR borrador con las decisiones por ID, la hoja y la casilla "Hoja de contacto aprobada por el
   usuario", sin marcar.

**Sin PR** si la revisión de pantallas no llega a PASS, o si queda sin PASS un lote con pantallas o
con decisiones, también cuando no se construyó o no se integró. `detenida` dice por qué y la rama
queda local.

## Después

- El resultado es un reporte, no una prueba. Mirá `git log` de la rama, el PR y
  `<carpeta>/<ola>/cierre.md`.
- `decisionesDelUsuario` trae cada decisión por ID: cumplida, no cumplida con el motivo, pendiente o
  diferida. Una no cumplida va al informe para el usuario, primera en la lista.
- El usuario marca la casilla de la hoja de contacto. No la marques vos.
- No edites el PR mientras corre su CI. No mergees ni actives el merge automático.

## Versiones

| Versión | Fecha | Qué cambió | Formato del manifiesto |
|---|---|---|---|
| 1.0.0 | 2026-11-15 | Primera versión: sucesor genérico de `backlog-r11`. | `oleada-ui/1` |
