# Encargo de construcción: @TITULO@

> Plantilla de la skill `corrida-creativa` (sale de los encargos que funcionaron en el rediseño de
> Cozy / Medieval, 2026-10-31). El director de cada iteración la copia a la carpeta de la corrida,
> reemplaza cada `@MARCA@`, borra las secciones de "Qué se construye" que no son de esta iteración y
> la deja autocontenida: quien la recibe no conoce la conversación ni el encargo creativo de memoria.
> Cada constructor recibe además, en su mensaje, **su parte**: su idea o su zona y sus archivos.

## Por qué existe este encargo

DonaTrack (TP de Diseño de Sistemas, UTN-FRBA) tiene un cliente web (`cliente-liviano`: Spring MVC +
Thymeleaf + HTMX + Pico.css + CSS propio, JavaScript propio solo como mejora) con varias identidades
visuales, cada una con su juego de plantillas, sus hojas y su voz. Esta corrida diseña
**@ALTERNATIVA_NOMBRE@** (`@ALTERNATIVA@`). Las demás identidades **no se tocan**.

Lo que pidió el usuario, en sus palabras (encargo completo: `@ENCARGO@`):

> @PEDIDO@

Referencias: `@REFERENCIAS@` (mirá varias imágenes y su README: qué se toma de cada una).

Lo que **NO hay que repetir** (mirá estas capturas antes de empezar): @ANTES@

Los errores que llevan a un resultado tímido, y que esta corrida no repite:

- encargos de "cambio mínimo" en una exploración;
- variantes que son dos versiones del mismo orden (la comparación nunca toca la composición);
- críticas que sacan lo más llamativo para cumplir una regla.

**Tu encargo es construir @QUE@ en serio, con audacia real.** @COMPARACION@ No es un retoque del
esqueleto actual: partí de cero en la composición. Mantené el código simple (lo más corto que
funcione), pero **nunca recortes la idea para simplificar**. Cada iteración es más ambiciosa que la
anterior: más oficio, más cuerpo, más identidad, sin perder seriedad.

## Dónde estamos

Iteración @ITERACION@: **@TIPO@**. @PREVIO@

Lo que los críticos le pidieron al ganador de la iteración anterior, conservando la idea:
@PIDEN@

## Qué se construye

### Concepto (iteración de una pantalla)

Una **variante de estructura** de `@ANCLA@` que reemplaza la página entera, con su navegación (en
escritorio y en el celular) y su voz propia en títulos y rótulos. Los conceptos de esta iteración
difieren en **composición y navegación**, no solo en estilo.

- `cliente-liviano/src/main/resources/templates/variantes/<variante>.html`: la página completa, con
  `<!DOCTYPE html>` propio. Puede usar las piezas de la alternativa: la cabeza
  (`~{alternativas/@ALTERNATIVA@/layout :: cabeza(~{::title})}`, que trae Pico, `tokens.css`,
  `app.css` y el CSS de la variante), los fragmentos de `templates/alternativas/@ALTERNATIVA@/fragmentos/`
  y la marca, o marcado propio si el concepto lo pide.
- `cliente-liviano/src/main/resources/static/css/variantes/<variante>.css`: tus estilos. **El primer
  comentario del archivo es la descripción que ve el usuario en el visor** (2 o 3 frases simples).
  El `app.css` de la alternativa se carga antes: podés apoyarte en él o pisarlo. Clases nuevas con
  el prefijo de tu concepto.
- Dibujos propios, si los necesitás: SVG en línea (decorativos con `aria-hidden="true"` y
  `focusable="false"`) o `static/img/@ALTERNATIVA@/<variante>-*.svg`. Hechos por vos, en el trazo
  de la alternativa; nada bajado de internet.
- El modelo de la página: @MODELO@.
- Se ve con `@ANCLA@?alternativa=@ALTERNATIVA@&variante=<variante>`.

### Sistema (el juego real de plantillas)

El ganador pasa al juego de plantillas de la alternativa
(`cliente-liviano/src/main/resources/templates/alternativas/@ALTERNATIVA@/`): navegación, layouts,
pantallas y componentes del catálogo `/estilos`.

- **Gemelas:** cada plantilla tiene su gemela en la otra identidad. No se crean plantillas ni
  fragmentos nuevos (no tendrían gemela): lo nuevo va dentro de las que existen.
- Tus estilos van en la hoja de tu zona (`static/css/alternativas/@ALTERNATIVA@/<zona>.css`, ya
  enlazada después de `app.css`). En `app.css` solo podés borrar reglas de tu zona; lo de afuera,
  si te molesta, se pisa en tu hoja y se anota en tu respuesta. Tokens nuevos: al final del
  `tokens.css` de la alternativa, en un bloque con el nombre de tu zona.
- Las variantes de concepto (`templates/variantes/concepto-*.html` y `static/css/variantes/concepto-*.css`)
  las borra solo la zona "estructura", en su commit.

### Flujo (el prototipo navegable)

Las pantallas del flujo completo, en `/prototipo/<pantalla>` (`PrototipoController`, perfil
`maqueta`): `templates/prototipo/@ALTERNATIVA@/<pantalla>.html`. El nombre empieza con el rol
(`publico-`, `donante-`, `entidad-` o `admin-`); una pantalla de nivel 3 lleva adelante el nombre de
su sección (`donante-donaciones-detalle`). Leé el controller para ver los menús de cada rol.

- Las privadas usan `~{alternativas/@ALTERNATIVA@/layout-privado :: pagina(~{::title}, ~{::contenido})}`;
  las públicas, `~{alternativas/@ALTERNATIVA@/layout :: pagina(...)}`.
- Estilos en `static/css/alternativas/@ALTERNATIVA@/prototipo/<grupo>.css`, enlazados desde tus
  plantillas. Solo tokens de uso: ni colores literales ni `var(--p-*)`.
- **Datos de ejemplo escritos en la plantilla**, sintéticos y coherentes con los del catálogo (Ana
  dona; Comedor Barrio Ejemplo, Merendero Ejemplo Sur y Escuela Ejemplo 12 son entidades; un solo
  calendario: @HOY@).
- Las pantallas se enlazan entre sí con enlaces reales; los formularios tienen `action` y `method`
  reales (`post` si envían datos, `get` en filtros y búsquedas), `required` en lo obligatorio y
  `autocomplete` en ingreso y registro.
- Cada pantalla con sus estados (vacío, error, éxito) cuando el flujo los pide, y una salida clara:
  sin callejones. Un aviso que viene escrito al cargar lleva `tabindex="-1"` y `autofocus`.

### Arreglos (la pasada después de la crítica)

Leé la lista de tu zona en `@ARREGLOS@`. Hacé cada arreglo **sin perder la idea ni la audacia**.
Si uno no se puede hacer sin traicionar la idea, no lo hagas y explicá por qué.

## Mismos datos y mismas acciones (obligatorio)

La pantalla muestra **exactamente los mismos datos y las mismas acciones** que @DATOS@. Podés
reordenarlos, partirlos y darles otra forma; no podés sacar ni agregar.

- Los controles (enlaces, botones, campos, `summary`) con el mismo nombre accesible y el mismo
  destino. No agregues controles: la voz y la composición son lo propio, no las acciones.
- Conservá los `id` a los que apuntan los enlaces, `main id="contenido" tabindex="-1"`, la región
  de avisos `aria-live` y un `h1` (puede ser `solo-lectores`), que va primero en el HTML de `main`.
- `InterfazFitnessTest` compara cada par de gemelas: si una expresión de datos o un control de la
  otra identidad falta, falla.

## Voz propia: títulos y rótulos

La alternativa habla a su modo **solo en títulos y rótulos**; datos, cifras y nombres de controles
no cambian. Tabla de voz: `@VOZ@` (formato en el archivo; cada línea en uso). Reglas: lenguaje llano
(el usuario es argentino: palabras de todos los días, sin jerga), el tono del encargo, un rótulo no
lleva números y nunca va dentro de un control.

## El piso (no se negocia; lo comprueba la verificación visual)

- Contraste AA (axe sin violaciones) y foco visible.
- Todo control visible de 48 × 48 px o más (los enlaces dentro de un `p` o un `small` no cuentan).
- Nada desbordado ni superpuesto entre 360 y 1920 px, también en los anchos intermedios.
- **Primera pantalla del celular (360 × 640):** se ven el título, la tarea y la acción principal.
- Lo esencial sin JavaScript; plegables con `details`/`summary`. Movimiento solo con
  `@media (prefers-reduced-motion: no-preference)`.
- `@media (forced-colors: active)`: bordes y signos pintados se siguen viendo.
- El color nunca es la única señal. Lo que dice un dibujo está también en texto: cada mapa con su
  lista al lado.
- Pensá el caso grande aunque la muestra traiga el chico: más secciones en el menú (el admin tiene
  7), nombres largos, más avisos.

## Reglas técnicas (las comprueba `InterfazFitnessTest`)

- Solo tokens de uso en `var()`: `--color-*`, `--fuente-*`, `--tamano-*`, `--espacio-*`,
  `--radio-*`, `--borde-*`, `--sombra-*`, `--movimiento-*`, `--capa-*`, `--medida-*` (los de
  `static/css/alternativas/@ALTERNATIVA@/tokens.css`). Una variante puede declarar valores base
  nuevos en líneas que empiezan con `--p-` y usarlos con `var(--p-…)`; ningún otro color literal.
  Nada de `--pico-*`.
- `@media` solo con `min-width` y los quiebres de `tokens.css` (mobile first). `@container`, libre.
- Sin atributos `style`, sin `<style>`, sin `th:style`. Texto con `th:text` o literal.
- HTML semántico: `header`, `nav`, `main`, `section` con título, listas, `th scope`.
- JavaScript propio: @JAVASCRIPT@

## Libertad y oficio

Leé "Libertad creativa" y "Oficio" en `docs/interfaz/direccion-de-arte.md`, y el brief de la
alternativa (`@BRIEF@`): materiales, signos y "lo que evitamos". Un concepto puede reinterpretar
los materiales si lo justifica.

## Cómo comprobar

1. Tu worktree es tuyo y está vacío de cambios: ponelo en la base con `git reset --hard @BASE@`.
2. Reglas: `mvn test -pl cliente-liviano -Dtest=InterfazFitnessTest`.
3. Piso y capturas (en segundo plano si tarda; las rutas van sin la barra inicial por Git Bash):

   ```bash
   mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.alternativa=@ALTERNATIVA@ -Dverificacion.paginas=@RUTAS@ @VARIANTES@
   ```

   Deja `cliente-liviano/target/capturas/<ruta>--@ALTERNATIVA@[--<variante>]-{360,1280,1920}.png`,
   la primera pantalla `-360x640.png` y `reporte.json`, que tiene que dar `[]`. No toques
   `paginas.txt`.
4. **Mirá tus capturas** (las cuatro de cada ruta) con la herramienta de lectura de imágenes y
   criticalas como un director de arte exigente: ¿podría ser la interfaz de cualquier otro producto?
   ¿Cuál es la audacia? ¿Qué se ve primero? ¿En 360 × 640 se ve la acción principal? ¿Algo es solo
   adorno? ¿Se lee bien? Iterá hasta que te guste de verdad: **al menos dos vueltas**.
5. Las barras fijas salen a mitad de la captura de página completa: es un efecto de la captura. Si
   dudás, abrí la página en el navegador con un puerto propio
   (`mvn -pl cliente-liviano spring-boot:run -Dspring-boot.run.profiles=maqueta -Dspring-boot.run.arguments=--server.port=<puerto libre>`)
   y frenalo al terminar.
6. Antes de editar una línea por número, leela: una edición de un solo comando con un número de
   línea vacío puede triplicar un archivo.

## Qué entregás

- Un commit en tu rama con **solo tus archivos**, sin push: `@COMMIT@`.
- Tu respuesta final, en el formato que te pidan: rama y SHA; la idea en tres frases; cómo navega en
  escritorio y en el celular; la voz que sumaste; qué pisaste fuera de tu zona; lo mejor y lo más
  flojo según tu propia crítica; el resultado de `reporte.json` y de `InterfazFitnessTest`.
