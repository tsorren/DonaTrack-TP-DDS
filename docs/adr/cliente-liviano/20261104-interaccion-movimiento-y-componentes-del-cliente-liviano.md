# Interacción, movimiento y componentes del cliente liviano: Alpine CSP, CSS nativo y capa CUBE con cerco

- Status: proposed
- Date: 2026-11-04
- Deciders: tsorren (decisiones D31 a D33, confirmadas en la sesión del 2026-11-04); análisis y redacción del agente
- Tags: cliente-liviano, interfaz, alpine, css, movimiento, componentes, agentes, entrega-6

> **Origen:** análisis del 2026-11-04 sobre qué cliente liviano conviene y cómo mejorar el stack
> para que los agentes construyan una interfaz más rica, con movimiento y componentes reutilizables.
> Todo el diseño y la implementación del cliente liviano los hace Claude Code (dicho por el usuario).
> El usuario confirmó: Alpine con el build CSP, "anda sin JavaScript" solo en lo esencial, CSS nativo
> moderno, sistema de movimiento, herramientas para los agentes, componentes de forma incremental con
> backlog de migración obligatorio y la capa CUBE propia con cerco. Tailwind queda diferido.

## Contexto y Problema

- `[DOCUMENTED]` El enunciado de E6 (págs. 26–27 de
  [`Enunciado-TP-Anual-2026.pdf`](../../entregas/Enunciado-TP-Anual-2026.pdf)) pide un cliente liviano
  desacoplado que consuma las APIs REST, con un **motor de plantillas** cuyas vistas "deberán renderizar
  dinámicamente los datos obtenidos de las APIs". Las consideraciones de maquetado piden toasts,
  modales y banners en toda acción, tooltips y *onboarding hints*, indicadores de carga sobre los 300 ms,
  WCAG AA y tokens de diseño unificados. No prohíben JavaScript ni frameworks de cliente.
- `[DOCUMENTED]` El decálogo del TP (punto 9) habilita "alternativas tecnológicas o de implementación
  (protocolos, frameworks, estilos de API)", y el prólogo pide que la tecnología responda a decisiones
  fundamentadas.
- `[DOCUMENTED]` [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
  (`proposed`) fijó D18 (HTMX + JavaScript puntual, sin frameworks de cliente) y D19 (Pico.css + CSS
  propio). RNF-CL-02 pide que las pantallas anden sin JavaScript y RNF-CL-03, sin build de frontend
  ([requerimientos de E6](../../requerimientos/requerimientos-e6.md)).
- `[OBSERVED]` Estado del módulo el 2026-11-04:
  - 8.924 líneas de CSS en 29 hojas: `tokens.css`, cuatro compartidas (`app.css`, `privado.css`, `sellos.css`, `formularios.css`) y el resto, comunes por rol o una por pantalla; la más grande, `publico-portada.css`, tiene 1.732.
  - 212 clases que aparecen una sola vez en las hojas.
  - 96 fragmentos Thymeleaf y 169 usos, con parámetros por posición (`sello(tono)`, `error(titulo, volver)`)
    y sin forma de recibir un bloque de HTML.
  - `tokens.css` no define ningún token `--movimiento-*`, aunque `cliente-liviano/AGENTS.md` lo lista
    como familia permitida. Hay 13 `@keyframes` en 5 hojas y 28 duraciones literales fuera de `tokens.css`.
  - Se usan `animation-timeline`, `prefers-reduced-motion` y `popover` (en 4 hojas y 6 plantillas).
    No se usan View Transitions, `@layer`, `@container` ni `@starting-style`.
  - El catálogo `/estilos` (RF-CL-05) se borró con el
    [ADR de una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md): hoy no hay
    catálogo de componentes.
  - HTMX todavía no se usa: `hx-post` y `hx-indicator` aparecen solo en comentarios de la maqueta.
    El JavaScript propio es `static/js/publico.js` (41 líneas).
- `[OBSERVED]` En dos semanas el sitio pasó por tres identidades. La skill `corrida-creativa` propone
  identidades nuevas en ramas: los rediseños son parte del proceso.

Problema: ¿qué hay que sumar al stack para que agentes sin memoria entre sesiones construyan una
interfaz rica y con movimiento, reutilicen componentes en lugar de inventar estilos y sostengan una
identidad propia, sin salir del enunciado ni sumar un build de frontend?

## Atributos de Calidad y Drivers de Decisión

| Driver | Peso | Por qué |
|---|:-:|---|
| Adherencia al enunciado (motor de plantillas, MVC, cliente liviano) | 20 | Mandatorio (AGENTS.md §2) |
| Riqueza visual y movimiento | 20 | Pedido del usuario |
| Consistencia bajo autoría de agentes (vocabulario cerrado, poca deriva) | 20 | Todo lo escribe Claude Code |
| Identidad propia y rediseños baratos | 15 | Proceso de corridas creativas |
| Costo y riesgo antes del 13/11 | 15 | Fecha de E6 |
| Accesibilidad y mejora progresiva | 10 | WCAG AA del enunciado |

## Relaciones Arquitectónicas

1. **Modifica en parte:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`). D18 pasa a "HTMX + Alpine CSP + JavaScript puntual". D19 se mantiene (Pico + CSS
   propio), con el CSS propio ordenado en capas CUBE. D17, D20 y D21 no cambian. No lo reemplaza
   entero: no es `superseded`.
2. **Respeta:** [una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md) (`accepted`).
   La capa CUBE ordena el CSS de Raíces; no cambia la identidad.
3. **Complementa:** [verificación visual con Playwright y axe](./20261030-verificacion-visual-con-playwright-y-axe.md)
   (`proposed`), que suma el video del movimiento.
4. **No cambia:** la [arquitectura de capas y puertos](../20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md).
5. **Corregido en parte por (propuesto):** [diálogo con `popover` y fragmentos solo donde hay estructura](./20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md)
   (`proposed`). Dos frases de este ADR no son lo que el sitio hace hoy: en D31, el `<dialog>` con
   `showModal()` que atrapa el foco (el del sitio es un `<dialog popover>`, que no lo atrapa, para que
   una baja se confirme sin JavaScript) y, en D33, "un componente es un fragmento con parámetros con
   nombre y slots" (solo tienen fragmento los componentes con estructura o parámetros). Nota agregada
   el 2026-11-04 con el OK del usuario; el texto de D31 y D33 no se editó.
   Los datos siguen entrando por los controllers.

## Contraste con el enunciado

| Texto del enunciado | Condición que impone este ADR |
|---|---|
| "motor de plantillas", "renderizar dinámicamente los datos obtenidos de las APIs" | Toda página y todo fragmento salen de Thymeleaf. Alpine no hace `fetch`, no arma listas con datos del negocio y no guarda tokens |
| "patrones de arquitectura MVC a través de la implementación de un Cliente Liviano" | El controller sigue en Spring. Alpine maneja solo estado visual que no sale del navegador |
| "aplicación de estilos sobre maquetas Web a través de CSS" | El CSS sigue escrito por el equipo, en capas, sin generador |
| "Estilo visual unificado… tokens de diseño" | El movimiento entra a `tokens.css`; las capas usan solo tokens de uso |
| "Cumplimiento de normas WCAG" | Lo esencial anda sin JavaScript; los diálogos son `<dialog>` nativos; "reducir movimiento" se resuelve dentro de las primitivas |

## Alternativas Consideradas

Puntajes de 1 a 5; estimaciones del agente (`[INFERRED]`).

### D31 — Estado de interfaz en el navegador

| Alternativa | Enunciado (20) | Riqueza (20) | Consistencia (20) | Identidad (15) | Costo (15) | A11y (10) | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **A1. Alpine.js, build CSP (WebJar `alpinejs__csp`)** | 5 | 4 | 4 | 4 | 4 | 4 | **4,20** |
| A2. Alpine.js estándar | 5 | 4 | 4 | 4 | 4 | 4 | 4,20, pero necesita `unsafe-eval` en una CSP |
| A3. Petite-Vue | 5 | 4 | 3 | 4 | 3 | 3 | 3,75 |
| A0. JavaScript propio por pantalla (status quo) | 5 | 3 | 2 | 4 | 4 | 3 | 3,50 |

A1 y A2 empatan en los drivers; desempata la seguridad. A2 evalúa expresiones con `new Function` y
una política de seguridad de contenido sin `unsafe-eval` (posible en E7, "Seguridad") la rompe. El
build CSP solo acepta nombres de propiedades y métodos: ese límite también impide meter lógica en el HTML.

### D32 — Movimiento y CSS moderno

| Alternativa | Enunciado | Riqueza | Consistencia | Identidad | Costo | A11y | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **M1. CSS nativo (View Transitions, `@starting-style`, `@container`, `@layer`, `popover`) + tokens y primitivas de movimiento + Web Animations API** | 5 | 5 | 5 | 5 | 4 | 5 | **4,85** |
| M2. Librería de animación (GSAP o Motion) como WebJar | 5 | 5 | 3 | 4 | 3 | 3 | 3,95 |
| M0. `@keyframes` por pantalla con duraciones literales (status quo) | 5 | 4 | 2 | 4 | 5 | 3 | 3,85 |

### D33 — Organización del CSS y componentes

| Alternativa | Enunciado | Riqueza | Consistencia | Identidad | Costo | A11y | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **C1. Capa CUBE propia con cerco mecánico + componentes con slots, migración incremental con backlog** | 5 | 4 | 4 | 5 | 4 | 5 | **4,45** |
| C2. Tailwind v4 (CLI standalone) | 4 | 4 | 4 | 3 | 1 | 4 | 3,40 |
| C3. Spring ViewComponent / JTE | 5 | 3 | 4 | 4 | 2 | 4 | 3,70 |
| C0. Una hoja por pantalla (status quo) | 5 | 4 | 2 | 4 | 5 | 4 | 3,95 |

## Resultado de la Decisión

### D31 — Alpine.js con el build CSP, para estado que no sale del navegador

- WebJar `org.webjars.npm:alpinejs__csp`, que se servirá como HTMX y Pico (versión en el `pom.xml`,
  sin versión en las plantillas; pendiente, B0-05 del backlog). `[OBSERVED]` 3.17.5 publicada el 2026-10-22 en Maven Central. Sin
  plugins: el plugin Focus va dos versiones menores atrás (3.15.12) y el `<dialog>` nativo con
  `showModal()` ya atrapa el foco.
- **Regla de reparto:**

  | HTMX: habla con el servidor | Alpine: estado que nunca sale del navegador |
  |---|---|
  | Filtros, paginación, "marcar leída" | Abrir y cerrar menús, diálogos y acordeones |
  | Polling del camión (D22) | Paso actual de un asistente |
  | Enviar formularios, confirmar recepción | Vista previa de fotos antes de enviar |
  | Progreso de la importación CSV | Pestañas, contadores de caracteres, "no volver a mostrar" una pista |
  | Cualquier dato del negocio | Mostrar y ocultar la cola de avisos (el contenido lo trae HTMX) |

- **Prohibido en Alpine:** `fetch` o `XMLHttpRequest`, armar listas con datos del negocio, guardar
  tokens o datos personales en `localStorage`, y `x-html`.
- `x-data` va fuera del bloque que HTMX reemplaza, para no perder el estado. `x-cloak` solo en
  elementos que no son esenciales.
- **RNF-CL-02 se ajusta a "lo esencial":** navegar, ver datos y enviar formularios andan sin
  JavaScript. Lo que Alpine o el JavaScript propio suman (vista previa, animación, pestañas) puede
  degradar a una versión más simple, nunca a una pantalla vacía.

### D32 — CSS nativo moderno y sistema de movimiento

- **Capas:** `@layer pico, tokens, composicion, utilidades, bloques, excepciones, pantallas;`
  declarado una vez en `app.css`. Pico queda abajo de todo y las hojas propias lo superan sin pelear
  por especificidad.
- **CSS nativo como mejora progresiva:** View Transitions entre páginas (`@view-transition { navigation: auto; }`)
  y en los intercambios de HTMX (`transition:true`); `@starting-style` y `transition-behavior: allow-discrete`
  para abrir `<dialog>` y `popover`; `@container` en los componentes; API `popover` para menús y
  tooltips (ya se usa en 4 hojas; se extiende). `[INFERRED]` Un navegador sin soporte navega igual,
  sin animación.
- **Tokens de movimiento** en `tokens.css` (dos niveles, como D20): duraciones (`corta`, `media`,
  `larga`), curvas (`entrada`, `salida`, `enfasis`) y distancias.
- **Primitivas con nombre** en `static/css/movimiento.css` (`aparece`, `sube`, `resalta`, `crece`,
  `traza`), aplicadas con `data-movimiento`. "Reducir movimiento" se resuelve una vez, dentro de
  ellas. Las animaciones de una identidad (por ejemplo, el viento de la portada) también se declaran
  ahí, con tokens.
- **JavaScript de movimiento:** Web Animations API nativa. Una librería, solo con una coreografía que
  CSS y WAAPI no resuelvan, y con su propio análisis.

### D33 — Capa CUBE propia con cerco, y componentes migrados de a poco

- **Capas CUBE** (Composición, Utilidad, Bloque, Excepción):

  | Capa | Carpeta | Contenido |
  |---|---|---|
  | Composición | `static/css/composicion.css` | Disposiciones: `pila`, `racimo`, `barra-lateral`, `cubierta`, `grilla`, `centro`, `marco`. El catálogo anota el nombre de *Every Layout* al lado (`stack`, `cluster`, `sidebar`, `cover`, `grid`, `center`, `frame`) para aprovechar lo que el modelo ya sabe |
  | Utilidades | `static/css/utilidades.css` | Pocas, una por token de uso: espacio, tamaño de texto, tono, `data-movimiento` |
  | Bloques | `static/css/componentes/<componente>.css` | Un componente por hoja, con su fragmento en `templates/componentes/<componente>.html` |
  | Excepciones | dentro del bloque | Variantes por atributo: `[data-estado="pendiente"]`, `[data-tono="acento"]` |
  | Pantallas | `static/css/<pantalla>.css` | Solo disposición propia de la pantalla: sin colores, tipografía ni movimiento |

- **Componentes con slots:** un fragmento recibe parámetros con nombre y bloques de HTML con
  *fragment expressions* de Thymeleaf 3.1 (`~{::#cuerpo}`).
- **Catálogo vivo:** vuelve `/estilos` (RF-CL-05) como catálogo de la capa CUBE, más
  `docs/interfaz/componentes.md` como índice para los agentes: nombre, cuándo usarlo, parámetros,
  slots, estados, movimiento y patrón de Material o HIG.
- **Cerco mecánico** (en `InterfazFitnessTest` y en las skills):
  1. Toda clase usada en una plantilla existe en el catálogo.
  2. Tope de clases de un solo uso, que baja con cada ítem del backlog.
  3. Las hojas de pantalla no declaran colores, fuentes, `@keyframes` ni duraciones.
  4. Sin duraciones ni curvas literales fuera de `tokens.css`.
  5. Una clase o un componente nuevo entra con su entrada en el catálogo y en `/estilos`.
  6. `vista-ui` empieza por el catálogo; `revision-ui` rechaza clases fuera de él.
- **Migración incremental obligatoria:** los componentes nuevos nacen en la capa CUBE. Los existentes
  se migran con el [backlog de migración](../../interfaz/backlog-migracion-componentes.md): toda tarea
  que toca una hoja de pantalla migra antes lo que esa pantalla usa del backlog. El backlog se revisa
  al cerrar cada entrega.

### Herramientas para los agentes

- Captura de video del movimiento con Playwright (`recordVideo`) en el perfil `visual`.
- `vista-ui` y `revision-ui` cargan `componentes.md` como primer paso.

## Consecuencias

### Positivas

- Interacción rica sin build y sin salir de SSR: diálogos, pestañas, asistentes y vista previa con
  Alpine; transiciones entre páginas con CSS nativo.
- El movimiento tiene vocabulario y tokens: los agentes componen en lugar de inventar.
- Las plantillas dicen qué es cada cosa, no cómo se ve: un rediseño cambia sobre todo el CSS.
- Sin dependencia externa en el CSS: sin versiones mayores que migrar y bajo costo de salida.
- RF-CL-05 vuelve a cumplirse con el catálogo.

### Negativas

- Dos modelos de interacción (HTMX y Alpine): la regla de reparto se tiene que vigilar en la revisión.
- El build CSP de Alpine limita las expresiones: la lógica va en `Alpine.data()` dentro de `static/js/`.
- El vocabulario CUBE es privado: el catálogo entra en el contexto de cada tarea de interfaz.
- Mientras dure la migración conviven hojas por pantalla y capas CUBE.
- `[INFERRED]` Algunas funciones de CSS (View Transitions entre documentos) pueden faltar en Firefox:
  quedan como mejora.

### Riesgos

- Si el cerco no se implementa o se relaja, la capa CUBE repite el problema de hoy (CSS que solo
  crece). Por eso el cerco es parte de la decisión, no un agregado.

## Análisis por alternativa

- **A1. Alpine CSP (elegida):** estado declarativo sin build, compatible con una CSP estricta y con
  la lógica obligada a vivir en `Alpine.data()`; en contra, un segundo modelo de interacción junto a
  HTMX y expresiones limitadas.
- **M1. CSS nativo + movimiento con tokens (elegida):** 0 KB, mejora progresiva y movimiento con
  vocabulario cerrado; en contra, soporte desparejo entre navegadores (View Transitions entre
  documentos) y trabajo inicial de tokens y primitivas.
- **C1. CUBE con cerco (elegida):** vocabulario propio que lleva la identidad, plantillas que dicen
  qué es cada cosa y sin dependencia externa; en contra, el catálogo entra en el contexto de cada
  tarea y el cerco hay que construirlo y sostenerlo.
- **A2. Alpine estándar:** expresiones libres en el HTML, más cómodas; en contra, `unsafe-eval` y
  lógica dispersa en atributos.
- **A3. Petite-Vue:** más liviano; en contra, `[INFERRED]` sin versiones nuevas desde 2022, sin build
  CSP y sin plugin de foco.
- **A0. JavaScript propio:** sin dependencia; en contra, cada pantalla reinventa el manejo de estado
  y los agentes generan más código para revisar.
- **M2. GSAP o Motion:** coreografías largas con poco código; en contra, una dependencia más para lo
  que hoy resuelven CSS y WAAPI, y movimiento fuera de los tokens.
- **M0. Status quo:** cero trabajo; en contra, duraciones y curvas sueltas que contradicen la familia
  `--movimiento-*` ya prevista en las reglas del módulo.
- **C2. Tailwind v4:** los agentes lo escriben con mucha soltura y no deja CSS muerto. En contra:
  - cambia D19 y RNF-CL-03, porque el binario standalone es un paso de build;
  - choca con Pico (su *preflight* resetea los mismos elementos);
  - pide traducir los tokens a `@theme`;
  - deja la identidad repartida en cada atributo `class`, lo que encarece los rediseños;
  - tira hacia el aspecto genérico de Tailwind;
  - obliga a migrar 8.924 líneas antes del 13/11.

  Queda diferido (ver Trabajo futuro).
- **C3. Spring ViewComponent / JTE:** componentes con clase Java; en contra, biblioteca de nicho y
  migración de todas las plantillas.
- **C0. Status quo:** sin costo; en contra, 212 clases de un solo uso y cada pantalla con su propio
  vocabulario.

## Principios de diseño

- **Alta cohesión y bajo acoplamiento (GRASP):** cada componente reúne fragmento, hoja, estados y
  movimiento; las pantallas dependen del catálogo y no de los estilos de otras pantallas.
- **Responsabilidad única (SOLID):** HTMX habla con el servidor, Alpine solo con el estado del
  navegador, y las hojas de pantalla solo disponen.
- **Abierto/cerrado (SOLID):** un componente se extiende por excepciones (`data-estado`,
  `data-tono`) sin editar su bloque.
- **Composite (GoF):** los fragmentos con slots componen piezas dentro de piezas, y la página es el
  árbol resultante.
- **Variaciones protegidas (GRASP):** la identidad queda detrás de los tokens y del catálogo, así
  que un rediseño cambia esas capas y no las plantillas.
- **DDD:** no aplica de forma directa, porque es capa de presentación. Respeta el desacople del
  dominio (RNF-CL-01): Alpine no toca datos del negocio.

## Validación

- Hoy: `node scripts/agent-check.js` (herramienta del repo, no del cliente: RNF-CL-03 sigue sin Node
  en el módulo) y la validación de enlaces de los documentos tocados.
- Al implementar cada ítem del backlog:
  - `mvn clean test -pl cliente-liviano -am`, con las reglas nuevas de `InterfazFitnessTest`;
  - `mvn test -pl cliente-liviano -Pvisual` (axe AA, 48 px, sin desbordes en 360, 1280 y 1920 px), más
    el video del movimiento;
  - una pasada sin JavaScript sobre los flujos esenciales.

## Trabajo futuro

- **Tailwind:** reevaluar después de E6 solo si se cumplen tres condiciones: quitar Pico en el mismo
  ADR, componentes ya migrados (para que no duplique clases) y un cerco equivalente (valores
  arbitrarios prohibidos por test).
- **Pico:** medir cuánto CSS propio existe para pisarlo antes de decidir si se quita; hoy es
  `Insufficient evidence to determine`.
- Implementación y deuda: [DTI-34](../DEUDA_TECNICA.md#dti-34) y el
  [backlog de migración](../../interfaz/backlog-migracion-componentes.md).
