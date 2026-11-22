# Criterios de aceptación: Artifact «Guía de estilo y pantallas» (fase 3, 2026-11-20)

> Actualizado el 2026-11-20 al sumar la sexta pantalla, el inicio de quien dona: cambian los
> criterios 4, 5 y 11, y se agregan el 17 y el 18.
> Actualizado el 2026-11-21 al sumar el inicio de la organización y la recepción confirmada: cambian
> los criterios 4, 5, 11 y 12, y se agregan el 19, el 20 y el 21. Lo demás rige igual para las ocho
> pantallas.
> Actualizado el 2026-11-21 (segunda vez) al sumar la tabla de donaciones de quien dona, el detalle de
> la donación con problema y las entregas de la organización: cambian los criterios 4, 5, 7, 11 y 12,
> y se agregan del 22 al 25. Lo demás rige igual para las once pantallas.
> Actualizado el 2026-11-22 (versión 10) al sumar la donación asignada, la entrega con problema vista
> por la organización y Necesidades después de confirmar: cambian los criterios 4, 5, 11, 12 y 25, y se
> agregan del 26 al 29. Lo demás rige igual para las trece pantallas.

Encargo: guía de estilo y trece pantallas en alta fidelidad del cliente liviano de DonaTrack, sobre
la mezcla C, «Huerta y galpón» (un esqueleto por rol: aireado y cálido para lo público, quien dona
y la organización; denso para el admin).

El artefacto es `guia-y-pantallas.html`, armado por `armar.js` a partir de `partes/`. Fuentes de
verdad para contrastar: `docs/interfaz/criterios-catedra-ux-ui.md` (manda), `direccion-de-arte.md`
(el piso), `tono-y-comunicacion.md` y `cliente-liviano/src/main/resources/static/css/tokens.css`.

## Criterios

1. **Tokens en un solo bloque.** Los tokens del producto son propiedades CSS en un solo bloque
   (`<style id="tokens">`, una regla `:root`): color, escala de letra, espaciado, radio, elevación
   y movimiento. Conservan los nombres de `tokens.css` donde ya existían; los nuevos usan los
   prefijos documentados (`--color-`, `--fuente-`, `--tamano-`, `--espacio-`, `--radio-`,
   `--borde-`, `--sombra-`, `--movimiento-`, `--capa-`, `--medida-`). La guía dice cuáles son
   nuevos y cuáles cambian. Los colores de la lámina de presentación van en un bloque aparte y
   declarado.
2. **Sin colores literales fuera de los bloques de tokens**, ni en atributos `style` ni en SVG
   (evidencia: la comprobación de `armar.js`).
3. **Componentes con todos sus estados:** botones, campos, listas y tablas, tarjetas, chapas de
   estado, notificaciones y navegación. Estados: normal, al pasar el cursor, con foco, activo,
   deshabilitado y con error (donde aplica); vacío, cargando y error en las listas.
4. **Trece pantallas** en alta fidelidad: Portada (público), Necesidades con sus dos estados
   (organización), Bandeja (admin), Inicio de quien dona (donante), Donaciones de quien dona, en tabla
   (donante), Donación asignada (donante), Seguimiento de una donación (donante), Donación con
   problema (donante), Inicio de la organización con sus dos estados (organización), Entregas de la
   organización con sus dos estados (organización), Entrega con problema, a sus dos horas
   (organización), Confirmar recepción con su estado de error (organización) y Recepción confirmada
   (organización). La página dice por qué estas.
5. **Navegables entre sí:** se llega de una a otra por la lámina y por los enlaces reales del
   producto (Necesidades → Confirmar recepción → su error → volver; Inicio de quien dona →
   «Seguir esta donación» → Seguimiento → «Inicio» → Inicio de quien dona; Inicio de la
   organización → Confirmar recepción → su error → Recepción confirmada → «Volver al inicio» →
   Inicio de la organización, después de confirmar; Inicio de quien dona → «Ver las 6 en una tabla» →
   Donaciones → la fila 0041 → Seguimiento, y la fila 0040 → Donación con problema; el aviso del
   inicio de quien dona → Donación con problema; «Entregas», desde las cuatro pantallas de la
   organización → Entregas, en el estado que corresponde a la hora de la pantalla de origen; Entregas
   → Confirmar recepción; la fila 0043 de Donaciones y de «También en curso» → Donación asignada →
   migas → Donaciones; el aviso del aceite del inicio de la organización y «Ver el detalle» de
   Entregas → Entrega con problema, en la hora de la pantalla de origen → migas → Entregas, en esa
   misma hora; «Necesidades», desde cada pantalla de la organización → Necesidades, en el estado de la
   hora de la pantalla de origen). Los enlaces a pantallas que no están entre las trece lo dicen. Cada pantalla nueva está en la barra de pantallas, tiene su ficha y sus enlaces
   «Anterior» y «Siguiente». Sin JavaScript se llega a cada estado por un enlace de la ficha.
6. **360 y 1280 px sin desplazamiento horizontal**, y sin desbordes a 320 y 1920 px (evidencia:
   `verificacion.txt`, salida de `comprobar.js`).
7. **Detalles gráficos de C:** D1 (sello) en huerta y galpón; D2 (raíz), D3 (etiqueta), D4
   (frutos), D5 (trama) y D7 (sombra de paja) solo en la huerta; D6 (folio) no se usa. Cada uno
   con un solo significado; D7, como máximo una vez por pantalla; un fruto es una entrega
   confirmada y se puede contar contra una cifra con su período. D2 suma un estado de paso, «no pudo
   llegar», con signo y texto además del color; no cambia de significado.
8. **Análisis de la portada:** el Artifact decide, con argumentos escritos, si la pieza grande de
   la portada es la foto de una entrega o una ilustración propia; dice qué reemplaza de la mezcla C
   y dónde quedan D3, D4, D5 y D7 en esa pantalla. En el resto, la foto de la entrega es una escena
   dibujada con la paleta de Raíces, rotulada «foto de ejemplo» y con texto alternativo. Dibujos
   nuevos: no se copian los SVG del repo (sin licencia documentada).
9. **Piso de accesibilidad comprobado** (WCAG 2.2 AA, principios POUR, Ley 26.653): controles de
   48×48 px (los enlaces dentro de una oración quedan exceptuados, como en WCAG 2.5.8); foco
   visible y nunca tapado; el estado nunca solo por color; contraste 4,5:1 en texto y 3:1 en
   partes de la interfaz, calculado desde los tokens; movimiento reducido respetado; lo esencial
   funciona sin JavaScript (enlaces y formularios reales; la maqueta misma se recorre sin
   JavaScript).
10. **Criterios de la cátedra (§11):** cada navegación y control nombra el patrón conocido que
    sigue (§4 y §5); largo de línea del texto corrido entre 50 y 75 caracteres en pantalla grande,
    medido; reparto 60-30-10 dicho para la huerta y para el galpón; textos alineados a la
    izquierda.
11. **Piso heredado de la fase 2:** como máximo una acción llena por pantalla; título y acción
    llena dentro de los primeros 640 px del celular (salvo el formulario y su error) y por encima
    del menú fijo, que a 360 × 640 empieza a los 576 px; secciones del
    admin a la vista en escritorio; rótulos de la barra inferior sin partir. Rige también para el
    inicio de la organización y para Entregas, con la entrega en la puerta. Donaciones, Donación
    asignada, Donación con problema y Entrega con problema no tienen acción llena: son pantallas de
    consulta, como Seguimiento. Los estados posteriores a confirmar (inicio, Entregas y Necesidades) y
    Recepción confirmada no tienen acción llena: no queda nada pendiente.
12. **Datos:** sin lorem ipsum, solo datos sintéticos, en español rioplatense (voseo), sin
    contradicciones entre pantallas ni con `DATOS-FASE3.md`. Un solo «hoy»: jueves 15 de octubre de
    2026. Cada pantalla tiene su hora (`DATOS-FASE3.md`, «Reloj de cada pantalla»); las de quien dona, a las 16:25; las de la
    organización están dibujadas a las 18:30, a las 18:45 y a las 18:50 (el inicio, Entregas,
    Necesidades y la entrega con problema, a las 18:30 y a las 18:50), y un enlace entre pantallas de
    la organización abre el estado de la hora de la pantalla de origen: desde una pantalla posterior a
    la confirmación no se llega a una que todavía pide confirmar. Ya no hay enlaces que avisen por
    cruzar de hora. Las dos deudas de la fase 2 quedan resueltas (Aceite de girasol; «Espera tu confirmación»
    con chapa propia).
13. **Tono** (`tono-y-comunicacion.md`): los estados dicen lo que pasó y lo pendiente se dice
    pendiente; cada cifra con período y fuente; se agradece solo un hecho confirmado; quien recibe
    es «organización» y la empresa que dona no; errores con qué pasó y cómo seguir.
14. **Lo que se evita** (`direccion-de-arte.md` 52-61 y la skill frontend-design): sin rótulos en
    mayúsculas sobre cada título, sin una frase del título en otro color, sin separadores de punto
    medio como recurso general, sin flechas en los enlaces, sin tarjetas idénticas con sombra gris,
    sin emojis.
15. **Tema:** el producto va en claro (no tiene tema oscuro); la lámina sigue el tema de quien la
    mira, y los controles nativos dentro del producto no se oscurecen.
16. **Alcance:** no se tocó `cliente-liviano/` ni `docs/`; las únicas escrituras son el Artifact,
    sus fuentes en la carpeta temporal y `.claude/handoff-fable-auditoria-ui.md`.
17. **Inicio de quien dona.** Esqueleto de la huerta; mira Distribuidora Ejemplo SA a las 16:25 del
    jueves 15 de octubre de 2026. Tiene el saludo con el nombre de la cuenta, el aviso de la
    donación con problema, la lámina protagonista con el mapa, las entregas confirmadas con su foto
    y la lista «También en curso» con el enlace a la tabla. Reusa los componentes de la guía; lo
    que es pieza o uso nuevo está en la guía con sus estados. La página de decisiones deja escritas
    dos cosas: (a) qué donación es la protagonista, dicho frente a la regla anterior (primero la
    que tiene un problema; si no, la que está en camino y llega antes; si no, la última que cambió
    de estado), con el argumento y el costo si se aparta de ella; (b) el destino de la donación
    0043, definido en `DATOS-FASE3.md` de modo que las seis pantallas no se contradigan.
18. **El producto se dibuja solo con sus tokens.** Dentro de `.dt`, lo que se dibuja no depende de
    la hoja de la lámina ni del tema de quien mira: títulos, enlaces y aro de foco salen de las
    hojas del producto (evidencia: `verificacion.txt`, sección 9). La versión 6 no lo cumplía: las
    siete reglas base de `partes/02-lamina.css` alcanzaban a las pantallas. La corrección toca las
    cinco pantallas ya auditadas y está declarada en la página (`#correccion-titulos`).

19. **Inicio de la organización.** Esqueleto de la huerta; mira Comedor Barrio Ejemplo. Una sola
    pantalla con dos estados en un solo bloque de marcado (lo que cambia de un estado al otro
    lleva la clase `solo-puerta` o `solo-despues`; la pantalla no está copiada dos veces): a las 18:30, con una entrega en la puerta (la
    lámina protagonista lleva la única acción llena, «Confirmar recepción»); a las 18:50, después
    de confirmar (sin sombra de paja y sin acción llena). Tiene el saludo con el nombre de la
    cuenta, el aviso de la entrega con problema, la lámina de la puerta con su dibujo, «Lo que
    viene» y «Tus necesidades». Aplica la misma regla que el inicio de quien dona: el problema va
    arriba de todo, en un aviso, y nunca también en la lámina protagonista. La página de
    decisiones dice por qué son dos estados y qué cuesta ese orden.
20. **Recepción confirmada.** Página propia, a las 18:45: dice qué se confirmó y cuándo, qué quedó
    asentado (remito, quién recibió, cuándo, cuántas fotos), a quién se avisó, cómo queda la
    necesidad y qué sigue. La raíz muestra los 5 pasos cumplidos, con su fecha. No nombra un
    efecto que no se puede probar. Se llega desde el formulario (segundo envío) y, sin
    JavaScript, por un enlace de la ficha de Confirmar recepción.
21. **El dibujo de la puerta y su movimiento.** Dibujo nuevo, sin trazos copiados del repo. Sus
    frutos son las entregas que confirmó la organización en el mes y se pueden contar contra la
    cifra del pie, que dice período y fuente: 4 antes de confirmar y 5 después. El último tramo de
    la raíz se traza una sola vez, al llegar desde el formulario; el estado de base es el dibujo
    completo; con movimiento reducido no hay trazo. Usa tokens de movimiento que ya existían. La
    página lo declara donde antes decía que ninguna sección entra animada.
22. **Donaciones de quien dona, en tabla.** Esqueleto de la huerta; mira Distribuidora Ejemplo SA a
    las 16:25. Las 6 donaciones de octubre en una tabla real (`<table>`, con `<caption>` y `scope`)
    a 1280 px. En el celular la misma tabla se apila: cada fila es un bloque y cada dato lleva su
    rótulo a la vista; no se esconde ninguna columna ni hay desplazamiento horizontal. Cada fila
    nombra la donación, para quién es, su estado (sello y raíz, con el paso en texto) y el último
    movimiento con su fecha. El nombre de la donación es el enlace al detalle, de 48 px de alto: la
    0041 lleva a Seguimiento y la 0040 a Donación con problema; las demás avisan. Pestañas por
    estado con su cuenta. Sin acción llena. La cuenta del pie coincide con el inicio de quien dona.
    La pieza «tabla que se apila» está en la guía, ancha y angosta, con sus estados. La página dice
    por qué esta tabla se apila y la del admin no.
23. **Donación con problema (0040).** Esqueleto de la huerta; la plantilla de Seguimiento sin el mapa
    (migas, cabecera con sello, recorrido paso a paso, etiqueta que espera la foto, línea de
    privacidad). Dice qué pasó, qué se sabe y el próximo paso, con fecha: no da a entender que todo
    sigue normal ni promete una hora de llegada. El problema va una sola vez como aviso; no hay
    lámina protagonista ni sombra de paja ni acción llena (quien dona no tiene nada que hacer, y la
    pantalla lo dice). La raíz muestra el paso 4 como «no pudo llegar», con signo y texto. Se llega
    desde el aviso del inicio de quien dona y desde la fila 0040 de la tabla.
24. **Entregas de la organización.** Esqueleto de la huerta; mira Comedor Barrio Ejemplo. Una pantalla
    con dos estados en un solo bloque de marcado, como el inicio: a las 18:30 (la entrega que espera
    confirmación va primero, en la lámina protagonista, con la única acción llena) y a las 18:50
    (sin lámina protagonista ni acción llena; la entrega pasa a las confirmadas y los frutos van de
    4 a 5). Orden: por confirmar, con problema, por salir, confirmadas del mes; es el orden que usa
    hoy el producto. Las confirmadas llevan su etiqueta con foto; dos entregas no muestran la misma
    foto. Los frutos se cuentan contra su cifra, con período y hora. «Entregas» deja de avisar
    «fuera» en Necesidades, el inicio, Confirmar recepción y Recepción confirmada, y cada una abre
    el estado de su hora, también sin JavaScript.
25. **A dónde llevan los avisos (reemplaza el desvío declarado de la versión 9).** El aviso del
    inicio de quien dona lleva al detalle de la 0040. El aviso del inicio de la organización es de
    otra entrega (Aceite de girasol) y, desde la versión 10, lleva a su detalle, «Entrega con
    problema», en la hora del inicio (decisión del usuario, 2026-11-22). La página lo dice.
26. **Donación asignada (0043).** Esqueleto de la huerta; mira Distribuidora Ejemplo SA a las 16:25.
    La plantilla de Seguimiento sin el mapa y sin aviso: migas, cabecera con sello, «Qué sigue»,
    recorrido paso a paso, etiqueta que espera la foto y línea de privacidad. Dice lo que pasó y lo
    que falta como pendiente: la raíz tiene un paso cumplido, uno actual y tres que faltan, cada uno
    con su texto. No promete hora de salida ni de llegada. Muestra la necesidad que cubre, con los
    mismos números que Necesidades y con su fuente y su hora. Sin lámina protagonista, sin sombra de
    paja y sin acción llena. Se llega desde la fila 0043 de la tabla y desde «También en curso».
27. **Entrega con problema (aceite de girasol), vista por la organización.** Esqueleto de la huerta;
    mira Comedor Barrio Ejemplo. La plantilla de Donación con problema, con la voz de quien recibe:
    el porqué del problema va una sola vez, en un aviso (el estado lo nombran además el sello y la raíz); dice qué pasó, qué se sabe y el próximo paso con fecha;
    dice qué le toca a la organización (nada por ahora; confirmar cuando llegue) y cómo queda su
    necesidad frente a la fecha del pedido. La raíz muestra el paso 4 como «no pudo llegar», con
    signo y texto. Sin contacto, sin lámina protagonista, sin sombra de paja y sin acción llena
    (decisión del usuario, 2026-11-22). Un solo bloque de marcado con dos horas (18:30 y 18:50): lo
    único que cambia son la hora del pie y los enlaces de vuelta, que abren el estado de esa hora.
28. **Necesidades, después de confirmar.** Necesidades pasa a tener dos estados en un solo bloque de
    marcado, con el mismo mecanismo que el inicio y Entregas. A las 18:50 no hay lámina protagonista
    ni acción llena; la necesidad de lentejas dice 25 de 40 kg y qué parte se confirmó hoy, sin chapa;
    las otras tres no cambian. El estado de las 18:30 se dibuja igual que en la versión 9. Los
    enlaces a Necesidades que antes avisaban (desde Recepción confirmada, el inicio de las 18:50 y
    Entregas de las 18:50) ahora navegan al estado de las 18:50, también sin JavaScript.
29. **Las once pantallas anteriores no cambian salvo en sus enlaces** y en lo que este documento
    nombra: las filas 0043 dejan de avisar; el aviso y «Ver el detalle» del aceite llevan a su
    detalle; los enlaces a Necesidades abren el estado de su hora. Evidencia: comparación elemento por
    elemento con la versión 9 (`_once-iguales.txt`).

## Notas del Generator para la auditoría

- La maqueta dibuja cada pantalla dentro de un marco: las reglas de ancho son consultas de
  contenedor (`@container`), que en el producto pasan a `@media` con los mismos valores
  (`--medida-quiebre-*`). Está dicho en la guía.
- El estado posterior a confirmar se dibuja desde la versión 8, a pedido del usuario (2026-11-21).
  Las cifras que cambia (la necesidad de lentejas, los frutos del comedor) quedan resueltas con el
  reloj de cada pantalla: Necesidades y Confirmar recepción siguen a las 18:30 y no se tocaron.
- Al sumar la sexta pantalla se corrigió una fuga de estilos de la lámina hacia el producto
  (criterio 18). Eso cambió cómo se ven los títulos de las cinco pantallas anteriores y obligó a dos
  ajustes: las columnas de la portada (13 a 11) y el tamaño del título de cada paso de «Cómo
  funciona». `_02-lamina-antes.css` es la hoja anterior, para comparar.
- En la versión 9 se tocan pantallas ya auditadas solo en sus enlaces: «Donaciones» y el aviso, en el
  inicio de quien dona; «Donaciones» y las migas, en Seguimiento; «Entregas», en las cuatro de la
  organización, y el aviso del aceite en el inicio. No cambia nada más de esas pantallas.
- En la versión 10 la lámina cambia de «once» a «trece» pantallas y suma la sección
  `#decisiones-cierre`. El guion deja de tener el aviso de cruce de hora (`data-antes`), que ya no
  hace falta.
- Decisiones propias de esta fase (están en la guía, sección «Decisiones»): la pieza grande de la
  portada, la forma (qué es redondeado y qué es recto), el retiro de la sombra granulada, los
  estados previos al viaje con un mismo par de color y un signo que se llena.
