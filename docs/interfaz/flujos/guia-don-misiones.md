# Flujo: Misiones de la persona con la guía «Huerta y galpón» (don-misiones)

> **Estado:** aprobado · **Fecha:** 2026-11-23 · **Aprobó:** el usuario, 2026-11-23 («va asi», con las preguntas de §9 en la opción recomendada)
> Ficha del lote `don-misiones` de la ola DON del
> [backlog de la guía al código](../backlog-guia-al-codigo.md) (§5 y §12). La pide DA-08 y se
> escribe en sesión con el usuario (DA-19).
> Hereda de la ficha [Misiones e insignias](raices-misiones.md) (aprobada el 2026-11-07, con los
> cambios de la usuaria del 2026-11-09 de su §9) los datos (§1), los estados (§5), los textos de la
> conexión con Incentivos (§9, cierre de E6) y la accesibilidad (§7). La base visual es la huerta
> que se sirve hoy (`variantes/misiones-b`, incrustada en `donante-misiones.html:22`). Esta ficha
> dice solo **qué cambia**: cómo se ve con las piezas de la guía, el menú (DA-03b) y el toque.
> Donde las dos fichas difieren, manda esta.

## 1. Para quién y para qué

| | |
|---|---|
| Quién | La persona que dona por su cuenta (en la maqueta, Martina Ruiz). Entra de vez en cuando, casi siempre desde el celular |
| Tono | Panel del donante: cálido y agradecido, sin presionar; con la regla del [juego](../tono-y-comunicacion.md#juego-y-celebración-extensión) |
| Qué quiere lograr | «Ver qué misiones completé, cuánto me falta en la que tengo en curso y qué viene después» |
| Dispositivo principal | Celular primero |
| Requerimientos | RF-07, RF-CL-07 y RNF-CL-02, como la ficha anterior |
| Datos que usa | Los mismos: `DonanteMisionesPuerto` (Incentivos). Ningún dato nuevo; no toca adaptadores `rest` |
| Guía | Sin arquetipo (DA-08). Piezas: lámina y sombra de paja (P07), sello (P06), avance (P12), avisos (P13), estados de lista (P19), dibujos (P24) |

## 2. Dónde está en el sitio

Nivel 2, `/donante-misiones`. Cambia el menú de la persona (DA-03b, DA-03a y DA-05): **Inicio,
Donaciones, Organizaciones, Misiones y Cómo donar**. Seguimiento sale del menú; Cómo donar lleva a
`/publico-para-donantes`. La empresa no tiene Misiones (RF-07): Inicio, Donaciones, Organizaciones
y Cómo donar. Los dos menús salen de `web/Pantallas.java:92-130`; en la maqueta, de
`maqueta/CuentaDeEjemplo.java:94-99`.

## 3. Recorrido

Sin cambios respecto de la ficha anterior (§3), con el arreglo del 2026-11-12: elegir una maceta
abre su ficha en la misma pantalla y «Ver toda la huerta», tocar afuera o Esc la cierran.

## 4. Pantallas

**Patrón conocido:** un tablero de logros en forma de huerta: cada misión es un lugar, con su
planta si está completa o su maceta si no.

```text
┌──────────────────────────────────────────────────────────────┐
│ MISIONES                                                     │
│ Tus misiones e insignias                                     │
│ Gracias por donar, Martina. Sos donante Sostenedor desde…    │
│ Completaste 2 de las 6 misiones. Al 15 de octubre, 16:25.    │
├─ lámina protagonista (P07, sombra de paja) ──────────────────┤
│ [la huerta: un lugar por misión, en el orden de Incentivos]  │
│  🌱 Racha Inicial  🌱 Explorador  ( ? ) Gran Aporte  ( ) …   │
│  cada lugar es un enlace de 48 × 48 px como mínimo           │
├─ la ficha del lugar elegido (o el resumen, sin elegir) ──────┤
│ Gran Aporte · [sello: en curso]                              │
│ Una donación con al menos 50 bienes                          │
│ [avance P12: ██░░░░░░░░] Llevás 12 de 50.                    │
│ [Ver toda la huerta]                                         │
├──────────────────────────────────────────────────────────────┤
│ Tus insignias (2 de 6): sellos llenos y huecos (P06)         │
│ Colaborador · completa  ▸ (plegable, cerrado)                │
│ Transformador · se abre cuando completes Sostenedor          │
└──────────────────────────────────────────────────────────────┘
```

**Qué cambia respecto de hoy:**

| Parte | Hoy (`misiones-b`) | Con la guía |
|---|---|---|
| Hoja | `static/css/variantes/misiones-b.css`, una hoja de variante | Las piezas del catálogo; lo que quede propio, en una hoja de pantalla que solo dispone. La hoja de variante sale de `donante-misiones` |
| La huerta | Su propio fondo y marco | La lámina protagonista (P07) con la sombra de paja: es la única de la pantalla |
| Plantas y macetas | Dibujos de la ronda del 2026-11-09 | Los mismos dibujos, con el trazo y la tinta de la guía (P24, P27). **Planta = misión completa**, solo en esta pantalla; los frutos siguen siendo entregas confirmadas (P10) y acá no aparecen |
| Toque de cada lugar | Blancos chicos en el celular (bitácora del 2026-11-12) | Cada lugar es un enlace de 48 × 48 px como mínimo, con el patrón de C1-12: el área de toque encima del dibujo. `/donante-misiones?cuenta=persona`, con y sin `escenario=vacio`, sale de la línea de base de §10.4 |
| Estado de la misión | Texto y color de la maceta | El sello de la guía (P06) con su texto: «Completa», «En curso», «En espera» y, para las cerradas, ninguno |
| Progreso | Barra propia | El avance de P12, con la cuenta en texto al lado («Llevás 12 de 50.») |
| Categorías completas | Desplegable propio | El plegable del catálogo (`.plegable-ver`, el de ORG-04), cerrado |
| Insignias | Sellos llenos y huecos | Igual, con el sello P06 |
| Cargando, error y sin permiso | Los de la ficha anterior | Estados de lista de P19 y aviso en la página de P13; los textos no cambian, salvo los de §6 |
| Acción llena | «Ver mi huerta» llega desde el inicio | En esta pantalla no hay acción llena: es de consulta. «Ver toda la huerta» es secundaria |

- **El estado no depende del color:** cada misión dice su estado en palabras, con el sello.
- **Sin JavaScript:** elegir un lugar es un enlace con `?lugar=`; el JavaScript de
  `static/js/paneles.js` solo suma tocar afuera y Esc.
- **Celular:** la huerta en una columna de lugares; la ficha, debajo. En la computadora, la ficha
  al lado de la huerta.

## 5. Estados

Los de la ficha anterior (§5): con datos, recién empieza (`?escenario=vacio`), cargando, error, sin
permiso (la empresa), racha cortada y todas completas. Ninguno nuevo.

## 6. Textos

Los de la ficha anterior (§6 y los del cierre de E6 en §9), con este cambio:

| Elemento | Antes | Ahora | Por qué |
|---|---|---|---|
| Entrada del menú | «Misiones», entre Organizaciones y Seguimiento | «Misiones», entre Organizaciones y Cómo donar | DA-03a y DA-03b |
| Sin permiso y el pie | «lo que donaron se ve en Mis donaciones» · «Ver Mis donaciones» | «lo que donaron se ve en Donaciones» · «Ver tus donaciones» | El menú de la guía dice «Donaciones» |

Filtro de tono: sin cambios.

## 7. Accesibilidad

La de la ficha anterior (§7), más: cada lugar de la huerta es un enlace con nombre («Gran Aporte,
en curso»), de 48 × 48 px como mínimo, que recibe el foco con el aro de la guía (P01); el dibujo es
decorativo (`aria-hidden`). `VerificacionVisualTest` mide esos enlaces sin excepciones nuevas.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Sello con texto y la cuenta en texto en la que está en curso | — |
| 2 | Lenguaje del usuario | «Llevás 12 de 50» sin unidad (Incentivos no la da) | Sin la unidad, la cuenta se entiende menos |
| 3 | Control y libertad | Ver toda la huerta, tocar afuera, Esc y Atrás | — |
| 4 | Consistencia | Sello, avance y plegable del catálogo, los mismos de ORG | La planta significa «misión» solo acá |
| 5 | Prevención de errores | No hay acciones | — |
| 6 | Reconocer antes que recordar | La ficha del lugar dice qué pide y cuánto falta | — |
| 7 | Flexibilidad | Huerta para el panorama, ficha para el detalle | — |
| 8 | Diseño minimalista | Una sola lámina protagonista y ninguna acción llena | — |
| 9 | Recuperarse de errores | El error dice que nada cambió y ofrece volver a cargar | — |
| 10 | Ayuda | El pie de la categoría actual dice cómo se pasa a la siguiente | — |

## 9. Decisiones y preguntas

- Decisiones del usuario que baja: DA-08 (la ficha), DA-03a y DA-03b (el menú), DA-05 (Cómo
  donar), DA-13 (se migra con la persona) y DA-21 (el patrón de 48 px).
- Decisiones de esta ficha (agente, para aprobar con la ficha): la planta es la misión completa
  solo en esta pantalla; la hoja de variante sale de la pantalla; sin acción llena.
- Pregunta (ronda 5, [§12 del backlog](../backlog-guia-al-codigo.md)), respondida por el usuario el 2026-11-23 con la recomendada (la primera):
  - **DON-P1 · La prueba A/B:** ¿se cierra con la huerta como la única Misiones? Recomendada: sí;
    `misiones-a` (el álbum) y su hoja dejan de servirse y se borran en el lote, con su mención en
    los comentarios. Otra: dejar `?variante=misiones-a` como está, sin migrarla a la guía.
