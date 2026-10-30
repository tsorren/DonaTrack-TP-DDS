# ADR-E6-06 — Stack del cliente liviano: Thymeleaf, HTMX, Pico.css, tokens y mapas

- Status: proposed
- Date: 2026-10-30
- Deciders: Grupo 5 (decisiones D17–D21 de [`requerimientos-e6.md`](../../requerimientos/requerimientos-e6.md), marcadas ✅); versiones y forma de servir las librerías: propuesta del agente, aprobada por tsorren con el plan de directivas de E6
- Tags: cliente-liviano, interfaz, thymeleaf, htmx, pico-css, tokens, maplibre, entrega-6

> **Origen:** §11 de `requerimientos-e6.md` (ADR-E6-06, "Listo" para redactar) y decisiones
> D17–D21. `[OBSERVED]` El módulo `cliente-liviano` ya implementa D17–D20 (rama
> `E6_directivas_diseno_interfaz`); D21 (mapas) todavía no tiene pantalla.

## Contexto y Problema

El enunciado de E6 pide un proyecto nuevo para la interfaz gráfica, desacoplado de la lógica de
negocio, con vistas generadas por un **motor de plantillas** que renderiza los datos de las APIs
(RQ-E6-01 y RQ-E6-03), y con consideraciones de maquetado: usabilidad, patrones conocidos, WCAG
AA, responsive y tokens de diseño (RQ-E6-08, RNF-UI-01 a 11).

El grupo conoce Java y Spring, no un toolchain de frontend. Gran parte de la interfaz se va a
escribir con agentes de IA (Claude Code), así que importa cuánto CSS y JavaScript propio hay que
generar y revisar. Una diseñadora va a iterar la identidad visual.

Problema: ¿con qué tecnologías se construye el cliente liviano para cumplir el enunciado, con poco
código propio que revisar y una identidad visual propia?

## Atributos de Calidad y Drivers de Decisión

- Cumplir RQ-E6-03 (motor de plantillas, renderizado en el servidor).
- Sin toolchain nueva para el grupo (sin Node ni build de frontend; RNF-CL-03).
- Poco código propio para revisar: con IA, menos CSS y JS propio es menos deriva (D19).
- Accesibilidad de base y mejora progresiva (RNF-UI-09, RNF-CL-02).
- Una sola fuente de verdad para la identidad visual, editable por la diseñadora (D20).

## Relaciones Arquitectónicas

1. **Aplica:** [20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores](../20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md) (`proposed`): controllers como adaptadores de entrada, puertos para los datos y adaptadores de ejemplo (perfil `maqueta`) o REST.
2. **No usa:** [20260901-limites-y-responsabilidades-del-shared-kernel-common-lib](../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md): el cliente no depende de `common-lib`, porque su `@RestControllerAdvice` devolvería en JSON los errores de las vistas (RF-CL-08) y RNF-CL-01 pide desacople. La propagación de `traceId` se decide al integrar el gateway (ADR-E6-02).
3. **Complementa:** [20261030-verificacion-visual-con-playwright-y-axe](./20261030-verificacion-visual-con-playwright-y-axe.md) (`proposed`): cómo se comprueba lo que este stack produce.

## Alternativas Consideradas

Cada decisión se evaluó por separado; las tablas completas están en D17–D21 de
`requerimientos-e6.md`.

| Decisión | Elegida | Descartadas |
|---|---|---|
| D17 Motor de vistas | **Spring MVC + Thymeleaf** | Node + Express; SPA (React/Vue) `[REJECTED]` |
| D18 Interactividad | **HTMX + JS puntual** | SPA; JavaScript propio por pantalla |
| D19 Estilos | **Pico.css + CSS propio** | Solo CSS propio con tokens; Bootstrap 5 tematizado; Material (MDC Web) |
| D20 Identidad | **`tokens.css` en el repo, dos niveles** | Figma Variables exportadas; Style Dictionary |
| D21 Mapas | **MapLibre GL + OpenFreeMap** | Leaflet + OSM; Mapbox GL JS |
| Cómo se sirven HTMX y Pico | **WebJars + `webjars-locator-lite`** | Copiar los `.min` a `static/`; CDN |
| Versión de HTMX | **2.0.11** | 4.0.0 |

## Resultado de la Decisión

- **D17 — Spring MVC + Thymeleaf.** Las plantillas se renderizan en el servidor con los datos de
  las APIs (RQ-E6-03). Integración nativa con Spring Security (`sec:authorize`). Las *natural
  templates* se abren como HTML y sirven de maqueta.
- **D18 — HTMX.** El servidor devuelve fragmentos HTML; los indicadores de carga son declarativos
  (`hx-indicator`, RNF-UI-06). Todo lo esencial funciona sin JavaScript: cada `hx-*` está sobre un
  `form` o un `a` reales (RNF-CL-02). Sin JavaScript propio salvo en los mapas.
- **D19 — Pico.css como base**, tematizado solo por sus variables `--pico-*`, más CSS propio para
  los componentes. Los patrones de Material y HIG se siguen como **patrones**, no como librería
  (RNF-UI-03); cada componente del catálogo `/estilos` cita el suyo (RF-CL-05).
- **D20 — `tokens.css`** con valores base (`--p-*`) y tokens de uso (`--color-*`, `--espacio-*`,
  etc.). Las pantallas usan solo los de uso; las variables de Pico se asignan en `tokens.css` a
  partir de ellos. Git guarda la historia; Figma queda para explorar.
- **D21 — MapLibre GL + OpenFreeMap**, solo en las pantallas con mapa, con atribución y una lista
  con los mismos datos al lado (RF-CL-06).
- **WebJars.** HTMX y Pico se declaran en `cliente-liviano/pom.xml` como WebJars
  (`org.webjars.npm:htmx.org` y `org.webjars.npm:picocss__pico`). Con `webjars-locator-lite`
  (versión del BOM de Spring Boot) las plantillas piden `/webjars/htmx.org/dist/htmx.min.js` sin
  versión y Spring la completa: la versión vive en un solo lugar.
- **HTMX 2.0.11.** Es la última de la línea 2, que sigue recibiendo versiones: `[OBSERVED]` el
  WebJar 2.0.11 se publicó en Maven Central el 2026-09-24, después que el de la 4.0.0
  (2026-08-28).
- `[OBSERVED]` Versiones verificadas en Maven Central el 2026-10-30: `htmx.org` 2.0.11,
  `picocss__pico` 2.1.1 (la última). `webjars-locator-lite` 1.1.3 viene del BOM de Spring Boot
  4.0.7.

## Consecuencias

### Positivas

- Mismo lenguaje, build y CI que el resto del monorepo; el módulo entra solo a `build-and-test`
  y a Sonar por el descubrimiento de `*/pom.xml` de `main.yml`.
- Poco CSS y JavaScript propio que revisar; la accesibilidad de base sale del HTML semántico.
- Cambiar la identidad es editar los valores base de `tokens.css`.

### Negativas

- Pico condiciona la estructura del HTML. Si la identidad elegida necesita dejar Pico, hace
  falta un ADR sucesor de este (ver [dirección de arte](../../interfaz/direccion-de-arte.md)).
- Thymeleaf no tiene componentes con atributos libres: un componente es un fragmento con
  parámetros fijos, y las variantes se resuelven con parámetros o clases.
- HTMX 4 es una versión mayor, con cambios incompatibles con la 2. Migrar es una decisión aparte,
  cuando la línea 2 deje de recibir versiones.
- Los media queries no aceptan `var()`: los puntos de quiebre se declaran en `tokens.css` y se
  repiten como literal en `app.css`. `InterfazFitnessTest` exige que coincidan.

## Análisis por alternativa

- **Node + Express / SPA:** toolchain y build nuevos para el grupo; una SPA contradice RQ-E6-03
  (vistas renderizadas por un motor de plantillas). `[REJECTED]` en D18.
- **Solo CSS propio con tokens:** fidelidad total al diseño, pero mucho CSS que generar y revisar,
  con alto riesgo de deriva con IA.
- **Bootstrap 5 tematizado:** poco CSS propio, pero clases utilitarias en cada plantilla y una
  estética difícil de despegar de "Bootstrap".
- **Material (MDC Web):** baja fidelidad al diseño propio y JavaScript de componentes.
- **Figma Variables / Style Dictionary:** cada cambio pasa por Figma, o se suma Node al build.
- **Leaflet + OSM:** más liviano (~40 KB), pero el mapa no toma la identidad visual.
  **Mapbox GL JS:** licencia propietaria y token.
- **Copiar los `.min` a `static/`:** sin dependencias nuevas, pero la versión se fija a mano y
  Sonar analizaría código de terceros. **CDN:** depende de un tercero en ejecución y complica la
  política de seguridad de contenido.
- **HTMX 4.0.0:** la versión más nueva, con cambios incompatibles recientes y menos ejemplos y
  documentación probados; la 2 cubre todo lo que pide E6.

## Validación

- `mvn clean test -pl cliente-liviano -am`: `EstilosControllerTest` comprueba que el layout carga
  Pico y HTMX desde los WebJars; `InterfazFitnessTest` comprueba tokens, colores, quiebres,
  catálogo y contraste.
- `mvn test -pl cliente-liviano -Pvisual`: el catálogo `/estilos` pasa axe AA, toque de 48 px y
  sin desbordes en 360, 1280 y 1920 px.
