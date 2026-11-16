# Interfaz de DonaTrack — índice y proceso

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 4 · 2026-11-15 (harness de la opción A: sistema cerrado y tres skills cortas) · cliente liviano de E6**

Índice de la carpeta para las personas del equipo y los agentes. Las reglas del módulo, y lo único
que un agente tiene que leer antes de tocar la interfaz, están en
[`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md). La decisión que ordena este proceso
es el [ADR del harness de interfaz](../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md)
(`proposed`).

## Quién hace qué

| Quién | Hace |
|---|---|
| Usuario o diseñadora | Decide: aprueba cada flujo y elige entre alternativas mirándolas. |
| Agente | Construye con el sistema que hay, lo comprueba y explora cuando se lo piden. |
| Revisor | Revisa con ojos frescos y en solo lectura antes de cerrar (skill `revisar-ui`). |

## El proceso

| Para… | Skill | Qué sale |
|---|---|---|
| Producir una pantalla o un componente | `construir-ui` | La ficha del flujo si la pantalla es nueva (150 líneas o menos), las vistas, los tests y las capturas |
| Cerrar | `revisar-ui` | Un veredicto PASS o CHANGES_REQUIRED, con las pantallas comparadas entre sí |
| Buscar un aspecto o una estructura nuevos | `explorar-ui` | De 3 a 4 opciones que divergen en ejes de producto, en una rama o en prototipos, sin ranking |

Las skills están en `.agents/skills/<nombre>/SKILL.md`. Claude Code las encuentra mediante
`.claude/skills/<nombre>/SKILL.md`, que apunta a la versión de `.agents/`. El harness anterior (7
skills, la corrida creativa y sus plantillas) está en el
[archivo histórico](../IA/history/harness-ui-2026-11/README.md).

**Sistema cerrado.** Una sola identidad, con sus valores en `tokens.css` y su verdad visual en
`/estilos`. Las piezas están en el [catálogo](componentes.md), y toda lista con búsqueda o filtros
usa `componentes/listado`. `InterfazFitnessTest` lo cerca en el build.

**Exploración aparte.** Lo que se explora no se mergea. Se presenta sin opción recomendada y sin
matriz, y lo que se elige entra después con `construir-ui`.

## Cómo se le pregunta al usuario

1. **Una decisión por pregunta.** En producción, con una opción recomendada y su motivo en una
   línea. En una exploración, sin recomendada: las opciones van en el orden en que se hicieron.
2. **Como máximo 3 preguntas por ronda.**
3. **Mostrar antes que explicar:** una imagen, un ejemplo o un texto de muestra.
4. **No preguntar lo que el agente puede comprobar** por su cuenta: contraste, tamaños o si algo
   entra en el celular.
5. **Palabras de todos los días.** Sin "token", "WCAG", "breakpoint" ni "fragmento"; si un término
   es inevitable, va con una explicación de una línea ([glosario](#glosario)).
6. **El detalle técnico va a la bitácora y a los tests**, no a la conversación.

## Anchos de referencia

| Ancho | Representa |
|---|---|
| 360 px | Celular |
| 1280 px | Escritorio |
| 1920 px | Pantalla grande |

- Los define una constante de los tests, `AnchosDeReferencia`. La comprobación revisa también
  anchos intermedios, incluido el quiebre ancho de 1100 px, y la primera pantalla del celular
  (`<página>-360x640.png`).
- Las herramientas de las capturas están en [`scripts/visual/`](../../scripts/visual/README.md).

## Listo para entregar

**Un flujo está listo cuando** su ficha ([plantilla](plantillas/flujo.md)):
- [ ] no tiene huecos, y recorre el camino feliz y los desvíos;
- [ ] lista cada estado de cada pantalla con su texto, según el [tono](tono-y-comunicacion.md);
- [ ] fue aprobada por el usuario.

**Una pantalla está lista cuando:**
- [ ] usa solo tokens de uso y piezas del catálogo; sus listas usan `componentes/listado`;
- [ ] muestra todos sus estados con el texto del flujo;
- [ ] funciona sin JavaScript en lo esencial;
- [ ] pasa `-Pvisual` en los 3 anchos: AA sin errores, nada desbordado y toque de 48 px o más;
- [ ] pasó `revisar-ui` sin bloqueantes.

## Cómo leer los bocetos de E1

`Read` no muestra PDFs de más de 10 páginas sin `pdftoppm`. Alcanza con partir
`docs/entregas/1/interfaz/DonaTrack-Bocetos.pdf` en tramos de 6 páginas con `pypdf`
(`PdfReader` y `PdfWriter`) y leer cada tramo. El resumen página por página está en
[bocetos-e1.md](bocetos-e1.md).

## Del trabajo al entregable de la cátedra

Estos documentos son de trabajo. Para el entregable de E6 se pasan a un informe para personas:
portada, índice y pie con número de página; **sin evidencia del repo** (ni rutas, ni clases, ni
tests); cada decisión de la bitácora como recuadro **Decisión / Alternativas / Por qué / Costo**,
con sus capturas; y diagramas con texto de 7 pt o más.

## Cómo cambiar o extender el proceso

| Quiero… | Hago… |
|---|---|
| Sumar una pantalla | `construir-ui`: ficha en `flujos/`, `templates/<pantalla>.html` y, si hace falta, su hoja |
| Sumar un componente | Su fragmento en `templates/componentes/`, su hoja en `static/css/componentes/`, su fila en el [catálogo](componentes.md) y su muestra en `/estilos` |
| Comprobar una página más | Una línea en `cliente-liviano/src/test/resources/verificacion/paginas.txt`, o `-Dverificacion.paginas=<ruta>` |
| Correr una oleada | Con el usuario, una ronda de decisiones con ID; después, el manifiesto desde la [plantilla](plantillas/manifiesto-oleada.ejemplo.json), con cada decisión en un lote o diferida, y el workflow `oleada-ui` según su [guía](../IA/prompts/orquestacion-oleada-ui.md). El PR trae la hoja de contacto a 360 y 1280 px para que la apruebe el usuario |
| Probar alternativas | `explorar-ui`, en una rama o con variantes de la maqueta (`?variante=`) |
| Cambiar la identidad | La exploro con `explorar-ui` junto a la diseñadora; lo elegido entra a `tokens.css` con un ADR |
| Cambiar la voz o los mensajes | Edito [tono y comunicación](tono-y-comunicacion.md) a partir de una versión nueva del brief de tono del equipo |
| Cambiar una skill de UI | Edito `.agents/skills/<nombre>/SKILL.md` y copio su frontmatter al adaptador; `agent-check` controla que coincidan y que no pase su tope de líneas |

## Glosario

- **Token:** un valor con nombre (por ejemplo, "color de acción principal") que usan todas las
  pantallas. Cambiarlo cambia todo el sitio.
- **Contraste AA:** diferencia mínima entre texto y fondo para que se lea bien (WCAG nivel AA).
- **Punto de quiebre:** el ancho a partir del cual la pantalla se reacomoda.
- **Estado vacío:** lo que se ve cuando todavía no hay datos.
- **Mejora progresiva:** la página funciona sin JavaScript; con JavaScript, funciona más cómoda.

## Archivos de esta carpeta

Vigentes, para consultar cuando la tarea lo pide (ninguno es lectura obligatoria salvo el tono, para
los textos):

| Archivo | Para qué |
|---|---|
| [tono-y-comunicacion.md](tono-y-comunicacion.md) | Voz, tono por pantalla, estados, cifras y el filtro de 7 preguntas; sale del [brief de tono del equipo](identidad/brief-tono-y-comunicacion-v1.0.pdf) |
| [componentes.md](componentes.md) | Catálogo de capas CUBE y componentes, con el kit de listas; se consulta por búsqueda |
| [backlog-migracion-componentes.md](backlog-migracion-componentes.md) | Migración de las hojas heredadas a componentes, con su línea de base y su tope |
| [criterios-catedra-ux-ui.md](criterios-catedra-ux-ui.md) | Los criterios de UX/UI de la cátedra: factores, patrones, arquitectura de la información y las 10 heurísticas |
| [direccion-de-arte.md](direccion-de-arte.md) | Cómo apuntar alto en lo gráfico sin perder el piso |
| [patrones-y-microcopy.md](patrones-y-microcopy.md) | Patrones de estados, avisos y textos |
| [identidad/brief.md](identidad/brief.md) y [identidad/referencias/](identidad/referencias/README.md) | La intención de la identidad; los valores mandan desde `tokens.css` |
| [mapa-del-sitio.md](mapa-del-sitio.md) | Pantallas, roles y niveles |
| [plantillas/flujo.md](plantillas/flujo.md) | La plantilla de la ficha de flujo |
| [plantillas/manifiesto-oleada.ejemplo.json](plantillas/manifiesto-oleada.ejemplo.json) | La plantilla del manifiesto de una oleada (`oleada-ui/1`); `agent-check` la valida como a cualquier manifiesto |
| [flujos/](flujos/) | Las fichas de flujo: la de la pantalla que se toca se actualiza; las demás son historia |

Historia: registros de lo que se hizo. Ninguna regla manda leerlos, y no se mueven porque los
enlazan ADRs que no se editan:

| Archivo | Qué guarda |
|---|---|
| [bitacora.md](bitacora.md) | Las decisiones de diseño, con su porqué |
| [evidencia/](evidencia/) | Las capturas de cada decisión |
| [historico/](historico/README.md) | Las identidades retiradas el 2026-11-03 |
| [encargos/](encargos/README.md), [temas-ab.md](temas-ab.md) | Las entradas de la corrida creativa y del A/B del harness anterior |
| [roadmap-vistas-enunciado.md](roadmap-vistas-enunciado.md), [bocetos-e1.md](bocetos-e1.md), `plan-ronda-2026-11-09*.md` | El plan de vistas del enunciado, el inventario de los bocetos de E1 y las rondas del 2026-11-09 |
| [backlog-revision-2026-11-11.md](backlog-revision-2026-11-11.md) y su manifiesto | El backlog de la revisión del 2026-11-11 y la corrida R11 |
