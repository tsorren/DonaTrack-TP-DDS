# AGENTS.md — cliente-liviano (interfaz web)

> Este archivo especializa `/AGENTS.md` para trabajo dentro de `cliente-liviano/`.
> Todas las políticas del AGENTS.md raíz siguen vigentes sin excepción.
> Este archivo solo agrega reglas locales; nunca relaja guardrails globales.
> Origen: RNF-CL-06 de [`requerimientos-e6.md`](../docs/requerimientos/requerimientos-e6.md).

---

## Proceso obligatorio

Ninguna pantalla se construye sin su flujo diseñado y aprobado. El proceso completo, quién decide
qué y cuándo algo está listo están en [`docs/interfaz/README.md`](../docs/interfaz/README.md).

| Paso | Skill (`.agents/skills/`) |
|---|---|
| Identidad visual (paleta, tipografía, forma, voz) | `identidad-visual` |
| Comparar dos variantes de un tema | `iteracion-ab` |
| Diseñar un flujo (recorrido, estados, textos, usabilidad) | `ux-flujo` |
| Construir vistas y componentes | `vista-ui` |
| Revisión independiente (y panel de exploración para criticar conceptos) | `revision-ui` |
| Relevar con el usuario un diseño nuevo y creativo (referencias, mensaje, libertades, alcance) | `relevamiento-creativo` |
| Diseñar desde cero sin el usuario, a partir de un encargo aprobado (workflow `.claude/workflows/corrida-creativa.js`) | `corrida-creativa` |

Al hablar con el usuario o la diseñadora: una decisión por pregunta, como máximo 3 preguntas por
ronda, opción recomendada, palabras simples y sin jerga técnica
([reglas](../docs/interfaz/README.md#cómo-se-le-pregunta-al-usuario)).

## Pertenencia

- Solo presentación: controllers, modelos de vista, plantillas, estilos y clientes de las APIs.
- **Sin entidades de dominio ni acceso a bases de negocio** (RNF-CL-01). Los datos llegan por las
  APIs REST, a través del gateway, como DTOs propios del cliente.
- **No depende de `common-lib`.** Su `@RestControllerAdvice` convertiría los errores de las vistas
  en JSON. La propagación de `traceId` se decide al integrar el gateway.
- Stack fijo (D17–D21, ADR-E6-06): Spring MVC + Thymeleaf, HTMX, Pico.css + `tokens.css` y
  MapLibre solo en pantallas con mapa. Sin build de frontend, sin Node y sin frameworks SPA.

## Estructura

| Paquete o carpeta | Contiene |
|---|---|
| `grupo5.cliente.web` | Controllers: reciben el request, piden datos al puerto y eligen la vista. Sin lógica de negocio. |
| `grupo5.cliente.vistas` | Modelos de vista (records) que consume la plantilla |
| `grupo5.cliente.puertos` | Interfaces para obtener y enviar datos |
| `grupo5.cliente.maqueta` | Adaptadores con datos de ejemplo (perfil `maqueta`) y sus escenarios |
| `templates/layout.html` | Estructura de las pantallas públicas: encabezado, contenido, pie y región de avisos |
| `templates/layout-privado.html` | Estructura de las pantallas privadas: barra lateral (barra inferior en el celular), campana, cuenta y región de avisos |
| `templates/fragmentos/` | Un componente = un fragmento parametrizado |
| `templates/paginas/` | Una vista por pantalla |
| `templates/alternativas/<nombre>/` | El juego completo de plantillas de otra alternativa (hoy, `cozy-medieval`): su `layout.html`, `layout-privado.html`, `fragmentos/` y `paginas/`, con la misma estructura de carpetas que `templates/`. En la maqueta, `?alternativa=<nombre>` lo elige |
| `static/css/tokens.css` | Única fuente de colores, tipografías, espacios, bordes y puntos de quiebre de la Alternativa Glassmorphism, la que se ve por defecto |
| `static/css/app.css` | Estilos de componentes y pantallas de Glassmorphism, solo con tokens de uso |
| `static/css/alternativas/<nombre>/` | Los estilos de esa alternativa de identidad, con su `tokens.css` y su `app.css` completos (hoy, `cozy-medieval`) |
| `static/css/variantes/` | Variantes A/B activas (se borran al decidir); las piezas que comparten varias, en `variantes/comun/` |
| `templates/variantes/` | Las variantes de estructura: reemplazan la página entera (también una pantalla del prototipo) con `?variante=<nombre>`; se borran al decidir |
| `static/js/` | JavaScript propio, solo como mejora sobre lo que ya funciona sin él (hoy, `cozy-navegacion.js` de Cozy y `prototipo-entidad.js` del prototipo) |
| `templates/prototipo/cozy-medieval/` | El prototipo navegable del flujo completo de Cozy (perfil `maqueta`, `/prototipo/<pantalla>`, `PrototipoController`); sus estilos, en `static/css/alternativas/cozy-medieval/prototipo/`. No son pantallas del sistema ni tienen gemela: cada una se construye en las dos alternativas cuando se apruebe su flujo |
| `src/test/resources/voz/<alternativa>.txt` | La tabla de voz de cada alternativa: los títulos y rótulos que dice a su modo (los vigila `InterfazFitnessTest`) |

## Reglas de plantillas

- Cada componente es un fragmento en `templates/fragmentos/<componente>.html`. Figura en `/estilos`
  con todos sus estados y el patrón que sigue ([catálogo de patrones](../docs/interfaz/patrones-y-microcopy.md)).
  Un test falla si falta.
- **Cada alternativa tiene su propio juego de plantillas.** Toda plantilla y todo fragmento de
  `templates/` tiene su gemela, con el mismo nombre, en `templates/alternativas/<nombre>/` (y al
  revés). Una pantalla o un componente nuevo se crea en los dos lugares. Las dos muestran los
  mismos datos y las mismas acciones, con los mismos nombres de botones y enlaces: un test compara
  cada par. Cada alternativa puede decir a su modo los títulos y los rótulos (sin números y nunca en
  un control), declarados en su tabla de voz, `src/test/resources/voz/<alternativa>.txt`. En el
  catálogo `/estilos` también entra la descripción de un componente que esa alternativa dibuja de
  otro modo (en Cozy, la navegación son cajones y no una barra).
- El catálogo `/estilos` de cada alternativa
  (`templates/alternativas/<nombre>/paginas/estilos.html`) muestra todos los fragmentos de esa
  alternativa. Un test falla si falta.
- HTML semántico: `header`, `nav`, `main`, `footer`, `button`, `label`, `table` con `th scope`.
- Texto siempre con `th:text`. `th:utext` nunca con datos que vienen de afuera.
- Cada estado del flujo (vacío, cargando, éxito, error, sin permiso) tiene su representación. En el
  perfil `maqueta` se ve con `?escenario=<nombre>`.
- Los textos salen de la ficha del flujo (`docs/interfaz/flujos/`), no se improvisan.

## HTMX y mejora progresiva

- Todo lo esencial funciona sin JavaScript: cada `hx-post` o `hx-get` está sobre un `form` o un
  `a` con `action` o `href` reales (RNF-CL-02).
- Si el request trae `HX-Request`, el controller devuelve el fragmento; si no, la página completa.
- Toda petición que pueda tardar más de 300 ms lleva `hx-indicator` (RNF-UI-06).
- Los avisos se agregan a la región `aria-live` del layout.
- JavaScript propio, solo como mejora (aclaración del usuario, 2026-10-30: "está permitido usar
  javascript mientras se siga considerando Server Side Rendering"). Las páginas se arman en el
  servidor con Thymeleaf; el JavaScript suma interacción o movimiento encima de enlaces y
  formularios reales, respeta "reducir movimiento" y va en `static/js/`. Nada de aplicaciones de
  una sola página (SPA), frameworks de cliente ni build de frontend.

## Reglas de estilos

- Pantallas y componentes usan **solo tokens de uso** (`--color-*`, `--fuente-*`, `--tamano-*`,
  `--espacio-*`, `--radio-*`, `--borde-*`, `--sombra-*`, `--movimiento-*`, `--capa-*` y `--medida-*`).
- Los valores base (`--p-*`) y las variables de Pico (`--pico-*`) se usan solo dentro de un
  `tokens.css`.
- Fuera de los `tokens.css` no hay colores literales (`#…`, `rgb()`, `hsl()`) ni atributos `style`.
- Hay dos alternativas de identidad, cada una con sus plantillas, los mismos datos y acciones
  (vigilados por test) y sus hojas de estilo: cada componente o pantalla nueva se escribe y se
  estiliza en cada una
  ([ADR propuesto](../docs/adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)).
- Las media queries usan solo los puntos de quiebre de `tokens.css` (`--medida-quiebre-*`), con
  mobile first.
- Fuentes con licencia OFL, servidas desde `static/fuentes/` junto con su licencia.
- Criterios gráficos y lo que evitamos: [dirección de arte](../docs/interfaz/direccion-de-arte.md).

## Accesibilidad y anchos

- WCAG AA. Foco visible. Controles de al menos 48×48 px (`--medida-toque-minimo`).
- Enlace para saltar al contenido. Cada mapa tiene al lado una lista con los mismos datos
  (RF-CL-06).
- Anchos de referencia: 360, 1280 y 1920 px. Sin desbordes en ningún ancho intermedio.

## Datos y seguridad

- Los datos de ejemplo son sintéticos: nada de personas, documentos ni direcciones reales.
- Sin secretos en el código ni en `application*.properties`.

## Validación

```bash
mvn spotless:check
mvn clean test -pl cliente-liviano -am             # unitarios y reglas de tokens y catálogo
mvn test -pl cliente-liviano -Pvisual              # capturas en 3 anchos, accesibilidad y toque
mvn test -pl cliente-liviano -Pvisual -Dverificacion.paginas=estilos/armazon   # solo esas rutas
mvn -pl cliente-liviano spring-boot:run -Dspring-boot.run.profiles=maqueta   # http://localhost:8090
```

La verificación visual deja también la primera pantalla del celular (`-360x640.png`). Para pasar
capturas a JPG de evidencia, comparar las de antes y después o armar la página de antes y después:
[`scripts/visual/`](../scripts/visual/README.md).

Una pantalla está lista cuando cumple el checklist de
[`docs/interfaz/README.md`](../docs/interfaz/README.md#listo-para-entregar) y pasó `revision-ui`
sin hallazgos bloqueantes.
