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
Ronda 4 (2026-11-23, después del merge de C1): DA-04, DA-03b, DA-03c, DA-05, DA-06, DA-08, DA-13
y la casilla del buzón, que pasa a ser DA-21. Respuesta del usuario: «Todas las recomendadas».
G1 no entra en esta ronda: se responde por área, mirando la hoja.
Corrida desatendida (2026-11-24): DA-09, DA-10, DA-12 y DA-14b se resuelven con la opción
recomendada y DA-11 cambia a Opus medium, «delegada por el relevamiento del 2026-11-24» (plan
`.claude/plan-guia-resto.md`, aprobado por el usuario).
Manifiesto de PUB1 (2026-11-24, misma corrida): DA-22, DA-23 y DA-24 son decisiones nuevas que el
usuario no respondió. Van con la opción recomendada, «Delegada por el relevamiento del 2026-11-24
(corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md)»; la opción descartada
queda en la columna de la recomendación.

| ID | Pregunta | Recomendación | Respuesta del usuario | Fecha | Ola |
|---|---|---|---|---|---|
| DA-01 | Público: ¿el Mapa sale del menú y queda dentro de Entregas? | Sí, como dibuja la guía | Sí («3 si», ronda 2) | 2026-11-22 | PUB1 |
| DA-02 | Organización: ¿«Donaciones recibidas» y «Seguir entregas» se funden en «Entregas»? | Sí | Sí («3 si», ronda 2) | 2026-11-22 | ORG |
| DA-03a | Quien dona: ¿«Seguimiento» deja de ser una sección del menú? (el seguimiento de cada donación se abre desde su detalle) | Sí | Sí («3 si», ronda 2) | 2026-11-22 | DON |
| DA-03b | Quien dona, la persona: ¿dónde va Misiones? | Como cuarta entrada del menú de la persona: Inicio, Donaciones, Organizaciones, Misiones y Cómo donar | Como cuarta entrada («Todas las recomendadas», ronda 4) | 2026-11-23 | DON |
| DA-03c | Quien dona, la persona: ¿cómo es su inicio, que la guía no dibuja? | Deriva de A04 (`#inicio-donante`): saludo, aviso de problema, «En camino ahora» y noticias, más la tarjeta de acceso a la huerta de hoy y sin lo que es solo de la empresa. Con ficha (DA-08) | Deriva de A04, con ficha («Todas las recomendadas», ronda 4) | 2026-11-23 | DON |
| DA-04 | Admin: ¿«Por decidir» reemplaza a Inicio y a Asignar? | Sí; los gráficos de «Cómo está todo» de `admin-inicio` van a una ficha de ADM1, debajo de la bandeja | Sí («Todas las recomendadas», ronda 4) | 2026-11-23 | ADM1 |
| DA-05 | «Cómo funciona», «Cómo donar» y «Ayuda»: ¿páginas nuevas o las de hoy con otro nombre? | Las de hoy (Nosotros, Para donantes, Para organizaciones) | Las de hoy: Nosotros pasa a «Cómo funciona», Para donantes a «Cómo donar» y Para organizaciones a «Ayuda» («Todas las recomendadas», ronda 4) | 2026-11-23 | PUB1, DON y ORG |
| DA-06 | ¿Página propia de respuesta solo al confirmar una recepción, y aviso breve en el resto? | Sí, es la regla de la guía | Sí («Todas las recomendadas», ronda 4) | 2026-11-23 | Roles |
| DA-07 | Sello para «vencida», «cancelada» y «en revisión» | El par neutro, con su texto | El par neutro, con su texto («Todas las recomendadas», ronda 3) | 2026-11-22 | C1 |
| DA-08 | Pantallas sin arquetipo: ¿ficha aprobada por el usuario o derivación del agente? | Ficha para Misiones, Ranking, inicio de la persona y buzón; el resto, derivación | Ficha para esas cuatro; el resto, derivación («Todas las recomendadas», ronda 4) | 2026-11-23 | Roles |
| DA-09 | HG-01, la nueva salida: ¿función nueva o texto sin fecha? | Texto sin fecha; la función, después de la defensa | Texto sin fecha; HG-01 no se construye (delegada por el relevamiento del 2026-11-24) | 2026-11-24 | Z |
| DA-10 | HG-03: ¿el nombre de quien recibió se muestra en lo público? | En lo privado sí; en lo público, la organización | En lo público, la organización (delegada por el relevamiento del 2026-11-24) | 2026-11-24 | Z |
| DA-11 | ¿Se acepta Opus high en los lotes con pantallas, como fija el workflow? | Sí | Sí («todas las recomendadas», ronda 1). Cambiada el 2026-11-24: desde DON, los lotes con pantallas van en Opus medium, como en C1 y ORG (delegada por el relevamiento del 2026-11-24) | 2026-11-22 | Todas |
| DA-12 | ¿Los huecos de backend van como PRs sueltos a `ENTREGA_7`? | Sí | Sí, PRs sueltos a `ENTREGA_7` (delegada por el relevamiento del 2026-11-24) | 2026-11-24 | Z |
| DA-13 | Las 6 plantillas de `persona/` que solo sirve la maqueta: ¿se migran con su gemela o se borran? | Se migran; borrarlas es otra decisión | Se migran («Todas las recomendadas», ronda 4) | 2026-11-23 | DON |
| DA-14a | En C0: ¿los 10 nombres de color de hoy pasan a ser alias del token de la guía del mismo valor (sin cambiar ningún píxel) y se borran los 3 tokens que nadie usa? | Sí | Sí («todas las recomendadas», ronda 1). Corregida el 2026-11-22: se borran 3 y no 4, porque `--medida-quiebre-medio` sí se usa (ver §3.2). El usuario lo aceptó al elegir, entre tres caminos para la corrida de C0, «parar, corregir la decisión y relanzar» (su respuesta: «Opción B»). Esa «B» no tiene relación con ninguna otra lista de opciones | 2026-11-22 | C0 |
| DA-14b | En Z: ¿se borran los alias que quedaron sin usos y los 11 tokens que no usa ninguna hoja ni plantilla y solo nombra `InterfazFitnessTest`, cambiando su lista de pares por la de la guía? | Sí | Sí (delegada por el relevamiento del 2026-11-24) | 2026-11-24 | Z |
| DA-15 | Si el 30/11 no llegó todo: ¿corte por área con la regla G2, o todo o nada? | Corte por área | Corte por área («todas las recomendadas», ronda 1) | 2026-11-22 | Z |
| DA-16 | La excepción de toque que el cierre de C0 agregó a `VerificacionVisualTest` para la casilla del buzón en `/estilos` (`e7ba71c36`), sin consultar: ¿se mantiene o se revierte? | Ninguna: se presentaron las dos | Se mantiene («Mantenerla»). Queda en el [ADR de la excepción de toque](../adr/cliente-liviano/20261122-excepcion-de-toque-para-la-casilla-del-buzon-en-estilos.md) (`proposed`) y en [DTI-46](../adr/DEUDA_TECNICA.md#dti-46) | 2026-11-22 | C0 |
| DA-21 | La casilla del buzón: `VerificacionVisualTest` mide el `input` de 12 px y no su etiqueta de 48 px, y ORG, DON y ADM2 rehacen el buzón. ¿Cómo pasa el toque? | Que la casilla mida 48 px de verdad, con el patrón de C1-12 (el control nativo transparente encima de todo el bloque): no cambia el test ni pide ADR. Las otras: extender la excepción de DA-16 a las rutas del buzón, o que el test mida la etiqueta (las dos cambian el test y piden ADR) | La casilla de 48 px («Todas las recomendadas», ronda 4) | 2026-11-23 | ORG, DON y ADM2 |
| DA-22 | Portada: A01 no dibuja las abejas, el corte «bajo tierra» ni el cierre de la portada de hoy (la variante K), y el ADR «Huerta y galpón» deja abierto qué pasa con ellas. ¿La portada sigue a A01 sin esas piezas, o las conserva debajo de A01? | A01 sin esas piezas. `AbejasYRenglonesTest` y `BajoTierraRotulosTest`, que las miden en `/publico-portada`, no se editan: sus fallos se anotan para que decida el usuario, como el lema del pie en C1 (§10.5, [DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). `static/js/abejas.js` y los fragmentos de `componentes/huerta.html` que la portada deja de usar no se borran en PUB1. Descartada: conservarlas debajo de A01 (los tests siguen pasando, pero una pantalla «directa» deja de ser fiel a su arquetipo y suma composición que la guía no tiene) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | PUB1 |
| DA-23 | El pie: con DA-05, Nosotros, Para donantes y Para organizaciones pasan a llamarse «Cómo funciona», «Cómo donar» y «Ayuda». La guía dibuja el pie con los nombres de hoy. ¿El pie los nombra con el nombre nuevo? | Sí: cambia solo el texto de esos tres enlaces de `pie.html`; las rutas y el orden siguen. Descartada: el pie con los nombres de hoy (cada enlace llevaría a una página que se llama de otra forma) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | PUB1 |
| DA-24 | Contacto: con DA-06, la respuesta es un aviso breve. ¿Adónde lleva el envío y dónde queda el número del mensaje? | A Contacto (`/publico-contacto`, con su `?volver=` si vino), con el aviso breve que lleva el número («Recibimos tu mensaje. Su número es 4821. Te respondemos por correo.»). Con `?volver=`, Contacto ofrece la salida «Volver a …» que hoy tiene la página de enviado. En la maqueta, `/publico-contacto-enviado` sigue y muestra Contacto con el aviso. Descartada: llevar a la pantalla de `?volver=` (la portada no tiene la región `#avisos` y el aviso se perdería) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | PUB1 |
| G0 | Con la medida de C0 (§3.4): ¿el plan sigue con todo el sitio o se recorta al recorrido de la demo? | Recorte al recorrido de la demo (ORG, DON y PUB1), con ADM1 por decidir en G1 | Todo el sitio («Quiero todo el sitio», ronda 3). Quedan 8 corridas: C1, ORG, DON, PUB1, ADM1, PUB2, ADM2 y Z | 2026-11-22 | Todas |
| G1 | Con la hoja de G1 (16 pantallas de hoy, §10.3): ¿cada área se puede mostrar así? | Ninguna: lo decide el usuario mirando la hoja | Las cuatro áreas se pueden mostrar así («Las 4 se muestran»). G2 permite que un área entre a medias. Es una excepción a la regla del piso del plan (§10: un área con una pantalla sin rehacer que falla el piso queda en «no»): las 11 rutas en rojo de la línea de base (§10.4), que tocan las cuatro áreas, fallan solo por toque, no por contraste. El usuario la mantuvo al conocer la regla («Mantener, excepción») | 2026-11-23 | Todas |
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
| C0-05 | `c0-tokens` | `/estilos` gana una sección «Tokens» (color, letra, espaciado, radio, borde, elevación y movimiento), antes de «Componentes». `/estilos` no tiene hoja propia y el cerco prohíbe `style` sueltos: las muestras de token van en `componentes/muestra.css`. `componentes.md` suma una sección de tokens, nueva | Hoy tiene Composición, Utilidades y Movimiento, sin sección de tokens: `cliente-liviano/src/main/resources/templates/estilos.html:21-147`. Encabezados de `docs/interfaz/componentes.md`: no hay ninguno de tokens | Parcial (`9e995ec42`, `ebce607ad` y `7d413d1b2`). Está la sección «Tokens» con 14 fichas y la sección de `componentes.md`. Falta: las 14 fichas están escritas a mano y no usan el fragmento `componentes/muestra :: muestra` (bloqueante de la revisión tras 3 rondas); `/estilos` pasa el gate visual solo por la excepción de `e7ba71c36`, que no decidió el usuario ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítems 1 y 2). **Actualización (2026-11-23):** C1-04 pasó las 14 fichas al fragmento (`1447644f2`), y la excepción quedó decidida por el usuario (DA-16) |
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
| Ayuda: no es una página nueva | — | La entrada del menú lleva a `publico-para-entidades` (DA-05) |

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
| `donante-misiones` | Sin arquetipo | DA-03b y DA-08 (ficha) |
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
| `publico-nosotros`, `publico-para-donantes`, `publico-para-entidades` | Deriva de A01 | «Cómo funciona», «Cómo donar» y «Ayuda», en ese orden (DA-05) |
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
| HG-01 | Fecha de la nueva salida de una entrega que no pudo llegar | No existe. Solo hay un booleano `replanificable` en el pedido: `logistica-service/src/main/java/grupo5/logistica/dto/entregas/CambioEstadoEntregaRequestDTO.java:13`; se guarda en `logistica-service/.../jpa/entities/EventoEntregaEntity.java:48-49` | A08, A11, avisos de A04 y A09, A02, A10 | No se construye: texto sin fecha (DA-09) | «Todavía sin fecha de nueva salida» |
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
| C1-01 | `c1-base` | P01 · La base y el aro de foco de la guía. El aro queda de un solo color (hoy usa dos, [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 5). El catálogo deja de nombrar a `pico` como primera capa | Guía 1-43. Hoy: `app.css`, desde el principio hasta antes de `.raices-cabecera` (`app.css:114`); capas en `app.css:8`; catálogo `:36` | Hecho (`1447644f2`): la base va en `@layer base`, el aro de foco es de un solo color y el catálogo ya no nombra a `pico` ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 5, saldado); el veredicto de su revisión no consta en el cierre |
| C1-02 | `c1-base` | P02 · La composición y la medida del texto | Guía 44-71 y 453-454. Hoy: `composicion.css`; muestras en `estilos.html:21-87`; catálogo `:40-86` | Hecho (`1447644f2`): la medida del texto en `main p`, `data-alinea` con varias palabras (`arriba`) y `data-espacio="0"`; `.pila` sigue con margen y no con `gap`; el veredicto de su revisión no consta en el cierre |
| C1-03 | `c1-base` | P27 · El movimiento reducido y el trazo, más las animaciones con nombre que piden otras piezas: el giro del botón que carga, el latido del esqueleto y el trazo de la raíz, con su asomo y su brote. Llevan el prefijo `mov-` de `movimiento.css`, no el `dt-` de la guía; `mov-traza` ya existe. Sus duraciones usan los tokens `--movimiento-duracion-*` que ya existen: la guía escribe 0,9 s para el giro (`:111`) y 1,4 s para el latido (`:412`), que no son tokens, y se usa el más cercano. Las demás piezas solo las nombran | Guía 428-431, 108-113 y 411-415; 15, 177-184. Hoy: `movimiento.css` (el prefijo, `:4-5`; `mov-traza`, `:131`); muestras en `estilos.html:89-145`; catálogo `:87-142`. Los `@keyframes` van solo en `movimiento.css` y cada `data-movimiento` va en el catálogo y en las muestras (`InterfazFitnessTest.java:433, 365-379`) | Hecho (`1447644f2`): `mov-gira`, `mov-late` y `mov-brote` nuevos; asoma y trazo reusan `mov-aparece` y `mov-traza`. El giro va con `--movimiento-duracion-larga` (0,75 s, la guía 0,9 s) y el latido con `--movimiento-duracion-trazo` (1,1 s, la guía 1,4 s), anotado; el veredicto de su revisión no consta en el cierre |
| C1-04 | `c1-base` | Las 14 fichas de la sección «Tokens» de `/estilos` pasan al fragmento `componentes/muestra :: muestra(nombre, cuando, cuerpo)` | Fichas a mano en `estilos.html:147-372`; fragmento en `componentes/muestra.html:11`; [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 1 | Hecho (`1447644f2`): las 14 fichas usan `componentes/muestra :: muestra` ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 1, saldado). Sus títulos en prosa salen ahora en monoespaciada ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)); el veredicto de su revisión no consta en el cierre |
| C1-05 | `c1-simbolos` | P25 · Los 17 íconos y los 9 signos de estado de la guía entran en `iconos.html`. Los `id` de hoy (`nav-*` y los de categorías) se conservan | 20, 6-24 (íconos) y 27-35 (signos). Hoy: `iconos.html`, 20 símbolos y ningún signo de estado | Hecho (`cb50bb372` y `b5cef7d93`): 17 íconos y 9 signos en los fragmentos nuevos `iconos :: comunes` e `iconos :: signos`; los `id` de hoy siguen. El signo es `.icono[data-forma="signo"]`, no `.signo`. Ningún layout carga todavía esos fragmentos; el veredicto de su revisión no consta en el cierre |
| C1-06 | `c1-simbolos` | P24 · Los dibujos de la guía: el recorrido, la puerta, el mapa y las 7 escenas. En la guía se colorean con las clases `f-*` y `s-*` de `docs/interfaz/guia/partes/08-dibujo.css`, que no existen en el producto: entran como los dibujos de hoy, sin clases nuevas | 20, 91 (recorrido), 131 y 151 (puerta), 157 (mapa) y 38-81 (escenas). Hoy: `static/img/`, con `ilustraciones/` y `huerta/` | Hecho (`cb50bb372`): 10 archivos en `static/img/ilustraciones/` (recorrido, puerta, mapa y 7 escenas), con los colores en hex como los dibujos de hoy. Las partes de la puerta que cambian con el estado las dibuja la pantalla (ORG); el veredicto de su revisión no consta en el cierre |
| C1-07 | `c1-armazon` | P26 · El armazón de la huerta en los dos layouts. El del galpón no entra (DA-18) | 15, 1-14 (común). Fuera: 15, 16-25 y 98-104 (galpón). Hoy: `layout-publico.html` (`header.raices-cabecera`, `:31`), `layout-privado.html` (`header.armazon`, `:26`), `app.css:114` y `privado.css` | Hecho (`41bc195b8` y `00e6aed69`): el armazón común de la huerta en los dos layouts; el del galpón queda para ADM1 (DA-18). PASS de su revisión en la ronda 1 |
| C1-08 | `c1-armazon` | P14 · La barra superior, el menú del celular, el salto al contenido y la campana. Las entradas de cada menú quedan como están (DA-17). El cajón no entra (DA-18) | Guía 315-356 (la campana, 340-341) y 445. Fuera: guía 417-427 (cajón). Hoy: `componentes/navegacion.css`, `componentes/campanita.css`, `componentes/campanita.html`, `publico-navegacion.css` y `privado.css`; muestras en `estilos.html:581` y `:669`; catálogo `:208` y `:210` | Hecho (`41bc195b8` y `00e6aed69`): la barra, el menú del celular hasta 56 em en todos los roles, el salto y la campana (sus reglas pasan a `componentes/campanita.css`); ninguna entrada de menú cambió (DA-17). Pendiente para el usuario: la barra no queda fija arriba en el celular, como pide la guía, porque rige R11-33 ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). PASS de su revisión en la ronda 1 |
| C1-09 | `c1-armazon` | P18 · El pie de la huerta | Guía 380-384. Hoy: `pie.html:9` (`footer.raices-pie`) y `app.css:360`; sin muestra en `/estilos` ni fila en el catálogo | Hecho (`41bc195b8`): el pie como corte de suelo, sin el lema ni los fondos. Pendiente para el usuario: `AbejasYRenglonesTest.loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos` da error porque mide el lema que la guía no dibuja ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). PASS de su revisión en la ronda 1 |
| C1-10 | `c1-armazon` | P16 · Las migas de pan, componente nuevo | Guía 369-373. Hoy no existe: ni `componentes/migas.css` ni ninguna clase `migas` | Hecho (`41bc195b8`): `componentes/migas.css` y su fragmento, con la flecha en línea y el enlace de 48 px. PASS de su revisión en la ronda 1 |
| C1-11 | `c1-formularios` | P03 · Los botones, con su estado «cargando» (`aria-busy`) | Guía 72-114 (el estado, 107-112). Hoy: `componentes/boton.css`; muestra en `estilos.html:378`; catálogo `:197` | Hecho (`6051f2a1a`, `c7814ee54`, `a5d384c7a` y `7d4fcefc0`): el botón con `aria-busy` y la ruedita (`--movimiento-animacion-gira`); sin las variantes de texto e ícono, que nadie usa. El lote quedó sin PASS tras 3 rondas, por C1-13 |
| C1-12 | `c1-formularios` | P04 · Los campos, la opción y la casilla. La casilla del buzón no se toca: su demo en `/estilos` tiene una excepción de toque decidida por el usuario (DA-16) | Guía 115-156. Hoy: `componentes/campo.css` y `formularios.css`; muestra en `estilos.html:401`; catálogo `:198`. Sin ficha de opción ni de casilla | Hecho (`6051f2a1a`, `c7814ee54`, `a5d384c7a` y `7d4fcefc0`): el campo, el error con su signo y la opción y la casilla sobre `.opciones`, con el control nativo transparente encima de todo el bloque para llegar a 48 px. La casilla sola de 48 × 48 px queda para ADM1. El lote quedó sin PASS tras 3 rondas, por C1-13 |
| C1-13 | `c1-formularios` | P05 · Subir fotos | Guía 456-461; 15, 60. Hoy: `componentes/subida.css`; muestra en `estilos.html:803`; catálogo `:219` | Hecho (`6051f2a1a`, `c7814ee54`, `a5d384c7a`, `79d0eaae0` y `311a01477`, después de la corrida: el control a todo el ancho, con el botón arriba). Antes faltaba: el control nativo de archivos corta su texto de estado a 360 px («N…n») y a 1280 px no lo muestra, así que sin JavaScript no se ve qué archivo se eligió (`subida.css:47-75`). Bloqueante de la revisión tras 3 rondas ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)) |
| C1-14 | `c1-estados` | P06 · El sello de los seis estados de la guía, cada uno con su signo y su palabra, y la chapa «Espera tu confirmación». Además, «vencida», «cancelada» y «en revisión» con el par neutro (DA-07). La guía no dibuja un signo para esos tres: el lote usa uno de los nueve de la guía y lo anota | Guía 157-178 (el par neutro, 159; los estados, 167-170; la chapa, 172). Hoy: `componentes/sello.css`, `componentes/sello.html:20` y `sellos.css`; muestras en `estilos.html:416` y `:430`; catálogo `:199-200` | Hecho (`ebb17dd0d`, `dfdb876d3` y `a976019af`): la chapa de los seis estados con su signo y su palabra, «vencida», «cancelada» y «en revisión» con el par neutro y el signo `e-info` (DA-07), y `.sello-pedido`. `/admin-donaciones` sigue mapeando «vencida» a `problema` hasta ADM1 ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). PASS de su revisión en la ronda 2 |
| C1-15 | `c1-estados` | P13 · Los avisos: en la página y breve. La lista de novedades del mismo bloque no entra: es del buzón | Guía 280-313 y 481. Hoy: `componentes/aviso.css`; muestra en `estilos.html:439`; catálogo `:201` | Hecho (`ebb17dd0d` y `a976019af`): `.aviso` nuevo con `data-tipo` y el aviso breve de la guía; los avisos breves reales todavía no traen signo ni botón de ícono, porque su marcado no es de la ola. PASS de su revisión en la ronda 2 |
| C1-16 | `c1-estados` | P12 · El avance de una necesidad | Guía 274-279. Hoy: `componentes/progreso.css`; muestra en `estilos.html:962`; catálogo `:225` | Hecho (`ebb17dd0d`): el avance con los tokens de la guía. PASS de su revisión en la ronda 2 |
| C1-17 | `c1-entrega` | P07 · La lámina y la sombra de paja | Guía 179-190. Hoy: `componentes/tarjeta.css`; muestra en `estilos.html:751`; catálogo `:212` | Hecho (`4c6d2a36c`, `0e574abf8` y `0f68f2e14`): la lámina con el filete fino y las variantes `data-papel` (protagonista, hueco, tierra); sale la sombra granulada. Con `tierra`, el atajo `background` borra la trama ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)); el veredicto de su revisión no consta en el cierre |
| C1-18 | `c1-entrega` | P08 · La foto de una entrega y su etiqueta, componente nuevo con el nombre `etiqueta-entrega`, que va de prefijo en todas sus clases. `.etiqueta`, `.foto` y `.foto-falta` ya son clases heredadas de pantallas que no son de la ola | Guía 191-223; 15, 121. Heredadas: `entidad-seguimiento-confirmar.css:47` y `cliente-liviano/src/test/resources/verificacion/clases-heredadas.txt:99-101, 106-107` | Hecho (`4c6d2a36c` y `0e574abf8`): `componentes/etiqueta-entrega.css` y su fragmento; la foto al costado se apila sola, sin media query. Las variantes de espera y de fila van con su pantalla en ORG; el veredicto de su revisión no consta en el cierre |
| C1-19 | `c1-entrega` | P11 · La trama, componente nuevo. Solo en fondos | Guía 268-273. Hoy no existe `componentes/trama.css` | Hecho (`4c6d2a36c` y `0f68f2e14`): `componentes/trama.css`, solo en fondos; el veredicto de su revisión no consta en el cierre |
| C1-20 | `c1-raiz` | P09 · La raíz de los 5 pasos, con sus cuatro formas (la base, acostada, numerada y tendida) y el paso «no pudo llegar» | Guía 224-263 (acostada, 250) y 490-502; 15, 39-40 (numerada) y 94-97 (tendida). Hoy: `componentes/huerta.css` y `componentes/huerta.html`; muestras en `estilos.html:785` y `:798`; catálogo `:216` | Hecho (`10f3d3b64` y `bd1c8b233`): el fragmento nuevo `huerta :: raiz-pasos`, con la base, acostada, numerada y numerada tendida (desde 56 em) y el paso con problema. La puerta y `.estado-raiz` quedan para ORG; el veredicto de su revisión no consta en el cierre |
| C1-21 | `c1-raiz` | P10 · Los frutos: un fruto por entrega confirmada | Guía 264-267 y 484. Hoy: fragmento `cifras(estado, valores)` en `componentes/huerta.html:314` | Hecho (`10f3d3b64`): el fragmento nuevo `huerta :: frutos(cuenta, periodo, recien)`, aparte de `cifras`; el veredicto de su revisión no consta en el cierre |
| C1-22 | `c1-listas` | P15 · Las pestañas | Guía 357-367 y 448. Hoy: `componentes/pestanas.css`; muestra en `estilos.html:830`; catálogo `:220` | Hecho (`c919e6848`, `6456a75fc` y `79d0eaae0`, después de la corrida: la ficha a todo el ancho). Antes faltaba la muestra: en `/estilos` a 1280 px la tercera pestaña queda escondida en la lista desplazable (`pestanas.css:64-69`, ficha dentro de la grilla). Bloqueante de la revisión tras 3 rondas ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)) |
| C1-23 | `c1-listas` | P17 · El paginador | Guía 374-379. Hoy: `componentes/paginas.css` y `componentes/paginas.html:20-24`; muestra en `estilos.html:1250`; catálogo `:234` | Hecho (`c919e6848`): el paginador de la guía, centrado como en el catálogo. El lote quedó sin PASS tras 3 rondas, por C1-22 y C1-24 |
| C1-24 | `c1-listas` | P19 · Las listas y sus estados: vacía, cargando (con el esqueleto) y con error | Guía 385-394 y 406-416. Hoy: `componentes/lista.css` y `componentes/carga.css`; muestras en `estilos.html:776`, `:469` y `:487`; catálogo `:217`, `:203` y `:204` | Hecho (`c919e6848`, `6456a75fc`, `d641c4b60` y `79d0eaae0`, después de la corrida: el aviso de error y la trama): la lista de filas, la vacía, el esqueleto que late y su caja. Antes faltaba: la lista con error usa el `.banner` y no el `.aviso` de error de la guía, y la vacía no lleva la trama. Además, el atajo `background` de `lista.css:74` la borraría. Bloqueante de la revisión tras 3 rondas ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)) |
| C1-25 | `c1-listas` | P20 · La tabla que se apila en el celular. La tabla del galpón, con selección, no entra (DA-18) | Guía 503-520; 15, 187-195. Fuera: guía 395-405. Hoy: `componentes/tabla.css`; muestra en `estilos.html:1235`; catálogo `:218` | Hecho (`c919e6848`, `2c97c0b57` y `6456a75fc`): se apila por debajo de 56 em, con el rótulo flotando en el margen de la celda, no en la grilla de la guía. Desde 56 em lleva la cabecera y la fila del galpón. `.tabla-caja` queda para una ola que edite plantillas. El lote quedó sin PASS tras 3 rondas, por C1-22 y C1-24 |
| C1-26 | `c1-listas` | P23 · La lista de datos, componente nuevo | 15, 172-176. Hoy no existe `componentes/datos.css`; `datos-figura` es una clase heredada de otra cosa (`clases-heredadas.txt:58`) | Hecho (`c919e6848`): `componentes/datos.css` y el fragmento `datos(filas)`. El lote quedó sin PASS tras 3 rondas, por C1-22 y C1-24 |

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

### 10.5 Cierre de C1 (2026-11-23)

Rama `E7_guia_ui_c1`, `d641c4b60`; 21 commits sobre `713608650`. Estado de cada ítem en §10.1:
23 hechos y 3 parciales (C1-13, C1-22 y C1-24). Los tres parciales eran los bloqueantes de la ronda 3 de `c1-formularios` y `c1-listas`; se arreglaron después de la corrida, en `79d0eaae0`, y quedan los 26 hechos. Lo que queda abierto está en
[DTI-47](../adr/DEUDA_TECNICA.md#dti-47); las decisiones, en la [bitácora](bitacora.md)
(2026-11-23).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am`: 834 tests, sin fallos.
  `InterfazFitnessTest` pasa con los topes de hoy (159 y 1192); ningún cerco se movió.
- `[VERIFIED]` `-Pvisual` sobre las 320 rutas: pasan 309. Fallan 11, todas de la línea de base de
  §10.4; ninguna es de una pieza de C1. `/estilos` pasa, con la excepción de DA-16.
- `[VERIFIED]` Los otros cinco tests visuales: siguen fallando los heredados
  (`ArmazonFijoTest.enElCelularConTabYShiftTabElFocoNoQuedaTapado`,
  `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen` y el inestable de
  `MenusQueSeCierranTest`). El armazón nuevo arregla dos de `ArmazonFijoTest`
  (`laCabeceraYLaBarraQuedanFijasSinHuecoEntreEllas` y
  `enElCelularElBotonDeConfirmarQuedaEncimaDelMenuHastaElFinal`). Uno nuevo da error:
  `AbejasYRenglonesTest.loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos` mide el lema del
  pie, que la guía no dibuja. Lo decide el usuario (DTI-47).
- El stack, el seed y el recorrido con login no se corrieron (DA-20): quedan para ORG y Z.
- Revisión de los lotes: `c1-armazon` con PASS en la ronda 1 y `c1-estados` en la ronda 2.
  `c1-formularios` y `c1-listas` quedaron sin PASS tras 3 rondas. Para `c1-base`, `c1-simbolos`,
  `c1-entrega` y `c1-raiz`, el veredicto no consta en el cierre. La revisión de pantallas
  de `/estilos` no encontró incumplimientos (U1 a U7).
- Muestra para G1: las capturas a 360 y 1280 px de las 16 rutas de §10.3. Se volvieron a tomar
  sobre `E7_guia_ui` en `ab79d7551`, después del merge (`-Pvisual` sobre las 16, exit 0). La hoja
  quedó fuera del repo, en `guia-c1/G1/hoja-g1.html`. G1, el 2026-11-23: las cuatro áreas se
  pueden mostrar así (§2.2).

## 11. Ola ORG: la organización

Rehace las 18 plantillas de la organización de §5 y el buzón, que es una pieza compartida con
quien dona y el admin. Manifiesto: [`backlog-guia-al-codigo.org.manifiesto.json`](backlog-guia-al-codigo.org.manifiesto.json).
Reparto en 6 lotes y 2 tandas, aprobado por el usuario el 2026-11-23 («OK al reparto»). Ficha del
buzón: [`flujos/guia-org-buzon.md`](flujos/guia-org-buzon.md), aprobada el mismo día (DA-08, DA-19).

### 11.1 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| ORG-01 | `org-buzon` | El buzón de los tres roles con la fila de la campana de la guía, según su ficha | Sin arquetipo: ficha (DA-08) | Hecho (`d9e8b73d7`): «Novedades» en los tres roles, la fila de la campana (punto lleno o hueco, sin fondo por el estado) y los estados de lista del catálogo en la organización y en quien dona; del admin cambian el título y la pieza. PASS de su revisión en la ronda 1 |
| ORG-02 | `org-buzon` | La casilla de cada fila y la de elegir todas con un control de 48 × 48 px | DA-21 | Hecho (`d9e8b73d7`): la casilla de cada fila y la de elegir todas miden 48 × 48 px, con el control nativo transparente encima de la casilla dibujada (DA-21). Las tres rutas `/…-notificaciones` salen de la línea de base de §10.4 `[VERIFIED]`. PASS de su revisión en la ronda 1 |
| ORG-03 | `org-inicio` | Inicio de la organización, con los dos estados que decide el servidor. El avance de una necesidad, con la cuenta de donaciones de hoy (HG-04) | A09 | Hecho (`74f441ee4`, ajustado por `e7c9d1327`): los dos estados (con entrega en la puerta y sin ella), la puerta con hasta 5 frutos y la cuenta debajo, «Lo que viene» y las necesidades con la cuenta de hoy (HG-04). `[VERIFIED]` en el recorrido con login. Sin rondas de arreglo; el veredicto de su revisión no consta en el cierre |
| ORG-04 | `org-necesidades` | Necesidades y su error al publicar | A02; el error deriva de A12 | Hecho (`279ce7cef`, `5f713b7db`, `b99ddff71` y los arreglos en sesión del 2026-11-23): la lámina protagonista, las tarjetas plegables y el error al publicar. Los bloqueantes de la ronda 3 se arreglaron en sesión: `.plegable-ver` con un solo rótulo y la flecha del catálogo, sin tamaño de letra literal y sin la barra punteada del período en curso ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 1) |
| ORG-05 | `org-necesidades` | Publicada, cambiada y dada de baja pasan a aviso breve en Necesidades; en la maqueta, sus rutas siguen y muestran Necesidades con el aviso | DA-06 | Hecho: el comportamiento está y se `[VERIFIED]` en el recorrido con login (publicar termina en Necesidades con el aviso breve «Publicamos tu necesidad…»); en la maqueta las rutas siguen y muestran Necesidades con el aviso. DA-06 cumplida con los arreglos en sesión de ORG-04 |
| ORG-06 | `org-perfil` | El perfil, y «guardado» pasa a aviso breve; en la maqueta, su ruta sigue | Deriva de A12; DA-06 (DA-06-perfil en el manifiesto) | Hecho (`728e65683`, `206096090` y `9e409b266`): «Mi perfil» con la columna del formulario de A12, la hoja nueva `entidad-perfil.css` (solo dispone) y «guardado» como aviso breve en el perfil (DA-06-perfil). PASS de su revisión en la ronda 2 |
| ORG-07 | `org-entregas` | Entregas: las donaciones recibidas y el seguimiento en una sola pantalla | A10; DA-02 | Hecho (`e7c9d1327`, `808d801b5` y `1520674a0`): Entregas (`/entidad-seguimiento`) con Para confirmar, Por llegar y Confirmadas; `/entidad-recibidas` redirige ahí con su consulta. En el sitio real las confirmadas salen sin foto, porque la lista de Logística no la trae ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 4). PASS de su revisión en la ronda 2 |
| ORG-08 | `org-entregas` | Página nueva: entrega con problema (`/entidad-entrega-problema`). La nueva salida, con «Todavía sin fecha de nueva salida» (HG-01) | A11 | Hecho (`e7c9d1327` y `1520674a0`): `/entidad-entrega-problema`, con la raíz de 4 pasos sin horas y «Todavía sin fecha de nueva salida» (HG-01). El seed no deja ninguna entrega con problema real: con datos solo se vio en la maqueta ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 9). PASS de su revisión en la ronda 2 |
| ORG-09 | `org-entregas` | El menú de la organización: Inicio, Necesidades, Entregas y Ayuda, que lleva a Para organizaciones | DA-02, DA-05 y DA-17 | Hecho (`e7c9d1327`): Inicio, Necesidades, Entregas y Ayuda (`/publico-para-entidades`), con el ícono `nav-ayuda`; los otros menús no cambian (DA-17). `[VERIFIED]` en el recorrido. PASS de su revisión en la ronda 2 |
| ORG-10 | `org-confirmar` | Confirmar recepción y su error; «llegó con un problema» | A12; el problema deriva de A12 | Hecho (`ea633995f`, `9231595e8`, `29123f4da`, `3eca434eb` y `35af5ec8e`): Confirmar recepción con «Revisá 2 cosas para confirmar», las migas y el error de la foto, y «llegó con un problema» en su página. `[VERIFIED]` en el recorrido, con foto y remito y con «Llegó menos». El lote quedó sin PASS tras 3 rondas, por ORG-11 y ORG-12; sus bloqueantes se arreglaron en sesión el 2026-11-23 |
| ORG-11 | `org-confirmar` | Recepción confirmada, en sus tres variantes. Quién recibió, con «Recibió: <la organización>» (HG-03) | A13 | Hecho (con los arreglos en sesión del 2026-11-23): las tres variantes, «Recibió: <la organización>, el <día>» (HG-03), los frutos del mes y las fechas de los pasos. Los frutos se cuentan en la plantilla, como en Inicio; «Tu necesidad» se muestra con la necesidad que cubre cuando no llega cómo va (HG-04), como en Llegó con un problema ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 2) |
| ORG-12 | `org-confirmar` | El adaptador real arma la vista de una entrega con el remito, la foto y quién confirmó, con lo que Logística ya entrega (§7) | — | Hecho (con los arreglos en sesión del 2026-11-23): el remito, la foto, quién confirmó y la hora (`[VERIFIED]` en el recorrido: `R-0001-00012345` y la foto subida). «Llegó a tu puerta» solo con el cambio a `EN_PUERTA` del historial, y el problema asentado sale del motivo del reporte de Logística (`EntidadEntregaVista.problema`); `?problema=` solo lo usa la maqueta ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 2) |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5).

### 11.2 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
- **Al cierre:** el stack con el seed y el recorrido con login, porque ORG toca un adaptador `rest`
  (`EntidadConfirmarRest`); `-Pvisual` sobre las rutas del área (las `/entidad-` de `paginas.txt` y
  los buzones de quien dona y del admin), no sobre las 320 (DA-20), con la línea de base de §10.4; y los
  otros cinco tests visuales, con la línea de base de §10.5.
- **Lo que cambia en la línea de base:** con DA-21, las tres rutas `/…-notificaciones` tendrían que
  pasar `[INFERRED]`; si pasan, se anota. Las otras 8 siguen siendo heredadas.

### 11.3 Cierre de ORG (2026-11-23)

Rama `E7_guia_ui_org`, `35af5ec8e`; 17 commits de lote sobre `8b13c79d1` (el manifiesto). Estado de
cada ítem en §11.1: 8 hechos (ORG-01 a ORG-03 y ORG-06 a ORG-10) y 4 parciales (ORG-04, ORG-05,
ORG-11 y ORG-12). Los parciales son los bloqueantes que dejaron `org-necesidades` y `org-confirmar`
tras 3 rondas. **Actualización (2026-11-23):** se arreglaron en sesión (`79b9e3f74`): los 12 ítems
quedan hechos; `mvn clean test -pl cliente-liviano -am` pasa con 861 tests y `-Pvisual` sobre las
rutas tocadas, sin excepciones. Lo que queda abierto está en [DTI-48](../adr/DEUDA_TECNICA.md#dti-48); las
decisiones, en la [bitácora](bitacora.md) (2026-11-23).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am`: 860 tests, sin fallos. `InterfazFitnessTest`
  pasa con los topes de hoy (159 y 1192); `tokens.css` y los tests de `visual/` no cambiaron. El test
  cambió solo en dos nombres: `entidad-notificaciones.css` sale de `HOJAS_HEREDADAS` (la hoja se
  borró) y en LISTAS `entidad-recibidas.html` pasa a `entidad-seguimiento.html`, como permite la
  regla de la ola.
- `[VERIFIED]` `-Pvisual` sobre las 74 rutas del área (las `/entidad-`, `/donante-notificaciones` y
  `/admin-notificaciones` de `paginas.txt`), a 360, 1280 y 1920 px: pasan las 74. Las tres
  `/…-notificaciones` de la línea de base de §10.4 ahora pasan (DA-21): la línea de base queda en 8
  rutas, ninguna del área. Esas 8 no se corrieron (DA-20).
- `[VERIFIED]` Los otros cinco tests visuales: 31 tests, 3 fallos y 1 error, todos de la línea de
  base de §10.5 (`ArmazonFijoTest.enElCelularConTabYShiftTabElFocoNoQuedaTapado`,
  `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`, el inestable de
  `MenusQueSeCierranTest` y el error del lema del pie, [DTI-47](../adr/DEUDA_TECNICA.md#dti-47),
  ítem 5). Ningún fallo nuevo.
- `[VERIFIED]` El stack con el seed y el recorrido con login (DA-20): con la organización, inicio
  (los dos estados), necesidades (con una necesidad publicada), entregas, confirmar con foto, llegó
  con un problema, perfil y el buzón; con quien dona y con el admin, el buzón. Para que anduviera
  hubo que mandar `DONACIONES_URL` directo al servicio: el gateway no enruta
  `/donaciones-independientes`, un hueco anterior a la ola ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48),
  ítem 8).
- Revisión de los lotes: `org-buzon` con PASS en la ronda 1; `org-perfil` y `org-entregas`, en la
  ronda 2; `org-necesidades` y `org-confirmar` quedaron sin PASS tras 3 rondas. Para `org-inicio`
  el veredicto no consta en el cierre (no tuvo rondas de arreglo).
- Commits de lote, en hora del 2026-11-23: `74f441ee4` (16:59), `d9e8b73d7` (17:03), `279ce7cef`
  (17:04), `728e65683` (17:06), `206096090` (17:14), `9e409b266` (17:34), `5f713b7db` (17:46),
  `b99ddff71` (18:02), `e7c9d1327` (18:37), `ea633995f` (18:46), `808d801b5` (18:49), `9231595e8`
  (18:51), `e71928b70` (18:56, integración de la tanda 2), `29123f4da` (19:19), `1520674a0`
  (19:21), `3eca434eb` (19:27) y `35af5ec8e` (19:46).
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); ninguna spec de `docs/specs/active/` es de esta ola.

## 12. Ola DON: quien dona

Rehace las 19 plantillas de quien dona de §5: 10 de la empresa y 9 de la persona (DA-13). El buzón
de quien dona ya cambió en ORG (ORG-01); acá solo sale su hoja heredada. Fichas, aprobadas por el
usuario el 2026-11-23 (DA-08, DA-19): [`flujos/guia-don-misiones.md`](flujos/guia-don-misiones.md) y
[`flujos/guia-don-inicio-persona.md`](flujos/guia-don-inicio-persona.md). Manifiesto:
[`backlog-guia-al-codigo.don.manifiesto.json`](backlog-guia-al-codigo.don.manifiesto.json). Se lanza con ORG ya mergeada en `E7_guia_ui`
([PR 1004](https://github.com/tsorren/DonaTrack-TP-DDS/pull/1004)).

### 12.1 Ronda 5 (2026-11-23): las preguntas de las fichas

Respuesta del usuario a las tres, a las fichas y al reparto: «va asi» (las recomendadas).

| ID | Pregunta | Recomendación | Respuesta del usuario | Fecha |
|---|---|---|---|---|
| DON-P1 | Misiones: ¿se cierra la prueba A/B con la huerta (`misiones-b`) como la única Misiones? | Sí; `misiones-a` y su hoja dejan de servirse y se borran en el lote | La recomendada («va asi») | 2026-11-23 |
| DON-P2 | Inicio de la persona: ¿qué pasa con el almanaque del año? | Se queda, chico: los 12 meses en una fila, con la cuenta y un fruto por entrega confirmada; la lista mes por mes sale | La recomendada («va asi») | 2026-11-23 |
| DON-P3 | Inicio de la persona sin nada en camino: ¿qué ocupa la lámina protagonista? | La huerta, con «Ver mi huerta» como acción llena | La recomendada («va asi») | 2026-11-23 |

### 12.2 Reparto

Aprobado por el usuario el 2026-11-23 («va asi»). Dos tandas: la segunda va después porque
`don-inicio-persona` toca `DonanteInicioController.java`, su maqueta y su test, que en la primera
toca `don-inicio`, y `don-misiones` depende del menú nuevo de la persona.

| Lote | Tanda | Plantillas | Arquetipo y decisiones | Ficha |
|---|:-:|---|---|---|
| `don-inicio` | 1 | `donante-inicio`; los menús de la empresa y de la persona | A04 directo; DA-03a, DA-03b, DA-05 y DA-17 | No |
| `don-donaciones` | 1 | `donante-donaciones` y `persona/donante-donaciones` | A05 directo | No |
| `don-detalle` | 1 | `donante-donaciones-detalle`, `donante-seguimiento` y sus 2 de `persona/` | A06, A07 o A08 según el estado; A07 | No |
| `don-organizaciones` | 1 | `donante-entidades` y `persona/donante-entidades` | Deriva de A02, con `componentes/listado` | No |
| `don-perfil` | 1 | `donante-perfil`, `-guardado` y sus 2 de `persona/`; la hoja heredada del buzón | Deriva de A12; DA-06 | No |
| `don-inicio-persona` | 2 | `persona/donante-inicio` | Deriva de A04; DA-03c | Sí |
| `don-misiones` | 2 | `donante-misiones` | Sin arquetipo | Sí |

Suma: 1 + 2 + 4 + 2 + 4 + 1 + 1 = 15, más las 4 del buzón que ya cambiaron en ORG: 19.

### 12.3 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| DON-01 | `don-inicio` | Inicio de la empresa | A04; HG-01 a HG-03 con su texto de reemplazo | Hecho (`524d86d6d`, `91f31ec86` y `2cd69d8ba`): A04 con el aviso del problema arriba, una sola lámina protagonista (la que viaja, con la raíz de 5 pasos y el mapa decorativo), «También en curso» y «Ya confirmadas en `<mes>`» con un fruto por entrega del mes. HG-01, «Todavía sin fecha de nueva salida»; HG-02, «En camino desde las `<hora>`» con la hora de la nota; sale el tablero de 3 columnas (R11-25). El escenario nuevo `?escenario=sin-camino` cubre A04 sin nada en camino. Lo que queda, en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 6 y 10. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-02 | `don-inicio` | Los menús de la empresa (Inicio, Donaciones, Organizaciones y Cómo donar) y de la persona (con Misiones antes de Cómo donar) | DA-03a, DA-03b, DA-05 y DA-17 | Hecho (`524d86d6d`): la empresa, Inicio, Donaciones, Organizaciones y Cómo donar; la persona, con Misiones antes de Cómo donar. Cómo donar lleva a `/publico-para-donantes` con el ícono `nav-ayuda` (DA-05); Seguimiento sale del menú y queda dentro de Donaciones (`EN_LA_SECCION`, DA-03a). Los menús de la organización, el admin y lo público no cambian (DA-17). Los tests del menú se actualizaron al comportamiento nuevo. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-03 | `don-donaciones` | Donaciones, de la empresa y de la persona | A05 | Parcial (`53712e382`, `d8403c8d5` y `642c1698e`): A05 con las vistas por estado y su cuenta (`.segmentos`), `componentes/listado`, la tabla con sello y raíz por fila y el paginador; el sello dice Registrada, Asignada o Entrega confirmada, como el detalle. El bloqueante de la revisión del lote tras 3 rondas se arregló después de la ola: la vista se llama «Confirmadas» y la opción del filtro, «Entrega confirmada»; «Pendientes» queda ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 1). Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-04 | `don-detalle` | El detalle de una donación, en sus tres variantes, con el enlace a su seguimiento | A06, A07 o A08; DA-03a; HG-01 | Parcial (`39efd6083`, `9572f7047` y `e4cc56f3f`): una plantilla con las variantes asignada (A06), en camino (A07, con «Ver dónde está el camión» a su seguimiento, DA-03a) y con problema (A08, HG-01); la entregada y la que espera destino siguen la composición de A06. La persona sale de `persona/datos-persona`, como su seguimiento. Los bloqueantes de la revisión del lote tras 3 rondas se arreglaron después de la ola: el sello y la raíz dicen lo mismo que la lista, «Donación 0005», «Cómo va» con el texto de HG-02 y las dos aserciones de `MisDonacionesControllerTest` que no podían fallar ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 2 y 3). Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-05 | `don-detalle` | Seguimiento, fuera del menú | A07; DA-03a; HG-02 | Parcial (`39efd6083`, `9572f7047`, `e4cc56f3f`, `2cd69d8ba` y `9b2d3e147`): A07 de una sola donación, fuera del menú, con el mapa y su recorrido sin hora estimada; el camión solo si va en camino; marcadores de 48 px y márgenes de encuadre propios (`data-mapa-margenes`); el mapa 3/4 en el celular. `/donante-seguimiento` sale de la línea de base de §10.4. Después de la ola: el sello dice Asignada, «Donación 0005» y la nota del mapa empieza con «En camino desde las `<hora>`» (HG-02). **Falta:** la decisión del usuario sobre el aviso «Todavía espera destino», que exige `DonanteDetallesTest` ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 2 y 4). Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-06 | `don-organizaciones` | Organizaciones, de la empresa y de la persona | Deriva de A02; HG-04 | Hecho (`f67f49d3e` y `2cd69d8ba`): deriva de A02 con una tarjeta del catálogo por organización, la necesidad con la cuenta en palabras y la barra solo con lo recibido (HG-04, «Si no llega»: sin barra con datos reales), el `.plegable` de un solo rótulo, «Cómo llega tu donación» como `.aviso` y «Ver tus donaciones». La persona usa el mismo fragmento `hoja` (DA-13). Sale `.resultado` de `donante-comun.css`. Lo que queda, en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 7 y 9. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-07 | `don-perfil` | El perfil, y «guardado» como aviso breve | Deriva de A12; DA-06 | Hecho (`ae68989d2`): deriva de A12 como el perfil de la organización: un formulario por sección en las pestañas del catálogo, una columna, el CUIT y el documento como texto fijo; «guardado» es el aviso breve en el perfil (DA-06) y en la maqueta `/donante-perfil-guardado` muestra el perfil con el aviso. Salen las siete clases `perfil*`. Lo que queda, en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 7, 8 y 10. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-08 | `don-perfil` | De la hoja heredada del buzón de quien dona (`donante-notificaciones.css`) sale lo que el buzón del catálogo ya resuelve; si queda vacía, se borra | — | Hecho (`ae68989d2` y `69edbeb07`): `donante-notificaciones.css` se borró y sale de `HOJAS_HEREDADAS`; las cuatro plantillas del buzón de quien dona nombran `donante-comun.css`, donde vive la `.bajada` ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 7). Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-09 | `don-inicio-persona` | El inicio de la persona, según su ficha | Deriva de A04; DA-03c, DA-08, DON-P2 y DON-P3 | Hecho (`d282ddcc9`, `ee9c83914`, `0e6b9a9e3` y `9d7c5990c`): según su ficha, deriva de A04: el aviso del problema arriba, una sola protagonista (la que viaja o, sin ninguna, la huerta con «Ver mi huerta» llena, DON-P3), «También en curso», «Ya confirmadas en `<mes>`» solo con las del mes, la huerta chica, el almanaque de 12 meses en una fila con frutos (DON-P2) y las 3 últimas novedades. Salen la lista mes por mes y 4 clases heredadas. Lo que queda, en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 7 y 10. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |
| DON-10 | `don-misiones` | Misiones, según su ficha, con lugares de 48 px y sin la variante del álbum | Sin arquetipo: ficha; DA-08, DA-21 y DON-P1 | Hecho (`79b073dfb`, `2cd69d8ba` y `9b2d3e147`): la huerta con las piezas de la guía, una sola lámina protagonista, sin acción llena; cada lugar mide 48 × 48 px como mínimo con el toque encima del dibujo (DA-21-misiones) y, en el celular, una columna de lugares de 48 px. Se borran `misiones-a` y `misiones-b` con sus hojas (DON-P1); fuera de `docs/` ningún archivo nombra `misiones-a`, y una variante que no existe se ignora y muestra la misma pantalla (`VariantesInterceptor`). Las dos rutas de `/donante-misiones` salen de la línea de base de §10.4. Lo que queda, en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 5 y 7. Sin PASS de la revisión de pantallas de la ola tras 2 rondas (§12.5) |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5).

### 12.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla (en
  `don-misiones`, las dos rutas de la línea de base de toque).
- **Al cierre:** `-Pvisual` sobre las rutas `/donante-` de `paginas.txt`, con la línea de base de
  §10.4 sin las 3 del buzón que ORG sacó; y los otros cinco tests visuales, con la línea de base de
  §10.5. Sin el stack ni el recorrido con login: ningún lote toca un adaptador `rest`.
- **Lo que cambia en la línea de base:** con DA-21-misiones, las dos rutas de `/donante-misiones`
  tienen que pasar, y también `/donante-seguimiento`, que `don-detalle` rehace como A07. Las tres salen
  de la línea de base de §10.4 al cerrar la ola.

### 12.5 Cierre de DON (2026-11-24)

Rama `E7_guia_ui_don`, `9b2d3e147`; 18 commits de lote y de la revisión de pantallas sobre
`0d654435f` (las decisiones delegadas). Estado de cada ítem en §12.3: 7 hechos (DON-01, DON-02 y
DON-06 a DON-10) y 3 parciales (DON-03, DON-04 y DON-05), por los bloqueantes que dejaron
`don-donaciones` y `don-detalle` tras 3 rondas de su revisión. La revisión de pantallas de la ola
corrió 2 rondas de arreglos (`2cd69d8ba` y `9b2d3e147`) y no llegó a PASS: queda abierta, al menos,
la decisión del aviso «Todavía espera destino» en Seguimiento. Por eso las decisiones del usuario
de esta ola constan como no cumplidas, aunque su comportamiento esté en el código. Lo que queda
abierto está en [DTI-49](../adr/DEUDA_TECNICA.md#dti-49); las decisiones, en la
[bitácora](bitacora.md) (2026-11-24).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `9b2d3e147`: exit 0, 889 tests, sin
  fallos (867 en la línea de base de la corrida; 887 en `9d7c5990c`). `InterfazFitnessTest` pasa
  con los topes de hoy (159 y 1192). El test cambió solo en `HOJAS_HEREDADAS`: salen
  `donante-donaciones-detalle.css`, `donante-donaciones.css`, `donante-notificaciones.css` y
  `donante-seguimiento.css`. `clases-heredadas.txt` tiene 56 líneas menos y ninguna nueva.
- `[VERIFIED]` Cercos con `git diff 0d654435f..HEAD`: sin cambios en `rest/`, los puertos,
  `tokens.css`, los tests de `visual/`, `templates/componentes/` ni `static/css/componentes/`. Fuera
  de las plantillas y hojas de sus pantallas, los lotes y la revisión tocaron las maquetas
  (`CuentaDeEjemplo`, `DonacionesMaqueta`, `DonanteDonacionesDeEjemplo`, `DonanteInicioMaqueta`,
  `DonanteMisionesMaqueta` y `DonanteSeguimientoMaqueta`), los controllers de sus rutas y
  `Pantallas.java`, `layout-privado.html` (la línea de `misiones-b.css`), `static/js/mapa.js` (el
  atributo opcional `data-mapa-margenes`), `VariantesInterceptor` con su test (una variante que no
  existe se ignora), los tests funcionales de sus rutas (entre ellos `SitioControllerTest`, compartido),
  `seguridad/CuentaDelTokenTest.java` (la llamada de la persona pasa a `times(5)`, una por la lista y
  una por cada cuenta de vista; el `never()` con la otra cuenta queda igual), `paginas.txt`,
  `clases-heredadas.txt` y `docs/interfaz/componentes.md` (la fila del mapa).
  Se borraron `donante-seguimiento.css`, `donante-notificaciones.css` y las variantes `misiones-a` y
  `misiones-b` (DON-P1).
- `[VERIFIED]` `-Pvisual` sobre las 72 rutas `/donante-` de `paginas.txt`, en `9d7c5990c`: pasan
  las 72. En `9b2d3e147`, después de la revisión de pantallas, se volvieron a pasar
  `/donante-seguimiento` (con y sin `?cuenta=persona`) y `/donante-misiones?cuenta=persona` (con y sin
  `escenario=vacio`): pasan las 4. **Línea de base de §10.4:** salen `/donante-seguimiento` y las dos
  de `/donante-misiones` (DA-21-misiones); quedan 5 rutas, ninguna de quien dona
  (`/admin-camiones`, `/admin-camiones-agregar-error`, `/admin-donantes?donante=juan` y
  `/publico-mapa?escenario=muchas`, con y sin la página 2), que no se corrieron (DA-20).
- `[VERIFIED]` Los otros cinco tests visuales, en `9d7c5990c`: 31 tests, 3 fallos y 1 error, todos
  de la línea de base de §10.5 y de ORG (`ArmazonFijoTest.enElCelularConTabYShiftTabElFocoNoQuedaTapado`,
  `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`, el inestable de
  `MenusQueSeCierranTest` y el error del lema del pie). Ningún fallo nuevo; `DonanteDetallesTest`
  pasa sus 7. No se volvieron a correr después de la revisión de pantallas.
- Sin stack ni recorrido con login (DA-20): ningún lote toca un adaptador `rest`. Queda para Z.
- Revisión de los lotes: `don-donaciones` y `don-detalle` quedaron sin PASS tras 3 rondas. Para los
  demás, el veredicto no consta en el cierre; tuvieron rondas de arreglo `don-inicio` (1),
  `don-perfil` (1) y `don-inicio-persona` (2), y ninguna `don-organizaciones` ni `don-misiones`.
- Commits de la ola, en hora del 2026-11-24: `f67f49d3e` (09:35), `524d86d6d` (09:38), `53712e382`
  (09:49), `39efd6083` (09:54), `ae68989d2` (10:13), `91f31ec86` (10:43), `d8403c8d5` (10:43),
  `69edbeb07` (10:46), `9572f7047` (10:52), `642c1698e` (11:20), `e4cc56f3f` (11:44), `d282ddcc9`
  (12:25), `ee9c83914` (12:34), `79b073dfb` (12:34), `0e6b9a9e3` (12:49), `9d7c5990c` (13:03),
  `2cd69d8ba` (14:17, revisión de pantallas, ronda 1) y `9b2d3e147` (15:07, ronda 2).
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); ninguna spec de `docs/specs/active/` es de esta ola, así que no se mueve ninguna.
- Documentos: `patrones-y-microcopy.md` suma la regla 25 (lo que el servicio no da no se escribe);
  `mapa-del-sitio.md`, los cambios de la ola; `componentes.md`, las filas que nombran a quien dona.
  `tono-y-comunicacion.md` y [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no
  cambian: los nombres de los estados ya eran los de la tabla y ningún requerimiento cambia de estado.

## 13. Ola PUB1: el sitio público

Rehace las 13 plantillas de lo público de §5 (PUB1) y el menú público (§5.1; DA-01, DA-05 y
DA-17). No lleva fichas: Legales, sin arquetipo, es derivación del agente (DA-08). Manifiesto:
[`backlog-guia-al-codigo.pub1.manifiesto.json`](backlog-guia-al-codigo.pub1.manifiesto.json). Se
escribió el 2026-11-24 en la corrida desatendida, en paralelo con la corrida de DON: el reparto y
DA-22 a DA-24 no los aprobó el usuario y van con la opción recomendada (§2.2). Se lanza desde la
`E7_guia_ui` de ese momento: PUB1 no comparte pantallas con DON; los archivos que comparte
(`SitioControllerTest`, `paginas.txt`, `InterfazFitnessTest`) se reparten por líneas (regla 4).

### 13.1 Decisiones de la ola

| ID | Qué decide | Lote |
|---|---|---|
| DA-01 | El Mapa sale del menú y queda dentro de Entregas: el menú lo saca y Entregas lo enlaza | `pub1-portada` (menú) y `pub1-entregas` (el enlace y las migas) |
| DA-05 | Nosotros, Para donantes y Para organizaciones pasan a «Cómo funciona», «Cómo donar» y «Ayuda», con las rutas de hoy; «Cómo funciona» entra al menú público | `pub1-portada` (menú) y `pub1-conocer` (las páginas) |
| DA-06 | La respuesta de Contacto pasa a aviso breve | `pub1-contacto`, con DA-24 |
| DA-10 | En lo público, quien recibió es la organización (HG-03) | `pub1-entregas` |
| DA-17 | Cambia solo el menú público | `pub1-portada` |
| DA-22 | La portada sigue a A01, sin las abejas, el corte «bajo tierra» ni el cierre de hoy | `pub1-portada` |
| DA-23 | El pie nombra las tres páginas con su nombre nuevo | `pub1-conocer` |
| DA-24 | Contacto: el envío vuelve a Contacto con el número en el aviso breve | `pub1-contacto` |

DA-22, DA-23 y DA-24 son nuevas de esta ola; su opción descartada está en §2.2.

### 13.2 Reparto

Una sola tanda, con 6 lotes. Ningún archivo de Java ni plantilla lo tocan dos lotes, salvo los que
la regla 4 reparte por líneas. El menú (`navegacion-publica.html`) tiene un solo dueño,
`pub1-portada`. Marca la sección por el nombre de la hoja de cada pantalla, y ninguna hoja cambia
de nombre.

| Lote | Tanda | Plantillas | Arquetipo y decisiones | Ficha |
|---|:-:|---|---|---|
| `pub1-portada` | 1 | `publico-portada`; el menú público (`navegacion-publica.html`) | A01 directo; DA-01, DA-05, DA-17 y DA-22 | No |
| `pub1-entregas` | 1 | `publico-donaciones` y `publico-mapa` | Deriva de A10 y de A07; DA-01, DA-10 y HG-03 | No |
| `pub1-organizaciones` | 1 | `publico-entidades` | Deriva de A02, con `componentes/listado`; HG-04 | No |
| `pub1-ingresar` | 1 | `publico-ingresar` y `publico-ingresar-error` | Deriva de A12 | No |
| `pub1-conocer` | 1 | `publico-nosotros`, `publico-para-donantes` y `publico-para-entidades`; los textos de tres enlaces de `pie.html` | Deriva de A01; DA-05 y DA-23 | No |
| `pub1-contacto` | 1 | `publico-contacto`, `publico-contacto-enviado`, `publico-contacto-error` y `publico-legales` | Deriva de A12; DA-06 y DA-24. Legales, sin arquetipo: página de lectura | No |

Suma: 1 + 2 + 1 + 2 + 3 + 4 = 13, las 13 de §5.

**Plantillas del área que no son pantallas de PUB1:**

| Plantilla | En PUB1 |
|---|---|
| `navegacion-publica.html` | `pub1-portada` (DA-01, DA-05 y DA-17) |
| `datos-publicos.html` | Compartida (regla 4): `avisoError` y `fotoVentana` conservan su firma, porque también las usan `publico-activar`, `publico-activar-pedir` y los dos registros (PUB2) |
| `pie.html` | `pub1-conocer`, solo el texto de tres enlaces (DA-23) |
| `layout-publico.html` | Fuera: es el armazón de C1 (C1-07); no se edita |
| `error/403`, `error/404`, `error/5xx`, `sin-dibujar` y `aviso-persona` | Fuera: son de PUB2 (§5) |
| `error/429.html` y `pagina-error.html` | Fuera: la primera es una página de estado y la segunda, la pieza que comparten las páginas de error. §5 no las nombra; quedan anotadas para PUB2 |
| `publico-entidades.html`, su fragmento `pantalla` | Es de `pub1-organizaciones`, pero lo usan también `donante-entidades` y `persona/donante-entidades` (DON): conserva su firma y lo que muestra para quien dona (regla 4) |
| `persona/datos-persona.html` | Fuera: son los datos de ejemplo de la persona (§5, DON) |
| `cuenta-menu`, `interaccion`, `envio`, `cerrar-sesion`, `paginador` e `iconos` | Fuera: son comunes a los cuatro roles o del catálogo; no se editan |
| `componentes/` | Fuera: es el catálogo; no se edita |

**Hojas heredadas que comparten pantallas de PUB2 o `/estilos`.** `publico-paginas.css` es la de
Contacto y Legales y la de `/estilos`, y la importan `publico-conocer.css`, `publico-listas.css` y
`publico-error.css`. `publico-listas.css` la importan Entregas, Organizaciones, el Mapa y
`publico-registro.css` (PUB2). `publico-ingresar.css` la usan Ingresar y las 7 plantillas de activar
y recuperar (PUB2). En PUB1 ninguna de las tres se borra ni sale de `HOJAS_HEREDADAS`: cada lote
saca solo las reglas que usan nada más que sus pantallas.

### 13.3 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| PUB1-01 | `pub1-portada` | La portada, en el orden de A01: el título con la trama, el dibujo con un fruto por entrega confirmada del mes, la última entrega confirmada con su foto y su sello, las cifras, «Cómo funciona» con la raíz numerada de 5 pasos y las entregas confirmadas del mes. Los datos salen del puerto que ya existe (`cifrasDePortada` y `entregasConfirmadas`) | A01; DA-22; HG-03 con su texto de reemplazo (DA-10) | Pendiente |
| PUB1-02 | `pub1-portada` | El menú público: Inicio, Entregas, Organizaciones y Cómo funciona (a `/publico-nosotros`), sin el Mapa; en el Mapa queda marcada Entregas | DA-01, DA-05 y DA-17 | Pendiente |
| PUB1-03 | `pub1-entregas` | Entregas: las entregas confirmadas, cada una con su foto, su etiqueta y quién la recibió, y un enlace al Mapa | Deriva de A10; DA-01, DA-10 y HG-03 | Pendiente |
| PUB1-04 | `pub1-entregas` | El Mapa, dentro de Entregas: el mapa con su lista y las migas a Entregas | Deriva de A07; DA-01 | Pendiente |
| PUB1-05 | `pub1-organizaciones` | Organizaciones: tarjetas sobre la lámina, con el avance de cada necesidad y la barra de `componentes/listado` | Deriva de A02; HG-04 | Pendiente |
| PUB1-06 | `pub1-ingresar` | Ingresar y su error, la puerta de la demo | Deriva de A12 | Pendiente |
| PUB1-07 | `pub1-conocer` | Cómo funciona, Cómo donar y Ayuda: las páginas de hoy, con su nombre nuevo y la composición de A01 | Deriva de A01; DA-05 | Pendiente |
| PUB1-08 | `pub1-conocer` | El pie nombra esas tres páginas con su nombre nuevo | DA-23 | Pendiente |
| PUB1-09 | `pub1-contacto` | Contacto y su error; la respuesta pasa a aviso breve en Contacto, con el número del mensaje | Deriva de A12; DA-06 y DA-24 | Pendiente |
| PUB1-10 | `pub1-contacto` | Términos y privacidad, una página de lectura | Sin arquetipo: derivación (DA-08) | Pendiente |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5), con el
límite de las hojas compartidas de §13.2.

### 13.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
  `pub1-organizaciones` corre además `-Pvisual` sobre `/donante-entidades` y
  `/donante-entidades?cuenta=persona&ya=si` antes de comitear, porque esas pantallas usan su
  fragmento `pantalla`; no entran en la revisión ni en la hoja de contacto.
- **Mientras dura la tanda:** cada lote ve el menú y el pie de hoy fuera de `pub1-portada` y de
  `pub1-conocer`. No es hallazgo del lote.
- **Al cierre:** `-Pvisual` sobre las 53 rutas de PUB1 de `paginas.txt` (las de `/publico-portada`,
  `-ingresar`, `-donaciones`, `-entidades`, `-mapa`, `-nosotros`, `-para-donantes`, `-para-entidades`,
  `-legales` y `-contacto`). No entra `/publico-donaciones?respuesta=404`, que muestra `error/404`
  (PUB2). Se compara con la línea de base de §10.4. Corren también los otros cinco tests visuales,
  con la línea de base de §10.5 y §11.3. No se levantan el stack ni el recorrido con login: los hace
  Z, y ningún lote cambia un adaptador `rest` (plan de la corrida, punto 5).
- **Lo que cambia en la línea de base:** las dos rutas de `/publico-mapa?escenario=muchas` (con y sin
  la página 2) siguen en la línea de base de §10.4. `[OBSERVED]` En la pasada de G1
  (`guia-c1/C1/muestra-g1/reporte.json`, fuera del repo) fallan a 360 px los marcadores numerados del
  mapa (`button.mapa-marcador`, de `static/css/componentes/mapa.css` y `static/js/mapa.js`), que son
  del catálogo y esta ola no edita. Si pasan, se anota. En los otros tests visuales, por DA-22 se espera que fallen
  `AbejasYRenglonesTest.conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras` y los dos de
  `BajoTierraRotulosTest`. Miden las abejas y el corte «bajo tierra», que la portada nueva no tiene.
  Siguen fallando, como antes, `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen` y
  `loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos`. Esos fallos se anotan para que
  decida el usuario; no son bloqueantes y nadie edita esos tests. Un fallo nuevo en `ArmazonFijoTest`,
  que también mide la portada y Entregas, sí es bloqueante.
