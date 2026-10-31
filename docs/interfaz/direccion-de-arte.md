# Dirección de arte

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 2 · 2026-10-31 (suma "Libertad creativa" y "Oficio") · parte del
> [proceso de diseño](README.md)**

La meta es una interfaz **con carácter propio**: que alguien la vea y no pueda confundirla con una
plantilla. La ambición tiene reglas, y debajo hay un piso que no se negocia.

## El piso (no se negocia)

- Contraste AA entre texto y fondo, y en los controles.
- Controles de al menos 48×48 px.
- Nada desbordado ni superpuesto entre 360 y 1920 px.
- Lo esencial funciona sin JavaScript.
- El movimiento respeta `prefers-reduced-motion`.
- Toda imagen con contenido tiene texto alternativo.
- El color nunca es la única señal: siempre va con texto o con ícono.

Una propuesta que rompe el piso no se le muestra al usuario. Se corrige antes.

## Cómo se arma una dirección

Cada dirección gráfica (la "A" o la "B" de una comparación) se describe con estas 7 piezas:

1. **Concepto en una frase**, anclado en el tema de DonaTrack: el recorrido de una donación, la
   comunidad, la transparencia y el alcance federal. Por ejemplo: "el camino visible", con el
   recorrido punteado de los mapas de E1 como motivo gráfico.
2. **Una audacia por pantalla.** Un solo elemento memorable (un título tipográfico grande, el
   motivo del recorrido, una foto protagonista). El resto, disciplinado.
3. **Tipografía.** Una o dos familias con licencia OFL, servidas desde el propio sitio, y una
   escala con proporción fija. El título se trata como elemento de diseño, no como texto más
   grande. Largo de línea de 80 caracteres o menos.
4. **Color.** Entre 4 y 6 colores base con nombre. Los estados de una entrega (pendiente, en
   camino, entregada, con problema) forman un sistema de color propio, siempre con texto.
5. **Forma.** Radios, bordes y sombras con una lógica única; no un poco de cada cosa.
6. **Imagen.** Las fotos reales de entregas son el contenido más fuerte del sitio. Las insignias
   de E1 ya marcan un estilo de ilustración (plana, geométrica, saturada) que se puede extender.
7. **Movimiento.** Solo para explicar un cambio: una entrega que pasa de estado, un aviso que
   aparece. Nada de animaciones de entrada en cada sección.

## Lo que evitamos

Rasgos de interfaz genérica que se ven en todas partes:

- degradados de adorno y fondos "de lavado";
- tarjetas idénticas con la misma sombra suave, repetidas en grilla;
- crema con terracota, o casi negro con verde ácido, solo porque están de moda;
- etiquetas en mayúsculas sobre cada título y separadores de punto medio en todos lados;
- flechas en todos los enlaces y efectos de hover en todas las tarjetas;
- un número gigante con una etiqueta chica como recurso por defecto;
- emojis en lugar de íconos;
- Pico.css sin tematizar: si se nota que es Pico, falta trabajo.

Si el usuario pide algo de esta lista, se respeta: es su decisión.

## Autocrítica con capturas

Antes de mostrar una propuesta, el agente mira sus capturas en los 3 anchos y se pregunta:

- ¿Podría ser la interfaz de cualquier otro producto? Si sí, falta concepto.
- ¿Cuál es la audacia de esta pantalla? Si no hay una, o hay tres, se ajusta.
- ¿Qué se ve primero, qué después y qué al final? ¿Ese orden es el correcto para la tarea?
- ¿En 360 px la acción principal está a la vista sin hacer scroll? La verificación visual deja la
  primera pantalla del celular (360 × 640) en una captura aparte para mirarlo.
- ¿Algo está ahí solo de adorno? Se saca.

## Libertad creativa

Cuando el usuario pide algo nuevo y creativo (un concepto, un rediseño o una alternativa desde
cero), la libertad es real. Lo que es libre y lo que no:

| Libre | Fijo |
|---|---|
| La composición de cada pantalla: qué va dónde, con qué forma y tamaño | Los datos y las acciones: los mismos, con el mismo nombre y el mismo destino (un test compara las gemelas) |
| La navegación: barra, cajones, índice, pestañas o lo que pida el concepto | El piso de arriba |
| Los materiales, los dibujos propios y el movimiento que explica algo | El stack: Thymeleaf, HTMX, Pico tematizado y tokens; JavaScript solo como mejora |
| La voz en títulos y rótulos, declarada en la tabla de voz | La otra alternativa: no se toca |
| La tipografía y la paleta de la alternativa, con contraste AA | Lo que el usuario fijó en el encargo creativo |

Reglas para encargar trabajo creativo, a una persona o a un agente (las plantillas de
[`plantillas/`](plantillas/) ya las traen: `encargo-creativo`, `encargo-constructor` y
`encargo-critico`):

- **Nunca "cambio mínimo" en una exploración.** El cambio mínimo es para arreglos.
- **Los conceptos difieren en composición y navegación**, no solo en estilo. Dos versiones del
  mismo orden son una sola idea.
- **Un concepto tímido pierde.** El encargo pide audacia y muestra lo que no hay que repetir (las
  capturas de lo actual).
- **Simple en el código, nunca en la idea:** lo más corto que funcione, sin recortar la idea para
  simplificar.
- **La crítica no diluye la audacia:** cada hallazgo trae un arreglo que conserva la idea; solo
  bloquea lo que rompe el piso, la seguridad o la tarea.
- **Cada iteración escala:** concepto (una pantalla) → sistema (navegación, pantallas y
  componentes) → flujo (el recorrido completo), cada una más ambiciosa que la anterior.

## Oficio

Reglas que salieron del rediseño de Cozy / Medieval (bitácora, 2026-10-31) y valen para cualquier
alternativa:

- **Un solo recurso material por objeto.** Una pieza es de madera, de papel o de metal; dos
  materiales en el mismo objeto compiten.
- **La letra decorativa, solo en títulos grandes.** Rótulos, datos y controles van en la letra de
  texto.
- **El nombre del menú siempre a la vista.** Si el título de la pantalla es propio (voz), el nombre
  del menú va como rótulo sobre él; en las pantallas de trabajo (el admin), el nombre llano es el
  título y la voz va de bajada.
- **Un solo calendario de ejemplo.** Un mismo "hoy" para todas las pantallas: lo pasado va antes,
  lo que viene después, y nada vence sin decirlo.
- **Lo que un dibujo dice sale de los datos.** Un mapa, un camino o un avance se dibuja desde el
  modelo y aguanta cualquier cantidad; no se dibuja a mano para el caso de ejemplo.
- **Cada mapa con su lista.** Todo lo que dice un dibujo está también en texto.
- **Pensar el caso grande:** más secciones en el menú, nombres largos, más avisos.
- **Nada se declara reemplazado sin decir qué reemplaza.** Una entrada de la bitácora que cambia
  una decisión anterior nombra cuál y quién la había tomado.

## Cómo se lleva a código

- **Pico.css es la base** (D19) y se tematiza por sus variables `--pico-*`. El CSS propio queda
  para los componentes y para la audacia de cada pantalla.
- **`tokens.css` tiene dos niveles** (D20):
  - **Valores base**, con prefijo `--p-` (por ejemplo, `--p-verde-500`). Solo se usan dentro de
    `tokens.css`.
  - **Usos**, que son los únicos que usan las pantallas y los componentes. Prefijos:
    `--color-`, `--fuente-`, `--tamano-`, `--espacio-`, `--radio-`, `--borde-`, `--sombra-`,
    `--movimiento-`, `--capa-` y `--medida-`. Por ejemplo, `--color-accion-primaria` o
    `--medida-toque-minimo`.
- Las variables `--pico-*` se asignan solo en `tokens.css`, a partir de los usos.
- Cada alternativa de identidad (Glassmorphism y Cozy / Medieval) tiene su propio `tokens.css`, su
  propio `app.css` y su propio juego de plantillas, con estas mismas reglas
  ([ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)).
- Los puntos de quiebre se declaran una vez en `tokens.css` como `--medida-quiebre-*`. Las media
  queries usan solo esos valores.
- Si una dirección elegida pide dejar Pico, eso es otra decisión de arquitectura: requiere un ADR
  sucesor de ADR-E6-06.
