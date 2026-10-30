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
   - Se ven con `?variante=<tema>-a` o `?variante=<tema>-b`.
   - En el nombre del archivo, el `:` del tema se escribe `-` (`layout:entregas` →
     `layout-entregas-a.css`): solo letras minúsculas, números y guiones.
4. **Comprobar y capturar:**

   ```bash
   mvn test -pl cliente-liviano -Pvisual -Dverificacion.variantes=<tema>-a,<tema>-b
   ```

   Deja capturas en 360, 1280 y 1920 px, el reporte de accesibilidad y
   `target/capturas/comparacion.html` con A y B lado a lado.
5. **Autocrítica.** Mirar las capturas con las preguntas de la dirección de arte, completar la
   tabla de criterios y elegir una recomendación.
6. **Mostrar al usuario:**
   - Mandar la comparación. En la app de escritorio, con la herramienta para enviar archivos; en
     la terminal, la ruta de `comparacion.html`.
   - Debajo, "A — <nombre>" y "B — <nombre>", con 2 o 3 líneas cada una.
   - Preguntar con `AskUserQuestion`: "¿Con cuál seguimos?" → A (Recomendada) · B · "Una mezcla:
     decime qué tomo de cada una" · "Ninguna, probemos otra cosa".
7. **Cerrar:**
   - Aplicar la elegida (en `tokens.css`, `app.css` o la plantilla real).
   - Borrar las dos variantes de `variantes/`.
   - Copiar las capturas de 360 y 1280 px de cada variante a `docs/interfaz/evidencia/<tema>/`
     (menos de 400 KB cada una).
   - Escribir la entrada en la [bitácora](../../../docs/interfaz/bitacora.md): Decisión /
     Alternativas / Por qué / Costo / Quién decidió / Evidencia.

## Criterio de salida

- [ ] Variante elegida aplicada y variantes borradas.
- [ ] Tests en verde (`mvn clean test -pl cliente-liviano -am`).
- [ ] Bitácora y evidencia actualizadas.
