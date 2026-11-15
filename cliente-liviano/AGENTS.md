# AGENTS.md — cliente-liviano (interfaz web)

> Especializa `/AGENTS.md` sin relajarlo. Origen: RNF-CL-06 ([E6](../docs/requerimientos/requerimientos-e6.md)).

## Lectura obligatoria y skills
- Este archivo, la skill de la tarea y, para todo texto de pantalla, el
  [tono](../docs/interfaz/tono-y-comunicacion.md). El catálogo se consulta por búsqueda. El resto de
  [`docs/interfaz/`](../docs/interfaz/README.md) (bitácora, evidencia, fichas cerradas) es historia.
- `construir-ui` para producir, `revisar-ui` antes de cerrar (sin bloqueantes = lista) y
  `explorar-ui` para buscar alternativas, que no se mergean. Canónicas en `.agents/skills/`.
- Con el usuario o la diseñadora: una decisión por pregunta, 3 o menos por ronda, palabras simples.

## Sistema visual cerrado
- Una sola identidad ([ADR](../docs/adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md)).
  Valores: `static/css/tokens.css`. Verdad visual: `/estilos` (`templates/estilos.html`). Intención:
  el [brief](../docs/interfaz/identidad/brief.md).
- Piezas: el [catálogo](../docs/interfaz/componentes.md). Lo que existe se usa como está; lo que
  falta se crea como componente (fragmento con parámetros con nombre en `templates/componentes/`, hoja
  en `static/css/componentes/`, fila en el catálogo y muestra en `/estilos`) antes de usarlo.
- **Listas:** toda búsqueda, filtro u orden usa `componentes/listado` con un `vistas.Listado`, y
  debajo el paginador `componentes/paginas`. Un nombre por parámetro en todo el sitio. Excepción
  registrada: Ranking ([DTI-44](../docs/adr/DEUDA_TECNICA.md#dti-44), ítem 4).
- `InterfazFitnessTest` cerca todo esto: barras propias, nombres de parámetro, clases fuera del
  catálogo o de `clases-heredadas.txt`, tope de clases de un solo uso y de medidas literales (solo
  bajan), colores y `style` sueltos, contraste y Pico (que salió del stack,
  [ADR](../docs/adr/cliente-liviano/20261114-sin-pico-css-en-el-cliente-liviano.md)).

## Stack y pertenencia
- Spring MVC + Thymeleaf, HTMX y Alpine.js (build CSP), CSS propio en capas CUBE y MapLibre solo
  en mapas (D17–D21, [D31–D33](../docs/adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)).
  Sin build de frontend, sin Node y sin SPA.
- Solo presentación (RNF-CL-01): sin entidades de dominio ni bases. Datos por las APIs REST, por
  el gateway, como DTOs propios. No depende de `common-lib`: su `@RestControllerAdvice` volvería
  JSON los errores de las vistas. `TraceIdFiltro` genera el `traceId` y lo manda en `X-Trace-Id`.
- `web`: controllers (piden al puerto y eligen la vista, sin lógica); `Pantallas.mostrar` arma el
  modelo; `ErroresHttp` lleva errores de las APIs a páginas (401 a Ingresar, 403, 404, 502);
  `Envios.listo` redirige con el aviso a `#avisos`. `seguridad`: Keycloak; `UsuarioActual` es lo
  único que lee claims. `vistas`: records para la plantilla. `puertos`: un adaptador de `maqueta`
  y uno `rest` por puerto (`cliente.datos.<puerto>`). `rest`: `@HttpExchange` sobre `RestClient`,
  URL en `cliente.api.<api>.url`, con `X-Trace-Id` y el token si hay ingreso.
- Plantillas: `templates/<rol>-<pantalla>.html` (`publico-`, `donante-`, `entidad-`, `admin-`; en el
  nivel 3, con su sección). Piezas compartidas sin rol en el nombre o en `templates/componentes/`.

## Plantillas, HTMX y Alpine
- HTML semántico (`th scope` en tablas). Texto con `th:text`; `th:utext` nunca con datos de afuera.
  Cada estado (vacío, cargando, éxito, error, sin permiso) tiene su representación; en `maqueta`,
  con `?escenario=<nombre>`. Los textos salen de la ficha del flujo y siguen el tono.
- Lo esencial anda sin JavaScript: cada `hx-get` o `hx-post` va sobre un `form` o un `a` reales.
  Con `HX-Request`, el controller devuelve el fragmento. Más de 300 ms, `hx-indicator` (RNF-UI-06).
- Filtrar, cambiar de pestaña o abrir una ficha en la misma pantalla lleva `hx-boost="true"`; el
  `main#contenido` de los layouts ya trae `hx-target`, `hx-select` y `hx-swap`. No en enlaces a otra
  pantalla. Los scripts dentro del main se registran una sola vez. Avisos, a la región `aria-live`.
- JavaScript propio solo como mejora, en `static/js/`, con "reducir movimiento". Alpine (D31):
  estado que no sale del navegador, lógica en `Alpine.data()`; nada de `fetch`, `XMLHttpRequest`,
  listas del negocio, `x-html` ni datos personales en `localStorage`; `x-data` fuera del bloque que
  HTMX reemplaza.
- Diálogos: `<dialog popover>` nativo; si confirman algo sin vuelta, `data-bloquea` (`showModal()`).

## Estilos
- Solo tokens de uso (`--color-*`, `--fuente-*`, `--tamano-*`, `--espacio-*`, `--radio-*`,
  `--borde-*`, `--sombra-*`, `--movimiento-*`, `--capa-*`, `--medida-*`); los `--p-*`, solo en
  `tokens.css`. Sin colores literales ni `style` fuera de él. Media queries con
  `--medida-quiebre-*`, mobile first. Fuentes OFL en `static/fuentes/`.
- Capas, una vez en `app.css`: `base, tokens, composicion, utilidades, bloques, excepciones, pantallas`.
  Las hojas de pantalla solo disponen: sin colores, fuentes, `@keyframes` ni duraciones. El
  movimiento, con `data-movimiento` (`movimiento.css`). Una hoja heredada que se toca migra antes
  lo suyo del [backlog](../docs/interfaz/backlog-migracion-componentes.md).

## Piso y datos
- WCAG AA, foco visible, toque de 48 px, salto al contenido, lista junto a cada mapa (RF-CL-06) y sin
  desbordes de 360 a 1920 px. Datos de ejemplo sintéticos; sin secretos.

## Validación
```bash
mvn spotless:check
mvn clean test -pl cliente-liviano -am
mvn test -pl cliente-liviano -Pvisual -Dverificacion.paginas=<ruta>,<ruta>   # axe, toque y capturas
mvn -pl cliente-liviano spring-boot:run -Dspring-boot.run.profiles=maqueta  # http://localhost:8090
```
Herramientas para las capturas: [`scripts/visual/`](../scripts/visual/README.md).
