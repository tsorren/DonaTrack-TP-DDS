---
name: construir-ui
description: >-
  Construye o cambia una pantalla, un fragmento o un estilo del cliente liviano de DonaTrack con el
  sistema visual cerrado (tokens.css, /estilos, el catálogo y el kit de listas componentes/listado).
  Usar al implementar o modificar cualquier pantalla, fragmento, controller o estilo dentro de
  cliente-liviano/.
---

# Construir una pantalla o un componente

Rigen [`AGENTS.md`](../../../AGENTS.md) y [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md).
Esto es producción: se usa el sistema que hay. Para buscar un aspecto nuevo, `explorar-ui`.

1. **El flujo, si la pantalla es nueva o cambia su recorrido.** Ficha en `docs/interfaz/flujos/`
   con la [plantilla](../../../docs/interfaz/plantillas/flujo.md), de 150 líneas o menos: para
   quién, qué viene a hacer, qué mira primero, estados con su texto y desvíos. La aprueba el
   usuario antes del código.
2. **El sistema antes de dibujar.** Mirá `/estilos` (perfil `maqueta`) y buscá la pieza en el
   catálogo, [`componentes.md`](../../../docs/interfaz/componentes.md), por nombre o con `grep`; no
   hace falta leerlo entero. Si la pieza existe, se usa como está.
3. **Listas.** Toda lista con búsqueda, filtros u orden usa `componentes/listado :: listado(l)`, con
   un `vistas.Listado` armado en el controller (`Listado.en(id, ruta, rotulo)` y sus campos en
   orden). Parámetros comunes: `buscar`, `desde`, `hasta`, `estado`, `categoria`, `quien` y `orden`.
   Un parámetro propio se registra en `PARAMETROS_PROPIOS` de `InterfazFitnessTest`; un nombre
   viejo solo sigue como alias en su controller. Debajo, el paginador `componentes/paginas`.
4. **Si falta una pieza, primero el componente:** fragmento con parámetros con nombre y slots
   (`~{::#cuerpo}`) en
   `templates/componentes/<nombre>.html`, hoja en `static/css/componentes/<nombre>.css` solo con
   tokens de uso, variantes por atributo (`data-estado`, `data-tono`), su fila en el catálogo y su
   muestra en `/estilos` con todos sus estados.
5. **Después, la pantalla.** Usa `layout-privado` (`pagina(titulo, hoja, contenido)`) o
   `layout-publico` y compone componentes. Su hoja, si hace falta, solo dispone (grilla,
   orden): sin colores, fuentes, `@keyframes`, duraciones ni medidas literales nuevas.
6. **Datos y controller.** Puerto en `puertos`, adaptador de maqueta con un escenario por estado
   (`?escenario=`) y uno REST; record en `vistas` con lo ya formateado. El controller pide al
   puerto y elige la vista; con `HX-Request` devuelve el fragmento.
7. **Estados.** Vacío, sin resultados, cargando (más de 300 ms, `hx-indicator`), error y sin
   permiso, cada uno con su texto según [el tono](../../../docs/interfaz/tono-y-comunicacion.md).
8. **Sin JavaScript primero.** Formularios GET reales con `action`; `hx-boost="true"` como mejora.
   Alpine solo para estado que no sale del navegador; `x-cloak` solo en lo que no es esencial. View
   Transitions, `@starting-style`, `@container` y `popover`, como mejora progresiva.
9. **Comprobá en el navegador** a 360, 1280 y 1920 px: nada se corta ni desborda, controles de
   48 px o más y foco visible. Sumá la ruta a `src/test/resources/verificacion/paginas.txt`.
10. **Validá:**
    ```bash
    mvn spotless:apply
    mvn clean test -pl cliente-liviano -am
    mvn test -pl cliente-liviano -Pvisual -Dverificacion.paginas=<rutas tocadas>
    ```
    Test `@WebMvcTest` por controller: cada escenario muestra su estado, con etiquetas, `aria-live`
    y `aria-current`. Pre-flight de SonarCloud para el Java
    ([`07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md)).
11. **Antes de cerrar, `revisar-ui`** sobre todo lo que tocaste, con las capturas.

No se sube un tope ni se afloja un test para que pase: se arregla el código.
