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
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md).

## 1. Precondiciones

- La ficha del flujo en `docs/interfaz/flujos/` está `aprobado`. Si no, usar `ux-flujo` primero.
- `tokens.css` tiene la identidad. Si no, avisar: se puede avanzar con la provisoria, pero hay que
  decirlo.

## 2. Construir

1. **Datos.**
   - Un puerto en `grupo5.cliente.puertos` y su adaptador de ejemplo en `grupo5.cliente.maqueta`.
   - El adaptador tiene un escenario por estado de la ficha (`vacio`, `error`, `lento`, etc.).
   - Datos sintéticos.
2. **Modelo de vista.** Un record en `grupo5.cliente.vistas`, con lo que la plantilla necesita ya
   formateado (fechas y cantidades en formato argentino).
3. **Controller** en `grupo5.cliente.web`.
   - Pide los datos al puerto y elige la vista. Nada más.
   - Con `HX-Request`, devuelve el fragmento; sin él, la página completa.
4. **Plantilla** en `templates/paginas/` y su gemela en `templates/alternativas/<alt>/paginas/`
   para cada alternativa (hoy, `cozy-medieval`).
   - Usa `layout.html` y los fragmentos existentes; la de cada alternativa, su propio `layout.html`
     y sus fragmentos.
   - Los textos son **los mismos** en las dos: un test (`InterfazFitnessTest`) falla si difieren.
   - Los textos salen de la ficha del flujo.
   - Formularios reales: `action` y `method` verdaderos. HTMX los mejora y lleva `hx-indicator`.
5. **Componente nuevo.**
   - Fragmento parametrizado en `templates/fragmentos/` y su gemela, con el mismo nombre y los
     mismos textos, en `templates/alternativas/<alt>/fragmentos/` para cada alternativa.
   - Agregarlo a `/estilos` con todos sus estados y el patrón que sigue, y también al `/estilos`
     de cada alternativa (`templates/alternativas/<alt>/paginas/estilos.html`).
6. **Estilos** en el `app.css` de cada alternativa de identidad: el de Glassmorphism
   (`static/css/`) y el de cada carpeta de `static/css/alternativas/`.
   - Solo tokens de uso y los puntos de quiebre de su `tokens.css`, con mobile first.
   - Una sola audacia por pantalla.
7. **Variante A/B.** Si surge una duda de disposición, usar `iteracion-ab`.

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
| Todos los estados de la ficha | | escenarios + test del controller |
| Funciona sin JavaScript | | formularios con `action` real |
| Accesibilidad AA sin errores en 3 anchos | | reporte de `-Pvisual` |
| Nada desbordado; toque de 48 px o más | | reporte de `-Pvisual` |
| Componentes nuevos en `/estilos` | | `InterfazFitnessTest` |
| Textos iguales a la ficha | | comparación |

## 4. Revisión

Correr `revision-ui` en modo **pantalla**. Corregir lo bloqueante y volver a comprobar.

## Criterio de salida

- [ ] Checklist "Una pantalla está lista" del README completo, con evidencia.
- [ ] `revision-ui` sin hallazgos bloqueantes.
- [ ] Pre-flight de SonarCloud para el Java
  ([`07-errores-frecuentes-sonarcloud-ia.md`](../../../docs/IA/07-errores-frecuentes-sonarcloud-ia.md)).
