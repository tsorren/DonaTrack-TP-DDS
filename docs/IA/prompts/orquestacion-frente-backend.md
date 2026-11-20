# Orquestación de un frente de backend

> **Versión 0.4.0 · 2026-11-20 · `[PROPOSED]`**
> Guía del workflow `frente-backend`, que implementa un frente de backend a partir de un manifiesto
> `frente-backend/1`. El script es `.claude/workflows/frente-backend.js` y esta guía es su contrato.
> **Todavía no corrió con agentes reales:** lo cubren los tests de la sección 20 de
> `scripts/tests/run-tests.js`, con agentes falsos. Primer uso: las tareas G1 a G4 de la Entrega 7
> ([SPEC-11](../../specs/active/SPEC-11-graphql-de-solo-lectura-en-donaciones.md) y su
> [manifiesto](../../specs/active/SPEC-11-graphql.manifiesto.json)). Toma el esqueleto de
> [`oleada-ui`](orquestacion-oleada-ui.md), sin las fases de pantallas.

## Qué quiere decir determinista

El orden del trabajo sale del manifiesto y lo ejecuta el script, no un agente. Qué agente corre,
con qué modelo, con qué encargo y en qué orden es función de tres cosas: el manifiesto, la fase y
los veredictos de las auditorías. Ningún agente decide el orden ni qué se revisa. Lo que sí decide
un agente (un nombre, un valor, un detalle de diseño, o tocar un archivo fuera de su zona porque es
imprescindible) queda anotado en su resultado y lo ve el auditor.

## Qué manda durante la corrida

Decisión del dueño del frente del 2026-11-20 (D07 de SPEC-11), `[PROPOSED]` para el grupo: **manda
el workflow**. Los agentes siguen este orden:

1. **Las reglas duras** del manifiesto (`precedencia.reglasDuras`). No se pisan nunca.
2. **El manifiesto:** las decisiones, las reglas de la corrida y la zona de archivos del lote.
3. **Esta guía.**
4. **`AGENTS.md` y el resto del repo**, como orientación. Si chocan con el manifiesto, gana el
   manifiesto: el agente anota el choque en `conflictos` (la regla, qué hizo y por qué) y sigue.

Las reglas duras de `frente-backend/1` son cuatro grupos:

| Grupo | Qué protege | Se exige en |
|---|---|---|
| Integridad | Tests y gates sin debilitar, sin secretos, `AGENTS.md` y los documentos históricos sin tocar, ningún ADR promovido, nunca un merge ni un push a la rama de destino | Cada tarea |
| Controles que corre el CI | `node scripts/agent-check.js` con 0 FAIL, `mvn spotless:check`, ArchUnit y el pre-flight de SonarCloud | Cada tarea |
| Arquitectura | Las reglas del ADR que implementa el frente; cada manifiesto las escribe | Cada tarea |
| Documentación y evidencia | Índices de `docs/` sincronizados y etiquetas de `AGENTS.md` §3 en el reporte | La auditoría de cierre |

Los índices los toca solo el paso de documentación. Por eso un índice desactualizado no bloquea la
auditoría de una tarea: se exige una vez, en el cierre.

Qué queda flexible, y con qué se reemplaza:

| Regla del repo | En la corrida |
|---|---|
| Alcance mínimo y refactors (`AGENTS.md` §6) | El alcance de un lote es su campo `archivos`. Tocar algo de afuera se permite si es imprescindible, y se declara en `fueraDeZona` |
| Niveles de tarea y los siete pasos (`AGENTS.md` §7) | Las fases del workflow |
| Ciclo de skills, de `define-spec` a `implementation-review` | La fase `disenar`, los controles humanos y la auditoría por tarea |
| Preferencias de sesión del usuario | No aplican a los agentes: no preguntan y hacen commits de control |

El control de dependencias de `AGENTS.md` §6 **no** es flexible: una dependencia que no esté en el
`que` del lote no se agrega, y el agente la anota en `pendientes`.

> **Límite de esta decisión.** `AGENTS.md` se declara obligatorio e inmutable para agentes (encabezado, campo «Ámbito») y
> solo se cambia con un PR dedicado y revisión humana (§13). Este régimen no está previsto ahí y no
> lo modifica: vale dentro de la corrida, y el reporte del PR lista cada regla flexibilizada con su
> `conflicto`. Antes de la primera corrida real, el grupo decide cómo se formaliza: un PR a
> `AGENTS.md`, o una excepción temporal en `docs/adr/DEUDA_TECNICA.md`.

## Antes de lanzar

1. **Hay una spec** en `docs/specs/active/` con las decisiones del frente por ID.
2. **Hubo ronda de decisiones con el dueño del frente.** Cada respuesta es una decisión con ID.
3. **El manifiesto** vive junto a la spec, con `formato: "frente-backend/1"`.
4. **`node scripts/agent-check.js` pasa.**
5. La rama `ramas.trabajo` existe y tiene comiteados la spec, el manifiesto, esta guía y el
   script. El workflow no la crea. El script se agrega con `git add -f`: `.claude/` puede estar
   excluido en la copia local, y sin el script el CI falla (lo lee `run-tests.js`).
6. No hay cambios sin comitear en archivos versionados. Los archivos sin versionar que no son de
   la corrida se toleran: ningún agente los comitea.
7. `gh` responde. `docker` también, si se quiere correr el Gate 3 o los tests con Testcontainers.
8. **El usuario dio el opt-in** para correr un workflow: lanza agentes en paralelo.

## Lanzar

```
Workflow({ name: "frente-backend",
           args: { manifiesto: <el JSON como objeto>, fase: "disenar",
                   carpeta: "<carpeta de trabajo, fuera del repo>" } })
```

| `args` | Valor |
|---|---|
| `manifiesto` | El JSON como objeto, no como texto |
| `fase` | `"disenar"`, `"construir"` o `"pr"` |
| `carpeta` | Fuera del repo, y **la misma en las tres corridas**: ahí vive el estado |
| `disenoAprobado` | Solo en `construir`: el SHA del diseño que aprobó el dueño |
| `stackDeLaRama` | Opcional, en `construir`: `true` si el dueño levantó los servicios de esta rama. Sin eso, los gates de `gates.siHayStack` no corren |
| `condiciones` | Opcional: las condiciones cumplidas, para los lotes con `condicion` |

Son tres corridas, con un control humano entre una y otra:

```
disenar  ──►  control 1: el dueño aprueba el diseño  ──►  construir  ──►  control 2: el dueño aprueba la rama  ──►  pr
```

## El estado entre corridas

La corrida guarda `<carpeta>/estado.json` y lo lee al empezar cada fase, por medio de agentes
mecánicos:

| Campo | Contenido |
|---|---|
| `formato`, `nombre` | Los del manifiesto: un estado de otro frente no se toma |
| `shaDiseno` | El commit del diseño auditado, al terminar `disenar` |
| `shaBaseConstruir` | Dónde estaba la rama al empezar la primera corrida de `construir` |
| `lotes` | Los ids de los lotes ya integrados |
| `tareas` | Por tarea: `veredicto` (`PASS` o `CHANGES_REQUIRED`), `shaAuditado`, `rondas` y `motivo` |
| `decisiones` | El estado de cada decisión que va a un lote, por ID |
| `reporte` | Lo que anotaron los agentes: `decisiones`, `conflictos`, `fueraDeZona` y `pendientes` |
| `cierre` | `veredicto` del cierre, los gates con su resultado, lo diferido y el `motivo` |
| `shaFinal` | El commit sobre el que corrieron los gates de cierre: el que empuja `pr` |
| `control` | El largo del resto del archivo, para detectar un estado truncado o cambiado |

El script no lee ni escribe archivos: el estado lo lee el agente de *Preparar* y lo escribe un
agente mecánico después de cada integración y de cada auditoría. Como pasa por agentes, el script
no confía en la copia:

- Al guardar, el agente copia el archivo anterior a `estado.json.anterior`, escribe, relee, y el
  script compara. Si no coincide en dos intentos, la corrida se corta y deja a la vista lo que
  sabía.
- Al leer, un archivo que existe pero no se puede leer entero (truncado, cambiado o de otro
  frente) frena la corrida. Nunca se empieza de cero sobre una rama con trabajo.
- Si el estado y la rama no coinciden en qué lotes están integrados (uno que falta en la rama, o
  uno que está en la rama y no en el estado), la corrida frena. Un lote revertido no cuenta.
- Un `estado.json` anterior a la versión 0.4.0 de esta guía, sin `control`, no se lee: no hay
  migración.

Los Review Contract de cada auditoría quedan en la misma carpeta, uno por tarea y ronda, en un
guardado aparte.

**Control 1.** El dueño lee el esquema y el documento de contrato. Si cambia algo, lo edita y lo
comitea él. Aprueba pasando en `disenoAprobado` el SHA que quiere construir; si lo cambió, ese SHA
ya no es `shaDiseno`, y `construir` vuelve a auditar el diseño antes de seguir, en una sola ronda
y sin preauditor ni arreglos. Si esa auditoría no da `PASS`, la corrida frena y devuelve los
hallazgos: lo que decidió el dueño no lo arregla un agente. Si una respuesta suya cambia una
decisión, edita el manifiesto antes de lanzar. El workflow no hace push: si otra
persona necesita el contrato, lo comparte el dueño.

**Control 2.** El dueño revisa la rama. Si la toca, HEAD deja de ser `shaFinal` y `pr` no corre:
hay que volver a lanzar `construir`.

**Relanzar `construir`.** Si `estado.json` ya tiene tareas, la fase reanuda en vez de empezar de
nuevo. Saltea las tareas en `PASS` cuyo `shaAuditado` sigue en la rama y que no tienen commits
posteriores con su prefijo. De las demás, no vuelve a construir los lotes ya integrados: construye
los que faltan y audita la tarea otra vez. Así, un commit del dueño sobre una tarea (`E7: G2, …`)
la hace auditar de nuevo, y uno sin prefijo de tarea va a la auditoría de cierre.

## Qué hace cada fase

**`disenar`**
1. *Preparar:* comprueba el punto 6 de "Antes de lanzar", pasa a `ramas.trabajo` y toma el
   baseline de Maven, `GREEN` o `RED`.
2. *Diseñar:* los lotes con `fase: "disenar"`, cada uno en su worktree, y su integración.
3. *Auditar* el diseño, con el ciclo de abajo. Se audita contra el `que` del lote, sus decisiones,
   los invariantes de la spec y las reglas duras de cada tarea.
4. Termina sin construir. Devuelve el diseño, sus decisiones y lo que el dueño tiene que aprobar.

**`construir`**
1. *Preparar,* como arriba. No arranca si falta `disenoAprobado`, si el diseño de ese SHA no está
   en la rama, o si su tarea no quedó en `PASS`.
2. Por cada tanda, en orden:
   - *Construir:* un agente por lote, en paralelo hasta `topes.constructoresEnParalelo`, cada uno
     en su worktree, sobre el último commit integrado.
   - *Integrar:* `cherry-pick` de cada lote, en el orden del manifiesto, y los gates de
     `gates.porLote`. Un lote con `modulos` vacío no corre Maven.
   - *Auditar* cada tarea cuya última tanda se acaba de integrar.
3. *Documentar:* un solo agente, sobre `documentacion.archivos`.
4. *Auditoría de cierre:* la documentación y los commits que no son de ninguna tarea, contra
   todas las reglas duras, incluida la de documentación. Sigue el mismo ciclo que la auditoría de
   una tarea, con el mismo tope de rondas.
5. *Gates de cierre,* al final y sobre el último commit: los de `gates.cierre`, los de
   `gates.siHayStack` si corresponde, y los diferidos, declarados. Comprueba además que el diseño
   no cambió desde el primer control. Acá nada se arregla: si un gate da rojo, el cierre queda sin
   `PASS`.
6. Termina **sin push ni PR**. Escribe `shaFinal`, el commit sobre el que corrieron esos gates, y
   devuelve el estado de cada decisión y lo que quedó pendiente.

**`pr`**
1. No hace nada, y dice por qué, si alguna tarea del manifiesto (también las de `disenar`) o el
   cierre no están en `PASS`, o si HEAD no es `shaFinal`.
2. `git push -u origin <ramas.trabajo>` y un PR borrador hacia `ramas.destino`, con la plantilla de
   [`04-checklist-antes-de-pr.md`](../04-checklist-antes-de-pr.md). Nunca mergea ni activa el merge
   automático.

## Cuando algo falla

| Qué pasa | Qué hace el script |
|---|---|
| El `cherry-pick` de un lote tiene un conflicto que no es de agregados | Lo aborta, anota el lote como no integrado y su tarea queda sin `PASS` |
| Los gates de una tanda integrada dan rojo | Un agente de arreglos, una vez, y los gates corren de nuevo. Si siguen rojos, se revierten los lotes que los causan y se corren otra vez sobre lo que queda |
| No se sabe qué lote causa el rojo | Se revierten todos los lotes de la tanda. Es el único caso en que una tarea arrastra a otra de su misma tanda |
| La reversión no se confirma, o los gates siguen rojos después | La corrida frena y pide revisar la rama a mano. No sigue sobre un árbol en rojo |
| El estado no se puede guardar o leer entero | La corrida frena (ver "El estado entre corridas") |
| Un lote no se integró | No corren las tandas siguientes de esa tarea, ni las tareas que vienen después |
| Una tarea agota las rondas sin `PASS` | No corren las tandas de las tareas siguientes. La corrida pasa al cierre con lo que hay |
| Queda algo sin `PASS` | `construir` termina, escribe el estado y la fase `pr` no corre |

Una tarea "viene después" de otra según el orden de `tareas` en el manifiesto. Las dos reglas de
"no corren" valen para las tandas posteriores: dentro de una tanda que comparten dos tareas, cada
una se integra y se audita por su cuenta, aunque la otra haya fallado.

## Auditoría de una tarea

La auditoría mira **los commits de esa tarea**, no un rango de la rama: así dos tareas que
comparten tanda no se mezclan. Se reconocen por el asunto: todo commit de una tarea empieza con
`prefijoCommit` más su id (`E7: G2, …`). Se suman los arreglos de integración de las tandas donde
la tarea tiene lotes (`E7: integración tanda 2`), que también cambian su código.

1. **Preauditor.** Revisa lo mecánico (citas, rutas, nombres, cifras, consistencia) y corrige lo
   trivial, con un commit "arreglos de la pre-auditoría".
2. **Auditor.** Solo lectura. Anota el `shaAuditado` y emite el Review Contract de
   [`evaluator.md`](../review/evaluator.md) (V1 a V9). Rinde cada decisión del lote en una línea
   `<ID>: CUMPLE — evidencia` o `<ID>: NO CUMPLE — evidencia`.
3. **Qué bloquea:** una regla dura de las que se exigen en cada tarea, una decisión `NO CUMPLE` o
   sin rendir, el `que` del lote sin cumplir, o los gates de sus lotes en rojo. Un hallazgo que
   solo se apoya en una regla flexible va como menor.
4. **Arreglos,** en el checkout de la rama, con commit "arreglos ronda N". Las auditorías van en
   serie, así que nadie más toca la rama en ese momento.
5. **Re-auditoría** solo de los commits nuevos de la tarea desde `shaAuditado`, con los hallazgos
   anteriores. El preauditor vuelve a pasar solo si esos commits tocan citas o cifras.
6. Hasta `topes.rondasDeReparacion` auditorías.

Cuando dos tareas comparten tanda, se auditan y se arreglan **en serie**, en el orden de `tareas`:
primero una completa, después la otra.

## Roles y modelos

Ningún agente hereda el modelo de la sesión.

| Rol | Modelo | Quién |
|---|---|---|
| `juicio` | Opus, esfuerzo medio | Diseñadores, constructores, arreglos y documentación |
| `mecanico` | Sonnet, esfuerzo alto | Preparar, integrar, revertir, guardar el estado, cerrar y PR |
| `preauditor` | Haiku | El agente `preauditor` de la sesión |
| `auditor` | Opus, esfuerzo medio | El agente `auditor` de la sesión |

`preauditor` y `auditor` son agentes del usuario, no del repo. Si la sesión no los tiene, o uno no
responde, el motor lanza un agente genérico con el mismo encargo y el mismo modelo.

## Lo que recibe todo agente

- La spec, el manifiesto y esta guía, con el orden de "Qué manda durante la corrida".
- En `construir`, además, el documento de `contrato` del manifiesto, tal como se aprobó.
- Las `reglas` del manifiesto.
- No frenar a preguntar: decidir, anotar en `decisiones` y seguir.
- Solo los archivos de su zona; lo demás, en `fueraDeZona`.
- Su resultado trae cuatro listas, que el motor copia al reporte: `decisiones`, `conflictos`,
  `fueraDeZona` y `pendientes` (lo que tiene que resolver una persona).
- Commits con `prefijoCommit` más la tarea (`E7: G2, …`), sin `--no-verify`, nombrando los
  archivos: nunca `git add -A`. Sin push.
- Nunca editar una línea por número sin leerla antes.

## Formato `frente-backend/1`

| Campo | Contenido |
|---|---|
| `formato`, `version`, `nombre`, `fecha` | Identificación; `nombre` en minúsculas, números y guiones |
| `spec`, `guia` | Rutas de la spec y de esta guía |
| `contrato` | Opcional: el documento que deja `disenar` y reciben los agentes de `construir` |
| `comoLeerlo` | Opcional: una explicación corta para quien abre el archivo; el script no lo lee |
| `prefijoCommit` | Prefijo de los commits |
| `ramas` | `base`, `trabajo` y `destino` |
| `topes` | `constructoresEnParalelo` y `rondasDeReparacion` |
| `modelos` | `juicio`, `mecanico`, `preauditor` y `auditor` |
| `precedencia` | `regla`, `ok`, `reglasDuras` (`id`, `texto` y `seAudita`: `tarea` o `cierre`) y `flexibles` |
| `reglas` | Reglas de la corrida, para todos los agentes |
| `tareas` | `id` y `nombre`, en el orden en que dependen |
| `decisiones` | `id`, `texto`, `quien` y `ok`, y exactamente uno de `lote` con `criterio`, o `aCargoDe` con `quien` y `ok` |
| `lotes` | `id`, `tarea`, `fase` (`disenar` o `construir`), `tanda` entera, `titulo`, `rol`, `modulos`, `archivos` (lista de globs) y `que`; `condicion` es opcional |
| `gates` | `porLote` (el texto `<módulos del lote>` se reemplaza por los módulos del lote), `cierre`, `siHayStack` (`gate` y `comando`), `diferidos` (`gate` y `a`) y `sinDocker` |
| `documentacion` | `archivos` (lista de rutas) y `que` |
| `pr` | `borrador`, `titulo` (sin comillas dobles, acentos graves, `$` ni barras invertidas) y `plantilla` |
| `pendientesDeUnaPersona` | Lo que ningún agente resuelve |

El script valida el manifiesto antes de llamar a un agente y no arranca si falla. Mira los campos
que usa (no `version`, `fecha`, `comoLeerlo` ni `pendientesDeUnaPersona`) y, además: ids repetidos,
un lote de una tarea que no existe, una decisión sin destino o con un lote que no existe, un
lote con una tanda posterior a la de una tarea que viene después de la suya, o dos lotes de la misma
fase y tanda con globs que se solapan. El orden de tandas se compara entre lotes de la misma fase.
Los SHA se guardan completos y se comparan tolerando uno abreviado; el que devuelve un agente solo
se usa si tiene forma de SHA.

## Después

- El resultado es un reporte, no una prueba. Mirá `git log` de la rama y la carpeta de la corrida.
- Una decisión no cumplida va primera en el informe para el usuario.
- No edites el PR mientras corre su CI. No mergees.

## Pendiente

- La primera corrida real: hasta ahora el script solo corrió con agentes falsos.
- Un check de `agent-check` para los manifiestos `frente-backend/1`: hoy la regla vive solo en el
  script, que la aplica al arrancar.
- Formalizar el régimen de precedencia con el grupo (ver "Límite de esta decisión").
- Ver si este motor y el W1 de la Entrega 7 pueden ser el mismo.

## Versiones

| Versión | Fecha | Qué cambió | Formato del manifiesto |
|---|---|---|---|
| 0.1.0 | 2026-11-20 | Primera versión, antes del script. | `frente-backend/1` |
| 0.2.0 | 2026-11-20 | Auditoría de la primera versión: estado entre corridas, aprobación del diseño por SHA, reglas de falla, auditoría por commits de la tarea y auditoría de cierre. | `frente-backend/1` |
| 0.3.0 | 2026-11-20 | Con el script escrito: campos reales de `estado.json`, commits de una tarea por su asunto, arreglos de la auditoría en el checkout y reversión de lotes con gates en rojo. | `frente-backend/1` |
| 0.4.0 | 2026-11-20 | Auditoría del script: el estado se verifica al guardar y al leer, los gates de cierre corren al final, los gates en rojo se vuelven a correr después del arreglo y de la reversión, y un commit del dueño sobre una tarea la hace auditar de nuevo. | `frente-backend/1` |
