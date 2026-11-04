# Backlog de migración a componentes y capas CUBE

> Obligatorio por el [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`, D31–D33). Deuda asociada: [DTI-34](../adr/DEUDA_TECNICA.md#dti-34).
> Línea de base medida el 2026-11-04 `[OBSERVED]`: 8.924 líneas de CSS en 29 hojas, 212 clases de un
> solo uso, 13 `@keyframes` en 5 hojas, 28 duraciones literales fuera de `tokens.css`, 0 tokens de
> movimiento y sin catálogo `/estilos`.

## Reglas del backlog

1. **Primero la base (fase 0).** Sin el cerco, migrar no sirve: la entropía vuelve.
2. **Quien toca, migra.** Toda tarea que modifica una hoja de pantalla migra antes los ítems de la
   fase 2 que esa pantalla usa. Una pantalla nueva nace en la capa CUBE.
3. **El tope baja.** Cada ítem cerrado baja el tope de clases de un solo uso del cerco al valor medido
   después del cambio. El tope nunca sube.
4. **Cada ítem cierra con:** su entrada en [`componentes.md`](componentes.md) y en `/estilos`;
   `mvn clean test -pl cliente-liviano -am` verde; `-Pvisual` sin cambios visuales no buscados
   (capturas antes y después); y la tabla de abajo actualizada con fecha y commit.
5. **Revisión por entrega.** Al cerrar cada entrega se revisa el avance, se repriorizan los ítems y
   se anota el tope vigente.

## Fase 0 — Base y cerco

> Cerrada el 2026-11-04. Notas de lo hecho: las duraciones se redondearon hasta 0,1 s para entrar en
> la escala de tokens; Pico no se carga en ninguna plantilla, así que la capa `pico` queda declarada y
> vacía; las clases que las plantillas ya usaban están en
> `cliente-liviano/src/test/resources/verificacion/clases-heredadas.txt` (527, solo baja); el cerco
> mide 211 clases de un solo uso (sin comentarios, textos ni urls): el tope quedó en 211, el valor medido (regla 3), y no en los 212 de la línea de base, que contaban una palabra de un comentario. Las capturas de
> `-Pvisual` antes y después son iguales salvo 7 de 584 de la portada con 8 a 19 px de diferencia,
> que también aparecen entre dos corridas del mismo código (ruido de la barra fija).

| ID | Ítem | Depende de | Hecho cuando | Estado |
|---|---|---|---|---|
| B0-01 | Tokens de movimiento en `tokens.css`: duraciones, curvas y distancias, en dos niveles | — | Las 28 duraciones literales tienen su token equivalente | Hecho (2026-11-04) |
| B0-02 | `@layer pico, tokens, composicion, utilidades, bloques, excepciones, pantallas` en `app.css`, con Pico en la capa `pico` | — | Capturas de `-Pvisual` iguales a las de antes | Hecho (2026-11-04) |
| B0-03 | `composicion.css` (`pila`, `racimo`, `barra-lateral`, `cubierta`, `grilla`, `centro`, `marco`) y `utilidades.css` | B0-01, B0-02 | Cada primitiva con su entrada en el catálogo | Hecho (2026-11-04) |
| B0-04 | `movimiento.css` con las primitivas (`aparece`, `sube`, `resalta`, `crece`, `traza`) en `data-movimiento` y "reducir movimiento" resuelto adentro | B0-01 | Video del movimiento en el perfil `visual` | Hecho (2026-11-04) |
| B0-05 | Alpine CSP: WebJar `alpinejs__csp` en `cliente-liviano/pom.xml`, carga `defer` en los layouts y `Alpine.data()` en `static/js/` | — | Test del layout que verifica la carga desde el WebJar | Hecho (2026-11-04) |
| B0-06 | Catálogo: `/estilos` y [`componentes.md`](componentes.md) (nombre, cuándo, parámetros, slots, estados, movimiento, patrón) | B0-03 | RF-CL-05 cumplido; axe AA sobre `/estilos` | Hecho (2026-11-04) |
| B0-07 | Cerco en `InterfazFitnessTest`: clases en el catálogo, tope de clases de un solo uso (211, el valor medido), hojas de pantalla sin color, fuente, `@keyframes` ni duraciones, sin duraciones literales fuera de `tokens.css` | B0-01, B0-06 | Las reglas fallan con un caso inventado y pasan sobre el sitio | Hecho (2026-11-04) |
| B0-08 | Skills: `vista-ui` empieza por el catálogo y `revision-ui` rechaza clases fuera de él (vector nuevo) | B0-06 | Las dos skills citan `componentes.md` | Hecho (2026-11-04) |
| B0-09 | Video de movimiento con Playwright (`recordVideo`) en `VerificacionVisualTest` | B0-04 | Un video por pantalla con movimiento | Hecho (2026-11-04) |
| B0-10 | View Transitions entre páginas y en los intercambios de HTMX | B0-02, B0-04 | Navegación sin animación donde no hay soporte; respeta "reducir movimiento" | Hecho (2026-11-04) |

## Fase 1 — Movimiento que hoy está suelto

> Cerrada el 2026-11-04. Notas de lo hecho: los 13 `@keyframes` de las 5 hojas pasaron a
> `movimiento.css` como 10 (`mov-*`): `aparece`, `traza` y `lamina-traza` eran iguales a las
> primitivas `mov-aparece` y `mov-traza` y se reusaron. Cada animación se declaró **completa**
> (`@keyframes`, duración, curva y espera, solo con tokens) como una custom property
> `--movimiento-animacion-*` (y `--movimiento-transicion-atenua`) de `movimiento.css`; la hoja de
> pantalla la pide con `animation: var(--movimiento-animacion-registro)` y no declara `@keyframes`,
> duraciones, curvas ni esperas. Se eligió ese camino y no `data-movimiento` en las plantillas
> porque las clases animadas (`.raiz`, `.fruto`, `.etiqueta`) las comparten varias pantallas y el
> cambio tocaba unos 40 elementos SVG de 10 plantillas. Las custom properties solo existen con
> movimiento permitido: "reducir movimiento" quedó resuelto adentro (el bloque `reduce` de
> `.linea` en `donante-seguimiento.css` quedó redundante). Quedan en la portada dos
> `animation-delay: var(--movimiento-retraso-ninguno)` (las reglas de la escena con línea de tiempo
> del scroll anulan así la espera de la animación del tiempo): es un reset con un token, no una
> duración. Nueva regla del cerco en `InterfazFitnessTest`: `@keyframes`, `--movimiento-duracion-*` y
> `--movimiento-curva-*` y las esperas (salvo `retraso-ninguno`) no se escriben en ninguna hoja
> de la carpeta `css/` salvo `movimiento.css`, `app.css` y `tokens.css`. No cambió ninguna clase: el tope de
> clases de un solo uso sigue en 211, el valor medido.

| ID | Hoja | Contenido a mover | Estado |
|---|---|---|---|
| B1-01 | `publico-portada.css` | 7 `@keyframes` y 16 duraciones (viento, traza, crece…) a `movimiento.css` y tokens | Hecho (2026-11-04) |
| B1-02 | `publico-paginas.css` | 2 `@keyframes` y 4 duraciones | Hecho (2026-11-04) |
| B1-03 | `entidad-seguimiento-confirmar.css` | 2 `@keyframes` y 4 duraciones | Hecho (2026-11-04) |
| B1-04 | `privado.css`, `donante-perfil.css` | 1 `@keyframes` cada una y sus duraciones | Hecho (2026-11-04) |
| B1-05 | `publico-mapa.css`, `donante-seguimiento.css` | 1 duración cada una | Hecho (2026-11-04) |

## Fase 2 — Componentes

Prioridad por uso: primero los que comparten varias pantallas. El patrón de cada uno está en
[patrones y microcopy](patrones-y-microcopy.md#componentes-y-su-patrón).

| ID | Componente | Origen hoy | Pantallas que lo usan | Estado |
|---|---|---|---|---|
| B2-01 | Botón | `app.css`, `formularios.css` | Todas | Pendiente |
| B2-02 | Campo de formulario | `formularios.css`, `publico-registro.css` | Registro, ingreso, perfil, necesidades, confirmar | Pendiente |
| B2-03 | Sello de estado | `sellos.css` | Privadas | Pendiente |
| B2-04 | Aviso (toast) y banner, con la cola en Alpine | `app.css`, `privado.css` | Todas las acciones | Pendiente |
| B2-05 | Diálogo (`<dialog>` + `@starting-style`) | Foto de la entrega en la portada, bajas | Portada, necesidades | Pendiente |
| B2-06 | Navegación por rol y barra pública | `privado.css`, `publico-navegacion.css` | Todas | Pendiente |
| B2-07 | Tarjeta | `donante-comun.css`, `entidad-comun.css`, `publico-listas.css` | Listas y seguimiento | Pendiente |
| B2-08 | Tabla o lista adaptable | `publico-listas.css`, `entidad-recibidas.css` | Listados | Pendiente |
| B2-09 | Estado vacío, esqueleto e indicador de carga | Varias hojas | Todas las listas | Pendiente |
| B2-10 | Carga de fotos con vista previa (Alpine) | `entidad-seguimiento-confirmar.css` | Confirmar recepción | Pendiente |
| B2-11 | Pestañas (Alpine) | `donante-perfil.css` | Mi perfil | Pendiente |
| B2-12 | Piezas de la entidad | `piezas-entidad.html`, `entidad-comun.css` | Sitio de la entidad | Pendiente |
| B2-13 | Mapa con su lista (RF-CL-06) | `publico-mapa.css` | Mapa público, seguimiento | Pendiente |

## Fase 3 — Hojas de pantalla reducidas a disposición

Al terminar las fases 1 y 2, cada hoja de pantalla queda solo con la disposición propia. Orden por
tamaño:

| ID | Hoja | Líneas (2026-11-04) | Estado |
|---|---|:-:|---|
| B3-01 | `publico-portada.css` | 1.732 | Pendiente |
| B3-02 | `entidad-necesidades.css` | 647 | Pendiente |
| B3-03 | `privado.css` | 522 | Pendiente |
| B3-04 | `entidad-seguimiento-confirmar.css` | 489 | Pendiente |
| B3-05 | `publico-registro.css` | 453 | Pendiente |
| B3-06 | Las 23 hojas restantes, de mayor a menor | 4.946 en total, sin contar `tokens.css` | Pendiente |

## Registro de avance

| Fecha | Ítems cerrados | Tope de clases de un solo uso | Commit |
|---|---|:-:|---|
| 2026-11-04 | — (línea de base) | 212 | — |
| 2026-11-04 | B0-01 a B0-10 (fase 0 completa) | 211 (el cerco mide sin comentarios, textos ni urls: 211; los 212 de la línea de base incluían una palabra de un comentario) | 2e7ace16 (rama `E6_raices`) |
| 2026-11-04 | B1-01 a B1-05 (fase 1 completa) | 211 (medido después del cambio; la fase no toca selectores) | c0884659 (rama `E6_raices`) |
