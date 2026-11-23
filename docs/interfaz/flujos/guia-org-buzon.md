# Flujo: El buzón de novedades con la guía «Huerta y galpón» (org-buzon)

> **Estado:** aprobado · **Fecha:** 2026-11-23 · **Aprobó:** el usuario, 2026-11-23 («ok», con las tres preguntas de §9 en la opción recomendada). Corrección de hecho del mismo día, después de aprobar: los filtros son la barra de `componentes/listado` desde R11-B04, no pestañas
> Ficha del lote `org-buzon` de la ola ORG del
> [backlog de la guía al código](../backlog-guia-al-codigo.md) (§5 y §10.2). Pedida por DA-08 y
> escrita en sesión con el usuario (DA-19).
> Hereda de la ficha [R11-14 del buzón](r11-p1-notificaciones.md) (aprobada el 2026-11-12) el
> recorrido (§3), los estados (§5), los textos (§6), la accesibilidad (§7) y lo que el servicio
> resuelve (páginas, filtros, conteo). Esta ficha solo dice **qué cambia**: cómo se ve, con las
> piezas de la guía; la casilla de 48 px (DA-21); y los textos que cambian por DA-02 y DA-06. Donde
> las dos fichas difieren, manda esta.

## 1. Para quién y para qué

| | |
|---|---|
| Quién | La organización, quien dona (empresa y persona) y el admin. Es la misma pieza para los tres: `componentes/buzon.html` y `componentes/buzon.css` |
| Tono | Pantalla de trabajo: claro, útil, tranquilizador ([tono](../tono-y-comunicacion.md#tono-según-la-pantalla)) |
| Qué quiere lograr | «Ver de un vistazo qué es nuevo y abrir lo que me importa» |
| Dispositivo principal | Ambos |
| Requerimientos | Los de R11-14: RF-08, RF-13 y RNF-UI-01 a 06 |
| Datos que usa | Los mismos de R11-14. Ningún dato nuevo |
| Guía | La lista de novedades de la campana (`guia/partes/10-componentes.css`, «El desplegable de la campanita»; muestra en `40-guia.html`, «Campanita: sin leer, con el cursor y leída») |

## 2. Dónde está en el sitio

Igual que hoy: nivel 2, desde la campana de la cabecera y desde el inicio de cada rol. Rutas:
`/entidad-notificaciones`, `/donante-notificaciones` (empresa y persona) y `/admin-notificaciones`,
con su variante `-leidas`.

## 3. Recorrido

Sin cambios respecto de R11-14 §3.

## 4. Pantallas

**Patrón conocido:** el de R11-14: bandeja de entrada, con la fila de la campana de la guía.

```text
┌──────────────────────────────────────────────────────────────┐
│ NOVEDADES                                                    │
│ Novedades  · 4 sin leer                                      │
│ [barra de componentes/listado: buscar, estado, categoría,    │
│  agrupar y ordenar], como hoy                                │
├─ lámina (P07) ───────────────────────────────────────────────┤
│ [☐ Elegir todas]                 [Marcar todas como leídas]  │
│ ● ☐ Entrega en camino                              [✓]       │
│      El camión salió con tus lentejas.                       │
│      Ver en Entregas · Hoy, 14:30 · Entregas en camino       │
│ ○ ☐ Entrega recibida                               [✉]       │
│      …                                                       │
├──────────────────────────────────────────────────────────────┤
│ Mostrando 1 a 20 de 57 novedades      ‹ 1 2 3 › (P17)        │
└──────────────────────────────────────────────────────────────┘
● punto lleno = sin leer · ○ punto hueco = leída
```

**Qué cambia respecto de R11-14:**

| Parte | R11-14 (hoy) | Con la guía |
|---|---|---|
| Contenedor | Su propio fondo | La lámina (P07) con su sombra de paja; las filas separadas por el borde fino |
| Marca de sin leer | Punto, fondo suave del acento y título en negrita | Punto de 10 px con el borde de tinta: **lleno** sin leer, **hueco** leída. Título en negrita solo sin leer. **Sin fondo propio**: en la guía, el salvia es el de pasar el mouse o del foco, no el de un estado |
| Pasar el mouse o enfocar | Fondo propio | El salvia de la guía (`--color-salvia`), con el aro de foco visible encima |
| Filtros y paginador | La barra de `componentes/listado` (desde R11-B04, con Estado: Todas, Sin leer o Leídas) y el paginador de R11 | La misma barra, sin cambios, y el paginador P17, retocado en C1 |
| Casilla de elegir | `input` nativo de 12 px | La casilla de P04 (C1-12): el control nativo transparente, de 48 × 48 px como mínimo, encima de la casilla dibujada (DA-21). Lo mismo para «Elegir todas» |
| Acciones de la fila | Tilde y sobre cerrado | Igual, como botón de ícono de P03 (48 px) |
| Avisos al marcar | Aviso en `#avisos` | El aviso breve de P13 (DA-06) |
| Vacío, cargando y error | Los de R11 | Los estados de lista de P19 y el aviso de error de P13; los textos no cambian |

- **El estado no depende del color:** quedan tres señales sin color (la forma del punto, la
  negrita y «Sin leer: » para lectores). Con colores forzados, el punto se dibuja con su borde.
- **El admin:** usa la misma pieza. La densidad del galpón (P26) llega con ADM1 y ADM2; esta ola
  no le da al admin un buzón propio.
- **Sin cambios de marcado que rompan a los otros roles:** el fragmento conserva sus parámetros
  (`boton()`, `que()` y `nota()` de los records); los tres buzones se ven con la pieza nueva en la
  misma ola.

## 5. Estados

Los de R11-14 §5, con dos cambios:
- «Éxito» al marcar: aviso breve de P13 (se cierra, no pide nada), en lugar del aviso en la página.
- «Todas leídas, en Todas»: las filas con el punto hueco y sin negrita; nada más cambia.

## 6. Textos

Los de R11-14 §6, con estos cambios:

| Elemento | Antes | Ahora | Por qué |
|---|---|---|---|
| Título de la pantalla | Organización: «Tus novedades»; quien dona: «Noticias de tus donaciones»; admin: «Notificaciones» | «Novedades» en los tres roles (la organización puede seguir con «Tus novedades») | La guía llama «Novedades» a la campana en todas las cabeceras (pregunta 1) |
| Total y paginador | «de 57 notificaciones» · «Páginas de notificaciones» | «de 57 novedades» · «Páginas de novedades» | El mismo nombre que el título |
| Vínculo de la organización | «Ver en Donaciones recibidas» | «Ver en Entregas», a la ruta de Entregas | DA-02 funde Recibidas en Entregas; la ruta la fija `org-entregas` |
| Casilla de cada fila, para lectores | — | «Elegir: <título>» | Cada casilla tiene un nombre único |

Filtro de tono: sin cambios respecto de R11-14.

## 7. Accesibilidad

La de R11-14 §7, más: el control de la casilla mide 48 × 48 px y es el que recibe el foco y el
toque; el dibujo de la casilla no es interactivo. `VerificacionVisualTest` mide ese control, sin
excepciones nuevas (DA-21).

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Punto lleno o hueco y negrita, como en la campana: la misma señal en los dos lugares | Sin fondo, una fila sin leer se distingue menos de lejos que con R11 |
| 2 | Lenguaje del usuario | «Novedades» en todo el sitio, como la campana | — |
| 3 | Control y libertad | Igual que R11-14 | «Marcar todas» sin deshacer en bloque |
| 4 | Consistencia | La fila del buzón es la de la campana, en grande | El admin queda con la huerta hasta ADM1 |
| 5 | Prevención de errores | La nota de alcance de «Marcar todas» sigue | — |
| 6 | Reconocer antes que recordar | Título, mensaje y vínculo en cada fila | — |
| 7 | Flexibilidad | Filtros y páginas en la dirección | — |
| 8 | Diseño minimalista | Sin fondo de estado; una sola señal de color | — |
| 9 | Recuperarse de errores | Los textos de error de R11-14 | — |
| 10 | Ayuda | La nota de «Marcar todas» | — |

## 9. Decisiones y preguntas

- Decisiones del usuario que baja: DA-08 (la ficha), DA-21 (la casilla de 48 px), DA-06 (aviso
  breve al marcar) y DA-02 (el vínculo a Entregas).
- Preguntas, respondidas por el usuario el 2026-11-23 con la recomendada (la primera de cada una):
  1. **Nombre:** ¿«Novedades» en los tres roles, como la campana de la guía? Otra: dejar los
     títulos de hoy («Noticias de tus donaciones», «Notificaciones»).
  2. **Sin leer sin fondo:** ¿se sigue a la guía (punto lleno y negrita, sin fondo), aunque R11-14
     había aprobado un fondo suave? Otra: conservar el fondo suave del acento.
  3. **El admin:** ¿cambia su buzón en esta ola junto con los otros dos (es la misma pieza)? Otra:
     separar una variante para que el admin quede como hoy hasta ADM2 (más código, dos buzones).
