# Tercera alternativa de identidad (Raíces) como prototipo propio, sin juego de plantillas hasta que se elija

- Status: superseded by cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano (href: ./20261103-una-sola-identidad-en-el-cliente-liviano.md)
- Date: 2026-11-01
- Deciders: tsorren, al elegir entre dos opciones de convivencia en la sesión de Raíces
- Tags: cliente-liviano, interfaz, identidad, tokens, prototipo, entrega-6

> **Origen:** el [brief de Raíces](../../interfaz/identidad/brief-raices.md) registraba este ADR como
> pendiente antes de construir Raíces como alternativa: con ella serían tres identidades, y con la regla del
> [ADR de dos alternativas](./20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)
> cada pantalla nueva se escribiría tres veces ([bitácora](../../interfaz/bitacora.md),
> 2026-10-31, "Una tercera alternativa, diseñada con el usuario por A/B"). Es un ADR nuevo y no una
> edición de aquel, porque aquel ya está publicado en la rama del PR 924.

## Contexto y Problema

El [ADR de dos alternativas](./20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)
(`proposed`) fija que cada alternativa tiene su juego completo de plantillas
(`templates/alternativas/<nombre>/`) y sus hojas (`static/css/alternativas/<nombre>/`), con los
mismos datos y acciones vigilados por test. Hoy hay dos: Glassmorphism y Cozy / Medieval.

Raíces es una tercera identidad (raíces y semillas, para las empresas que donan y quieren
transparencia), en exploración con el usuario por comparaciones A/B. Estado actual:

- `[OBSERVED]` Su única pantalla es la portada, armada como variante de estructura:
  `templates/variantes/concepto-b.html`, `static/css/variantes/concepto-b.css` y sus valores base
  `--p-*` en `static/css/variantes/comun/raices.css`. La portada usa esos valores base directamente
  (141 usos: 121 en `concepto-b.css` y 20 en `comun/raices.css`): tiene un solo nivel de tokens.
- `[OBSERVED]` Las variantes son temporales: se borran al decidir (`cliente-liviano/AGENTS.md`,
  Estructura). Raíces no puede quedarse ahí.
- `[OBSERVED]` El juego completo de una alternativa son hoy 18 plantillas: `layout.html`,
  `layout-privado.html`, 14 fragmentos y las 2 páginas del catálogo `/estilos`. Las pantallas del
  sitio todavía no existen como plantillas del sistema: son el prototipo navegable de Cozy
  (`templates/prototipo/cozy-medieval/`, servido por `PrototipoController`), que no tiene gemelas.
- `[OBSERVED]` `InterfazFitnessTest.cadaAlternativaTieneSuJuegoCompletoDePlantillas` exige que
  toda carpeta de `static/css/alternativas/` con su `tokens.css` tenga su juego en
  `templates/alternativas/`. Poner a Raíces ahí obliga a escribir las 18 plantillas antes de dibujar
  cualquier pantalla.

Problema: ¿cómo vive una tercera alternativa que todavía se explora, sin triplicar el trabajo de
cada pantalla y sin dejarla en un lugar temporal? Son cuatro preguntas:

1. **Dónde vive:** ¿juego completo como las otras dos, o algo más chico?
2. **Cómo se elige en la maqueta:** ¿con qué dirección se ve una pantalla de Raíces?
3. **Tokens:** ¿rige D20 (dos niveles de tokens) dentro de Raíces?
4. **Salida:** ¿qué pasa si el equipo la elige o la descarta?

## Atributos de Calidad y Drivers de Decisión

Los mismos pesos del ADR de dos alternativas, para comparar con aquella decisión: las otras dos
alternativas intactas 30 · libertad de diseño de Raíces 20 · comparar sobre las mismas pantallas 20 ·
costo de mantener cada pantalla 20 · reversibilidad 10. Requisito que decide si una opción sirve:
Raíces queda en un lugar estable, no temporal.

## Relaciones Arquitectónicas

1. **Refina:** [dos alternativas de identidad visual](./20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)
   (`proposed`). No lo reemplaza: Glassmorphism y Cozy siguen como están. Agrega una etapa previa al
   juego completo, el prototipo propio, para una alternativa que todavía no se eligió.
2. **Respeta:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`), decisión D20: dos niveles de tokens dentro de cada alternativa, también en Raíces.
3. **Complementa:** [verificación visual con Playwright y axe](./20261030-verificacion-visual-con-playwright-y-axe.md)
   (`proposed`): las pantallas de Raíces entran en la lista de páginas que recorre.

## Alternativas Consideradas

Puntajes de 1 a 5; son estimaciones del agente (`[INFERRED]`).

### Dónde vive

| Alternativa | Lugar estable | Las otras dos intactas | Libertad | Comparar | Costo por pantalla | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| R1. Tercer juego completo ya (`templates/alternativas/raices/` y `static/css/alternativas/raices/`), más su prototipo | Sí | 5 | 5 | 5 | 1 | 4 | 4,1 |
| **R2. Prototipo propio: sus pantallas y hojas en una carpeta de prototipo; el juego completo, solo si el equipo la elige** | Sí | 5 | 5 | 3 | 4 | 5 | **4,4** |
| R3. Seguir como variantes de estructura (lo que hay) | No | 5 | 4 | 3 | 4 | 4 | 4,1, descartada por el requisito |
| R4. Reemplazar a Cozy o a Glassmorphism | Sí | — | — | — | — | — | `[DOCUMENTED]` descartada por el usuario el 2026-10-31, cuando eligió una tercera alternativa (bitácora) |
| R5. Una rama propia | Sí | — | — | — | — | — | `[REJECTED]` en el ADR de dos alternativas (opción C) |

El agente recomendó R2 y el usuario la eligió.

### Cómo se elige en la maqueta

| Alternativa | Las otras dos intactas | Se mantiene al navegar | Costo | **Propuesta** |
|---|:-:|:-:|:-:|:-:|
| E1. `?alternativa=raices` | No: el interceptor solo acepta carpetas de `static/css/alternativas/`, que obligan al juego completo | No: los enlaces internos pierden el parámetro | Bajo | |
| E2. `?prototipo=raices` | Sí | No, por el mismo motivo | Bajo | |
| **E3. Una dirección propia: `/prototipo/raices/<pantalla>`** | Sí | Sí: el menú y los enlaces de Raíces apuntan a su dirección | Bajo | **Sí** |

Decisión del agente (`[PROPOSED]`): E3 es la única que no pierde la alternativa al pasar de una
pantalla a otra, el problema que el ADR anterior dejó abierto para `?alternativa=`.

### Tokens

| Alternativa | Respeta D20 | Costo ahora | **Propuesta** |
|---|:-:|:-:|:-:|
| **K1. Dos niveles, como las otras: valores base `--p-*` solo en el `tokens.css` de Raíces; pantallas solo con tokens de uso** | Sí | Medio: la portada pasa sus 141 usos de `--p-*` a tokens de uso | **Sí** |
| K2. Un nivel mientras sea prototipo: las hojas de Raíces usan `--p-*` directo | No | Nulo | |

Decisión del agente (`[PROPOSED]`): K2 abriría en el test de reglas de interfaz una excepción sin
fecha de vencimiento, y esas reglas son un quality gate que no se debilita (AGENTS.md §4.3). Con
K1, el día que Raíces se elija sus hojas ya cumplen las reglas de una alternativa.

## Resultado de la Decisión

Se elige **R2** (usuario), con **E3** y **K1** (propuestas del agente, sin elegir por el usuario):

- **Carpetas.** Las pantallas de Raíces viven en `templates/prototipo/raices/<pantalla>.html`, con
  los mismos nombres que las del prototipo de Cozy (`publico-portada`, `donante-seguimiento`). Sus
  piezas propias (la estructura de la página pública y la privada, la navegación) van en la misma
  carpeta y no se apoyan en las plantillas de Glassmorphism ni en las de Cozy. Sus hojas viven en
  `static/css/prototipo/raices/`: `tokens.css` (valores base y tokens de uso) y las hojas de sus
  pantallas. Sus imágenes, en `static/img/raices/` (ya existe).
- **Sus hojas no van en `static/css/alternativas/`.** `[OBSERVED]` Las del prototipo de Cozy sí
  están dentro de su alternativa (`static/css/alternativas/cozy-medieval/prototipo/`), porque Cozy
  tiene juego. Raíces no puede seguir esa convención: una carpeta suya en `alternativas/` con su
  `tokens.css` obliga al juego completo (regla de abajo). Por eso usa `static/css/prototipo/raices/`.
  Ahí van su `tokens.css`, su `app.css` (las piezas comunes de sus pantallas: marca, botones, sellos,
  grano de impresión y cifras, lo que antes estaba en `variantes/comun/raices.css`) y una hoja por
  pantalla.
- **Sin gemelas ni tabla de voz.** Como el prototipo de Cozy, las pantallas de Raíces no son
  pantallas del sistema: no tienen gemela y no tienen tabla de voz. Siguen el
  [tono común](../../interfaz/tono-y-comunicacion.md) y muestran los mismos datos de ejemplo que la
  pantalla del mismo nombre del prototipo de Cozy, para compararlas. Si la de Cozy toma sus datos
  de un fragmento de datos (como `prototipo/cozy-medieval/viaje-datos.html`), la de Raíces usa el
  mismo fragmento, sin copiarlo. Lo demás lo vigila la revisión (`revision-ui`), no un test.
- **Dirección.** En el perfil `maqueta`, `PrototipoController` sirve `/prototipo/raices/<pantalla>`
  desde `templates/prototipo/raices/`, con su propio menú por rol, cuyos enlaces llevan el mismo
  prefijo. Una sección del menú que Raíces todavía no dibujó muestra su `sin-dibujar.html` si lo
  tiene, y si no, no existe (404). `/prototipo/<pantalla>` sigue mostrando Cozy, sin cambios. `?variante=` de las
  comparaciones A/B sigue funcionando sobre las pantallas de Raíces.
- **Reglas de estilo, sin cambiar el test.** `[OBSERVED]` `InterfazFitnessTest` ya recorre todo
  `static/css/`: aplica a cada `tokens.css` el contraste AA de los pares de tokens de uso y pide
  sus puntos de quiebre (`paresDeTokensCumplenContrasteAA`, `mediaQueriesUsanSoloLosQuiebresDeTokens`),
  y a toda otra hoja, solo tokens de uso y ningún color literal
  (`estilosUsanSoloTokensDeUso`, `soloTokensCssDefineColores`); la única excepción son las hojas de
  `variantes/`, que pueden usar y redefinir valores base `--p-*`. La regla de juego completo solo
  cuenta las carpetas de `static/css/alternativas/`, así que no alcanza a `prototipo/raices/`. Por
  eso el `tokens.css` de Raíces declara los mismos tokens de uso de los pares de contraste que las
  otras dos, y sus `--medida-quiebre-*`.
- **La portada se muda.** La variante `concepto-b` y su base `variantes/comun/raices.css` pasan a
  `templates/prototipo/raices/publico-portada.html` y a `static/css/prototipo/raices/`, con sus
  valores base convertidos a tokens de uso. Se ve igual: lo comprueba la comparación píxel a píxel
  de las capturas de antes y después.
- **Verificación visual.** Las pantallas de Raíces se agregan a
  `src/test/resources/verificacion/paginas.txt`, con sus estados.
- **Excepción a "cada pantalla en cada alternativa".** La regla de
  [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md) (cada pantalla o componente nuevo
  se escribe en cada alternativa) rige para las plantillas del sistema (`templates/` y los juegos de
  `templates/alternativas/`). Raíces, sin juego, queda afuera hasta que se elija. Al implementarse
  este ADR, ese archivo suma a su tabla de Estructura las carpetas `templates/prototipo/raices/` y
  `static/css/prototipo/raices/`, y aclara la excepción en sus reglas de estilos.
- **Salida.** Si el equipo elige Raíces como alternativa, rige el ADR de dos alternativas: se arma su
  juego completo en `templates/alternativas/raices/` y `static/css/alternativas/raices/`, con su
  tabla de voz, y el test de juego completo lo exige desde ese momento. Si la descarta, se borran
  `templates/prototipo/raices/`, `static/css/prototipo/raices/` y `static/img/raices/`.

## Consecuencias

### Positivas

- Glassmorphism y Cozy no cambian: ni sus plantillas, ni sus hojas, ni su dirección.
- Raíces se puede seguir diseñando pantalla por pantalla sin escribir antes 18 plantillas que
  todavía nadie eligió.
- Al navegar el prototipo de Raíces no se pierde la alternativa: cada enlace lleva su prefijo.
- Raíces queda en un lugar estable, con las mismas reglas de tokens que las otras dos.
- Descartarla es borrar tres carpetas.

### Negativas

- **El catálogo `/estilos` no tiene versión Raíces.** Sus componentes (sellos de estado, etiquetas,
  botones) se ven solo dentro de sus pantallas, y comparar componente por componente con las otras
  dos no se puede hasta que tenga juego.
- **Los datos de ejemplo no tienen test.** Cuando la pantalla de Cozy escribe sus datos en la
  propia plantilla (y no en un fragmento de datos), la de Raíces los copia, y que muestren lo mismo
  depende de la revisión. Una diferencia puede pasar sin aviso.
- **El prototipo crece en dos carpetas.** Una pantalla que se quiera ver en las dos identidades se
  dibuja en `prototipo/cozy-medieval/` y en `prototipo/raices/`.
- **Mudar la portada cuesta ahora:** convertir 141 usos de valores base a tokens de uso, con el
  riesgo de un cambio visual que tiene que atrapar la comparación de capturas.
- **Los puntos de quiebre se suman entre todos.** `[OBSERVED]`
  `mediaQueriesUsanSoloLosQuiebresDeTokens` junta en un solo conjunto los `--medida-quiebre-*` de
  todos los `tokens.css`. Si Raíces declara un quiebre que las otras no tienen, las hojas de las
  otras dos también pueden usarlo sin que el test lo note.
- **Si Raíces se elige, el trabajo de juego queda pendiente:** las 18 plantillas, la tabla de voz y
  pasar sus pantallas al sistema siguen sin hacerse. Este ADR lo posterga, no lo ahorra.
- `PrototipoController` suma una segunda carpeta y un segundo prefijo de direcciones: más casos que
  probar en un adaptador de maqueta.

## Análisis por alternativa

Dónde vive:

- **R1. Juego completo ya:** deja a Raíces al mismo nivel que Cozy y permite comparar el catálogo,
  pero cada plantilla y componente nuevo se escribe tres veces desde hoy, para una alternativa que
  puede descartarse.
- **R2. Prototipo propio (elegida):** el costo crece solo con las pantallas que se dibujan, y la
  salida queda clara en los dos sentidos. Compara peor: sin catálogo y sin test de datos.
- **R3. Variantes de estructura:** cero trabajo nuevo, pero las variantes se borran al decidir y
  usan los valores base sin reglas; Raíces quedaría en un lugar pensado para durar una comparación.
- **R4. Reemplazar a una de las dos:** el usuario la descartó al empezar Raíces; quiere comparar
  las tres.
- **R5. Rama propia:** ya descartada para Cozy: las alternativas no se pueden ver juntas.

Cómo se elige:

- **E1. `?alternativa=raices`:** reutiliza el mecanismo de Cozy, pero obliga al juego completo y se
  pierde al navegar.
- **E2. `?prototipo=raices`:** no toca el interceptor, pero se pierde al navegar.
- **E3. Dirección propia (propuesta):** un prefijo más en el controlador del prototipo, a cambio de
  navegar sin perder la alternativa.

Tokens:

- **K1. Dos niveles (propuesta):** respeta D20 y deja las hojas listas para un juego, con el costo
  de mudar la portada.
- **K2. Un nivel:** gratis hoy, pero relaja una regla de calidad sin fecha y deja el costo entero
  para el día que se elija.

## Validación

- `[VERIFIED]` 2026-11-01: `mvn clean test -pl cliente-liviano -am`, 38 tests en verde (3 nuevos
  de `PrototipoControllerTest`: la pantalla de Raíces con su menú en `/prototipo/raices/`, lo que
  Raíces no dibujó da 404 y un formulario vuelve a Raíces; los de Cozy, sin cambios). El primer
  intento falló porque al `tokens.css` de Raíces le faltaba `--color-foco`, que piden los pares de
  contraste.
- `[VERIFIED]` 2026-11-01, prueba de mutación de `InterfazFitnessTest` sobre las hojas de Raíces:
  un color literal en `app.css` hace fallar `soloTokensCssDefineColores`; un `--p-*` en
  `publico-portada.css`, `estilosUsanSoloTokensDeUso`; y `--color-texto` con un verde claro,
  `paresDeTokensCumplenContrasteAA`.
- `[VERIFIED]` 2026-11-01: la portada mudada es igual a la variante `concepto-b`. `Imagenes.java
  comparar` da IGUAL en sus 16 capturas (la portada y sus tres estados, en 360, 360×640, 1280 y
  1920 px), con una segunda corrida de antes como ruido.
- `[VERIFIED]` 2026-11-01: `mvn test -pl cliente-liviano -Pvisual` sobre las cuatro rutas de
  `/prototipo/raices/publico-portada`, sin hallazgos (`reporte.json` vacío).
