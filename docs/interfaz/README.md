# Interfaz de DonaTrack — proceso de diseño

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · cliente liviano de E6**

Este es el punto de entrada para diseñar y construir la interfaz del cliente liviano. Lo usan las
personas del equipo y los agentes de IA. Las reglas técnicas del módulo están en
[`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md).

## Quién hace qué

| Quién | Hace |
|---|---|
| Usuario o diseñadora | Decide: elige entre opciones, aprueba el brief de identidad y cada flujo. |
| Agente | Propone, construye y comprueba. Pregunta solo lo que no puede resolver por su cuenta. |
| Revisor independiente | Revisa con ojos frescos al cerrar un flujo o una pantalla (skill `revision-ui`). |

## El proceso en 6 pasos

```mermaid
flowchart LR
  A[1. Insumos] --> B[2. Identidad visual]
  B --> C[3. Diseño del flujo]
  C --> D[4. Pantallas]
  D --> E[5. Comprobación]
  E --> F[6. Revisión]
  F -->|ajustes| C
```

| Paso | Qué entra | Qué sale | Skill |
|---|---|---|---|
| 1. Insumos | Prototipo v2 de la diseñadora (punto de partida, ver [brief](identidad/brief.md)), bocetos de E1, mapa del sitio, requerimientos de E6 | Contexto listo | — ([bocetos](bocetos-e1.md), [mapa](mapa-del-sitio.md)) |
| 2. Identidad visual | Respuestas del usuario en 2 o 3 rondas cortas | `identidad/brief.md` y la paleta, tipografía y forma en `tokens.css` | `identidad-visual` |
| 3. Diseño del flujo | Un flujo (por ejemplo, confirmar recepción) | `flujos/<flujo>.md` con recorrido, estados, textos y revisión de usabilidad | `ux-flujo` |
| 4. Pantallas | El flujo aprobado | Vistas y componentes en `cliente-liviano/` | `vista-ui` |
| 5. Comprobación | Las vistas | Tests en verde y capturas en los 3 anchos | (dentro de `vista-ui`) |
| 6. Revisión | Flujo, vistas y capturas | Veredicto con hallazgos | `revision-ui` |

**Dos alternativas de identidad.** El sitio tiene dos identidades con las mismas pantallas,
componentes y textos, cada una con sus plantillas y sus hojas de estilo, para comparar qué mensaje
transmite cada una
([ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)):

| Alternativa | Mensaje | Brief | Plantillas y hojas de estilo | Cómo se ve |
|---|---|---|---|---|
| Glassmorphism | Profesional y seria | [brief.md](identidad/brief.md) | `templates/` y `static/css/tokens.css` y `app.css` | Por defecto |
| Cozy / Medieval | Cálida y de aventura | [brief-cozy-medieval.md](identidad/brief-cozy-medieval.md) | `templates/alternativas/cozy-medieval/` y `static/css/alternativas/cozy-medieval/` | `?alternativa=cozy-medieval` (perfil `maqueta`) |

Cada pantalla o componente nuevo se escribe y se estiliza dos veces, una por alternativa, con los
mismos textos (un test compara los dos juegos). Cozy tiene además su propia
[estructura y navegación](estructura-cozy-medieval.md).

**Comparaciones A/B.** Cuando una decisión de gusto o de forma tiene más de una buena respuesta
(una paleta, una tipografía, el orden de una pantalla), el agente arma dos variantes que difieren
solo en ese tema y el usuario elige. Skill `iteracion-ab`; temas posibles en el
[catálogo](temas-ab.md). Cada elección queda en la [bitácora](bitacora.md).

Las skills están en `.agents/skills/<nombre>/SKILL.md`. Claude Code las encuentra mediante
`.claude/skills/<nombre>/SKILL.md`, que apunta a la versión de `.agents/`.

## Cómo se le pregunta al usuario

El proceso no debe cansar a quien decide. Reglas para el agente:

1. **Una decisión por pregunta**, con una opción recomendada y el motivo en una línea.
2. **Como máximo 3 preguntas por ronda.**
3. **Mostrar antes que explicar:** una imagen, un ejemplo o un texto de muestra.
4. **No preguntar lo que el agente puede decidir o comprobar por su cuenta**: contraste, tamaños o
   si algo entra en móvil.
5. **Palabras de todos los días.** En lo que ve el usuario no van "token", "WCAG", "breakpoint" ni
   "fragmento". Si un término es inevitable, va con una explicación de una línea (ver
   [glosario](#glosario)).
6. **El detalle técnico va a la bitácora y a los tests**, no a la conversación.
7. Al presentar un A/B: **2 o 3 líneas por variante** y la imagen comparativa. Nada más.

## Anchos de referencia

| Ancho | Representa |
|---|---|
| 360 px | Celular |
| 1280 px | Escritorio |
| 1920 px | Pantalla grande |

- Se diseña, se captura y se compara en estos tres anchos.
- Los define una sola constante de los tests: `AnchosDeReferencia`, en
  `cliente-liviano/src/test/java/`. Esta tabla la explica.
- Además, la comprobación automática revisa anchos intermedios (incluida una tableta) para que
  nada se desborde. No genera capturas para el usuario.
- Los puntos de quiebre del CSS se declaran una sola vez, en `tokens.css`.

## Listo para entregar

**Un flujo está listo cuando** su ficha (`flujos/<flujo>.md`, según la
[plantilla](plantillas/flujo.md)):

- [ ] no tiene huecos ni "a definir";
- [ ] recorre el camino feliz y los desvíos;
- [ ] lista cada estado de cada pantalla con su texto;
- [ ] pasó la revisión de usabilidad (las 10 heurísticas);
- [ ] fue aprobada por el usuario.

**Una pantalla está lista cuando:**

- [ ] usa solo los colores, tipografías y espacios con nombre de `tokens.css`, y se ve bien en
      las dos alternativas de identidad;
- [ ] muestra todos sus estados (vacío, cargando, éxito, error, sin permiso) con el texto del flujo;
- [ ] funciona sin JavaScript en lo esencial (ver, navegar y enviar formularios);
- [ ] pasa la comprobación automática en los 3 anchos: accesibilidad AA sin errores, nada
      desbordado y controles de al menos 48×48 px;
- [ ] sus componentes nuevos están en el catálogo `/estilos`, con todos sus estados;
- [ ] tiene la revisión de `revision-ui` sin hallazgos bloqueantes.

## Cómo leer los bocetos de E1

`Read` no muestra PDFs de más de 10 páginas sin `pdftoppm`. Alcanza con partir el PDF en tramos
con `pypdf` y leer cada tramo:

```python
# Parte los bocetos en tramos de 6 páginas dentro de una carpeta temporal.
from pypdf import PdfReader, PdfWriter

origen = "docs/entregas/1/interfaz/DonaTrack-Bocetos.pdf"
destino = "<carpeta-temporal>"
paginas = PdfReader(origen).pages
for inicio in range(0, len(paginas), 6):
    tramo = PdfWriter()
    for pagina in paginas[inicio:inicio + 6]:
        tramo.add_page(pagina)
    tramo.write(f"{destino}/bocetos-{inicio + 1:02d}.pdf")
```

El resumen página por página está en [bocetos-e1.md](bocetos-e1.md).

## Del trabajo al entregable de la cátedra

Estos documentos son de trabajo. Para el entregable de E6 se pasan a un informe para personas:

- portada, índice, pie con número de página y epígrafes;
- **sin evidencia del repo**: ni rutas, ni clases, ni nombres de tests;
- cada decisión de la bitácora como recuadro: **Decisión / Alternativas / Por qué / Costo**, con
  las capturas A/B como figura;
- diagramas legibles impresos (texto de 7 pt o más).

## Cómo cambiar o extender el proceso

Cada regla vive en un solo archivo. Para cambiar algo, se edita ese archivo y nada más.

| Quiero… | Hago… |
|---|---|
| Diseñar un flujo nuevo | Copio [`plantillas/flujo.md`](plantillas/flujo.md) a `flujos/<flujo>.md` y uso la skill `ux-flujo` |
| Comparar un tema nuevo con A/B | Agrego una fila en [`temas-ab.md`](temas-ab.md) |
| Sumar un componente | Creo su archivo en `templates/fragmentos/` y lo agrego a `/estilos` (un test falla si falta) |
| Comprobar una página más | Agrego una línea en `cliente-liviano/src/test/resources/verificacion/paginas.txt` |
| Cambiar la identidad (colores, letras) | Edito solo la sección de valores base del `tokens.css` de esa alternativa |
| Sumar una alternativa de identidad | Creo su brief en `identidad/` y la carpeta `static/css/alternativas/<nombre>/` con su `tokens.css` y su `app.css`; la comprobación visual la recorre sola |
| Cambiar los anchos de referencia | Edito la constante `AnchosDeReferencia` y la tabla de arriba |
| Cambiar una regla de diseño | Edito [dirección de arte](direccion-de-arte.md) o [patrones y textos](patrones-y-microcopy.md) |
| Sumar una skill | Creo `.agents/skills/<nombre>/SKILL.md` y su adaptador `.claude/skills/<nombre>/SKILL.md` (copiar uno existente y cambiar nombre y descripción); la agrego a `docs/context-index.md` |

## Glosario

Los términos que el usuario puede llegar a ver, explicados en una línea:

- **Token:** un valor con nombre (por ejemplo, "color de acción principal") que usan todas las
  pantallas. Cambiarlo cambia todo el sitio.
- **Contraste AA:** diferencia mínima entre texto y fondo para que se lea bien (norma WCAG nivel AA).
- **Breakpoint o punto de quiebre:** el ancho a partir del cual la pantalla se reacomoda.
- **Estado vacío:** lo que se ve cuando todavía no hay datos.
- **Mejora progresiva:** la página funciona sin JavaScript; con JavaScript, funciona más cómoda.

## Archivos de esta carpeta

| Archivo | Para qué |
|---|---|
| [bocetos-e1.md](bocetos-e1.md) | Inventario de los bocetos de E1 |
| [mapa-del-sitio.md](mapa-del-sitio.md) | Pantallas, roles y niveles |
| [direccion-de-arte.md](direccion-de-arte.md) | Cómo apuntar alto en lo gráfico sin perder el piso |
| [estructura-cozy-medieval.md](estructura-cozy-medieval.md) | Estructura, navegación y momentos de la Alternativa Cozy / Medieval |
| [patrones-y-microcopy.md](patrones-y-microcopy.md) | Componentes, estados, avisos y textos |
| [temas-ab.md](temas-ab.md) | Temas que se pueden comparar con A/B |
| [bitacora.md](bitacora.md) | Decisiones de diseño, con su porqué |
| [plantillas/](plantillas/) | Ficha de flujo, iteración A/B y brief de identidad |
| `identidad/`, `flujos/`, `evidencia/` | Lo que produce el proceso: un brief por alternativa, las fichas de flujo y las capturas (las de Cozy / Medieval, en `evidencia/cozy-medieval/`) |
