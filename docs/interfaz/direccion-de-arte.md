# Dirección de arte

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · parte del [proceso de diseño](README.md)**

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
- ¿En 360 px la acción principal está a la vista sin hacer scroll?
- ¿Algo está ahí solo de adorno? Se saca.

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
- Los puntos de quiebre se declaran una vez en `tokens.css` como `--medida-quiebre-*`. Las media
  queries usan solo esos valores.
- Si una dirección elegida pide dejar Pico, eso es otra decisión de arquitectura: requiere un ADR
  sucesor de ADR-E6-06.
