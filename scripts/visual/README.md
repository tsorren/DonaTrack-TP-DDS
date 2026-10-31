# Herramientas de imagen para la verificación visual

Un solo archivo de Java 21, sin dependencias: `Imagenes.java`. Trabaja sobre las capturas que deja
la verificación visual del cliente liviano (`cliente-liviano/target/capturas/`, ver
[`docs/interfaz/README.md`](../../docs/interfaz/README.md)). Lo usan las skills de interfaz, en
especial [`corrida-creativa`](../../.agents/skills/corrida-creativa/SKILL.md).

```bash
java scripts/visual/Imagenes.java <orden> <argumentos>
```

| Orden | Para qué | Ejemplo |
|---|---|---|
| `jpg` | Pasar capturas a JPG de 400 KB o menos (la evidencia de `docs/interfaz/evidencia/`). Con una carpeta, convierte todos los PNG que cumplen el glob | `jpg cliente-liviano/target/capturas docs/interfaz/evidencia/cozy-medieval/concepto-portada 400 "*--concepto-*"` |
| `comparar` | Comprobar que una alternativa no cambió: compara píxel a píxel los PNG que se llaman igual y dice la caja de cada diferencia. Sin glob, solo los de Glassmorphism (sin `--` en el nombre). Con una carpeta de ruido (otra corrida de la misma base), no cuenta lo que ya cambia entre antes y ruido. Sale con 1 si algo cambió o falta, o si el glob no encuentra ninguna captura | `comparar <carpeta-antes> cliente-liviano/target/capturas "*.png" <carpeta-ruido>` |
| `recorte` | Una zona de una captura, opcionalmente escalada (para mirar de cerca o para un epígrafe) | `recorte a.png zona.png 0 0 1280 900 0.5` |
| `lado` | La misma zona de dos capturas, lado a lado y con rótulos | `lado antes.png despues.png lado.png 0 0 1280 900 0.5 "Antes" "Después"` |
| `tamano` | Ancho × alto de una o varias imágenes | `tamano a.png b.png` |
| `resumen` | La página de antes y después de una corrida: un HTML de un solo archivo, con las imágenes adentro, a partir de un guion de texto | `resumen guion.txt resumen.html` |

El guion de `resumen` tiene una indicación por línea:

```text
# Título de la página
Un párrafo (cualquier línea que no empiece con #, ## o img).
## Una sección
img antes/portada-1280.png | Antes, 1280 px
img final/portada-1280.png | Después, 1280 px
```

Las líneas `img` seguidas van en una misma fila. Las rutas relativas salen de la carpeta del guion.

## Comprobar solo algunas páginas

La verificación visual acepta las rutas por propiedad, sin tocar `paginas.txt`. La barra inicial es
opcional; en Git Bash conviene omitirla, porque convierte `=/estilos` en una ruta de Windows:

```bash
mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.alternativa=cozy-medieval -Dverificacion.paginas=estilos/armazon,prototipo/publico-portada
```

Además de la página completa en 360, 1280 y 1920 px, deja la primera pantalla del celular
(`<página>-360x640.png`): lo que se ve sin desplazar.

## Comparar dos corridas sin falsas alarmas

Las capturas congelan las animaciones de la página, pero no las que van dentro de una imagen (el
spinner de Pico, en los botones `aria-busy`): dos corridas del mismo código pueden diferir en unos
cientos de píxeles. Para comprobar que algo no cambió, corré la verificación dos veces antes de
empezar (la primera es el antes y la segunda, el ruido) y pasá las dos a `comparar`. Las celdas de
16 px donde antes y ruido ya difieren, y sus vecinas, no cuentan; un cambio en cualquier otro lugar
sí.

```bash
java scripts/visual/Imagenes.java comparar <antes> cliente-liviano/target/capturas "{estilos,estilos-armazon}-[0-9]*.png" <ruido>
```

## Grabar un recorrido en video

`Recorrido.java` graba el sitio según un guion (Playwright, el mismo que usa la verificación
visual) y arma un MP4 de 1280×720, sin sonido, con ffmpeg. Cada orden del guion va en una línea:
`pantalla <ancho> <alto>`, `ir <ruta>`, `esperar <s>`, `cartel <s> | <título> | <texto>`,
`bajar <px> <s>`, `subir`, `clic <nombre del enlace o botón>` y `foco <selector CSS>`. Los tramos
del celular quedan centrados sobre fondo oscuro. El video queda fuera del repo.

Con el sitio levantado en el perfil `maqueta` y ffmpeg en el `PATH`:

```bash
M=~/.m2/repository; CP="$M/com/microsoft/playwright/playwright/1.63.0/playwright-1.63.0.jar;$M/com/microsoft/playwright/driver/1.63.0/driver-1.63.0.jar;$M/com/microsoft/playwright/driver-bundle/1.63.0/driver-bundle-1.63.0.jar;$M/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar"
java -cp "$CP" scripts/visual/Recorrido.java scripts/visual/guion-encastre.txt encastre.mp4 http://localhost:8090
```

En Linux o macOS, el separador del classpath es `:` en lugar de `;`. `guion-encastre.txt` es la
presentación de Encastre para el equipo: los cuatro roles y un tramo en el celular.
