# Backlog: la guía «Huerta y galpón» al código del cliente liviano

> Versión 1 · 2026-11-22 · rama de integración `E7_guia_ui` (sale de `ENTREGA_7`).
> Fuente de verdad visual: la guía versionada en [`guia/`](guia/) (fase 3, versión 10; SHA-256 de
> `guia/guia-y-pantallas.html`: `b3a7ccc0…`, el de la copia del 2026-11-22). Valores: [`guia/partes/05-tokens.css`](guia/partes/05-tokens.css).
> `guia/armar.js` compara la guía con el `tokens.css` vivo: después de C0, volver a armarla da otro
> HTML y otro SHA. El HTML versionado no se vuelve a armar.
> Un manifiesto `oleada-ui/1` por ola, junto a este archivo: `backlog-guia-al-codigo.<ola>.manifiesto.json`.
> Este archivo es lo que leen los agentes del workflow `oleada-ui`: todo lo que necesitan está acá
> o en un archivo versionado que se cita con `archivo:línea`.
> Etiquetas: `[OBSERVED]` visto en un archivo · `[VERIFIED]` medido con un comando el 2026-11-22 en
> esta rama · `[INFERRED]` deducido · `[PROPOSED]` todavía no existe.

## 1. Cómo se lee

- Cada ítem tiene un ID (`C0-01`), su lote, qué se hace, su evidencia y su estado. El estado lo
  marca la documentación de cierre de cada ola: hecho, parcial (con lo que falta) o no hecho (con el
  motivo).
- Las decisiones del usuario están en §2, con su ID, la fecha y su texto. Los manifiestos apuntan
  ahí en el campo `ok`.
- **Regla común a todas las olas:** una pantalla que no es de la ola no se rediseña ni se arregla,
  aunque se vea inconsistente con el sistema nuevo. Quien construye y quien revisa la anotan como
  menor, nunca como bloqueante. Ese dato va a la decisión G1 (§9).
- Orden de las olas: C0 (piloto, tokens), C1 (piezas y armazón de la huerta), ORG, DON, PUB1, ADM1,
  PUB2, ADM2 y Z (cierre). Si hay que cortar, la prioridad es ORG, DON, PUB1, ADM1, PUB2, ADM2.
  El menú de cada rol cambia en la ola de ese rol (DA-17) y el galpón entra en ADM1 (DA-18).

## 2. Decisiones

### 2.1 Tomadas antes de este backlog

| ID | Decisión | Quién | Dónde consta |
|---|---|---|---|
| G-01 | Los tokens de la guía se suman a `tokens.css`: ningún token que ya existía cambia de nombre | usuario | Cierre de la fase 3 de la guía, 2026-11-22 (notas de la sesión de la guía, sección «Decisiones tomadas al construir», Tokens) |
| G-02 | La identidad pasa a «Huerta y galpón» (mezcla C): un esqueleto aireado para lo público, quien dona y la organización, y uno denso para el admin | usuario | Elección de la mezcla C, 2026-11-20 (notas de la sesión de la guía) |
| PLAN | El plan de olas de este backlog | usuario | «Apruebo el plan», 2026-11-22 (notas de la sesión del plan) |

Las tres constan en notas de sesión que no están en el repo. Esta tabla las registra para los
agentes: lo que necesitan es la decisión, que está acá.

**G-02, las alternativas que no se eligieron** (para el ADR de C0). Fase 2, 2026-11-20: cuatro
direcciones en baja fidelidad, las mismas 7 pantallas a 360 y 1280 px, sin ranking.

| Alternativa | Qué era | Por qué no |
|---|---|---|
| Poda | Evoluciona Raíces, densidad media: una sola metáfora y la tarea primero | No sola: el usuario pidió mezclarla con Acta, «con un poco más de calidez» y «detalles de diseño gráfico para embellecer» |
| Acta | Evoluciona Raíces, densidad alta, escritorio primero: cada registro con su prueba | No sola: el mismo pedido de mezcla |
| Recorrido | Evoluciona Raíces, densidad baja, celular primero: la línea como columna vertebral | El usuario eligió mezclar Poda y Acta |
| Remito | Rompe con Raíces: otra identidad, la donación como comprobante | El usuario eligió mezclar Poda y Acta |
| Mezcla A · Cuaderno | Esqueleto de Poda; cada fila con la prueba de Acta (foto, quién recibió, cuándo); densidad media | El usuario eligió C |
| Mezcla B · Herbario | Esqueleto de Acta (cajón, tablas, lista con detalle) con «para mirar hoy» de Poda; densidad alta | El usuario eligió C |
| **Mezcla C · Huerta y galpón (elegida)** | Un esqueleto por rol: Poda con más aire para lo público, quien dona y la organización; Acta para el admin | De las tres mezclas, la que más calidez pone |

En la fase 2b, la calidez se tradujo a estructura: la foto de la entrega, las personas con nombre,
la voz y el agradecimiento solo por un hecho confirmado. Los detalles gráficos D1 a D7 (sello, raíz
de 5 pasos, etiqueta, frutos, trama, folio y sombra de paja) se dibujaron en la fase 3. Fuente:
notas de la sesión de la guía, «Fase 2: el plan que se siguió» y «Fase 2b».

### 2.2 Ronda de decisiones de la fase B

Una decisión por pregunta, de a 3 por vuelta (`cliente-liviano/AGENTS.md:11`). La recomendación
iba primero. El texto del usuario se copia tal cual. DA-14 se partió en dos (a y b) para que cada
pregunta tuviera una sola decisión.

Ronda 1 (2026-11-22): DA-11, DA-14a y DA-15. Respuesta del usuario: «todas las recomendadas».
Ronda 2 (2026-11-22): DA-01, DA-02 y DA-03a. DA-03 se partió en tres (a, b y c). Respuesta del
usuario: «3 si», junto con el sí al push y al lanzamiento de C0 («1 si, 2 si, 3 si»).
Ronda 3 (2026-11-22, con la medida de C0): G0, DA-07 y DA-17 a DA-20. Respuestas del usuario: para
el alcance, «Quiero todo el sitio, y me gustaría reducir los tests o verificaciones innecesarias /
que generen demasiada fricción»; para el modo, la verificación y DA-07, «Todas las recomendadas».
Con DA-17, las decisiones de los menús pasan de C1 a la ola de cada rol (columna «Ola»).

| ID | Pregunta | Recomendación | Respuesta del usuario | Fecha | Ola |
|---|---|---|---|---|---|
| DA-01 | Público: ¿el Mapa sale del menú y queda dentro de Entregas? | Sí, como dibuja la guía | Sí («3 si», ronda 2) | 2026-11-22 | PUB1 |
| DA-02 | Organización: ¿«Donaciones recibidas» y «Seguir entregas» se funden en «Entregas»? | Sí | Sí («3 si», ronda 2) | 2026-11-22 | ORG |
| DA-03a | Quien dona: ¿«Seguimiento» deja de ser una sección del menú? (el seguimiento de cada donación se abre desde su detalle) | Sí | Sí («3 si», ronda 2) | 2026-11-22 | DON |
| DA-03b | Quien dona, la persona: ¿dónde va Misiones? | Como cuarta entrada del menú de la persona | Pendiente | | DON |
| DA-03c | Quien dona, la persona: ¿cómo es su inicio, que la guía no dibuja? | Pendiente de proponer | Pendiente | | DON |
| DA-04 | Admin: ¿«Por decidir» reemplaza a Inicio y a Asignar? | Sí | Pendiente | | ADM1 |
| DA-05 | «Cómo funciona», «Cómo donar» y «Ayuda»: ¿páginas nuevas o las de hoy con otro nombre? | Las de hoy (Nosotros, Para donantes, Para organizaciones) | Pendiente | | PUB1, DON y ORG |
| DA-06 | ¿Página propia de respuesta solo al confirmar una recepción, y aviso breve en el resto? | Sí, es la regla de la guía | Pendiente | | Roles |
| DA-07 | Sello para «vencida», «cancelada» y «en revisión» | El par neutro, con su texto | El par neutro, con su texto («Todas las recomendadas», ronda 3) | 2026-11-22 | C1 |
| DA-08 | Pantallas sin arquetipo: ¿ficha aprobada por el usuario o derivación del agente? | Ficha para Misiones, Ranking, inicio de la persona y buzón; el resto, derivación | Pendiente | | Roles |
| DA-09 | HG-01, la nueva salida: ¿función nueva o texto sin fecha? | Texto sin fecha; la función, después de la defensa | Pendiente | | Z |
| DA-10 | HG-03: ¿el nombre de quien recibió se muestra en lo público? | En lo privado sí; en lo público, la organización | Pendiente | | Z |
| DA-11 | ¿Se acepta Opus high en los lotes con pantallas, como fija el workflow? | Sí | Sí («todas las recomendadas», ronda 1) | 2026-11-22 | Todas |
| DA-12 | ¿Los huecos de backend van como PRs sueltos a `ENTREGA_7`? | Sí | Pendiente | | Z |
| DA-13 | Las 6 plantillas de `persona/` que solo sirve la maqueta: ¿se migran con su gemela o se borran? | Se migran; borrarlas es otra decisión | Pendiente | | DON |
| DA-14a | En C0: ¿los 10 nombres de color de hoy pasan a ser alias del token de la guía del mismo valor (sin cambiar ningún píxel) y se borran los 3 tokens que nadie usa? | Sí | Sí («todas las recomendadas», ronda 1). Corregida el 2026-11-22: se borran 3 y no 4, porque `--medida-quiebre-medio` sí se usa (ver §3.2). El usuario lo aceptó al elegir, entre tres caminos para la corrida de C0, «parar, corregir la decisión y relanzar» (su respuesta: «Opción B»). Esa «B» no tiene relación con ninguna otra lista de opciones | 2026-11-22 | C0 |
| DA-14b | En Z: ¿se borran los alias que quedaron sin usos y los 11 tokens que no usa ninguna hoja ni plantilla y solo nombra `InterfazFitnessTest`, cambiando su lista de pares por la de la guía? | Sí | Pendiente | | Z |
| DA-15 | Si el 30/11 no llegó todo: ¿corte por área con la regla G2, o todo o nada? | Corte por área | Corte por área («todas las recomendadas», ronda 1) | 2026-11-22 | Z |
| DA-16 | La excepción de toque que el cierre de C0 agregó a `VerificacionVisualTest` para la casilla del buzón en `/estilos` (`e7ba71c36`), sin consultar: ¿se mantiene o se revierte? | Ninguna: se presentaron las dos | Se mantiene («Mantenerla»). Queda en el [ADR de la excepción de toque](../adr/cliente-liviano/20261122-excepcion-de-toque-para-la-casilla-del-buzon-en-estilos.md) (`proposed`) y en [DTI-46](../adr/DEUDA_TECNICA.md#dti-46) | 2026-11-22 | C0 |
| G0 | Con la medida de C0 (§3.4): ¿el plan sigue con todo el sitio o se recorta al recorrido de la demo? | Recorte al recorrido de la demo (ORG, DON y PUB1), con ADM1 por decidir en G1 | Todo el sitio («Quiero todo el sitio», ronda 3). Quedan 8 corridas: C1, ORG, DON, PUB1, ADM1, PUB2, ADM2 y Z | 2026-11-22 | Todas |
| DA-17 | ¿El menú de cada rol cambia en la ola de ese rol, y no todos juntos en C1? | Sí: si una ola no llega, su área queda con el menú de hoy | Sí («Todas las recomendadas», ronda 3) | 2026-11-22 | PUB1, DON, ORG y ADM1 |
| DA-18 | ¿Las piezas del galpón (la tabla con selección, P21; el cajón, P22; y el armazón del galpón, la mitad de P26) pasan de C1 a ADM1? | Sí | Sí («Todas las recomendadas», ronda 3) | 2026-11-22 | ADM1 |
| DA-19 | ¿Las fichas de las pantallas sin arquetipo se escriben en una sesión con el usuario, sin corridas `disenar` del workflow? | Sí | Sí («Todas las recomendadas», ronda 3) | 2026-11-22 | Roles |
| DA-20 | ¿Se reduce la verificación de cada corrida sin cambiar el workflow ni ningún test? (1) La pasada de `-Pvisual` por las 320 rutas corre una vez al cierre de C1 y otra en Z. (2) El stack real con login se levanta solo en las olas que tocan un adaptador `rest` y en Z. (3) Cada manifiesto lleva 5 reglas o menos, solo criterios de producto; la excepción es la regla que le dice al cierre qué saltear y qué pasada correr, porque el cierre no tiene otra forma de recibirla. (4) En `rutasVisuales` va una ruta por plantilla; el piso automático sobre todas las rutas del área corre una vez, al cierre de cada ola. (5) Ningún lote de solo documentos lleva una decisión | Sí | Sí («Todas las recomendadas», ronda 3) | 2026-11-22 | Todas |

## 3. Ola C0: los tokens (piloto)

C0 es la primera corrida real del workflow `oleada-ui` y sirve de piloto: además de su trabajo,
mide cuánto tarda y cuánto gasta una ola (§3.4).

### 3.1 Ítems

| Ítem | Lote | Qué | Evidencia | Estado |
|---|---|---|---|---|
| C0-01 | `c0-adr` | ADR `proposed` «Huerta y galpón»: la guía de `docs/interfaz/guia/` como sistema visual y un esqueleto por rol. Dice su relación con el ADR de la piel Huerta, sin promover nada. `direccion-de-arte.md` deja de nombrar a Pico como base y apunta a la guía | Cambiar la identidad pide un ADR: `docs/interfaz/README.md:102`. ADR previo: `docs/adr/cliente-liviano/20261110-piel-huerta-con-abejas-como-identidad-del-cliente-liviano.md`. Pico: `docs/interfaz/direccion-de-arte.md:137-138` frente a `cliente-liviano/AGENTS.md:25-26` | Hecho (`34cf87ae7`, aprobado por su revisión en la primera corrida). La revisión de la segunda corrida quedó sin PASS por un trámite: el commit vacío del lote (`5b0eb78b4`) no se integró ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 6) |
| C0-02 | `c0-tokens` | `tokens.css` suma los 46 tokens nuevos de la guía y cambia el valor de los 6 que cambian (§3.2) | Guía: `docs/interfaz/guia/partes/05-tokens.css`. Tokens de uso de hoy: `cliente-liviano/src/main/resources/static/css/tokens.css:169-314` | Hecho (`9e995ec42`): 46 tokens de uso nuevos, con su valor base nuevo (`--p-velo`), y los 6 con su valor nuevo `[VERIFIED]`. El lote quedó sin PASS de su revisión por C0-05 |
| C0-03 | `c0-tokens` | Los 10 nombres de color en uso que la guía no trae pasan a ser alias del token de la guía del mismo valor; se retiran los 3 tokens sin ninguna referencia y se conserva `--medida-quiebre-medio` (§3.2) | Tablas de §3.2, `[VERIFIED]` | Hecho (`9e995ec42`): los 10 alias con `var()`; salen `--color-nodo`, `--color-estrato` y `--medida-estrato`; `--medida-quiebre-medio` queda `[VERIFIED]`. Salen también 3 valores base sin uso (`--p-nodo`, `--p-destacado` y `--p-suelo-hondo`), que DA-14a no nombra |
| C0-04 | `c0-tokens` | Los pares de contraste de los tokens nuevos entran en la lista del cerco; ningún par se quita | Lista: `cliente-liviano/src/test/java/grupo5/cliente/InterfazFitnessTest.java:57-90`; prueba: `:204`. Pares de la guía: `docs/interfaz/guia/partes/90-script.html:216-248` | Hecho (`9e995ec42`): 22 pares nuevos en la lista; ninguno se quitó y ningún tope subió `[OBSERVED]` |
| C0-05 | `c0-tokens` | `/estilos` gana una sección «Tokens» (color, letra, espaciado, radio, borde, elevación y movimiento), antes de «Componentes». `/estilos` no tiene hoja propia y el cerco prohíbe `style` sueltos: las muestras de token van en `componentes/muestra.css`. `componentes.md` suma una sección de tokens, nueva | Hoy tiene Composición, Utilidades y Movimiento, sin sección de tokens: `cliente-liviano/src/main/resources/templates/estilos.html:21-147`. Encabezados de `docs/interfaz/componentes.md`: no hay ninguno de tokens | Parcial (`9e995ec42`, `ebce607ad` y `7d413d1b2`). Está la sección «Tokens» con 14 fichas y la sección de `componentes.md`. Falta: las 14 fichas están escritas a mano y no usan el fragmento `componentes/muestra :: muestra` (bloqueante de la revisión tras 3 rondas); `/estilos` pasa el gate visual solo por la excepción de `e7ba71c36`, que no decidió el usuario ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítems 1 y 2) |
| C0-06 | `c0-tokens` | La copia de `tokens.css` del tema de Keycloak se vuelve a copiar con el script, y su prueba de contraste pasa | El tema lleva una copia idéntica: `auth-service/README.md:126`. Script: `scripts/sincronizar_tema.py`. Prueba: `scripts/tests/test_auth_theme.py`, en el CI: `.github/workflows/agent-governance.yml:67`. Hoy las dos copias son iguales `[VERIFIED]` | Hecho (`9e995ec42`): la copia del tema es idéntica (`cmp`) y `test_auth_theme.py` pasa (45 OK) `[VERIFIED]` en el cierre |

### 3.2 Datos de los tokens `[VERIFIED]`

Medido con `node docs/interfaz/guia/armar.js` y una comparación de valores base entre
`guia/partes/05-tokens.css` y `tokens.css` (2026-11-22, en esta rama).

| Grupo | Cantidad | Qué pasa en C0 |
|---|:-:|---|
| Tokens de uso de hoy | 117 | |
| Tokens de uso de la guía | 119 | |
| Se conservan con su nombre | 73 | 6 cambian de valor (tabla de abajo) |
| Nuevos | 46 | Se suman, con los valores base nuevos que piden (por ejemplo, `--color-velo` usa `--p-velo: #2a2a2266`, `guia/partes/05-tokens.css:33, 51`) |
| De hoy que la guía no trae | 44 | 10 de color pasan a alias; 3 se retiran; el resto no se toca en C0 |

**Los 6 que cambian de valor** (el valor nuevo es el de la guía):

| Token | Valor de hoy |
|---|---|
| `--color-accion-secundaria` | `#e4efc9` |
| `--color-accion-secundaria-activa` | `#8fcb4a` |
| `--color-enlace` | `#2a7035` |
| `--color-foco` | `#2a7035` |
| `--tamano-titulo-portada` | `clamp(2.6rem, 1.4rem + 4.4vw, 5rem)` |
| `--medida-menu-celular` | `3.8125rem` |

**Los 10 alias.** Cada nombre de hoy se define con `var()` del token de la guía; el color calculado
no cambia (los dos valores son iguales hoy):

| Nombre de hoy | Usos en hojas | Pasa a ser | Valor |
|---|:-:|---|---|
| `--color-tinta-suave` | 110 | `var(--color-texto-suave)` | `#5d5c4c` |
| `--color-acento` | 85 | `var(--color-accion-primaria)` | `#2a7035` |
| `--color-papel` | 71 | `var(--color-fondo-pagina)` | `#fbf2d3` |
| `--color-acento-claro` | 56 | `var(--color-salvia)` | `#e4efc9` |
| `--color-acento-hondo` | 10 | `var(--color-accion-primaria-activa)` | `#23602c` |
| `--color-suelo-hondo` | 9 | `var(--color-tinta)` | `#2a2a22` |
| `--color-sombra` | 4 | `var(--color-hoja)` | `#8fcb4a` |
| `--color-destacado` | 3 | `var(--color-raiz)` | `#f4c844` |
| `--color-titulo` | 1 | `var(--color-accion-primaria)` | `#2a7035` |
| `--color-sombra-grano` | 1 | `var(--color-tinta)` | `#2a2a22` |

**Los 3 que se retiran:** solo se definen en `tokens.css` (y en su copia del tema de Keycloak); no
los usa ninguna hoja, plantilla ni test, ni por nombre ni por valor (`git grep`, 2026-11-22). La
bitácora los menciona como historia: `docs/interfaz/bitacora.md:1505` nombra a `--color-estrato` y
a `--medida-estrato`.

| Token | Definición |
|---|---|
| `--color-nodo` | `tokens.css:188` |
| `--color-estrato` | `tokens.css:292` |
| `--medida-estrato` | `tokens.css:294` |

**`--medida-quiebre-medio` se conserva** (corrección de DA-14a, 2026-11-22). Nadie lo nombra, pero su
valor, `53.75em`, está en 39 media queries de las hojas, y `InterfazFitnessTest` exige que cada
media query use un quiebre declarado en `tokens.css` (`mediaQueriesUsanSoloLosQuiebresDeTokens`,
`InterfazFitnessTest.java:155`). La guía no trae otro quiebre con ese valor
(`guia/partes/05-tokens.css:184-186`). La primera corrida de C0, del 2026-11-22, lo encontró: la
búsqueda original miró los nombres y no los valores.

**No se tocan en C0:** los otros 31 de los 44: `--medida-quiebre-medio` y 30 más. De esos 30, 19 se usan en hojas (movimiento, capas,
medidas y dos de letra) y siguen en el sistema. Los otros 11 no los usa ninguna hoja ni plantilla y
solo los nombra `InterfazFitnessTest`: 10 en su lista de pares de contraste (`:57-90`) y
`--movimiento-retraso-ninguno` en la prueba del movimiento (`:651`). Se retiran en Z, cuando la
lista de pares pase a los pares de la guía (DA-14b). Aparte, el test también nombra a 3 tokens que
sí se usan en hojas: `--color-titulo` (`:61, 67`; pasa a alias y el test lo sigue midiendo, con el
mismo valor), `--movimiento-curva-base` (`:644`) y `--movimiento-retraso-sello` (`:646`).

### 3.3 Muestra para G1

Siete pantallas de hoy, sin rehacer, solo para mirar cómo quedan con los tokens nuevos: `/publico-portada`,
`/donante-inicio`, `/donante-donaciones`, `/entidad-inicio`, `/entidad-seguimiento-confirmar`,
`/admin-asignaciones` y `/admin-donaciones`. Sus capturas salen de la corrida completa de
`-Pvisual` (las 320 rutas de `cliente-liviano/src/test/resources/verificacion/paginas.txt`), que
corre el cierre de la ola y guarda fuera del repo, junto a su `cierre.md` (regla del manifiesto).
No entran en la revisión de pantallas ni en la hoja de contacto de la ola. Es la muestra de C0, una
primera señal; la de G1, con 16 pantallas, está en §10.3.

### 3.4 Qué mide el piloto

| Medida | Para qué |
|---|---|
| Duración de punta a punta y por paso | Decidir si las olas siguen en serie o en dos pistas (G0) |
| Tokens por lote y por paso | Proyectar la cuota de las 12 corridas que faltan (13 en total: 9 olas de `construir` y 4 de `disenar`, porque ORG, DON, ADM1 y ADM2 tienen pantallas con ficha para aprobar antes de construir). Así era el plan al medir; desde G0 quedan 8 después de C0, sin corridas `disenar` (§2.2, G0 y DA-19) |
| Rondas de reparación y de revisión de pantallas | Ajustar el tamaño de los lotes |
| `-Pvisual` sobre las 320 rutas con los tokens nuevos | El piso: contraste AA, desbordes y toque de 48 px |
| Las capturas de la muestra de §3.3 | Primera señal para G1 |
| Si el stack del cierre levantó | Saber si el recorrido con login real es viable en esta máquina |

La duración y los tokens los mide la sesión que lanza el workflow (hora de inicio y de fin, y el
uso del plan antes y después). Los agentes no tienen ese dato. El cierre anota la hora de cada
commit de la ola, sacada de `git log`.

**Resultado de C0** (2026-11-22, rama `E7_guia_ui_c0`; detalle en `guia-r2/C0/cierre.md`, fuera del
repo). `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` pasa (802 tests); el tema de Keycloak
pasa (45 OK). `-Pvisual` sobre las 320 rutas: pasan 308 y fallan 12, todas por área de toque, ninguna
por contraste; las 7 rutas de la muestra de §3.3 pasan. El stack del cierre levantó en la segunda
subida y se entró con login real. Los dos lotes quedaron sin PASS de su revisión tras 3 rondas
(C0-01 por un trámite, C0-05 por las fichas a mano). Después del cierre, `e7ba71c36` sumó una excepción
del gate para `/estilos` que el usuario no decidió. Lo que queda, en
[DTI-46](../adr/DEUDA_TECNICA.md#dti-46).

## 4. Arquetipos: las 13 pantallas dibujadas

Fuente: `docs/interfaz/guia/partes/50-pantallas.html` (el ancla es el `id` de cada sección; las 13
existen `[VERIFIED]`).

| ID | Pantalla de la guía | Ancla | ID | Pantalla de la guía | Ancla |
|---|---|---|---|---|---|
| A01 | Portada | `#portada` | A08 | Donación con problema | `#detalle-problema` |
| A02 | Necesidades, dos estados | `#necesidades` | A09 | Inicio de la organización, dos estados | `#inicio-organizacion` |
| A03 | Bandeja «Por decidir» | `#bandeja` | A10 | Entregas, dos estados | `#entregas` |
| A04 | Inicio de quien dona | `#inicio-donante` | A11 | Entrega con problema | `#entrega-problema` |
| A05 | Donaciones, en tabla | `#donaciones` | A12 | Confirmar recepción y su error | `#confirmar` |
| A06 | Donación asignada | `#detalle-asignada` | A13 | Recepción confirmada | `#confirmada` |
| A07 | Seguimiento | `#seguimiento` | | | |

## 5. Pantalla → arquetipo

Rutas bajo `cliente-liviano/src/main/resources/templates/`, con el nombre completo de cada
plantilla (sin `.html`). «Directo»: la guía la dibuja. «Deriva»: no está dibujada y sigue la
plantilla del arquetipo. «Sin arquetipo»: la guía no la resuelve (DA-08). Hoy hay 27 plantillas
`publico-`, 10 `donante-`, 18 `entidad-` y 40 `admin-` `[VERIFIED]`, más 9 pantallas en `persona/`
y 3 en `error/`. `persona/datos-persona.html` no es una pantalla: son los datos de ejemplo de la
persona (lo dice su comentario de cabecera).

**ORG (18)**

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `entidad-inicio` | A09 directo | Dos estados que decide el servidor |
| `entidad-necesidades` | A02 directo | |
| `entidad-necesidades-publicada`, `entidad-necesidades-cambiada`, `entidad-necesidades-baja` | Aviso breve | DA-06 |
| `entidad-necesidades-publicar-error` | Deriva de A12 | El error es un estado del formulario |
| `entidad-recibidas`, `entidad-seguimiento` | A10 directo | Se funden en «Entregas» (DA-02) |
| `entidad-seguimiento-confirmar`, `entidad-seguimiento-confirmar-error` | A12 directo | |
| `entidad-seguimiento-problema` | Deriva de A12 | El reporte «llegó con un problema», un formulario |
| `entidad-seguimiento-sellada`, `entidad-seguimiento-sellada-lentejas`, `entidad-seguimiento-sellada-ropa` | A13 directo | |
| `entidad-notificaciones`, `entidad-notificaciones-leidas` | Sin arquetipo | El buzón |
| `entidad-perfil`, `entidad-perfil-guardado` | Deriva de A12 | |
| Página nueva: Entrega con problema | A11 directo | El detalle de una entrega que no pudo llegar |
| Página nueva: Ayuda | Sin arquetipo | DA-05 |

**DON (19: 10 de la empresa y 9 de la persona)**

Las 9 de `persona/` se sirven en la maqueta con `?cuenta=persona`
(`cliente-liviano/src/main/java/grupo5/cliente/maqueta/CuentaDeEjemplo.java:30-40`). `-Pvisual`
recorre hoy 4 de ellas con esa cuenta: donaciones, entidades, inicio y perfil
(`cliente-liviano/src/test/resources/verificacion/paginas.txt:307-319`); la ola DON suma las otras
5 a `paginas.txt`. En el perfil real solo se usan 3
(`cliente-liviano/src/main/java/grupo5/cliente/web/Pantallas.java:129-130`). Las otras 6 siguen a
su gemela de la empresa (DA-13).

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `donante-inicio` | A04 directo | |
| `persona/donante-inicio` | Sin arquetipo | La guía dibuja solo a la empresa (DA-03) |
| `donante-donaciones`, `persona/donante-donaciones` | A05 directo | |
| `donante-donaciones-detalle`, `persona/donante-donaciones-detalle` | A06, A07 o A08 | Una plantilla; el estado elige la variante |
| `donante-seguimiento`, `persona/donante-seguimiento` | A07 directo | Deja de ser sección del menú (DA-03) |
| `donante-entidades`, `persona/donante-entidades` | Deriva de A02 | Tarjetas con `componentes/listado` |
| `donante-misiones` | Sin arquetipo | DA-03 |
| `donante-perfil`, `donante-perfil-guardado`, `persona/donante-perfil`, `persona/donante-perfil-guardado` | Deriva de A12 | |
| `donante-notificaciones`, `donante-notificaciones-leidas`, `persona/donante-notificaciones`, `persona/donante-notificaciones-leidas` | Sin arquetipo | El buzón |
| Página nueva: Cómo donar | Sin arquetipo | DA-05 |

**PUB1 (13)**

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `publico-portada` | A01 directo | |
| `publico-ingresar`, `publico-ingresar-error` | Deriva de A12 | La puerta de la demo |
| `publico-donaciones` | Deriva de A10 | Las etiquetas de entregas confirmadas |
| `publico-entidades` | Deriva de A02 | Tarjetas con `componentes/listado` |
| `publico-mapa` | Deriva de A07 | Mapa con su lista; sale del menú (DA-01) |
| `publico-nosotros`, `publico-para-donantes`, `publico-para-entidades` | Deriva de A01 | «Cómo funciona» (DA-05) |
| `publico-legales` | Sin arquetipo | Página de lectura |
| `publico-contacto`, `publico-contacto-enviado`, `publico-contacto-error` | Deriva de A12 | La respuesta, aviso breve (DA-06) |

**PUB2 (14 y 5 comunes)**

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `publico-recuperar`, `publico-recuperar-enviado` | Deriva de A12 | |
| `publico-activar`, `publico-activar-pedir`, `publico-activar-pedido`, `publico-activar-lista`, `publico-activar-error` | Deriva de A12 | |
| `publico-registro`, `publico-registro-donante`, `publico-registro-donante-error` | Deriva de A12 | |
| `publico-registro-entidad`, `publico-registro-entidad-corregir`, `publico-registro-entidad-enviada`, `publico-registro-entidad-error` | Sin arquetipo | El formulario en pasos |
| `error/403`, `error/404`, `error/5xx`, `sin-dibujar`, `aviso-persona` | Sin arquetipo | Páginas de estado |

**ADM1 (18)**

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `admin-inicio` | A03 directo | Pasa a ser «Por decidir» (DA-04); sus gráficos, sin arquetipo |
| `admin-asignaciones`, `admin-asignaciones-aprobada`, `admin-asignaciones-corrida`, `admin-asignaciones-descartada`, `admin-asignaciones-error` | A03 directo | |
| `admin-donaciones`, `admin-donaciones-detalle`, `admin-donaciones-detalle-vencida` | Deriva de A03 | «Vencida» no tiene sello en la guía (DA-07) |
| `admin-donaciones-registrar`, `admin-donaciones-registrar-error`, `admin-donaciones-registrada` | Deriva de A12 | En el galpón |
| `admin-camiones`, `admin-camiones-agregado`, `admin-camiones-agregar-error`, `admin-camiones-deshabilitado`, `admin-camiones-habilitado` | Deriva de A03 | El dibujo de la flota, sin arquetipo |
| `admin-ranking` | Sin arquetipo | |

**ADM2 (22)**

| Plantillas | Arquetipo | Nota |
|---|---|---|
| `admin-donantes`, `admin-donantes-importando`, `admin-donantes-importado`, `admin-donantes-importar-error` | Deriva de A03 | |
| `admin-donantes-registrar`, `admin-donantes-registrado`, `admin-donantes-registrar-error` | Deriva de A12 | |
| `admin-importaciones` | Deriva de A03 | |
| `admin-entidades`, `admin-entidades-aprobada`, `admin-entidades-correccion`, `admin-entidades-rechazada` | Deriva de A03 | |
| `admin-entidades-registrar`, `admin-entidades-registrada`, `admin-entidades-registrar-error` | Deriva de A12 | |
| `admin-administradores`, `admin-administradores-nuevo`, `admin-administradores-alta`, `admin-actividad` | Deriva de A03 | |
| `admin-notificaciones`, `admin-notificaciones-leidas` | Sin arquetipo | El buzón |
| `admin-perfil` | Deriva de A12 | |

**Sin arquetipo, en resumen:** el buzón de avisos de los tres roles, Misiones e insignias, el inicio
de la persona, Ranking, la flota, los gráficos del inicio del admin, el registro en pasos, las
páginas de estado, Legales y los estados fuera de los seis de la guía.

### 5.1 Los menús (cada uno cambia en la ola de su rol, DA-17)

Los menús de quien dona, la organización y el admin están en
`cliente-liviano/src/main/java/grupo5/cliente/web/Pantallas.java:92-126`; en la maqueta, la
persona toma el menú de la empresa y le suma Misiones (`maqueta/CuentaDeEjemplo.java:94-99`); el público, en `templates/navegacion-publica.html`.
Fuente de los menús nuevos: `docs/interfaz/guia/partes/50-pantallas.html`.

| Rol | Menú de hoy | Menú de la guía |
|---|---|---|
| Público | Inicio, Entregas, Mapa, Organizaciones | Inicio, Entregas, Organizaciones, Cómo funciona |
| Quien dona | Inicio, Mis donaciones, Organizaciones, Seguimiento (la persona suma Misiones) | Inicio, Donaciones, Organizaciones, Cómo donar |
| Organización | Inicio, Necesidades, Donaciones, Entregas | Inicio, Necesidades, Entregas, Ayuda |
| Admin | Inicio, Donantes, Organizaciones, Donaciones, Asignar, Camiones, Ranking | Por decidir, Donaciones, Donantes, Organizaciones, Camiones, Ranking |

## 6. Pieza de la guía → componente del catálogo

Línea de la guía en `docs/interfaz/guia/partes/10-componentes.css`, salvo que diga otra hoja (cada
línea citada abre el bloque de esa pieza `[VERIFIED]`). Destino bajo
`cliente-liviano/src/main/resources/static/css/` o `templates/`. Entran en C1, salvo P21, P22 y el
armazón del galpón (la mitad de P26), que pasan a ADM1 (DA-18).

| ID | Pieza | Guía | Destino | Qué se hace |
|---|---|---|---|---|
| P01 | Base y aro de foco | 1-43 | `app.css` | Se retoca |
| P02 | Composición y medida del texto | 44-71, 453-454 | `composicion.css` | Se retoca |
| P03 | Botones, con «cargando» | 72-114 | `componentes/boton.css` | Se retoca |
| P04 | Campos, opción y casilla | 115-156 | `componentes/campo.css`, `formularios.css` | Se extiende |
| P05 | Subir fotos | 456-461; `15-pantallas.css:60` | `componentes/subida.css` | Se retoca |
| P06 | Sello (D1) y «Espera tu confirmación» | 157-178 | `componentes/sello.css`, `sellos.css` | Se extiende |
| P07 | Lámina y sombra de paja (D7) | 179-190 | `componentes/tarjeta.css` | Se retoca |
| P08 | Foto de una entrega y etiqueta (D3) | 191-223; `15-pantallas.css:121` | `componentes/etiqueta-entrega.css` y `componentes/etiqueta-entrega.html` `[PROPOSED]` | Componente nuevo, con otro nombre: `.etiqueta` ya es una clase heredada (`entidad-seguimiento-confirmar.css:47`). Reemplaza al acta de `entidad-comun.css` en la ola ORG |
| P09 | Raíz (D2), cuatro formas y «no pudo llegar» | 224-263, 490-502; `15-pantallas.css:39-40, 94-97` | `componentes/huerta.css` y `componentes/huerta.html` | Se extiende |
| P10 | Frutos (D4) | 264-267, 484 | `componentes/huerta.html` (fragmento `cifras`) | Se extiende: un fruto por entrega confirmada |
| P11 | Trama (D5) | 268-273 | `componentes/trama.css` `[PROPOSED]` | Componente nuevo |
| P12 | Avance de una necesidad | 274-279 | `componentes/progreso.css` | Se retoca |
| P13 | Avisos: en la página y breve | 280-313, 481 | `componentes/aviso.css` | Se extiende. La lista de novedades de ese bloque es del buzón: va con su ficha, en las olas de rol |
| P14 | Barra superior e inferior, con el salto al contenido y la campana | 315-356 (la campana, 340-341), 445 | `componentes/navegacion.css`, `componentes/campanita.css` | Se retoca |
| P15 | Pestañas | 357-367, 448 | `componentes/pestanas.css` | Se retoca |
| P16 | Migas de pan | 369-373 | `componentes/migas.css` `[PROPOSED]` | Componente nuevo |
| P17 | Paginador | 374-379 | `componentes/paginas.css` | Se retoca |
| P18 | Pie de la huerta | 380-384 | `pie.html` | Se retoca |
| P19 | Listas y sus estados | 385-394, 406-416 | `componentes/lista.css`, `componentes/carga.css` | Se retoca |
| P20 | Tabla que se apila | 503-520 | `componentes/tabla.css` | Se retoca |
| P21 | Tabla del galpón, con selección | 395-405 | `componentes/tabla.css` | Se extiende |
| P22 | Cajón del galpón | 417-427 | `componentes/navegacion.css` | Componente nuevo |
| P23 | Lista de datos | `15-pantallas.css:172-176` | `componentes/datos.css` `[PROPOSED]` | Componente nuevo |
| P24 | Dibujos: recorrido, puerta, mapa y 7 escenas | `20-simbolos.html` | `static/img/` | Nuevos |
| P25 | 17 íconos y 9 signos de estado | `20-simbolos.html` | `iconos.html` | Se extiende |
| P26 | Armazones de la huerta y del galpón | `15-pantallas.css:1-14` (común); `16-25` y `98-104` (galpón) | `layout-publico.html`, `layout-privado.html` | Se extiende |
| P27 | Movimiento reducido y el trazo | 428-431; `15-pantallas.css:177-184` | `movimiento.css` | Se extiende |

Las hojas heredadas, sin capa, le ganan a los componentes
(`cliente-liviano/src/main/resources/static/css/app.css:3-8`): redibujar una pieza no alcanza a
una pantalla que todavía tiene su hoja heredada. Quedan 5 ítems de esa migración
(`docs/interfaz/backlog-migracion-componentes.md:246-250`); cada ola de rol termina la de sus
pantallas.

## 7. Backend: los huecos de la guía

Cuatro datos que las pantallas dibujadas muestran y el backend no entrega. Un PR por hueco hacia
`ENTREGA_7` (DA-12), aditivos primero. La pantalla se construye con el texto de «Si no llega» y el
dato real se conecta en Z, con un lote condicional por hueco.

| ID | Dato | Estado de hoy | Pantallas | Tipo | Si no llega |
|---|---|---|---|---|---|
| HG-01 | Fecha de la nueva salida de una entrega que no pudo llegar | No existe. Solo hay un booleano `replanificable` en el pedido: `logistica-service/src/main/java/grupo5/logistica/dto/entregas/CambioEstadoEntregaRequestDTO.java:13`; se guarda en `logistica-service/.../jpa/entities/EventoEntregaEntity.java:48-49` | A08, A11, avisos de A04 y A09, A02, A10 | Función nueva (DA-09) | «Todavía sin fecha de nueva salida» |
| HG-02 | Hora estimada de llegada | No existe. Solo `horaArribo`, la real: `logistica-service/src/main/java/grupo5/logistica/dto/entregas/SeguimientoEntregaResponseDTO.java:36` | A04, A05, A07 | Aditivo | «En camino desde las 14:30», sin hora estimada |
| HG-03 | Nombre de quien recibió | No existe. El `actor` es el identificador de la organización: `cliente-liviano/src/main/java/grupo5/cliente/rest/EntidadConfirmarRest.java:250` | La etiqueta en A01, A04, A10 y A13 | Aditivo; dato personal (DA-10) | «Recibió: Comedor Barrio Ejemplo» |
| HG-04 | Avance de una necesidad en unidades: llegaron, asignadas y sin asignar | Parcial: cuenta donaciones, no unidades: `donaciones-service/src/main/java/grupo5/donaciones/dto/PeriodoNecesidadDTO.java:12-19` | A02, A06, A09, A13 | Aditivo | La cuenta de donaciones, como hoy |

**No son huecos de backend** (el dato existe y lo resuelve el cliente en la ola de su pantalla):
los pasos con fecha y hora, el motivo del problema, la posición del camión (simulada), el remito y
las fotos, qué necesidad cubre una donación, la entrega en la puerta, el porqué de una propuesta y
las cuentas del mes. El adaptador real arma la vista de una entrega sin el remito, la foto ni quién
confirmó: esos campos van en vacío (`EntidadConfirmarRest.java:152-176`). Los completa la ola ORG
con lo que Logística ya entrega.

**Estados:** la guía usa seis y el backend tiene ocho para la donación
(`donaciones-service/.../donacionesIndependientes/TipoEstadoDonacion.java:3-13`). «Preparada» es
`LISTA_PARA_ENTREGAR`; `EN_PUERTA` se muestra «En camino» con la chapa «Espera tu confirmación».
`VENCIDA`, de la donación, y `CANCELADA` y `REVISION`, de la entrega
(`logistica-service/.../models/entities/entregas/EstadoEntrega.java:3-13`), no tienen sello en la
guía (DA-07).

## 8. Criterios de aceptación de cada ola

1. **Gates:** `mvn spotless:check`, `mvn clean test -pl cliente-liviano -am` y `-Pvisual` sobre
   las rutas de la ola, a 360, 1280 y 1920 px: AA sin errores, nada desbordado y toque de 48 px.
   Donde la ola toca Java, el pre-flight de SonarCloud (`AGENTS.md` §4.3).
2. **Sistema cerrado:** `InterfazFitnessTest` pasa sin subir ningún tope. Las hojas de pantalla solo
   disponen.
3. **Fiel a la guía:** cada pantalla «directa» se compara con su arquetipo a 360 y 1280 px; cada
   pieza, con su muestra de la guía. Las diferencias se declaran.
4. **El piso de la guía:** una sola acción llena por pantalla; el estado nunca solo por color; lo
   esencial sin JavaScript; movimiento reducido (criterios 9 a 11 de
   `docs/interfaz/guia/CRITERIOS-FASE3.md:70-87`; el resto de ese archivo es de la fase 3 y es historia).
5. **Revisión:** `revisar-ui` independiente con PASS y hoja de contacto aprobada por el usuario.
6. **Datos:** las pantallas usan su puerto; sin datos de la guía escritos en la plantilla.
7. **Documentación** al día en el mismo PR.

## 9. Cortes

- **G0, después de C0:** con la medida de §3.4, el usuario decide si las olas siguen en serie o en
  dos pistas, y si el plazo alcanza. **Resultado (2026-11-22):** todo el sitio, en 8 corridas (C1,
  ORG, DON, PUB1, ADM1, PUB2, ADM2 y Z). Las decisiones están en §2.2: G0 y DA-17 a DA-20. Las olas
  siguen en serie; una segunda pista, si el plazo la pide después del reinicio de la cuota del
  27/11, sigue lo que ya decía el plan (decisión M4).
- **G1, después de C1:** el usuario mira 16 pantallas de hoy sin rehacer (4 por área: lo público,
  quien dona, la organización y el admin) y responde, por área, «se puede mostrar así» o «no se
  puede mostrar así». La lista de las 16 está en §10.3.
- **G2, el 30/11:** el corte del PR final a `ENTREGA_7`, con la respuesta a DA-15.

## 10. Ola C1: las piezas

C1 lleva al catálogo y a `/estilos` 25 de las 27 piezas de §6, con el armazón de la huerta entre
ellas, y pasa al fragmento las fichas de tokens que dejó C0: 26 ítems. No cambia
ninguna pantalla: ni entradas de menú (DA-17), ni rutas, ni plantillas de pantalla. Manifiesto:
[`backlog-guia-al-codigo.c1.manifiesto.json`](backlog-guia-al-codigo.c1.manifiesto.json).

Abreviaturas de esta sección: «guía» es `docs/interfaz/guia/partes/10-componentes.css`; «15» es
`docs/interfaz/guia/partes/15-pantallas.css`; «20» es `docs/interfaz/guia/partes/20-simbolos.html`;
`estilos.html` es `cliente-liviano/src/main/resources/templates/estilos.html`; «catálogo» es
`docs/interfaz/componentes.md`; las hojas van bajo `cliente-liviano/src/main/resources/static/css/`
y los fragmentos bajo `cliente-liviano/src/main/resources/templates/`. Las líneas se releyeron el
2026-11-22 sobre `397896a79` `[VERIFIED]`.

### 10.1 Ítems

| Ítem | Lote | Pieza y qué se hace | Evidencia | Estado |
|---|---|---|---|---|
| C1-01 | `c1-base` | P01 · La base y el aro de foco de la guía. El aro queda de un solo color (hoy usa dos, [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 5). El catálogo deja de nombrar a `pico` como primera capa | Guía 1-43. Hoy: `app.css`, desde el principio hasta antes de `.raices-cabecera` (`app.css:114`); capas en `app.css:8`; catálogo `:36` | Sin empezar |
| C1-02 | `c1-base` | P02 · La composición y la medida del texto | Guía 44-71 y 453-454. Hoy: `composicion.css`; muestras en `estilos.html:21-87`; catálogo `:40-86` | Sin empezar |
| C1-03 | `c1-base` | P27 · El movimiento reducido y el trazo, más las animaciones con nombre que piden otras piezas: el giro del botón que carga, el latido del esqueleto y el trazo de la raíz, con su asomo y su brote. Llevan el prefijo `mov-` de `movimiento.css`, no el `dt-` de la guía; `mov-traza` ya existe. Sus duraciones usan los tokens `--movimiento-duracion-*` que ya existen: la guía escribe 0,9 s para el giro (`:111`) y 1,4 s para el latido (`:412`), que no son tokens, y se usa el más cercano. Las demás piezas solo las nombran | Guía 428-431, 108-113 y 411-415; 15, 177-184. Hoy: `movimiento.css` (el prefijo, `:4-5`; `mov-traza`, `:131`); muestras en `estilos.html:89-145`; catálogo `:87-142`. Los `@keyframes` van solo en `movimiento.css` y cada `data-movimiento` va en el catálogo y en las muestras (`InterfazFitnessTest.java:433, 365-379`) | Sin empezar |
| C1-04 | `c1-base` | Las 14 fichas de la sección «Tokens» de `/estilos` pasan al fragmento `componentes/muestra :: muestra(nombre, cuando, cuerpo)` | Fichas a mano en `estilos.html:147-372`; fragmento en `componentes/muestra.html:11`; [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 1 | Sin empezar |
| C1-05 | `c1-simbolos` | P25 · Los 17 íconos y los 9 signos de estado de la guía entran en `iconos.html`. Los `id` de hoy (`nav-*` y los de categorías) se conservan | 20, 6-24 (íconos) y 27-35 (signos). Hoy: `iconos.html`, 20 símbolos y ningún signo de estado | Sin empezar |
| C1-06 | `c1-simbolos` | P24 · Los dibujos de la guía: el recorrido, la puerta, el mapa y las 7 escenas. En la guía se colorean con las clases `f-*` y `s-*` de `docs/interfaz/guia/partes/08-dibujo.css`, que no existen en el producto: entran como los dibujos de hoy, sin clases nuevas | 20, 91 (recorrido), 131 y 151 (puerta), 157 (mapa) y 38-81 (escenas). Hoy: `static/img/`, con `ilustraciones/` y `huerta/` | Sin empezar |
| C1-07 | `c1-armazon` | P26 · El armazón de la huerta en los dos layouts. El del galpón no entra (DA-18) | 15, 1-14 (común). Fuera: 15, 16-25 y 98-104 (galpón). Hoy: `layout-publico.html` (`header.raices-cabecera`, `:31`), `layout-privado.html` (`header.armazon`, `:26`), `app.css:114` y `privado.css` | Sin empezar |
| C1-08 | `c1-armazon` | P14 · La barra superior, el menú del celular, el salto al contenido y la campana. Las entradas de cada menú quedan como están (DA-17). El cajón no entra (DA-18) | Guía 315-356 (la campana, 340-341) y 445. Fuera: guía 417-427 (cajón). Hoy: `componentes/navegacion.css`, `componentes/campanita.css`, `componentes/campanita.html`, `publico-navegacion.css` y `privado.css`; muestras en `estilos.html:581` y `:669`; catálogo `:208` y `:210` | Sin empezar |
| C1-09 | `c1-armazon` | P18 · El pie de la huerta | Guía 380-384. Hoy: `pie.html:9` (`footer.raices-pie`) y `app.css:360`; sin muestra en `/estilos` ni fila en el catálogo | Sin empezar |
| C1-10 | `c1-armazon` | P16 · Las migas de pan, componente nuevo | Guía 369-373. Hoy no existe: ni `componentes/migas.css` ni ninguna clase `migas` | Sin empezar |
| C1-11 | `c1-formularios` | P03 · Los botones, con su estado «cargando» (`aria-busy`) | Guía 72-114 (el estado, 107-112). Hoy: `componentes/boton.css`; muestra en `estilos.html:378`; catálogo `:197` | Sin empezar |
| C1-12 | `c1-formularios` | P04 · Los campos, la opción y la casilla. La casilla del buzón no se toca: su demo en `/estilos` tiene una excepción de toque decidida por el usuario (DA-16) | Guía 115-156. Hoy: `componentes/campo.css` y `formularios.css`; muestra en `estilos.html:401`; catálogo `:198`. Sin ficha de opción ni de casilla | Sin empezar |
| C1-13 | `c1-formularios` | P05 · Subir fotos | Guía 456-461; 15, 60. Hoy: `componentes/subida.css`; muestra en `estilos.html:803`; catálogo `:219` | Sin empezar |
| C1-14 | `c1-estados` | P06 · El sello de los seis estados de la guía, cada uno con su signo y su palabra, y la chapa «Espera tu confirmación». Además, «vencida», «cancelada» y «en revisión» con el par neutro (DA-07). La guía no dibuja un signo para esos tres: el lote usa uno de los nueve de la guía y lo anota | Guía 157-178 (el par neutro, 159; los estados, 167-170; la chapa, 172). Hoy: `componentes/sello.css`, `componentes/sello.html:20` y `sellos.css`; muestras en `estilos.html:416` y `:430`; catálogo `:199-200` | Sin empezar |
| C1-15 | `c1-estados` | P13 · Los avisos: en la página y breve. La lista de novedades del mismo bloque no entra: es del buzón | Guía 280-313 y 481. Hoy: `componentes/aviso.css`; muestra en `estilos.html:439`; catálogo `:201` | Sin empezar |
| C1-16 | `c1-estados` | P12 · El avance de una necesidad | Guía 274-279. Hoy: `componentes/progreso.css`; muestra en `estilos.html:962`; catálogo `:225` | Sin empezar |
| C1-17 | `c1-entrega` | P07 · La lámina y la sombra de paja | Guía 179-190. Hoy: `componentes/tarjeta.css`; muestra en `estilos.html:751`; catálogo `:212` | Sin empezar |
| C1-18 | `c1-entrega` | P08 · La foto de una entrega y su etiqueta, componente nuevo con el nombre `etiqueta-entrega`, que va de prefijo en todas sus clases. `.etiqueta`, `.foto` y `.foto-falta` ya son clases heredadas de pantallas que no son de la ola | Guía 191-223; 15, 121. Heredadas: `entidad-seguimiento-confirmar.css:47` y `cliente-liviano/src/test/resources/verificacion/clases-heredadas.txt:99-101, 106-107` | Sin empezar |
| C1-19 | `c1-entrega` | P11 · La trama, componente nuevo. Solo en fondos | Guía 268-273. Hoy no existe `componentes/trama.css` | Sin empezar |
| C1-20 | `c1-raiz` | P09 · La raíz de los 5 pasos, con sus cuatro formas (la base, acostada, numerada y tendida) y el paso «no pudo llegar» | Guía 224-263 (acostada, 250) y 490-502; 15, 39-40 (numerada) y 94-97 (tendida). Hoy: `componentes/huerta.css` y `componentes/huerta.html`; muestras en `estilos.html:785` y `:798`; catálogo `:216` | Sin empezar |
| C1-21 | `c1-raiz` | P10 · Los frutos: un fruto por entrega confirmada | Guía 264-267 y 484. Hoy: fragmento `cifras(estado, valores)` en `componentes/huerta.html:314` | Sin empezar |
| C1-22 | `c1-listas` | P15 · Las pestañas | Guía 357-367 y 448. Hoy: `componentes/pestanas.css`; muestra en `estilos.html:830`; catálogo `:220` | Sin empezar |
| C1-23 | `c1-listas` | P17 · El paginador | Guía 374-379. Hoy: `componentes/paginas.css` y `componentes/paginas.html:20-24`; muestra en `estilos.html:1250`; catálogo `:234` | Sin empezar |
| C1-24 | `c1-listas` | P19 · Las listas y sus estados: vacía, cargando (con el esqueleto) y con error | Guía 385-394 y 406-416. Hoy: `componentes/lista.css` y `componentes/carga.css`; muestras en `estilos.html:776`, `:469` y `:487`; catálogo `:217`, `:203` y `:204` | Sin empezar |
| C1-25 | `c1-listas` | P20 · La tabla que se apila en el celular. La tabla del galpón, con selección, no entra (DA-18) | Guía 503-520; 15, 187-195. Fuera: guía 395-405. Hoy: `componentes/tabla.css`; muestra en `estilos.html:1235`; catálogo `:218` | Sin empezar |
| C1-26 | `c1-listas` | P23 · La lista de datos, componente nuevo | 15, 172-176. Hoy no existe `componentes/datos.css`; `datos-figura` es una clase heredada de otra cosa (`clases-heredadas.txt:58`) | Sin empezar |

### 10.2 Qué queda fuera de C1

| Qué | Adónde va | Por qué |
|---|---|---|
| Las entradas de los cuatro menús (DA-01, DA-02, DA-03a, DA-03b, DA-04 y DA-05) | PUB1, ORG, DON y ADM1 | DA-17 |
| La tabla con selección (P21), el cajón (P22) y el armazón del galpón (la mitad de P26) | ADM1 | DA-18 |
| La lista de novedades y el buzón | Las olas de rol, con su ficha | Es una pantalla sin arquetipo (DA-08) |
| Bajar los topes de `InterfazFitnessTest` | Z | Dos lotes que bajan la misma línea chocan al integrar |
| `tokens.css` | No se toca | Los tokens entraron en C0. Si falta uno, se informa como bloqueante |

### 10.3 Muestra para G1

Dieciséis pantallas de hoy, sin rehacer, 4 por área. Están tal cual en
`cliente-liviano/src/test/resources/verificacion/paginas.txt`. No entran en `rutasVisuales`: sus
capturas salen de la pasada completa de `-Pvisual` que corre el cierre de la ola, y la hoja de G1
la arma la sesión que cierra C1.

| Área | Rutas |
|---|---|
| Lo público | `/publico-portada`, `/publico-donaciones`, `/publico-entidades`, `/publico-registro-donante` |
| Quien dona | `/donante-inicio`, `/donante-donaciones`, `/donante-donaciones-detalle`, `/donante-entidades` |
| La organización | `/entidad-inicio`, `/entidad-necesidades`, `/entidad-seguimiento`, `/entidad-seguimiento-confirmar` |
| El admin | `/admin-inicio`, `/admin-asignaciones`, `/admin-donaciones`, `/admin-entidades` |

### 10.4 Qué se verifica y cuándo (DA-20)

- **En cada lote:** `mvn clean test -pl cliente-liviano -am` y `-Pvisual` sobre sus `rutasVisuales`,
  a 360, 1280 y 1920 px. Todos los lotes llevan solo `/estilos`: una pantalla real con el armazón
  nuevo y el contenido de hoy le daría bloqueantes falsos a la revisión, que trata como bloqueante
  toda inconsistencia entre pantallas (`.claude/workflows/oleada-ui.js:407, 510`). `c1-armazon`
  comprueba además el armazón en `/publico-portada`, `/entidad-inicio` y `/admin-inicio` antes de
  comitear; esas tres no entran en la revisión ni en la hoja de contacto.
- **Al cierre de la ola, una vez:** `-Pvisual` sobre las 320 rutas. Es el piso de G1.
- **Línea de base de esa pasada.** `[OBSERVED]` El cierre de C0 midió 12 rutas en rojo, todas por
  área de toque y ninguna por contraste (§3.4; [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítems 3 y
  4). `[INFERRED]` Quedan 11, porque `/estilos` pasa desde la excepción de DA-16: `/admin-notificaciones`, `/donante-notificaciones`, `/entidad-notificaciones`,
  `/admin-camiones`, `/admin-camiones-agregar-error`, `/donante-seguimiento`,
  `/donante-misiones?cuenta=persona` (con y sin `escenario=vacio`), `/admin-donantes?donante=juan` y
  `/publico-mapa?escenario=muchas` (con y sin la página 2). Una ruta que falle y no esté en esta
  lista es de una pieza de C1.
- **Los otros tests visuales.** `ArmazonFijoTest`, `MenusQueSeCierranTest`, `BajoTierraRotulosTest`,
  `AbejasYRenglonesTest` y `DonanteDetallesTest` solo se compilan y corren con `-Pvisual`
  (`cliente-liviano/pom.xml:41-47`), y el gate visual del workflow corre solo
  `VerificacionVisualTest`. Los corre `c1-armazon` antes de comitear y el cierre de la ola. Nadie
  edita un test de `visual/`: si uno afirma algo que la guía cambia, se informa y lo decide el
  usuario.
- **Línea de base de esos cinco tests** `[VERIFIED]` (2026-11-22; `mvn test -pl cliente-liviano -am
  -Pvisual -Dtest=…`, 31 tests). Fallan 4, igual sobre `397896a79` que sobre `093aed884`, antes de
  C0: `ArmazonFijoTest.laCabeceraYLaBarraQuedanFijasSinHuecoEntreEllas`,
  `ArmazonFijoTest.enElCelularConTabYShiftTabElFocoNoQuedaTapado`,
  `ArmazonFijoTest.enElCelularElBotonDeConfirmarQuedaEncimaDelMenuHastaElFinal` y
  `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`. Además,
  `MenusQueSeCierranTest.lasPastillasDeFiltroSeCierranTocandoAfueraYConEsc` falló en 1 de 3
  corridas: es inestable. Ninguna de las cinco es de C1; si `c1-armazon` arregla alguna de las de
  `ArmazonFijoTest` al rehacer el armazón, lo anota.
- **El stack real con login no se levanta en C1.** `/estilos` es de la maqueta y C1 no toca ningún
  adaptador `rest`. El recorrido real se hace en ORG y en Z.
- **Los topes no se tocan.** Clases de un solo uso, 159 (`InterfazFitnessTest.java:253`); medidas
  literales, 1192 (`:567`). El test acepta un valor igual o menor (`:392-393, 613-615`).
  `[VERIFIED]` El de medidas literales no tiene margen: hoy el conteo es 1192, igual al tope
  (réplica de `medidasLiterales`, `:569-572` y `:643-651`, sobre las 74 hojas, 2026-11-22). Cada
  lote deja el conteo de sus hojas igual o menor: una medida que la guía escribe como número pasa
  al token más cercano o se compensa en las hojas del lote, y la diferencia se anota.
