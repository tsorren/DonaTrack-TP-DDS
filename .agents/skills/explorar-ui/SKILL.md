---
name: explorar-ui
description: >-
  Explora cómo podría verse o funcionar una pantalla, un flujo o la identidad del cliente liviano
  de DonaTrack con opciones que divergen en ejes de producto (para qué sirve la pantalla, metáfora y
  densidad), en una rama o en prototipos sueltos, y deja que el humano elija mirándolas. Usar cuando
  el usuario pide alternativas, un rediseño o "algo nuevo", antes de producir con construir-ui.
---

# Explorar alternativas de interfaz

Rige [`AGENTS.md`](../../../AGENTS.md). La exploración no es producción: **lo explorado no se
mergea**. Vive en una rama propia o en prototipos sueltos, y lo que el humano elige entra al sitio
después, con `construir-ui`, donde rigen todas las reglas de
[`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md) y el cerco del build.

1. **El encargo, en una ronda corta con el usuario:** qué pantalla o flujo, para quién, qué tiene
   que lograr y qué no se toca (el piso: AA, sin JavaScript en lo esencial, datos sintéticos).
2. **Los ejes, fijados antes de generar.** Cada opción toma una posición distinta en al menos dos:
   - **para qué sirve la pantalla:** tablero, bandeja de tareas, relato, feed, formulario guiado;
   - **metáfora:** de qué mundo vienen las formas y las palabras;
   - **densidad:** cuánto se ve de una vez y cuánto se pide para llegar.
   Cambiar solo la disposición, los colores o la letra no cuenta como divergir.
3. **Referencias nuevas.** Cada opción trae al menos una referencia externa propia (un sitio, una
   pieza impresa, un objeto), con su enlace o su imagen en la carpeta de la exploración.
4. **Entradas libres.** Una opción puede dejar de lado la piel, los componentes y los textos
   actuales si su eje lo pide; lo que no se suelta es el piso del paso 1.
5. **Construí de 3 a 4 opciones**, cada una en su prototipo o en su variante de la maqueta
   (`?variante=<tema>-a|b|c`), con la misma pantalla y los mismos datos de ejemplo. Capturas a 360 y
   1280 px con `-Pvisual -Dverificacion.variantes=<tema>-a,<tema>-b,<tema>-c`; para las vueltas de
   boceto alcanza con
   `-Dtest=VerificacionVisualTest#paginasCumplenElPisoEnTodosLosAnchos -Dverificacion.boceto=true`
   (solo 1280 px, sin el piso).
6. **Presentá sin ranking:** las opciones en el orden en que se hicieron, cada una con su captura,
   su posición en los ejes y 2 o 3 líneas de qué propone. Sin "Recomendada", sin matriz y sin
   ganadora. Elige el humano mirándolas.
7. **Si no elige ninguna,** otra vuelta con ejes nuevos; no se promedian las que hubo.
8. **Al elegir:** la decisión va a la [bitácora](../../../docs/interfaz/bitacora.md) con las
   capturas en `docs/interfaz/evidencia/<tema>/`. Si cambia la identidad o el sistema, un ADR
   `proposed` en `docs/adr/cliente-liviano/`. Después, `construir-ui` lleva lo elegido a los tokens,
   al catálogo y a las pantallas, y `revisar-ui` lo cierra.

Las variantes de la maqueta (`static/css/variantes/`, `templates/variantes/`) se borran al decidir.
