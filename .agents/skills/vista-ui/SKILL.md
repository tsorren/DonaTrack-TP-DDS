---
name: vista-ui
description: >-
  Construye vistas y componentes del cliente liviano de DonaTrack (Spring MVC + Thymeleaf + HTMX +
  Pico.css + tokens.css) a partir de un flujo aprobado, con todos sus estados, mejora progresiva,
  accesibilidad AA y comprobación visual en 360, 1280 y 1920 px. Usar al implementar o modificar
  cualquier pantalla, fragmento, controller o estilo dentro de cliente-liviano/.
---

# Skill: vista-ui — Construir una pantalla

> **Ámbito:** `cliente-liviano/` (E6).
> **Alineación:** [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md) (reglas
> obligatorias), [`docs/interfaz/README.md`](../../../docs/interfaz/README.md) (cuándo está lista) y
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md). **Criterios de la cátedra:**
> [`criterios-catedra-ux-ui.md`](../../../docs/interfaz/criterios-catedra-ux-ui.md) (patrones de interacción, largo de línea, color 60-30-10 y accesibilidad).
> **Catálogo:** [`componentes.md`](../../../docs/interfaz/componentes.md) (D33): se lee primero.

## 0. El catálogo primero

Antes de escribir una plantilla o una hoja, leer [`componentes.md`](../../../docs/interfaz/componentes.md)
y mirar `/estilos`.

- Lo que existe en el catálogo (composición, utilidades, movimiento, componentes) se usa como está. No
  se crea una clase parecida.
- Lo que falta se agrega primero al catálogo y a `/estilos` (`templates/estilos.html`), y después se
  usa: una clase o un componente nuevo nace en la capa CUBE (`composicion`, `utilidades`, `bloques`
  o `excepciones`), nunca en una hoja de pantalla.
- Una hoja de pantalla nueva solo dispone: sin colores, fuentes, `@keyframes` ni duraciones. El
  movimiento se pide con `data-movimiento` (`movimiento.css`) y sus tiempos salen de los tokens
  `--movimiento-*`.
- Si la tarea toca una hoja de pantalla heredada, migrar antes lo que esa pantalla usa del
  [backlog de migración](../../../docs/interfaz/backlog-migracion-componentes.md), y bajar el tope del
  cerco al valor medido.
- Estado que no sale del navegador (abrir y cerrar, pestañas, vista previa): Alpine CSP, con la
  lógica en `Alpine.data()` de `static/js/interfaz.js`. Lo que habla con el servidor es HTMX.

## 1. Precondiciones

- La ficha del flujo en `docs/interfaz/flujos/` está `aprobado`. Si no, usar `ux-flujo` primero.
- `tokens.css` tiene la identidad. Si no, avisar: se puede avanzar con la provisoria, pero hay que
  decirlo.

## 2. Construir

1. **Datos.**
   - En la maqueta (perfil `maqueta`), `SitioController` sirve `templates/<pantalla>.html` con la
     navegación y la cuenta de su rol; los datos de ejemplo los escribe la plantilla o una pieza
     compartida `datos-*.html`. Cada estado de la ficha es un escenario (`?escenario=vacio`,
     `error`, `lento`) o, si es la vuelta de un formulario, su propia pantalla
     (`<pantalla>-<estado>.html`). Datos sintéticos.
   - Con las APIs reales: un puerto en `grupo5.cliente.puertos`, su adaptador de ejemplo en
     `grupo5.cliente.maqueta` (un escenario por estado de la ficha) y un record en
     `grupo5.cliente.vistas`, con lo que la plantilla necesita ya formateado (fechas y cantidades
     en formato argentino).
2. **Controller.** Solo con las APIs reales, en `grupo5.cliente.web`.
   - Pide los datos al puerto y elige la vista. Nada más.
   - Con `HX-Request`, devuelve el fragmento; sin él, la página completa.
3. **Plantilla** en `templates/<pantalla>.html`. El nombre empieza con su rol
   (`publico`, `donante`, `entidad` o `admin`) y, en el nivel 3, lleva su sección adelante
   (`donante-donaciones-detalle`).
   - Usa `layout-privado.html` (`pagina(titulo, hoja, contenido)`) o `layout-publico.html`, y las
     piezas compartidas que ya existen.
   - Los textos salen de la ficha del flujo y siguen el
     [tono común](../../../docs/interfaz/tono-y-comunicacion.md).
   - Formularios reales: `action` y `method` verdaderos. HTMX los mejora y lleva `hx-indicator`.
4. **Pieza compartida.** Si la usan varias pantallas, va en `templates/<pieza>.html`, con un nombre
   que no empieza con un rol (así no se sirve como pantalla), como fragmento parametrizado.
5. **Estilos.** Lo reutilizable va a la capa CUBE (sección 0). En la hoja de la pantalla,
   `static/css/<pantalla>.css` (la que el layout recibe en `hoja`), solo la disposición propia.
   - Solo tokens de uso y los puntos de quiebre de `tokens.css`, con mobile first.
   - Una sola audacia por pantalla.
6. **Variante A/B.** Si surge una duda de disposición, usar `iteracion-ab`.

## 3. Comprobar

1. Test `@WebMvcTest` del controller.
   - Verifica que cada escenario muestra su estado.
   - Verifica que la estructura accesible está presente: etiquetas, `aria-live` y `aria-current`.
2. Agregar la ruta de la pantalla, y una línea por escenario relevante, a
   `cliente-liviano/src/test/resources/verificacion/paginas.txt`.
3. Correr:

   ```bash
   mvn spotless:apply
   mvn clean test -pl cliente-liviano -am
   mvn test -pl cliente-liviano -Pvisual
   ```

4. Mirar las capturas de `target/capturas/` y hacer la autocrítica de la dirección de arte.
   Corregir antes de seguir.
5. Completar la tabla de evidencia:

| Criterio | Cumple | Evidencia |
|---|---|---|
| Solo tokens de uso, sin colores ni `style` sueltos | | `InterfazFitnessTest` |
| Clases del catálogo; lo nuevo, en `componentes.md` y `/estilos` | | `InterfazFitnessTest` |
| Todos los estados de la ficha | | escenarios + test del controller |
| Funciona sin JavaScript | | formularios con `action` real |
| Accesibilidad AA sin errores en 3 anchos | | reporte de `-Pvisual` |
| Nada desbordado; toque de 48 px o más | | reporte de `-Pvisual` |
| Textos iguales a la ficha | | comparación |

## 4. Revisión

Correr `revision-ui` en modo **pantalla**. Corregir lo bloqueante y volver a comprobar.

## Criterio de salida

- [ ] Checklist "Una pantalla está lista" del README completo, con evidencia.
- [ ] `revision-ui` sin hallazgos bloqueantes.
- [ ] Pre-flight de SonarCloud para el Java
  ([`07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md)).
