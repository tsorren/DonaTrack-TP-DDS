---
name: iteracion-ab
description: >-
  Compara dos variantes (A y B) de un solo tema de diseño del cliente liviano (dirección general,
  paleta, tipografía, forma, densidad, layout de una pantalla, componente o textos), las comprueba,
  las muestra con capturas y deja que el usuario elija. Usar cuando una decisión de gusto o de forma
  tiene más de una buena respuesta, o cuando el usuario pide "probemos otra opción".
---

# Skill: iteracion-ab — Una comparación A/B por tema

> **Ámbito:** cliente liviano (E6).
> **Alineación:** [`docs/interfaz/README.md`](../../../docs/interfaz/README.md),
> [`temas-ab.md`](../../../docs/interfaz/temas-ab.md),
> [`plantillas/iteracion-ab.md`](../../../docs/interfaz/plantillas/iteracion-ab.md),
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md) y el
> [brief](../../../docs/interfaz/identidad/brief.md) (sobre todo, su intensidad).
> **Criterios de la cátedra:** [`criterios-catedra-ux-ui.md`](../../../docs/interfaz/criterios-catedra-ux-ui.md).

## Dos modos

| | Exploración | Ajuste |
|---|---|---|
| Cuándo | Se busca un aspecto que todavía no existe: `direccion-general`, `concepto`, `paleta`, `color-y-acabado`, `estilo`, `tipografia`, `forma`, `imagen` | Se retoca algo ya elegido ("probemos otro verde"), `densidad`, `microcopy`, `componente` o `layout` puntual |
| Qué tan lejos | **Lejos.** Nunca "cambio mínimo" en la idea; la intensidad la fija el brief | Cerca: el cambio que resuelve el retoque |
| Vueltas | En **boceto** (paso 4) | Con la comprobación completa |
| Lo que evitamos | Aviso: se puede proponer y se revisa al aplicar la elegida | Regla |

Si el pedido no dice cuál, es exploración cuando el tema todavía no tiene una decisión en la
bitácora; si no, ajuste.

**En exploración, lo simple es el código, nunca la idea.** Las reglas de superficie mínima,
minimalismo suficiente y la skill ponytail se aplican a cómo se escribe la variante, no a cuánto se
aleja de lo actual.

## Reglas

- **Un tema por iteración.** A y B difieren solo en ese tema; todo lo demás, igual.
- **Eje de divergencia (exploración).** Antes de construir, cada opción declara en qué se separa de
  las otras, en la sección 2 de la plantilla. En `paleta`, el eje es al menos uno de: qué color
  domina, claro u oscuro, temperatura o saturación; cambiar solo el tono de cada color en su mismo
  rol no cuenta. Si dos vueltas de `paleta` dan opciones parecidas, se pasa a `color-y-acabado`.
  Desde la segunda vuelta, al menos una opción sale de la familia que el usuario
  eligió en la anterior. Si dos opciones se ven parecidas lado a lado, se rehace una antes de
  mostrarlas.
- **Patrones conocidos.** En navegación, controles e interacción, cada variante sigue un patrón de
  los [criterios de la cátedra](../../../docs/interfaz/criterios-catedra-ux-ui.md) (secciones 4 y 5)
  y lo nombra. No se compara un patrón conocido contra uno inventado. Si un patrón es el estándar
  para el caso (por ejemplo, la barra inferior para 3 a 5 destinos en el celular), queda fijo y la
  comparación va sobre lo que sí admite más de una buena respuesta.
- **Con el tema `concepto`, de 2 a 4 conceptos** (`concepto-a`, `-b`, `-c`…), cada uno una variante
  de estructura que **difiere en composición y navegación**, no solo en estilo. Se encargan con la
  [plantilla del constructor](../../../docs/interfaz/plantillas/encargo-constructor.md) y se
  critican con el panel de exploración de `revision-ui`. Para hacerlo sin el usuario, con
  iteraciones que escalan: [`corrida-creativa`](../corrida-creativa/SKILL.md).
- Si hay boceto de E1 para la pantalla, puede ser una de las variantes (la de control). No es
  obligatorio.
- **El piso** de la [dirección de arte](../../../docs/interfaz/direccion-de-arte.md): en ajuste, las
  dos variantes lo pasan antes de mostrarse. En exploración, las vueltas en boceto se muestran sin
  comprobarlo (el agente igual evita lo que se ve roto a simple vista), y la elegida lo pasa antes
  de aplicarse; si falla, el arreglo conserva la idea.
- Al usuario le llegan solo la imagen comparativa y 2 o 3 líneas simples por variante. Las tablas
  y los criterios quedan en el trabajo del agente.
- Tope: 3 vueltas por tema. Si no converge, hacer **una** pregunta para entender qué falta.

## Pasos

1. **Tema y modo.** Elegir el tema del [catálogo](../../../docs/interfaz/temas-ab.md); si no está,
   agregar la fila primero. Decidir el modo con la tabla de arriba.
2. **Antes de construir.** Completar las secciones 1 y 2 de la plantilla: qué se decide, qué queda
   fijo, de 2 a 4 criterios y, en exploración, el eje de cada opción.
3. **Construir A y B** en el perfil `maqueta`:
   - Cambios de estilo: `cliente-liviano/src/main/resources/static/css/variantes/<tema>-a.css` y
     `<tema>-b.css`. Redefinen valores base y a qué uso va cada uno (tokens de uso) o reglas de
     `app.css` y de los componentes, nunca colores sueltos.
   - Si el eje pide mover un rol que no tiene token propio (como pasó con la sombra, que las
     hojas tomaban de `--color-hoja` hasta que se creó `--color-sombra`), la variante **crea el
     token de uso** y redefine en su hoja las reglas que lo usan. `InterfazFitnessTest` ya lo permite. Al aplicar la
     elegida, el token pasa a `tokens.css`, las hojas lo usan y, si es un par de texto y fondo, se
     suma a la lista de contrastes del test.
   - Cambios de estructura: `templates/variantes/<tema>-a.html` y `<tema>-b.html`.
     Una variante de estructura **reemplaza la pantalla entera** (`/<pantalla>?variante=…`), con
     el modelo de esa pantalla.
   - En ajuste, preferir variantes solo de estilo: no se duplica HTML. En exploración, si el eje
     lo pide, se cambia la estructura.
   - Se ven con `?variante=<tema>-a` o `?variante=<tema>-b`.
   - Cada vuelta usa letras nuevas (`<tema>-c`, `-d`…), para no pisar en `target/capturas/` las
     capturas de la anterior, que se copian a la evidencia al cerrar.
   - En el nombre del archivo, el `:` del tema se escribe `-` (`layout:entregas` →
     `layout-entregas-a.css`): solo letras minúsculas, números y guiones.
   - Lo que comparten varias variantes (un marco, una lista) va en `variantes/comun/`, que no
     cuenta como variante: las plantillas lo usan como fragmento y el CSS con `@import`.
4. **Capturar.**
   - **Boceto** (vueltas de exploración): solo capturas a 1280 px y el visor, sin el piso ni los
     otros tests de la clase:

     ```bash
     mvn test -pl cliente-liviano -Pvisual -Dspotless.check.skip=true -Dtest=VerificacionVisualTest#paginasCumplenElPisoEnTodosLosAnchos -Dverificacion.boceto=true -Dverificacion.variantes=<tema>-a,<tema>-b -Dverificacion.paginas=<ruta>
     ```

   - **Completa** (ajuste, y la elegida de una exploración antes de aplicarla):

     ```bash
     mvn test -pl cliente-liviano -Pvisual -Dverificacion.variantes=<tema>-a,<tema>-b -Dverificacion.paginas=<ruta>
     ```

   `-Dverificacion.paginas` acota la corrida a las rutas que se comparan (separadas por coma; sin
   la barra inicial en Git Bash) sin tocar `paginas.txt`. La completa deja capturas en 360, 1280 y
   1920 px, la primera pantalla del celular (`-360x640.png`), el reporte de accesibilidad y
   `target/capturas/comparacion.html`: un visor para 1920 × 1080 con índice de secciones, dos
   variantes lado a lado, cambio de ancho y desplazamiento a la par. El primer comentario del CSS
   de cada variante es su descripción en el visor. Con más de dos opciones, se nombran
   `<tema>-a`, `-b`, `-c`… y el visor las recorre con las flechas. Con varios temas a la vez
   (decisiones en tanda), se pasan todas en una sola corrida y un enlace
   `comparacion.html#izq=<variante>&der=<variante>&ancho=<px>` abre cada comparación.
5. **Autocrítica.** Mirar las capturas con las preguntas de la dirección de arte, completar la
   tabla de criterios y elegir una recomendación. En exploración, además: si dos opciones no
   se distinguen a simple vista, o alguna es tímida frente a la intensidad del brief, se rehace.
6. **Mostrar al usuario:**
   - Mandar la comparación. En la app de escritorio, con la herramienta para enviar archivos; en
     la terminal, la ruta de `comparacion.html`.
   - Debajo, "A — <nombre>" y "B — <nombre>", con 2 o 3 líneas cada una.
   - Preguntar con `AskUserQuestion`: "¿Con cuál seguimos?" → A (Recomendada) · B · "Una mezcla:
     decime qué tomo de cada una" · "Ninguna, probemos otra cosa".
   - Si sigue otra vuelta, volver al paso 2. Entre vueltas no se escribe evidencia ni bitácora.
7. **Cerrar**, una sola vez, con la elegida:
   - En exploración, correr la comprobación completa sobre la elegida y arreglar lo que falle sin
     perder la idea.
   - Aplicar la elegida (en `tokens.css`, `app.css`, la hoja de la pantalla o la plantilla real).
   - Borrar las variantes de `variantes/`.
   - Copiar a `docs/interfaz/evidencia/<tema>/` las capturas de 1280 px de las opciones de cada
     vuelta y las de 360 y 1280 px de la elegida (menos de 400 KB cada una, con
     `java scripts/visual/Imagenes.java jpg`; ver
     [`scripts/visual/README.md`](../../../scripts/visual/README.md)).
   - Escribir la entrada en la [bitácora](../../../docs/interfaz/bitacora.md): Decisión /
     Alternativas / Por qué / Costo / Quién decidió / Evidencia, con el formato corto de las
     decisiones A/B (15 líneas o menos, una línea por vuelta).

## Criterio de salida

- [ ] Variante elegida aplicada, con la comprobación completa en verde, y variantes borradas.
- [ ] Tests en verde (`mvn clean test -pl cliente-liviano -am`).
- [ ] Bitácora y evidencia actualizadas.
