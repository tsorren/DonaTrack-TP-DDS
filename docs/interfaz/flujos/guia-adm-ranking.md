# Flujo: Ranking de donantes del admin con la guía «Huerta y galpón» (adm1-ranking)

> **Estado:** aprobado · **Fecha:** 2026-11-24 · **Aprobó:** Delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md). El usuario no la revisó: las preguntas de §9 van con la opción recomendada
> Ficha del lote `adm1-ranking` de la ola ADM1 del
> [backlog de la guía al código](../backlog-guia-al-codigo.md) (§5 y §14). La pide DA-08 (Ranking
> no tiene arquetipo). DA-19 pedía escribirla en sesión con el usuario; en esta corrida la escribe
> el autor del manifiesto de ADM1 y la aprueba la delegación de arriba.
> Hereda de la ficha [El admin opera la plataforma](raices-admin-navegable.md) (§4.9) y de los
> cambios de R11-23 y R11-43 los datos, el criterio de orden, los estados y los textos. La base es
> la pantalla de hoy (`templates/admin-ranking.html`). Esta ficha dice solo **qué cambia**: cómo se
> ve en el galpón, con las piezas de la guía. Donde las dos fichas difieren, manda esta.

## 1. Para quién y para qué

| | |
|---|---|
| Quién | El equipo que administra (en la maqueta, Leo Gómez). Entra desde la computadora, de vez en cuando, al cerrar un mes |
| Tono | Galpón: sobrio y preciso; el criterio del orden va arriba de la tabla ([cifras y datos](../tono-y-comunicacion.md#cifras-y-datos)) |
| Qué quiere lograr | «Ver qué donantes completaron más misiones en un mes y con qué criterio se ordenó» |
| Dispositivo principal | Computadora primero; en el celular se lee entero |
| Requerimientos | RF-19, RF-CL-07 (cada cifra con su fecha) y RNF-CL-02 (sin JavaScript) |
| Datos que usa | Los mismos: `RankingPuerto.ranking(mes, pagina)` (Incentivos). Ningún dato nuevo; no toca adaptadores `rest` ni el puerto |
| Guía | Sin arquetipo (DA-08). Piezas: el armazón del galpón (P26) y el cajón (P22), la tabla (P20, con el encabezado del galpón desde 56 em), la barra de filtros de hoy (`componentes/filtros`), el paginador (P17), los estados de lista (P19) y el aviso en la página (P13) |

## 2. Dónde está en el sitio

Nivel 1 del admin, `/admin-ranking`, la última entrada del cajón: Por decidir, Donaciones,
Donantes, Organizaciones, Camiones y **Ranking** (DA-04, con el ícono `i-podio` de la guía). En el
celular, Ranking está en «Más». El menú lo cambia `adm1-galpon`.

## 3. Recorrido

Sin cambios respecto de la ficha anterior: se entra por el menú, se ve el último mes calculado y se
puede elegir otro mes en la pastilla «Mes»; con un mes que no es el último, «Volver al último mes».
El paginador recorre las páginas de meses (R11-43).

## 4. Pantallas

**Patrón conocido:** una tabla de clasificación (leaderboard) con un selector de período.

```text
┌─ cajón ─┬────────────────────────────────────────────────────────────┐
│ …       │ ADMINISTRACIÓN                                             │
│ Ranking │ Ranking de donantes de septiembre de 2026                  │
│         │ [Mes: septiembre de 2026 ▾]  Volver al último mes          │
│         │ Ordenado por la cantidad de misiones que cada donante      │
│         │ completó en el mes. Calculado el 30 de septiembre, 23:59.  │
│         │ El de octubre se calcula al cerrar el mes.                 │
│         │ ┌──────────────────────────────────────────────────────┐   │
│         │ │ Puesto │ Donante                  │ Misiones compl.  │   │
│         │ │ 1      │ Ana Ejemplo              │ 5                │   │
│         │ │ 2      │ Juan Ejemplo             │ 4                │   │
│         │ └──────────────────────────────────────────────────────┘   │
│         │ [paginador de meses]                                       │
└─────────┴────────────────────────────────────────────────────────────┘
```

**Qué cambia respecto de hoy:**

| Parte | Hoy | Con la guía |
|---|---|---|
| Armazón | La mesa de la huerta (`.mesa`, `.mesa-cabecera`) | El galpón: papel claro, título un escalón más chico y contenido denso (P26, de `adm1-galpon`). La cabecera es la del galpón, con el rótulo «Administración» y el título |
| Tabla | `.tabla` de la huerta | La misma pieza: desde 56 em, con el encabezado sobre el papel de la cabecera del galpón y la fila bajo el cursor; por debajo, se apila con sus rótulos. Sin selección: no hay nada que decidir (DA-28) |
| Primeros puestos | Sin marca | Igual: el puesto en número, sin color, dibujo ni podio (DA-28) |
| El mes | Pastilla «Mes» y «Volver al último mes» | Igual (DA-31), con las piezas del catálogo |
| Criterio y fecha | Un párrafo arriba de la tabla | Igual, en el texto denso del galpón |
| Vacío, cargando y error | `.vacio`, `.esqueleto` y `.banner` | Los estados de lista del catálogo (P19) y el aviso en la página con `data-tipo="error"` (P13), con los mismos textos |
| Acción llena | Ninguna | Ninguna: es una pantalla de consulta |

- **El estado no depende del color:** no hay estados por fila; el puesto es un número.
- **Sin JavaScript:** la pastilla «Mes» es un formulario GET; `hx-boost` solo evita recargar.
- **Celular:** la tabla apilada, cada puesto con su donante y sus misiones.
- **Lo que no cambia:** `admin-ranking.html` sigue en `BARRAS_EXCEPTUADAS` de
  `InterfazFitnessTest` (el mes es un selector de período, no una búsqueda).

## 5. Estados

Los de hoy: con datos, otro mes (`?mes=2026-08`), vacío (`?escenario=vacio`: todavía no hay ningún
ranking), cargando (`?escenario=lento`), error (`?escenario=error`) y un mes calculado sin donantes.
Ninguno nuevo.

## 6. Textos

Los de hoy, sin cambios. Filtro de tono: sin cambios.

## 7. Accesibilidad

La de hoy, más: la tabla lleva su `caption` y la primera celda de cada fila como `th scope="row"`
(el donante); el aro de foco de la guía (P01) en la pastilla y en el paginador; el toque de 48 px en
todos los controles, sin excepciones nuevas en `VerificacionVisualTest`.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | El mes en el título y la fecha del cálculo arriba de la tabla | — |
| 2 | Lenguaje del usuario | «Misiones completadas», «Volver al último mes» | — |
| 3 | Control y libertad | Volver al último mes; el paginador de meses | — |
| 4 | Consistencia | La tabla y la barra de filtros del catálogo, las mismas de Donaciones y Donantes | — |
| 5 | Prevención de errores | No hay acciones | — |
| 6 | Reconocer antes que recordar | El criterio va arriba de la tabla | — |
| 7 | Flexibilidad | Elegir el mes; el paginador para los meses viejos | Incentivos no da el tipo de donante ni períodos de más de un mes |
| 8 | Diseño minimalista | Sin podio ni destacados: una sola tabla | — |
| 9 | Recuperarse de errores | El error dice que es un problema nuestro y ofrece volver a cargar | — |
| 10 | Ayuda | El párrafo del criterio | — |

## 9. Decisiones y preguntas

- Decisiones que baja: DA-08 (la ficha), DA-04 (Ranking en el cajón) y DA-18 (el galpón).
- Decisiones de esta ficha (agente): la pantalla no tiene acción llena; los textos no cambian.
- Preguntas, resueltas con la opción recomendada. Nota del OK: «Delegada por el relevamiento del
  2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md)». Constan en
  [§2.2 del backlog](../backlog-guia-al-codigo.md):
  - **DA-28 · Los primeros puestos.** Recomendada (la que vale): la tabla del catálogo con el
    encabezado del galpón, sin selección, y el puesto en número, sin color, dibujo ni podio para
    los tres primeros. Otra: un podio dibujado con los tres primeros; es una pieza que el catálogo
    no tiene y el tono pide no comparar donantes más allá del criterio.
  - **DA-31 · Cómo se elige el mes.** Recomendada (la que vale): como hoy, la pastilla «Mes» de
    `componentes/filtros`, «Volver al último mes» y el paginador de meses. Otra: pestañas por mes;
    A03 usa pestañas para lo que hay por decidir y doce meses no entran a 360 px.
