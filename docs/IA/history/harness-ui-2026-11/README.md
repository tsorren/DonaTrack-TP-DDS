# Harness de interfaz anterior (archivado el 2026-11-15)

> **Histórico.** No describe las reglas vigentes y ninguna regla manda leerlo. El harness vigente
> está en [`cliente-liviano/AGENTS.md`](../../../../cliente-liviano/AGENTS.md) y en las skills
> `construir-ui`, `revisar-ui` y `explorar-ui` de `.agents/skills/`.

Se archivó al adoptar la opción A del harness de frontend: un sistema visual propio y cerrado, un
componente único de listas y un cerco en el build
([ADR `proposed`](../../../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md)).

| Qué | Dónde estaba | Lo reemplaza |
|---|---|---|
| `skills/vista-ui` | `.agents/skills/` y su adaptador en `.claude/skills/` | `construir-ui` |
| `skills/ux-flujo` | ídem | `construir-ui`, paso 1 (ficha de 150 líneas o menos) |
| `skills/revision-ui` (vectores U1–U10 y panel de exploración) | ídem | `revisar-ui` (U1–U7); el panel, sin reemplazo: elige el humano |
| `skills/identidad-visual`, `skills/iteracion-ab`, `skills/relevamiento-creativo` y `skills/corrida-creativa` | ídem | `explorar-ui` |
| `workflows/corrida-creativa.js` | `.claude/workflows/` | `explorar-ui` (sin workflow desatendido) |
| `plantillas/encargo-constructor.md`, `encargo-creativo.md`, `encargo-critico.md` e `iteracion-ab.md` | `docs/interfaz/plantillas/` | `explorar-ui` |

Los adaptadores de `.claude/skills/` solo apuntaban a la versión canónica y no se conservan. Los
enlaces relativos de estos archivos apuntan a su ubicación anterior y pueden estar rotos.
