# Spec: R11-B04-kit-listado — Kit de listas de la opción A del harness de frontend

> **Estado:** implementada (2026-11-15, rama `E6_harness_frontend_kit`). Se escribió **después del
> código**, a partir del plan aprobado por el usuario el 2026-11-14 ("Sí, arrancá"), que vivía fuera
> del repo. Es un desvío de AGENTS.md §7.3: ahí la spec va antes de implementar. Ningún ADR se
> promueve.
> **Nivel:** ARCHITECTURAL. Implementa la opción A, que eligió el usuario (paso 2 del plan de migración
> del harness de frontend), y retira una dependencia del stack (Pico.css).
> **ADR:** uno nuevo, `proposed`: [sin Pico.css](../../adr/cliente-liviano/20261114-sin-pico-css-en-el-cliente-liviano.md).
> **Deuda:** [DTI-44](../../adr/DEUDA_TECNICA.md#dti-44).

## 1. Objetivo Funcional (Goal)

Que toda lista del cliente liviano con búsqueda o filtros use un solo componente,
`componentes/listado` (`vistas.Listado`), con un solo nombre de parámetro por campo. Además, que un
cerco del build impida volver a armar barras propias o sumar medidas sueltas. Es el requisito de
R11-B04 que se había perdido en la corrida R11 (síntoma 3 del diagnóstico del harness).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope
- **El componente de la línea base (iii):** el fragmento, su hoja, `vistas.Listado` y sus tests, traído
  por cherry-pick. A eso se suma el orden de Organizaciones de la condición (ii) y el aviso de carga
  (`Listado.Armado.indicador`, RNF-UI-06).
- **Las 9 listas que faltaban:** Organizaciones, Camiones, Registrar donación, Actividad,
  Importaciones y Donantes del admin, y Entidades, Entregas confirmadas y el Mapa públicos. También
  Propuestas y Destino de Asignaciones.
- **El cerco en `InterfazFitnessTest`:**
  - el formulario de búsqueda o filtros propio está prohibido en toda plantilla;
  - los nombres de parámetro salen de una lista cerrada;
  - el tope de medidas literales solo baja;
  - la primera capa es la base propia.
- **Pico.css sale del `pom.xml`:** la capa `pico` pasa a llamarse `base`.

### Out-of-Scope
- **Identidad visual:** no se reabre (decisión del usuario, 2026-11-14). No hay tokens nuevos de medida
  ni se tocan las 655 medidas sueltas: el tope las contiene.
- **Ranking del admin:** queda con su selector de mes (DTI-44, ítem 4).
- **Pasos 3 y 4 del plan:** las skills, las reglas de UI y el workflow van en PRs aparte.
- **Backend:** no hay cambios en los servicios. Los filtros que una API no resuelve no se ofrecen con
  esa fuente.

## 3. Decisiones Acordadas y Trade-offs

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Opción A, sistema propio cerrado (usuario) | B, stock con brief corto; C, librería tematizada | En la línea base solo A unificó los nombres y contuvo las medidas sueltas |
| Las públicas, dentro del fragmento (usuario) | Excepción cercada | Un solo componente en todas las listas |
| Sacar Pico (usuario) | Cargarlo; dejarlo declarado | Ver la matriz del ADR |
| Nombres viejos como alias en el controller | Cortar los nombres viejos | Los enlaces guardados siguen andando; el cerco impide dibujarlos |
| Cerco negativo (toda plantilla) | Lista de las pantallas migradas | Una pantalla nueva con su barra propia no pasa |
| Ranking como excepción registrada | Pasarlo al fragmento | Cambiaría «Volver al último mes» (R11-23) |

## 4. Invariantes

- Todo lo esencial anda sin JavaScript. El fragmento es un `form` GET con `action` real.
- Nada se envía solo al cambiar un campo (WCAG 3.2.2).
- Ningún test pierde aserciones. La que la línea base (iii) aflojó (el mes de Mis donaciones, que pasó
  a desde y hasta sin el nombre del campo) vuelve a atar cada valor a su campo; la otra que (iii)
  aflojó (el cerco de las skills, que pasaba a pedir "catálogo") no se trae. La que aflojó la
  migración de Organizaciones del admin (el total «· 55») vuelve a exigirse.
- Sin colores ni medidas literales nuevas fuera de `tokens.css`.

## 5. Criterios de Aceptación

- **Dado** cualquier plantilla fuera de `componentes/listado.html`, Ranking y su muestra, **cuando**
  tiene un `<form>` con `role="search"` o la clase `filtro`, `filtros` o `filtros-barra`, **entonces**
  `InterfazFitnessTest` falla.
- **Dado** un controller que arma una barra con un parámetro que no es común ni propio registrado,
  **cuando** corre el cerco, **entonces** falla.
- **Dado** el perfil maqueta, **cuando** se elige cada campo de la barra en cada lista, **entonces** la
  lista se filtra y aparece «Sacar los filtros». Lo prueban `ListadoEnLaMaquetaTest`,
  `ListadoEnAdminOperacionTest`, `ListadoEnAdminRegistrosTest` y `ListadoEnLasPublicasTest`.
- **Dado** un enlace con un nombre viejo (`?q=`, `?ver=`, `?pedidos=` o `?fecha=`), **cuando** se abre,
  **entonces** filtra igual que con el nombre nuevo.
- **Dado** Actividad o Importaciones, **cuando** HTMX trae la lista, **entonces** se ve «Buscando…».

## 7. Especificación Técnica de Diseño (Technical Design)

- **Two-Gate (AGENTS.md §9.1):**
  - El componente y su migración implementan la decisión ya tomada por el usuario: no hay Gate A, no
    lleva ADR.
  - Retirar Pico cambia el stack de ADR-E6-06: pasa Gate A y Gate B, con un ADR `proposed`.
- **Plan de tests:**
  - un test MockMvc por grupo de pantallas con el perfil maqueta, más los existentes ajustados sin
    debilitar aserciones;
  - casos inventados del cerco en `elCercoFallaConUnCasoInventadoYPasaConUnoSano`;
  - `-Pvisual` contra el piso registrado de la línea base.
- **Validación:**
  - `mvn clean test -pl cliente-liviano -am`;
  - `mvn test -pl cliente-liviano -am -Pvisual`;
  - `node scripts/agent-check.js`.
