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
> [`temas-ab.md`](../../../docs/interfaz/temas-ab.md) y
> [`plantillas/iteracion-ab.md`](../../../docs/interfaz/plantillas/iteracion-ab.md).

## Reglas

- **Un tema por iteración.** A y B difieren solo en ese tema; todo lo demás, igual.
- Si hay boceto de E1 para la pantalla, puede ser una de las variantes (la de control). No es
  obligatorio.
- Las dos variantes pasan el piso de la [dirección de arte](../../../docs/interfaz/direccion-de-arte.md)
  antes de mostrarse. Una variante que falla se corrige; no se le muestra al usuario.
- Al usuario le llegan solo la imagen comparativa y 2 o 3 líneas simples por variante. Las tablas
  y los criterios quedan en el trabajo del agente.
- Tope: 3 vueltas por tema. Si no converge, hacer **una** pregunta para entender qué falta.

## Pasos

1. **Tema.** Elegirlo del [catálogo](../../../docs/interfaz/temas-ab.md). Si no está, agregar la
   fila primero.
2. **Antes de construir.** Completar la sección 1 de la plantilla: qué se decide, qué queda fijo y
   de 2 a 4 criterios.
3. **Construir A y B** en el perfil `maqueta`:
   - Cambios de estilo: `cliente-liviano/src/main/resources/static/css/variantes/<tema>-a.css` y
     `<tema>-b.css`. Solo redefinen tokens o reglas de `app.css`, nunca colores sueltos.
   - Cambios de estructura: `templates/variantes/<tema>-a.html` y `<tema>-b.html`.
     Una variante de estructura **reemplaza la página entera**, también cuando hay alternativa
     (`?alternativa=`): gana sobre la plantilla propia de la alternativa. En `/estilos` el modelo
     no trae `cuenta` ni `navegacion`, así que la variante no puede usar `layout-privado`.
   - **Preferir variantes solo de estilo** sobre el juego de plantillas de la alternativa: no se
     duplica HTML y las dos opciones salen de la misma estructura.
   - Se ven con `?variante=<tema>-a` o `?variante=<tema>-b`.
   - En el nombre del archivo, el `:` del tema se escribe `-` (`layout:entregas` →
     `layout-entregas-a.css`): solo letras minúsculas, números y guiones.
   - Lo que comparten varias variantes (un marco, una lista) va en `variantes/comun/`, que no
     cuenta como variante: las plantillas lo usan como fragmento y el CSS con `@import`.
   - Para comparar sobre otra alternativa de identidad (por ejemplo, Cozy / Medieval), la
     variante se suma a esa alternativa: `?alternativa=<nombre>&variante=<tema>-a`. Sin
     `alternativa`, la variante va sobre Glassmorphism. Cozy tiene su propio juego de plantillas
     (`templates/alternativas/cozy-medieval/`): una variante de estilo se ve sobre ese juego; una de
     estructura lo reemplaza.
4. **Comprobar y capturar:**

   ```bash
   mvn test -pl cliente-liviano -Pvisual -Dverificacion.variantes=<tema>-a,<tema>-b
   ```

   Deja capturas en 360, 1280 y 1920 px, el reporte de accesibilidad y
   `target/capturas/comparacion.html`: un visor para 1920 × 1080 con índice de secciones, dos
   variantes lado a lado, cambio de ancho y desplazamiento a la par. El primer comentario del CSS
   de cada variante es su descripción en el visor. Con más de dos opciones, se nombran
   `<tema>-a`, `-b`, `-c`… y el visor las recorre con las flechas. Con varios temas a la vez
   (decisiones en tanda), se pasan todas en una sola corrida y un enlace
   `comparacion.html#izq=<variante>&der=<variante>&ancho=<px>` abre cada comparación. Sobre otra
   alternativa, se suma `-Dverificacion.alternativa=<nombre>`. Sin propiedades, la corrida
   compara las alternativas entre sí.
5. **Autocrítica.** Mirar las capturas con las preguntas de la dirección de arte, completar la
   tabla de criterios y elegir una recomendación.
6. **Mostrar al usuario:**
   - Mandar la comparación. En la app de escritorio, con la herramienta para enviar archivos; en
     la terminal, la ruta de `comparacion.html`.
   - Debajo, "A — <nombre>" y "B — <nombre>", con 2 o 3 líneas cada una.
   - Preguntar con `AskUserQuestion`: "¿Con cuál seguimos?" → A (Recomendada) · B · "Una mezcla:
     decime qué tomo de cada una" · "Ninguna, probemos otra cosa".
7. **Cerrar:**
   - Aplicar la elegida (en `tokens.css`, `app.css` o la plantilla real). Si la comparación fue
     sobre Cozy, se aplica en las plantillas de `templates/alternativas/cozy-medieval/` y en su
     `app.css` (`static/css/alternativas/cozy-medieval/`), no en las de Glassmorphism. Si la
     elegida cambia la estructura o un texto de una plantilla que existe en las dos alternativas,
     se lleva a la gemela, o se deja constancia de por qué no.
   - Borrar las dos variantes de `variantes/`.
   - Copiar las capturas de 360 y 1280 px de cada variante a `docs/interfaz/evidencia/<tema>/`
     (menos de 400 KB cada una); las de otra alternativa, a
     `docs/interfaz/evidencia/<alternativa>/<tema>/`.
   - Escribir la entrada en la [bitácora](../../../docs/interfaz/bitacora.md): Decisión /
     Alternativas / Por qué / Costo / Quién decidió / Evidencia.

## Criterio de salida

- [ ] Variante elegida aplicada y variantes borradas.
- [ ] Tests en verde (`mvn clean test -pl cliente-liviano -am`).
- [ ] Bitácora y evidencia actualizadas.
