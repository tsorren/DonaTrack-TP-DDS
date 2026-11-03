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
| `grupo5.cliente.maqueta` | Lo que sirve el sitio con datos de ejemplo (perfil `maqueta`): `SitioController` (`/` es la portada; `/<pantalla>` muestra `templates/<pantalla>.html`, y una sección del menú sin plantilla, `sin-dibujar`), los escenarios y las variantes A/B |
| `templates/<pantalla>.html` | Una plantilla por pantalla. El nombre empieza con el rol (`publico-`, `donante-`, `entidad-`, `admin-`) y, en el nivel 3, con el de su sección (`donante-donaciones-detalle`) |
| `templates/layout-privado.html` y `layout-publico.html` | Estructura de las pantallas privadas y de las públicas (la portada tiene la suya) |
| `templates/` (sin rol en el nombre) | Piezas compartidas que no se sirven como pantalla: `iconos`, `pie`, `navegacion-publica`, `piezas-entidad`, los datos de ejemplo (`datos-*`, `viaje-datos`) y `sin-dibujar` |
| `static/css/tokens.css` | Única fuente de colores, tipografías, espacios, bordes y puntos de quiebre |
| `static/css/app.css`, `privado.css` y `<pantalla>.css` | Estilos comunes, los de las pantallas privadas y una hoja por pantalla, solo con tokens de uso |
| `static/css/variantes/` | Variantes A/B activas (se borran al decidir); las piezas que comparten varias, en `variantes/comun/` |
| `templates/variantes/` | Las variantes de estructura: reemplazan la pantalla entera con `?variante=<nombre>`; se borran al decidir |
| `static/js/` | JavaScript propio, solo como mejora sobre lo que ya funciona sin él (hoy, `publico.js`, las mejoras de las páginas públicas) |

Una sola identidad visual ([ADR propuesto](../docs/adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md)).
Una identidad o un rediseño nuevo se propone en una rama propia, sobre el sitio (skills
`relevamiento-creativo` y `corrida-creativa`).

## Reglas de plantillas

- Una pieza que usan varias pantallas va en una plantilla compartida de `templates/` (sin rol en el
  nombre) como fragmento parametrizado; no se copia.
- HTML semántico: `header`, `nav`, `main`, `footer`, `button`, `label`, `table` con `th scope`.
- Texto siempre con `th:text`. `th:utext` nunca con datos que vienen de afuera.
- Cada estado del flujo (vacío, cargando, éxito, error, sin permiso) tiene su representación. En el
  perfil `maqueta` se ve con `?escenario=<nombre>`.
- Los textos salen de la ficha del flujo (`docs/interfaz/flujos/`), no se improvisan, y siguen el
  [tono común](../docs/interfaz/tono-y-comunicacion.md): lo pendiente se dice pendiente, las cifras
  van con período y fecha, sin urgencia ni culpa. La voz propia de la identidad cambia la forma de
  un título o un rótulo, no lo que significa.

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
- Los valores base (`--p-*`) y las variables de Pico (`--pico-*`) se usan solo dentro de
  `tokens.css`.
- Fuera de `tokens.css` no hay colores literales (`#…`, `rgb()`, `hsl()`) ni atributos `style`.
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
mvn clean test -pl cliente-liviano -am             # unitarios y reglas de tokens y contraste
mvn test -pl cliente-liviano -Pvisual              # capturas en 3 anchos, accesibilidad y toque
mvn test -pl cliente-liviano -Pvisual -Dverificacion.paginas=donante-inicio   # solo esas rutas
mvn test -pl cliente-liviano -Pvisual -Dverificacion.variantes=<tema>-a,<tema>-b   # una pasada por variante A/B
mvn -pl cliente-liviano spring-boot:run -Dspring-boot.run.profiles=maqueta   # http://localhost:8090
```

Cuando axe no puede medir el contraste (por un pseudo-elemento, un degradado o una imagen de
fondo), la verificación visual lo mide con el color de fondo de los contenedores, sin ver lo que
pinta el pseudo-elemento, y falla por debajo de AA. El texto sobre un degradado lineal, o sobre una
imagen sin color opaco debajo, no se puede medir así: queda para la revisión a ojo.

La verificación visual deja también la primera pantalla del celular (`-360x640.png`). Para pasar
capturas a JPG de evidencia, comparar las de antes y después o armar la página de antes y después:
[`scripts/visual/`](../scripts/visual/README.md).

Una pantalla está lista cuando cumple el checklist de
[`docs/interfaz/README.md`](../docs/interfaz/README.md#listo-para-entregar) y pasó `revision-ui`
sin hallazgos bloqueantes.
