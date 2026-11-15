# Sin Pico.css en el cliente liviano: la base es CSS propio con tokens

- Status: proposed
- Date: 2026-11-14
- Deciders: el usuario (2026-11-14, al aprobar el plan del kit de la opción A del harness de
  frontend: "Sacarlo del pom"); este registro, el agente que implementa el kit. Pendiente de
  aprobación del equipo en el PR
- Tags: cliente-liviano, interfaz, estilos, pico-css, tokens, entrega-6

> `[OBSERVED]` El código ya funcionaba sin Pico antes de esta decisión: ninguna plantilla carga su
> hoja desde el commit `3a821691a` (2026-11-03, "una sola identidad"). Este ADR registra esa
> situación y retira la dependencia que quedó declarada.

## Contexto y Problema

[ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md), con la
decisión D19, eligió Pico.css como base, tematizado por sus variables `--pico-*`, más CSS propio.
El fundamento era este: "con IA, menos CSS y JS propio es menos deriva".

Desde que Raíces pasó a ser la única identidad (2026-11-03), los layouts no cargan la hoja de Pico.
Pasó esto:
- `[OBSERVED]` El WebJar `picocss__pico` 2.1.1 sigue en `cliente-liviano/pom.xml`.
- `[OBSERVED]` `app.css` declara una capa `pico` "para cuando se cargue". En realidad la usa para las
  reglas base propias: el color de los enlaces, el anillo de foco y el título de sección de
  `privado.css`.
- `[OBSERVED]` `tokens.css` no asigna ninguna variable `--pico-*`.
- `[DOCUMENTED]` El diagnóstico del harness de frontend (2026-11-14, C1.5) la marcó como "base
  declarada que no es la real".

La pregunta es qué hacer con una base que el stack declara y nadie carga, ahora que la identidad
no se reabre.

## Atributos de Calidad y Drivers de Decisión

- Una sola fuente de verdad del sistema visual: lo declarado tiene que ser lo que se ve.
- No cambiar el aspecto del sitio: la identidad no se reabre (decisión del usuario, 2026-11-14).
- Superficie de dependencias mínima: sin WebJars que no se sirven.
- Reversibilidad: volver a Pico tiene que seguir siendo posible.

## Relaciones Arquitectónicas

1. **Reemplaza en parte a:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`), solo en D19 y en el WebJar de Pico. D17, D18, D20 y D21 siguen como están.
2. **Se apoya en:** [una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md)
   (`proposed`), que es la que dejó de cargar Pico.
3. **Reemplaza en parte a:** [interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
   (`proposed`, D32 y D33), solo en el nombre de la primera capa: el orden que fija
   (`@layer pico, tokens, …`) pasa a `@layer base, tokens, …`. Las capas CUBE siguen igual.

## Alternativas Consideradas

| Opción | Qué implica |
|---|---|
| **Sacar Pico del `pom.xml`** | Se borra el WebJar. La capa `pico` pasa a llamarse `base`, en el mismo lugar, así que la cascada no cambia. D19 queda como "CSS propio con tokens" |
| Cargar Pico en la capa `pico` | La hoja de Pico vuelve a todas las pantallas, debajo del CSS propio |
| Dejarlo como está | El WebJar sigue declarado y sin uso. Se anota como deuda |

Matriz, con puntaje de 1 a 5 y pesos que suman 100:

| Criterio | Peso | Sacar | Cargar | Dejar |
|---|---|---|---|---|
| Lo declarado coincide con lo que se ve | 30 | 5 | 5 | 1 |
| No cambia el aspecto del sitio | 30 | 5 | 1 | 5 |
| Superficie de dependencias | 20 | 5 | 2 | 2 |
| Costo de revertir | 20 | 4 | 3 | 5 |
| **Total** | 100 | **480** | 290 | 330 |

Descartadas:
- **Cargar Pico:** sus estilos de elementos (`button`, `input`, `table`, `nav`) cambiarían todas
  las pantallas. Eso reabre la identidad, que el usuario dejó cerrada.
- **Dejarlo:** mantiene la contradicción que el diagnóstico pidió resolver, a cambio de no tocar
  nada.

## Resultado de la Decisión

Se elige **sacar Pico**:
- El `pom.xml` del cliente liviano deja de declarar `picocss__pico` y su versión.
- En `app.css`, el orden de capas pasa a ser
  `@layer base, tokens, composicion, utilidades, bloques, excepciones, pantallas;`.
- Las reglas que estaban en `@layer pico` (`app.css` y `privado.css`) pasan a `@layer base`, sin
  cambiar su contenido.
- D19 se lee así: estilos con **CSS propio**, solo con tokens de uso. El cerco de `InterfazFitnessTest`
  sigue prohibiendo `--pico-*` fuera de `tokens.css`, por si alguien vuelve a declararlas.

## Consecuencias

### Positivas
- El stack declarado (`cliente-liviano/AGENTS.md`, README del módulo) coincide con lo que se sirve.
- Un WebJar menos para actualizar.
- El sitio no cambia de aspecto: la capa cambia de nombre, no de lugar.

### Negativas
- Se pierde el argumento de D19 ("menos CSS propio"). Todo el estilo es propio y lo sostienen el
  catálogo, los tokens y los cercos.
- Volver a Pico pide un ADR nuevo y una revisión visual de todas las pantallas.

## Análisis por alternativa

- **Sacar:** cuesta 3 archivos de código y la documentación del stack. Se revierte con un commit:
  se vuelve a declarar el WebJar y se carga en la capa `base`.
- **Cargar:** cuesta una pasada visual completa, con 39 fallas de toque ya registradas como piso, y
  probablemente arreglos en muchas hojas.
- **Dejar:** no cuesta nada hoy, pero el próximo agente vuelve a leer "Pico.css como base" en el
  stack y en la skill `vista-ui`.

## Validación

- `mvn clean test -pl cliente-liviano -am` en verde. `InterfazFitnessTest` comprueba el orden de las
  capas declarado en `app.css`.
- `mvn test -pl cliente-liviano -Pvisual` sin fallas nuevas respecto del piso registrado. El aspecto
  no cambia.
- `git grep -i picocss` sin resultados en el código.
