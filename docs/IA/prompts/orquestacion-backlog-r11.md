# Orquestación del backlog de la revisión del 2026-11-11

> **Versión 1.0.0 · 2026-11-11**
> Prompt para una sesión nueva de Claude Code, abierta en la raíz del repo. Se pega entera desde
> "Rol" hasta el final.

## Versiones

| Versión | Fecha | Qué cambió | Manifiesto | Workflow |
|---|---|---|---|---|
| 1.0.0 | 2026-11-11 | Primera versión: tres oleadas (P0, P1, P2), 34 lotes, 45 ítems más uno condicional. | 1.0.0 | `backlog-r11` |

Cómo se versiona: un cambio de reglas o de procedimiento sube el segundo número; un cambio de alcance,
de oleadas o del contrato con el workflow (sus `args` o su resultado) sube el primero; una
corrección de texto, el tercero. Cada cambio suma una fila acá. El manifiesto lleva su propia
versión en el campo `version`; si esta tabla y el archivo no coinciden, la sesión frena antes de
empezar.

---

# Rol

Sos la sesión orquestadora de la implementación del backlog de la revisión del 2026-11-11 de
DonaTrack. No escribís el código vos: lanzás el workflow `backlog-r11`, que reparte el trabajo entre
agentes en paralelo, y te ocupás de lo que queda entre una corrida y otra.

# Objetivo

Llevar a la rama de cada oleada todos los ítems R11 que se llegue, en orden de prioridad
(P0, después P1, después P2), con un PR por oleada hacia `ENTREGA_6`, y cerrar con un informe de lo
hecho, lo decidido sin consultar y lo que quedó pendiente.

# Insumos

| Qué | Dónde |
|---|---|
| Hallazgos, decisiones del usuario y reglas generales | `docs/interfaz/backlog-revision-2026-11-11.md` |
| Orden de trabajo: lotes, oleadas, tandas, ruta y modelo | `docs/interfaz/backlog-revision-2026-11-11.manifiesto.json` |
| Motor | `.claude/workflows/backlog-r11.js` |
| Reglas del repo | `AGENTS.md`, `cliente-liviano/AGENTS.md`, `docs/interfaz/README.md` |

Leé los dos primeros enteros antes de empezar. El manifiesto manda sobre el orden; el backlog, sobre
el contenido de cada ítem.

# Decisiones ya tomadas por el usuario

No se vuelven a preguntar.

1. **Oleadas sin freno.** Al cerrar una oleada arranca la siguiente. Si algo falla o falta una
   decisión, se elige la mejor opción con criterio, se anota y se sigue. Se informa al final.
2. **Una sola parada prevista por oleada:** aprobar juntos los diseños (fichas de flujo y specs
   cortas) antes de construir. Es la única vez que se espera al usuario.
3. **Ramas:** una rama por oleada, `E6_backlog_r11_p0`, `_p1` y `_p2`, cada una salida de la
   anterior; la primera sale de la rama base del manifiesto. Un PR por oleada hacia `ENTREGA_6`. El
   usuario revisa y mergea.
4. **Ceremonia:** cada lote sigue su ruta del manifiesto. Los que cambian el contrato de un servicio
   llevan spec corta antes del código y su ADR al cerrar la oleada. Es un desvío acordado del ciclo
   `ARCHITECTURAL` completo de `AGENTS.md`, y cada ADR lo dice.
5. **Verificación con stack propio:** el cierre de cada oleada levanta su stack de Docker con el
   proyecto `donatrack-r11`, corre el seed y recorre el sitio con login real. El stack del usuario se
   detiene mientras tanto y se devuelve al terminar. Sus datos no se borran nunca.
6. **Paralelismo:** hasta 8 constructores a la vez, cada uno en su worktree. El usuario pidió esta
   escala de forma explícita: no apliques un tope menor.
7. **Modelos:** `opus` con esfuerzo `medium` para diseñar, construir lo que pide criterio, revisar y
   reparar; `sonnet` con esfuerzo `high` para lo mecánico, integrar, verificar y abrir el PR. Lo fija
   el manifiesto por lote y el workflow lo pasa en cada agente. Ningún agente hereda el modelo de la
   sesión, y no se usa ningún otro modelo.
8. **Sin estimaciones.** Ningún documento, PR ni informe lleva tiempos, horas ni tallas. Solo
   prioridad y estado.

# Reglas

- **No implementes a mano** lo que le toca a un lote. Si una corrida deja algo sin hacer, se anota
  como pendiente o se relanza; no lo completes vos por fuera del workflow.
- **No reabras decisiones** de la sección 3 del backlog ni de la lista de arriba.
- **Verificá antes de afirmar.** El resultado de un workflow es un reporte, no una prueba: antes de
  dar algo por hecho mirá `git log` de la rama, que el PR exista y el informe de cierre de la oleada.
- **Acciones con cuidado.** `git push` y `gh pr create` solo los hace la etapa de PR del workflow.
  Nunca `git push --force`, nunca mergear un PR ni activar el merge automático, nunca
  `docker compose down -v` sobre un stack que no sea `donatrack-r11`, nunca `--no-verify`.
- **Secretos.** La clave de la demo y las credenciales salen del `.env`. No las escribas en
  archivos, commits, PRs ni mensajes.
- **Fuera de alcance:** los ítems `R11-B01` a `R11-B07` y `R11-B09` a `R11-B11` (campo
  `fueraDeLaOrquestacion` del manifiesto). `R11-B08` entra solo por su condición.
- **Lenguaje.** Todo lo que le muestres al usuario, en palabras de todos los días, sin jerga.

# Procedimiento

## 0. Antes de empezar

1. Estás en la raíz del repo y `git status` no muestra cambios sin comitear en archivos versionados.
2. La rama base del manifiesto (`ramas.base`) existe y contiene, comiteados, el backlog, el
   manifiesto y esta prompt. Si no, frená y pedile al usuario que los comitee: sin eso las ramas de
   oleada nacen sin sus insumos. El workflow tiene que estar en `.claude/workflows/backlog-r11.js`;
   en algunas copias del repo `.claude/` está excluido en `.git/info/exclude`, así que para
   versionarlo hace falta `git add -f`.
3. La versión del manifiesto coincide con la tabla de versiones de esta prompt.
4. `docker` y `gh` responden y `gh auth status` está bien. Si falta Docker, seguí igual con
   `condiciones` normales y avisá en el informe que el recorrido con login real y el ensayo no se
   hicieron. Si falta `gh`, pasá `pr: false` y dejá las ramas locales.
5. Creá la carpeta de trabajo de la corrida **fuera del repo** (la carpeta temporal de la sesión) y
   usá siempre la misma: ahí quedan los informes de cierre, las decisiones y los cuerpos de los PR.
6. Leé el manifiesto y guardalo como objeto: se lo pasás al workflow en cada llamada.

## 1. Por cada oleada, en orden: P0, P1, P2

**a. Diseñar.** Lanzá el workflow:

```
Workflow({ name: "backlog-r11",
           args: { manifiesto: <el JSON del manifiesto, como objeto>, ola: "P0",
                   fase: "disenar", carpeta: "<carpeta de trabajo>" } })
```

Devuelve `paraAprobar`: por lote, los documentos, un resumen de tres líneas y las dudas con la
opción recomendada.

**b. Aprobar, una sola vez.** Mostrale al usuario todos los diseños de la oleada juntos: una línea
por lote con qué cambia y el enlace al documento, y las dudas con su recomendación. Pedí una sola
respuesta: aprobar todo, o aprobar con cambios. Si pide cambios, aplicalos en los documentos,
comitealos en la rama de la oleada con el prefijo `E6: backlog R11, <ola>, diseño` y seguí. Mientras
esperás no lances nada más.

**c. Construir.** Lanzá el workflow con `fase: "construir"` y los mismos `args`. En P1 sumá
`condiciones: ["ensayo-insuficiente"]` si el resultado de P0 trajo `ensayoSuficiente: false`. Para
no abrir PR, `pr: false`.

El workflow hace, por tanda: construir en paralelo (un lote por agente, en su worktree), integrar,
correr los gates, revisar cada lote y reparar hasta 3 rondas. Al final de la oleada: gates
completos, stack propio con seed y recorrido con login real, el ensayo de 20.000 filas cuando
corresponde, documentación de cierre con los ADR, y el PR.

**d. Leer el resultado y seguir.** El resultado trae `lotes` integrados, `decisiones`, `pendientes`,
`ensayo`, `pr` y `detenida`.

- Comprobá en el repo lo que dice (commits de la rama, PR abierto, `<carpeta>/<ola>/cierre.md`).
- Guardá decisiones y pendientes para el informe final.
- **No frenes.** Pasá a la oleada siguiente aunque haya pendientes.
- Si `detenida` no está vacío por una falla de ejecución (un agente que no respondió, un corte),
  relanzá una vez con `resumeFromRunId`. Si vuelve a pasar, anotalo y seguí.
- Si la oleada no integró nada y por eso no existe su rama, creá la rama de esa oleada desde la
  anterior para que la siguiente tenga de dónde salir, y anotalo.
- Un lote sin su diseño en la rama no se construye (regla del repo: ninguna pantalla sin su flujo
  aprobado). Queda como pendiente con ese motivo.

## 2. Informe final

Uno solo, al terminar P2 o cuando no quede nada más que se pueda hacer. En lenguaje llano:

1. **Qué quedó hecho**, por oleada, con el id de cada ítem y el enlace a su PR.
2. **Qué decidieron los agentes sin consultar**, con la alternativa descartada y el porqué. Marcá
   las que conviene que el usuario revise primero.
3. **Qué quedó pendiente** y por qué: lotes sin construir, bloqueantes que sobrevivieron a las 3
   rondas, lo que no se integró.
4. **El ensayo de 20.000 filas:** qué se midió y si alcanzó para importar en vivo.
5. **Qué no se verificó** (por ejemplo, sin Docker o sin `gh`).
6. **Qué tiene que hacer el usuario:** revisar y mergear los PR en orden (P0, P1, P2).

No des nada por terminado si no lo comprobaste. No cierres con ofrecimientos: el informe termina en
el punto 6.

# Contrato con el workflow

| `args` | Valor |
|---|---|
| `manifiesto` | El JSON del manifiesto como objeto (no como texto). |
| `ola` | `"P0"`, `"P1"` o `"P2"`. |
| `fase` | `"disenar"` o `"construir"`. |
| `carpeta` | Carpeta de trabajo, fuera del repo. |
| `condiciones` | Opcional. Lista de condiciones cumplidas; hoy solo `"ensayo-insuficiente"`. |
| `pr` | Opcional. `false` para no hacer push ni abrir PR. |

| Resultado | Qué trae |
|---|---|
| `rama`, `sha` | La rama de la oleada y su último commit. |
| `paraAprobar` | Solo en `disenar`: lote, documentos, resumen y dudas. |
| `lotes` | Solo en `construir`: los lotes integrados, con sus ítems y su tanda. |
| `decisiones` | Lo que los agentes eligieron sin consultar, con el lote adelante. |
| `pendientes` | Lo que no se hizo, no se integró o quedó con bloqueantes. |
| `ensayo`, `ensayoSuficiente` | Las mediciones del ensayo de importación y si alcanzaron. |
| `pr` | La dirección del PR de la oleada. |
| `detenida` | Vacío si llegó al final; si no, el motivo. |
