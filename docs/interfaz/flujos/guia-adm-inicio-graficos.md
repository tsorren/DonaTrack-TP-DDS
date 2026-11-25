# Flujo: Debajo de «Por decidir», el resumen y los gráficos del inicio del admin (adm1-por-decidir)

> **Estado:** aprobado · **Fecha:** 2026-11-24 · **Aprobó:** Delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md). El usuario no la revisó: las preguntas de §9 van con la opción recomendada
> Ficha del lote `adm1-por-decidir` de la ola ADM1 del
> [backlog de la guía al código](../backlog-guia-al-codigo.md) (§5 y §14). La pide DA-04: «Por
> decidir» reemplaza a Inicio y a Asignar, y los gráficos de «Cómo está todo» de `admin-inicio` van
> a una ficha de ADM1, debajo de la bandeja. La bandeja es A03 directo
> (`docs/interfaz/guia/partes/50-pantallas.html`, `#bandeja`, desde la línea 163) y no la cubre
> esta ficha, salvo en cómo se arma con lo de abajo (§4). DA-19 pedía escribirla en sesión con el
> usuario; en esta corrida la escribe el autor del manifiesto de ADM1 y la aprueba la delegación de
> arriba.
> Hereda de la ficha [El admin en Raíces](raices-entidad-navegable.md) (§4.4, «El inicio del admin,
> por temas», con R11-21 y los gráficos de R11-42) los datos, los estados y los textos. Esta ficha
> dice solo **qué cambia**. Donde las dos fichas difieren, manda esta.

## 1. Para quién y para qué

| | |
|---|---|
| Quién | El equipo que administra (en la maqueta, Leo Gómez). Entra todos los días, desde la computadora |
| Tono | Galpón: sobrio y preciso; cada cifra con su fecha y su nombre ([cifras y datos](../tono-y-comunicacion.md#cifras-y-datos)) |
| Qué quiere lograr | Arriba, «decidir lo que espera»; debajo, «ver de un vistazo cómo está todo y cómo vienen los últimos 12 meses» |
| Dispositivo principal | Computadora primero; en el celular se lee entero |
| Requerimientos | RF-16, RF-17, RF-18 y RF-21 (resumidos), RF-CL-07 (cada cifra con su fecha) y RNF-CL-02 (sin JavaScript) |
| Datos que usa | Los mismos: `AdminPersonasPuerto.inicio()` (`AdminInicioVista`: decisiones, cifras y evolución). Para la tabla de propuestas de la bandeja, `AdminOperacionPuerto.asignaciones(…)`, que ya existe. Ningún dato nuevo; no toca adaptadores `rest` ni puertos |
| Guía | La bandeja, A03 directo. Lo de abajo, sin arquetipo (DA-08). Piezas: la lista de datos (P23), la tabla (P20), los estados de lista (P19), el aviso en la página (P13) y el armazón del galpón (P26) |

## 2. Dónde está en el sitio

Nivel 1 del admin, `/admin-inicio`, la primera entrada del cajón: **Por decidir**, Donaciones,
Donantes, Organizaciones, Camiones y Ranking (DA-04). Es donde cae el admin al ingresar. La entrada
queda marcada también en `/admin-asignaciones` y sus respuestas (DA-25).

## 3. Recorrido

Arriba, la bandeja de A03: la cabecera «Por decidir», las pestañas con su cuenta y la pestaña
Propuestas abierta (DA-25). Debajo, en este orden, el resumen «Cómo está todo» y los dos gráficos.
Cada cifra del resumen con una salida lleva a su lista (por ejemplo, «En el depósito, sin asignar»
a `/admin-donaciones?estado=deposito`), como hoy.

## 4. Pantallas

**Patrón conocido:** una mesa de trabajo con un panel de indicadores debajo (dashboard de
operación).

```text
┌─ cajón ──────┬────────────────────────────────────────────────────────────┐
│ Por decidir 9│ Por decidir                     [Correr la asignación]      │
│ Donaciones   │ Jueves 15 de octubre, 18:30.    [Registrar donación]        │
│ …            │ Propuestas 4 · Conflictos 2 · Solicitudes 3 · Por vencer 4  │
│              │ [2 elegidas, para 2 organizaciones] [Descartar] [Aprobar…] │
│              │ [tabla con selección: las propuestas sin conflicto]         │
│              │ ─────────────────────────────────────────────────────────── │
│              │ Cómo está todo · Al 15 de octubre de 2026, 18:30.           │
│              │  En el depósito, sin asignar ........ 7   Ver ›             │
│              │  Asignadas, por salir ............... 3   Ver ›             │
│              │  En camino o en la puerta ........... 2   Ver ›             │
│              │  No se pudo entregar ................ 1                     │
│              │  Vencidas en el depósito ............ 1   Ver ›             │
│              │  Entregas confirmadas en octubre .... 9 (1.161 en total)    │
│              │  La red ...... 24 organizaciones · 310 de 412 donantes      │
│              │  Camiones ............... 1 en ruta, 3 libres  Ver ›        │
│              │ ─────────────────────────────────────────────────────────── │
│              │ Mes a mes · de noviembre de 2025 a octubre de 2026          │
│              │ [columnas]                       [tabla: mes, reg., entr.]  │
│              │ Por categoría · de noviembre de 2025 a octubre de 2026      │
│              │ [barras]                         [tabla: categoría, …]      │
└──────────────┴────────────────────────────────────────────────────────────┘
```

**Qué cambia respecto de hoy:**

| Parte | Hoy | Con la guía |
|---|---|---|
| Arriba | «Hola, Leo» y «Lo que espera tu decisión», una tarjeta por tema | La bandeja de A03 (DA-25): las tarjetas pasan a las pestañas con su cuenta (Propuestas, Conflictos, Solicitudes y Por vencer) |
| «Cómo está todo» | Cuatro tarjetas (Donaciones por estado, Entregas confirmadas, La red y Camiones), cada una con su período | Un solo resumen denso con la lista de datos del catálogo (P23) y su fecha arriba; las mismas cifras, en el mismo orden, con las mismas salidas «Ver ›» (DA-29) |
| «Entregas para mirar hoy» de A03 | No existe | No se dibuja: `AdminInicioVista` trae cuentas, no entregas. Se anota como deuda del lote (DA-29) |
| Camiones sin Logística | Su tarjeta con un aviso | Su fila del resumen con el aviso en la página (P13), con el texto de hoy; el resto se ve igual |
| Los gráficos | «Mes a mes» y «Por categoría», cada uno a todo el ancho, con su tabla al lado (debajo en el celular) | Igual, como parte de la pantalla, en `admin-inicio.css` (DA-30). Sus reglas usan los nombres de la guía con el mismo valor: `--color-accion-primaria` por `--color-acento`, `--color-salvia` por `--color-acento-claro` y `--color-texto-suave` por `--color-tinta-suave`. Ningún píxel cambia |
| Las tablas de los gráficos | Una `table` propia | La tabla del catálogo (P20), con el encabezado del galpón desde 56 em |
| Tarjetas | `.tarjeta registro-mesa` | Salen del resumen; cada gráfico va en una lámina común (P07), sin protagonista: la única acción llena de la pantalla es «Aprobar» de la bandeja |
| Acción llena | «Ver las propuestas» y otras, en tarjetas | Una sola, en la bandeja: «Aprobar las N» (A03) |

- **El estado no depende del color:** cada serie de los gráficos tiene su leyenda en texto, el mes
  en curso va punteado y dice «en curso», y la tabla dice lo mismo que el dibujo.
- **Sin JavaScript:** el dibujo es un SVG armado en el servidor y oculto al lector; la tabla es lo
  esencial.
- **Celular:** el resumen en una columna, cada dato con su rótulo arriba; cada gráfico con su tabla
  debajo.

## 5. Estados

Los de hoy: con datos, nada que decidir (`?escenario=vacio`: la bandeja con su vacío y el resto
igual), cargando (`?escenario=lento`), error (`?escenario=error`), sin Logística
(`?escenario=error-camiones`), sin gráficos (`?escenario=error-evolucion`), plataforma nueva
(`?escenario=evolucion-corta`) y todavía sin donaciones (`?escenario=evolucion-vacia`). Ninguno
nuevo. Cargando y error usan los estados de lista del catálogo (P19) y el aviso en la página (P13).

## 6. Textos

Los de hoy, con estos cambios:

| Elemento | Antes | Ahora | Por qué |
|---|---|---|---|
| Título de la pantalla | «Hola, Leo» | «Por decidir» | A03; DA-04 |
| Título de la sección de decisiones | «Lo que espera tu decisión» | Las pestañas de A03 | A03; DA-25 |
| Título del resumen | «Cómo está todo» y una tarjeta por tema | «Cómo está todo», un solo resumen | DA-29 |

Filtro de tono: sin cambios.

## 7. Accesibilidad

La de hoy, más: el resumen es un `dl` (rótulo y dato), con la fecha antes; cada salida «Ver ›»
lleva su `aria-label` de hoy («Ver las donaciones en el depósito») y mide 48 px de alto; las tablas
de los gráficos llevan `caption` y su fila de total en `tfoot`. Sin excepciones nuevas en
`VerificacionVisualTest`.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | La fecha arriba del resumen; el período en cada gráfico | — |
| 2 | Lenguaje del usuario | Los nombres de las cinco cuentas de donaciones, los de la lista del admin | — |
| 3 | Control y libertad | Cada salida lleva a su lista filtrada; Atrás vuelve a la bandeja | — |
| 4 | Consistencia | La lista de datos y la tabla del catálogo, las mismas del resto del galpón | Los gráficos son propios de esta pantalla (DA-30) |
| 5 | Prevención de errores | Lo de abajo no tiene acciones | — |
| 6 | Reconocer antes que recordar | Cada cifra con su nombre y su salida | — |
| 7 | Flexibilidad | La bandeja para decidir; el resumen y los gráficos para mirar | — |
| 8 | Diseño minimalista | Un solo resumen en lugar de cuatro tarjetas | Sin «Entregas para mirar hoy» falta lo que A03 dibuja (DA-29) |
| 9 | Recuperarse de errores | Si falta Logística o la evolución, solo esa parte muestra su aviso | — |
| 10 | Ayuda | La nota debajo de «Mes a mes» explica en qué mes cuenta una entrega | — |

## 9. Decisiones y preguntas

- Decisiones que baja: DA-04 (Por decidir y los gráficos debajo), DA-08 (la ficha), DA-18 (el
  galpón) y DA-25 (las dos rutas de Por decidir).
- Decisiones de esta ficha (agente): el orden de abajo (primero el resumen, después los gráficos);
  la única acción llena es la de la bandeja.
- Preguntas, resueltas con la opción recomendada. Nota del OK: «Delegada por el relevamiento del
  2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md)». Constan en
  [§2.2 del backlog](../backlog-guia-al-codigo.md):
  - **DA-29 · Las cuatro tarjetas y «Entregas para mirar hoy».** Recomendada (la que vale): las
    tarjetas pasan a un solo resumen denso con la lista de datos del catálogo, con las mismas
    cifras y salidas, y «Entregas para mirar hoy» no se dibuja porque no hay datos; se anota como
    deuda. Otras: conservar las cuatro tarjetas (la densidad de la huerta en el galpón); armar
    «Entregas para mirar hoy» con datos de la maqueta (en el sitio real quedaría vacía).
  - **DA-30 · Dónde viven los gráficos.** Recomendada (la que vale): como parte de `admin-inicio`,
    en `admin-inicio.css`, con su tabla al lado y los nombres de token de la guía del mismo valor.
    Otra: una pieza nueva del catálogo con su muestra en `/estilos`; la usa una sola pantalla y
    pediría un lote de catálogo en la tanda 1.
