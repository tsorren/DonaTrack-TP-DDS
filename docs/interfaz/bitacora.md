# Bitácora de diseño de la interfaz

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Se actualiza en cada decisión · parte del [proceso de diseño](README.md)**

Cada decisión de diseño queda acá, de la más nueva a la más vieja, con el mismo formato que usa el
entregable para la cátedra. Las comparaciones A/B agregan sus capturas en `evidencia/<tema>/`.

## Formato

```markdown
### AAAA-MM-DD · <tema> · <decisión en una línea>

- **Decisión:** qué se eligió.
- **Alternativas:** qué otras opciones hubo (en un A/B, la variante descartada).
- **Por qué:** el motivo, en palabras simples.
- **Costo:** qué se resigna o qué queda pendiente.
- **Quién decidió:** usuario, diseñadora o equipo.
- **Evidencia:** capturas o enlace (si hay).
```

## Decisiones

### 2026-10-30 · proceso · Las comparaciones se ven en un visor, no en una galería

- **Decisión:** `target/capturas/index.html` y `comparacion.html` son un visor pensado para
  1920 × 1080: índice de secciones a la izquierda, dos variantes lado a lado elegibles con
  selectores o con las flechas, ancho de 360, 1280 o 1920 (teclas 1, 2 y 3) y desplazamiento a la
  par. Cada variante muestra el primer comentario de su CSS como descripción.
- **Alternativas:** la galería con todas las capturas una debajo de la otra; páginas armadas a
  mano para cada comparación.
- **Por qué:** con varias opciones y capturas de página completa, la galería no se podía recorrer
  ni comparar.
- **Costo:** el visor usa JavaScript (solo en una herramienta de trabajo, no en el sitio).
- **Quién decidió:** usuario.

### 2026-10-30 · paleta · Pendiente entre la del prototipo y Grafito cálido

- **Decisión:** pendiente. Quedan dos candidatas como variantes `paleta-a` (la del prototipo:
  petróleo, menta y turquesa) y `paleta-b` (Grafito cálido y menta). `tokens.css` sigue con la del
  prototipo hasta decidir.
- **Alternativas:** se descartaron "Verdes y sol"; Bosque y lima, Océano y aqua, Ciruela y menta;
  Café y menta, Vino y menta, Grafito y sol; Grafito y menta (gustó, pero con dudas para un
  proyecto solidario); y tres nuevas: Atardecer, Girasol y Mar y fucsia.
- **Por qué:** el grafito gustó por sobrio; falta confirmar que transmita lo comunitario.
- **Costo:** los estados se armonizan recién con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/paleta/`.

### 2026-10-30 · densidad · Como el prototipo

- **Decisión:** los espacios y las alturas del prototipo v2.
- **Alternativas:** una versión compacta, con cerca de un cuarto menos de aire.
- **Por qué:** cómoda en el celular, que es donde la usan la entidad y el donante.
- **Costo:** las tablas del admin pueden necesitar una versión compacta propia más adelante.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/densidad/`.

### 2026-10-30 · tipografia · DM Sans sola

- **Decisión:** DM Sans para todo, como el prototipo.
- **Alternativas:** DM Sans para leer y Bricolage Grotesque para títulos y números.
- **Por qué:** el cambio era sutil y una sola familia es más simple de mantener.
- **Costo:** ninguno.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/tipografia/`.

### 2026-10-30 · componente:insignia · Estados en tinte, un color por estado

- **Decisión:** cada estado de una entrega es una píldora con fondo claro, texto, borde e ícono del
  mismo tono: ámbar (pendiente), azul petróleo (en camino), verde (entregada) y rojo terroso (con
  problema).
- **Alternativas:** la píldora oscura del prototipo, con solo el ícono de color (se distinguía
  mal); píldoras de color sólido.
- **Por qué:** se distinguen de un vistazo, como pidió el usuario, sin competir con los paneles
  oscuros.
- **Costo:** hay que verificar que los colores no choquen con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/componente-insignia/`.

### 2026-10-30 · direccion-general · Fiel al prototipo v2

- **Decisión:** la identidad del prototipo v2, llevada al cliente tal cual: paleta, DM Sans,
  curvas topográficas, vidrio, ruta punteada con nodos que brillan, panel destacado con su red,
  tiles de cifras, recorrido numerado, misión y menú lateral.
- **Alternativas:** primera vuelta: "el prototipo tal cual" en versión reducida y "el camino
  visible" (plano y firme). Las dos se rechazaron: la B, por plana y vacía; la A, por ser una
  versión degradada del prototipo.
- **Por qué:** el prototipo ya es el diseño de la diseñadora y del equipo; la tarea es llevarlo
  bien, no reinterpretarlo.
- **Costo:** tres desvíos por el piso de accesibilidad: controles de 48 px (el prototipo usa 44),
  borde de los campos con contraste 3:1 y verde oscuro para textos verdes sobre fondo claro.
  Quedan para F4 el armazón de las pantallas privadas (barra lateral fija, buscador, campana y
  avatar) y la foto real de las tarjetas (la del prototipo no tiene licencia clara). Usa
  degradados, vidrio y números grandes, que la dirección de arte marca como genéricos: es decisión
  del usuario. Las capturas de la primera vuelta no se guardaron.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/direccion-general/`.

### 2026-10-30 · identidad · Brief aprobado

- **Decisión:** [brief de identidad](identidad/brief.md): cercana, confiable y viva con un toque
  alegre; números de impacto como protagonistas; voseo cercano; formas mixtas.
- **Alternativas:** las opciones de las tres rondas de preguntas.
- **Por qué:** respuestas del usuario.
- **Costo:** ninguno.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · El prototipo v2 reemplaza a los bocetos de E1 como punto de partida

- **Decisión:** el punto de partida de la interfaz es el prototipo v2 de la diseñadora (Figma
  "Prototipo v2", [demo navegable](https://sofiadeane.github.io/frontend-donatrack/) y repositorio
  `sofiadeane/frontend-donatrack`). Es punto de partida, no molde: la primera comparación A/B lo
  enfrenta con una versión que lo lleva más lejos.
- **Alternativas:** tomarlo como identidad cerrada y comparar solo lo que dejó abierto; usarlo
  como una referencia más y armar dos direcciones nuevas.
- **Por qué:** los bocetos de E1 quedaron desactualizados. El prototipo ya tiene paleta, letra,
  un motivo propio (la ruta punteada) y pantallas del público y del donante.
- **Costo:** el prototipo no cubre todavía las pantallas de la entidad ni del admin. Los bocetos
  de E1 siguen sirviendo para esas pantallas, como referencia de contenido.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Flujo piloto: confirmar recepción con fotos

- **Decisión:** el proceso se prueba de punta a punta con el flujo "Confirmar recepción con fotos"
  de la entidad (RF-12, boceto pág. 28).
- **Alternativas:** registrar donación (admin), registro de cuentas e importar CSV.
- **Por qué:** es el que más exige a la interfaz (celular, cámara, carga de archivos, estados,
  avisos). Además es el momento con más valor emocional del sistema, y su decisión técnica (D27)
  ya está tomada.
- **Costo:** la integración real con Logística (subida multipart, HB-10) queda para después. El
  piloto usa datos de ejemplo.
- **Quién decidió:** equipo, al aprobar el plan.

### 2026-10-30 · proceso · Anchos de referencia: 360, 1280 y 1920 px

- **Decisión:** se diseña, se captura y se compara en 360 (celular), 1280 (escritorio) y 1920
  (pantalla grande).
- **Alternativas:** 360, 768 y 1280, como proponía RNF-UI-07 en su forma de verificar.
- **Por qué:** cubre desde el celular hasta las pantallas grandes, que hoy son comunes. La tableta
  sigue cubierta por una comprobación automática de anchos intermedios.
- **Costo:** ninguna captura fija de tableta para revisar a ojo.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Los bocetos de E1 son punto de partida, no molde

- **Decisión:** los bocetos de E1 orientan estructura y contenido, y se pueden mejorar. Cada
  cambio importante se anota acá.
- **Alternativas:** implementarlos tal cual (lectura literal de RQ-E6-06).
- **Por qué:** la cátedra no es estricta con eso y los bocetos tienen problemas de uso en celular
  (tablas anchas, carrusel, etiquetas dentro de los campos).
- **Costo:** hay que justificar cada desvío en el entregable.
- **Quién decidió:** usuario.
