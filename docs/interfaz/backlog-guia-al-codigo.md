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
Manifiesto de ADM1 (2026-11-24, misma corrida): DA-25 a DA-31 son decisiones nuevas que el usuario
no respondió, con la misma regla y la misma nota. DA-28 a DA-31 son las preguntas de las dos fichas
de la ola ([Ranking](flujos/guia-adm-ranking.md) y [los gráficos del inicio](flujos/guia-adm-inicio-graficos.md)).
Manifiesto de PUB2 (2026-11-24, misma corrida): DA-32 es una decisión nueva que el usuario no
respondió, con la misma regla y la misma nota.
Manifiesto de ADM2 (2026-11-25, misma corrida): DA-33 a DA-35 son decisiones nuevas que el usuario
no respondió, con la misma regla y la misma nota.
Manifiesto de Z (2026-11-25, misma corrida): DA-36 a DA-38 son decisiones nuevas que el usuario no
respondió, con la misma regla y la misma nota (§17.1).

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
| DA-25 | Admin: con DA-04, «Por decidir» reemplaza a Inicio y a Asignar, y §5 da A03 directo a `admin-inicio` y a `admin-asignaciones`. ¿Cómo se reparten las dos rutas? | Las dos siguen y llevan la misma cabecera de A03 (el título «Por decidir», la fecha y la última asignación, «Correr la asignación» y «Registrar donación») y la misma barra de pestañas con su cuenta: Propuestas (a `/admin-inicio`), Conflictos (a `/admin-asignaciones?pestana=conflictos`), Solicitudes (a `/admin-entidades`, de ADM2, que no cambia) y Por vencer (a `/admin-donaciones?vence=pronto`). `/admin-inicio` abre Propuestas: la primera página de las propuestas sin conflicto en la tabla con selección y, debajo, lo de su ficha. `/admin-asignaciones` es la mesa completa: Propuestas paginadas, Conflictos de a una, Destino y Decididas. La entrada del menú lleva a `/admin-inicio` y queda marcada en las dos y en las cuatro respuestas. Descartadas: una sola ruta que redirige a la otra (rompe los tests y las rutas de `paginas.txt` que llegan a cada una, y el ingreso del admin cae en `/admin-inicio`); Solicitudes y Por vencer como listas dentro de `/admin-inicio` (traen al inicio las listas de Organizaciones, de ADM2, y de Donaciones) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-26 | La tabla con selección de A03 aprueba o descarta varias propuestas juntas, y el puerto decide de a una (`AdminOperacionPuerto.decidirPropuesta`). ¿Cómo se hace sin cambiar el puerto? | El formulario manda varias `propuesta` a `/admin-asignaciones-aprobada` o `-descartada` (un botón con `formaction` cada uno) y `AdminAsignacionesEnvios` llama al puerto una vez por propuesta, en orden. Si una falla, para: el aviso dice cuántas se decidieron y por qué no la siguiente, y las demás quedan sin decidir. Con una sola, el aviso de hoy. Los Conflictos se siguen decidiendo de a una, como dice A03. Descartadas: cada fila con su botón, sin selección (A03 dibuja la selección); un pedido en lote nuevo a Donaciones (cambia el puerto y el adaptador `rest`) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-27 | El galpón en escritorio: la guía saca la barra superior y lleva la marca, la campana y la cuenta al cajón (`15-pantallas.css:98-101`). `ArmazonFijoTest` mide la cabecera (`body > header`) y la barra del listado en `/admin-donantes` y `/admin-donaciones`, y nadie edita ese test. ¿Qué se hace? | La barra superior se queda en todos los anchos, fina, con la marca, la campana y la cuenta; las secciones van al cajón, siempre abierto desde 56 em, a la izquierda y debajo de la barra. Hasta 56 em, la barra del celular de la guía (Por decidir, Donaciones, Camiones y Más). La diferencia con la guía se declara. Descartada: la guía tal cual (sin barra arriba desde 56 em), que cambia el lugar de la campana respecto de los otros roles y arriesga `laCabeceraYLaBarraQuedanFijasSinHuecoEntreEllas` y `conTabElFocoNoQuedaDebajoDeLaCabeceraNiDeLaBarra` | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-28 | Ranking (ficha): ¿cómo se marcan los primeros puestos? | La tabla del catálogo con el encabezado del galpón, sin selección; el puesto en número, sin color, dibujo ni podio para los tres primeros. Descartada: un podio dibujado con los tres primeros (es una pieza que el catálogo no tiene, y el tono pide no comparar donantes más allá del criterio que va arriba de la tabla) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-29 | Los gráficos del inicio (ficha): debajo de la bandeja, ¿qué pasa con las cuatro tarjetas de «Cómo está todo» y con «Entregas para mirar hoy», que dibuja A03? | Las cuatro tarjetas pasan a un solo resumen denso, «Cómo está todo», con la lista de datos del catálogo (P23) y su fecha: las cinco cuentas de donaciones por estado, las entregas confirmadas del mes y en total, la red y los camiones (o su aviso), con las salidas de hoy. «Entregas para mirar hoy» no se dibuja: `AdminInicioVista` trae cuentas y no las entregas; se anota como deuda. Descartadas: conservar las cuatro tarjetas (es la densidad de la huerta en el galpón); armar «Entregas para mirar hoy» con datos de la maqueta (en el sitio real quedaría vacía) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-30 | Los gráficos del inicio (ficha): «Mes a mes» y «Por categoría», ¿se quedan como parte de la pantalla o pasan al catálogo? | Se quedan como parte de `admin-inicio`, en `admin-inicio.css`, debajo del resumen y con su tabla al lado (debajo en el celular), como hoy. Sus reglas dejan los nombres de antes de la guía (`--color-acento`, `--color-acento-claro`, `--color-tinta-suave`) por los de la guía con el mismo valor (DA-14a), así no cambia ningún píxel y Z puede retirar esos alias. Descartada: una pieza nueva del catálogo con su muestra en `/estilos` (la usa una sola pantalla y pediría un lote de catálogo en la tanda 1) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-31 | Ranking (ficha): ¿cómo se elige el mes? | Como hoy: la pastilla «Mes» de `componentes/filtros`, «Volver al último mes» y el paginador de meses (R11-23 y R11-43). Descartada: pestañas por mes (A03 usa pestañas para lo que hay por decidir, y doce meses no entran a 360 px) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | ADM1 |
| DA-32 | Las respuestas de la cuenta: con DA-06 (página propia solo al confirmar una recepción), ¿cómo responden Recuperar la contraseña, Pedir el enlace de activación y Activar la cuenta? ¿Y «Solicitud enviada», de la organización? | Recuperar (`/publico-recuperar-enviado`) y Pedir el enlace (`/publico-activar-pedido`) muestran su misma pantalla con el aviso en la página del catálogo (`.aviso` con `data-tipo="exito"`, P13) en el lugar del formulario, como hoy, sin página propia. Activar (`/publico-activar-lista`) muestra la pantalla de Activar con ese aviso, «Tu cuenta está lista», y una sola acción llena, Ingresar. Las tres rutas siguen y ninguna lleva el sello de recibido dibujado a mano (`.recibido`). «Solicitud enviada» (`/publico-registro-entidad-enviada`) no es una respuesta: es la página de estado de la solicitud, a la que se vuelve con «Ver mi solicitud» (R11-41). Se queda como pantalla, con sus escenarios, y «Solicitud recibida» pasa al mismo aviso en la página. Descartadas: el aviso breve que se va solo, como Contacto (DA-24), porque el aviso dice qué hacer después («Revisá tu bandeja de entrada») y, cuando se va, queda el formulario vacío y la persona vuelve a pedir el enlace; llevar Activar a Ingresar con un aviso breve (edita `publico-ingresar.html`, una pantalla de PUB1); pasar «Solicitud enviada» a un aviso breve (rompe «Ver mi solicitud» y los escenarios de R11-41) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-24 | PUB2 |
| DA-33 | Registrar donante: con DA-06, «registrado» pasa a aviso breve en Donantes. La página de hoy ofrece «Registrar su donación» (RF-15, «en un mismo flujo»), y el puerto no dice quién quedó registrado (`AdminPersonasPuerto.registrarDonante` devuelve `void`). ¿Dónde queda esa salida? | La respuesta es el aviso breve en Donantes, con el texto de hoy («Registraste a Ana Ejemplo como donante.» y, con `?escenario=ya-era`, «Ana Ejemplo ya era donante. No cambiamos nada.»), como ya hace el envío real (`AdminDonantesEnvioController.registrar` termina en `/admin-donantes` con el aviso). Registrar su donación sigue desde la ficha del donante en la lista, que ya tiene «Registrar una donación». En la maqueta, `/admin-donantes-registrado` sigue y muestra Donantes con el aviso. El paso de más (buscar al donante en la lista) se anota como deuda. Descartadas: conservar la página con sus dos acciones (contradice DA-06, y el envío real ya no la muestra); un enlace dentro del aviso breve (el aviso breve no pide nada, P13, y el envío real no sabe la clave del donante nuevo sin cambiar el puerto) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | ADM2 |
| DA-34 | Donantes y Organizaciones son hoy una bandeja con ficha (`.maestro` y `.maestro-ficha`, R11-34), y ADM1 pasó Donaciones a la `.tabla` del catálogo con cada fila a su detalle (decisión del agente, [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 5). ¿Donantes y Organizaciones siguen como bandeja con ficha o pasan a tabla? | Siguen como bandeja con ficha, que es una pieza del catálogo, en el galpón: la ficha es donde se decide una solicitud (Aprobar, Pedir corrección y Rechazar) y donde sigue el donante (sus donaciones y «Registrar una donación»), y `?donante=` y `?entidad=` siguen siendo la dirección de cada ficha. Descartada: la tabla con una página de detalle por fila, como Donaciones (pide rutas nuevas, lleva las acciones de una solicitud a otra pantalla y obliga a cambiar los enlaces que llegan a cada ficha) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | ADM2 |
| DA-35 | Administradores: con DA-06, ¿«Contraseña provisoria» (`/admin-administradores-alta`), lo que ve el admin después de dar de alta a otro, pasa a aviso breve? | No: se queda como página propia, como en su ficha ([`flujos/r11-p1-admins.md`](flujos/r11-p1-admins.md) §4.4). No es solo una respuesta: muestra una contraseña que se ve una sola vez (la página va con `Cache-Control: no-store` y recargarla lleva a Administradores) y hay que anotarla con calma. Va en el galpón con la composición de A12 y una sola acción llena, «Volver a Administradores». `/admin-administradores-nuevo`, el formulario del alta sin JavaScript, sigue como página aparte: es un estado del formulario. Descartada: el aviso breve (se va solo y la contraseña no se vuelve a mostrar) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | ADM2 |
| DA-36 | Z retira los 10 alias (DA-14b). El tema de Keycloak lleva una copia idéntica de `tokens.css` (`auth-service/README.md:126`), y su hoja `donatrack.css` usa 6 de esos alias en 9 lugares: `--color-acento`, `-acento-hondo`, `-acento-claro`, `--color-papel`, `--color-sombra` y `--color-titulo`. También los nombra `scripts/tests/test_auth_theme.py`. ¿Qué pasa con el tema? | El tema también deja los alias. `donatrack.css` pasa al token de la guía del mismo valor (la tabla de §3.2), y `test_auth_theme.py` nombra esos tokens con las mismas aserciones y los mismos mínimos. La copia se vuelve a sincronizar con `scripts/sincronizar_tema.py`. Descartadas: conservar en `tokens.css` los alias que usa el tema (DA-14b no se cumpliría y quedarían alias vivos solo para otra imagen); que la copia del tema conserve los alias y deje de ser idéntica (rompe la regla de la copia y el `--verificar` del script) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | Z |
| DA-37 | DA-14b cambia la lista de pares de `InterfazFitnessTest` «por la de la guía». Al pie de la letra, saldrían también 6 pares de tokens que siguen en uso y que la guía no mide: el título sobre el papel, el texto sobre el botón secundario, éxito, info, advertencia y el foco sobre una lámina. ¿Qué pares salen? | Salen los 16 que nombran un token retirado. Cuatro de ellos miden un color que sigue en uso con un nombre de la guía, y vuelven con ese nombre y el mismo 4,5, como tres pares: `--color-raiz` sobre `--color-suelo` (eran `--color-destacado-texto-suave` y `--color-marca-brillante` sobre `--color-destacado-fondo`; los rótulos y enlaces de la tierra, `componentes/huerta.css:556-558` y `:599-600`), `--color-raiz` sobre `--color-suelo-medio` (era `--color-destacado-texto-suave` sobre `--color-destacado-fondo-alto`) y `--color-accion-primaria` sobre `--color-superficie` (era `--color-marca-texto` sobre `--color-superficie`; `.muestra-nombre`, `componentes/muestra.css:10` y `:15`). Los otros 12 miden un valor que ya mide un par que se queda (por ejemplo, `--color-destacado-texto` sobre `--color-destacado-fondo`, con `--color-texto-suelo` sobre `--color-suelo`). El del título sobre el papel pasa a `--color-accion-primaria` sobre `--color-fondo-pagina`, con el mismo 4,5. Ningún mínimo baja, y la lista ya tiene los 29 pares con piso de la guía (`guia/partes/90-script.html:216-245`): pasa de 51 a 38 pares. Descartadas: la lista de la guía tal cual, con 29 pares, y sacar los 16 sin reponer nada. La primera dejaría de medir 6 pares que están en uso; la segunda dejaría sin par de 4,5 la raíz sobre la tierra y el verde sobre una lámina, y el test pasaría aunque se oscurezca `--p-raiz`. Las dos debilitan el cerco (`AGENTS.md` §4.3) | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | Z |
| DA-38 | El plan saca del cerco `entidad-recibidas.css`, que ya no existe, y retira las hojas vacías. `HOJAS_HEREDADAS` «solo baja: una hoja migrada por completo sale de acá» (`InterfazFitnessTest.java:268-272`). ADM2 deja para Z dos preguntas: si `admin-inicio.css` puede salir de la lista y si `paginador.html` queda sin uso. ¿Qué sale? | Sale `entidad-recibidas.css`. Una hoja sin reglas se borra, con lo que la nombra. Sale de la lista toda hoja que el cerco acepta como hoja de pantalla nueva, es decir, sin color, fondo, fuente ni `@keyframes`: el test pasa sin ella. `paginador.html` se borra si no lo nombra ningún archivo de `cliente-liviano/src`. Ninguna hoja con reglas se reescribe para que salga: eso es la migración B3 (`backlog-migracion-componentes.md`). Descartada: sacar solo `entidad-recibidas.css`, que deja en la lista hojas que ya cumplen, cuando la lista dice que baja con cada una | Sin respuesta del usuario: la recomendada, delegada por el relevamiento del 2026-11-24 (corrida desatendida, rama = gate: opción recomendada; plan-guia-resto.md) | 2026-11-25 | Z |
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
| `admin-administradores`, `admin-actividad` | Deriva de A03 | |
| `admin-administradores-nuevo`, `admin-administradores-alta` | Deriva de A12 | Páginas propias, en el galpón (DA-35) |
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
| PUB1-01 | `pub1-portada` | La portada, en el orden de A01: el título con la trama, el dibujo con un fruto por entrega confirmada del mes, la última entrega confirmada con su foto y su sello, las cifras, «Cómo funciona» con la raíz numerada de 5 pasos y las entregas confirmadas del mes. Los datos salen del puerto que ya existe (`cifrasDePortada` y `entregasConfirmadas`) | A01; DA-22; HG-03 con su texto de reemplazo (DA-10) | Hecho (`26ebd4af3`, `1878a1e39`, `33e1c7284`, `5556086f0` y el arreglo después de la ola). Con `?escenario=error`, «Entregas confirmadas del último mes» dice qué falta en un renglón entre filetes, sin repetir el banner; el dibujo queda sin leyenda, porque sin datos los frutos dirían 0 ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 1) |
| PUB1-02 | `pub1-portada` | El menú público: Inicio, Entregas, Organizaciones y Cómo funciona (a `/publico-nosotros`), sin el Mapa; en el Mapa queda marcada Entregas | DA-01, DA-05 y DA-17 | Hecho (`26ebd4af3`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-03 | `pub1-entregas` | Entregas: las entregas confirmadas, cada una con su foto, su etiqueta y quién la recibió, y un enlace al Mapa | Deriva de A10; DA-01, DA-10 y HG-03 | Hecho (`8cfbaab5d`, `a56418c53` y `60a4c976f`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-04 | `pub1-entregas` | El Mapa, dentro de Entregas: el mapa con su lista y las migas a Entregas | Deriva de A07; DA-01 | Hecho (`8cfbaab5d`, `60a4c976f` y `fb9e34317`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-05 | `pub1-organizaciones` | Organizaciones: tarjetas sobre la lámina, con el avance de cada necesidad y la barra de `componentes/listado` | Deriva de A02; HG-04 | Hecho (`375608498`, `a1cb6457c` y `8cef6d2ee`). Con datos reales la cuenta de cada necesidad va en palabras, sin barra (HG-04). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-06 | `pub1-ingresar` | Ingresar y su error, la puerta de la demo | Deriva de A12 | Hecho (`5ce687f2d` y `09942eb6c`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-07 | `pub1-conocer` | Cómo funciona, Cómo donar y Ayuda: las páginas de hoy, con su nombre nuevo y la composición de A01 | Deriva de A01; DA-05 | Hecho (`6c5dc109c`, `7da9a7374`, `9738e96df`, `3b08f925f`, `fb9e34317`, `5556086f0` y el arreglo después de la ola). «Qué ves con tu cuenta» de Cómo donar es una tarjeta con h3 bajo el título de sección «Tu cuenta», como en Ayuda ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 2) |
| PUB1-08 | `pub1-conocer` | El pie nombra esas tres páginas con su nombre nuevo | DA-23 | Hecho (`6c5dc109c`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-09 | `pub1-contacto` | Contacto y su error; la respuesta pasa a aviso breve en Contacto, con el número del mensaje | Deriva de A12; DA-06 y DA-24 | Hecho (`35f6c8307` y `fb9e34317`). Revisión de pantallas de la ola sin PASS (13.5) |
| PUB1-10 | `pub1-contacto` | Términos y privacidad, una página de lectura | Sin arquetipo: derivación (DA-08) | Hecho (`35f6c8307`). Revisión de pantallas de la ola sin PASS (13.5) |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5), con el
límite de las hojas compartidas de §13.2.

### 13.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
  `pub1-organizaciones` corre además `-Pvisual` sobre `/donante-entidades` y
  `/donante-entidades?cuenta=persona&ya=si` antes de comitear, porque esas pantallas usan su
  fragmento `pantalla`; no entran en la revisión ni en la hoja de contacto.
- **Mientras dura la tanda:** cada lote ve el menú y el pie de hoy fuera de `pub1-portada` y de
  `pub1-conocer`. No es hallazgo del lote.
- **Al cierre:** `-Pvisual` sobre las 54 rutas de PUB1 de `paginas.txt` (53 cuando se escribió el manifiesto; `pub1-organizaciones` sumó `?abrir=comedor`) (las de `/publico-portada`,
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

### 13.5 Cierre de PUB1 (2026-11-24)

Rama `E7_guia_ui_pub1`, `5556086f0`; 17 commits de lote sobre `c8c0bee77` (el manifiesto
integrado), uno de cierre (`d548de261`) y dos de la revisión de pantallas de la ola (`fb9e34317` y
`5556086f0`). Estado de cada ítem en §13.3: 8 hechos y 2 parciales (PUB1-01 y PUB1-07), por los
bloqueantes que dejaron `pub1-portada` y `pub1-conocer` tras 3 rondas de su revisión. La revisión de
pantallas de la ola corrió 2 rondas de arreglos y no llegó a PASS. Por eso las decisiones del
usuario de esta ola (DA-01, DA-05, DA-10, DA-17, DA-22, DA-23 y DA-24) constan como no cumplidas,
aunque su comportamiento esté en el código y en los tests. Lo que queda abierto está en
[DTI-50](../adr/DEUDA_TECNICA.md#dti-50); las decisiones, en la [bitácora](bitacora.md)
(2026-11-24).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `5556086f0`: exit 0, 903 tests, sin fallos.
  En `3b08f925f`, antes de la revisión de pantallas: 903 tests, sin fallos (867 en la línea de base de
  la corrida). `InterfazFitnessTest` pasa con los topes de hoy (159 y 1192); el test cambió solo en
  una línea: `publico-donaciones.css` sale de `HOJAS_HEREDADAS` (la hoja se queda, solo dispone).
  `clases-heredadas.txt` no suma ninguna línea; la integración de la tanda 1 (`d2fd4d81a`) y los
  lotes borraron las que quedaron sin uso. `node scripts/agent-check.js` y `mvn spotless:check`: sin
  fallos.
- `[VERIFIED]` Cercos con `git diff 73d54b878..HEAD`: sin cambios en `rest/`, los puertos,
  `tokens.css`, los tests de `visual/`, `templates/componentes/`, `static/css/componentes/`,
  `layout-publico.html`, `static/js/` ni `iconos.html`. Ningún lote borró una plantilla ni una hoja.
  Fuera de las plantillas y hojas de sus pantallas se tocaron `PublicoEntidadesController` (los
  filtros y las páginas), los tests funcionales de sus rutas y
  `seguridad/ErroresConSesionTest.java` (la página de enviado ya no tiene «Volver a mi inicio», por
  DA-24; la aserción nueva es igual de fuerte).
- `[VERIFIED]` `-Pvisual` sobre las 54 rutas de PUB1 de `paginas.txt` (sin la barra inicial y sin
  `/publico-donaciones?respuesta=404`), en `d548de261`: pasan 52. Fallan las dos de la línea de base
  de §10.4, `/publico-mapa?escenario=muchas` y
  `/publico-mapa?escenario=muchas&pagina=2&abierta=ejemplo-12`, solo por el marcador numerado del
  mapa a 360 px (`button.mapa-marcador`, que el reporte marca por toque con «menos de 48x48 px»
  aunque mide 48 × 48): es del catálogo (`componentes/mapa.css`, `static/js/mapa.js`), que esta ola
  no edita. Ninguna ruta pasó de rojo a verde ni de verde a rojo. No se volvió a correr después de
  las dos rondas de la revisión de pantallas.
- `[VERIFIED]` Los otros cinco tests visuales, en `d548de261`: 31 tests, 4 fallos y 3 errores. Los de
  la línea de base de §10.5 y §11.3: `ArmazonFijoTest.enElCelularConTabYShiftTabElFocoNoQuedaTapado`,
  `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`, el inestable de
  `MenusQueSeCierranTest` y el error del lema del pie
  (`AbejasYRenglonesTest.loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos`,
  [DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). Los esperados por DA-22, que miden las abejas y el corte
  «bajo tierra» que la portada de A01 no dibuja, para que decida el usuario:
  `AbejasYRenglonesTest.conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras` y los dos de
  `BajoTierraRotulosTest` (error de Playwright: no encuentran lo que miden). `DonanteDetallesTest`
  pasa sus 7. Ningún fallo nuevo. No se volvieron a correr después de la revisión de pantallas.
- Sin stack ni recorrido con login (DA-20): ningún lote cambia un adaptador `rest`. Queda para Z.
- Revisión de los lotes: `pub1-portada` y `pub1-conocer` quedaron sin PASS tras 3 rondas
  ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítems 1 y 2). Para los demás, el veredicto no consta en
  el cierre; tuvieron rondas de arreglo `pub1-organizaciones` (2), `pub1-entregas` (1) y
  `pub1-ingresar` (1), y ninguna `pub1-contacto`. La revisión de pantallas de la ola: 2 rondas de
  arreglos (`fb9e34317`: el Mapa a 360 px, «Enviar mensaje» en un racimo, las preguntas frecuentes
  con `.plegable` y las salidas de Cómo funciona a los registros; `5556086f0`: los errores de carga de
  la portada y de Cómo funciona con el banner de error y «Cuidamos los datos» con DA-10), sin PASS.
- Commits de la ola, en hora del 2026-11-24: `5ce687f2d` (17:13), `375608498` (17:19), `8cfbaab5d`
  (17:30), `26ebd4af3` (17:38), `a56418c53` (17:40), `6c5dc109c` (18:06), `35f6c8307` (18:10),
  `7da9a7374` (18:11), `d2fd4d81a` (18:20, integración), `a1cb6457c` (18:46), `60a4c976f` (18:47),
  `09942eb6c` (18:51), `1878a1e39` (18:52), `9738e96df` (19:03), `33e1c7284` (19:30), `8cef6d2ee`
  (19:34), `3b08f925f` (19:37), `d548de261` (20:08, cierre), `fb9e34317` (20:27, revisión de
  pantallas, ronda 1) y `5556086f0` (20:49, ronda 2).
- Arreglo después de la ola (2026-11-24, un commit sobre `8e4a77b02`): el error de las entregas del
  mes en la portada (PUB1-01), la tarjeta de la cuenta en Cómo donar (PUB1-07), la lógica de
  Organizaciones del controller a `vistas.ConsultaOrganizaciones` con su test (DTI-50, ítem 4, la
  primera mitad), el paso «Viaja» con el mismo texto en la portada y en Cómo funciona («Ves en tu
  cuenta por dónde anda el camión.») y, mientras el catálogo no tenga el título sobre la trama
  (DTI-50, ítem 6a), la portada y las tres páginas de conocer dejan el h1 como lo da el sistema: sus
  hojas ya no declaran la caja de la trama, ni la letra, ni medidas literales.
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); DA-22 a DA-24 son decisiones de pantalla. Ninguna spec de `docs/specs/active/` es de esta
  ola, así que no se mueve ninguna.
- Documentos: `mapa-del-sitio.md` suma los cambios de la ola (el menú público, el Mapa dentro de
  Entregas, los tres nombres nuevos y la respuesta de Contacto); `componentes.md`, las filas de las
  piezas que ahora usan las públicas (la trama, la etiqueta de entrega, el plegable, el paginador, la
  huerta, la tarjeta y la navegación) y los tres enlaces del pie con su nombre nuevo (DA-23), también
  en la muestra de `/estilos`; `patrones-y-microcopy.md`, la regla 26 (un envío termina en la misma
  pantalla, con el número en el aviso) y la fila 6 de las reglas de la revisión (Contacto ya no
  tiene «Volver a mi inicio»). `tono-y-comunicacion.md` y
  [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no cambian: el primero no nombra
  páginas ni menús, y ningún requerimiento cambia de estado.

## 14. Ola ADM1: el admin, la operación

Rehace las 18 plantillas de ADM1 de §5, el menú del admin (§5.1; DA-04 y DA-17) y las piezas del
galpón que DA-18 pasó de C1 a esta ola: la tabla con selección (P21), con la casilla sola de
48 × 48 px que C1-12 dejó para acá, el cajón (P22) y el armazón del galpón (la mitad de P26).
Lleva dos fichas, aprobadas por delegación (DA-08): [`flujos/guia-adm-ranking.md`](flujos/guia-adm-ranking.md)
(Ranking) y [`flujos/guia-adm-inicio-graficos.md`](flujos/guia-adm-inicio-graficos.md) (lo que va
debajo de la bandeja en `admin-inicio`, DA-04). Manifiesto:
[`backlog-guia-al-codigo.adm1.manifiesto.json`](backlog-guia-al-codigo.adm1.manifiesto.json). Se
escribió el 2026-11-24 en la corrida desatendida, mientras corría PUB1: el reparto, las fichas y
DA-25 a DA-31 no los aprobó el usuario y van con la opción recomendada (§2.2). Se lanza desde la
`E7_guia_ui` que deje PUB1.

### 14.1 Decisiones de la ola

| ID | Qué decide | Lote |
|---|---|---|
| DA-04 | «Por decidir» reemplaza a Inicio y a Asignar en el menú; los gráficos van debajo de la bandeja, con ficha | `adm1-galpon` (menú) y `adm1-por-decidir` (la bandeja y los gráficos) |
| DA-17 | Cambia solo el menú del admin | `adm1-galpon` |
| DA-18 y DA-27 | El cajón y el armazón del galpón, con la barra superior en todos los anchos | `adm1-galpon` |
| DA-18 y DA-21 | La tabla con selección, con la casilla de 48 × 48 px | `adm1-asignaciones` |
| DA-25 | Cómo se reparten `/admin-inicio` y `/admin-asignaciones`: la misma cabecera y la misma barra de pestañas | `adm1-asignaciones` (la cabecera y las pestañas, como fragmento) y `adm1-por-decidir` (`/admin-inicio`) |
| DA-26 | Aprobar o descartar varias propuestas juntas, sin cambiar el puerto | `adm1-asignaciones` |
| DA-07 | «Vencida» con el par neutro, no como problema | `adm1-donaciones` |
| DA-06 | «Registrada» pasa a aviso breve en Donaciones | `adm1-registrar` |
| DA-08, DA-29 y DA-30 | La ficha de los gráficos: el resumen denso y los gráficos como parte de la pantalla | `adm1-por-decidir` |
| DA-08, DA-28 y DA-31 | La ficha de Ranking: la tabla sin podio y el mes como hoy | `adm1-ranking` |

DA-25 a DA-31 son nuevas de esta ola; su opción descartada está en §2.2.

### 14.2 Reparto

Dos tandas. La primera trae las piezas del galpón y las pantallas que no las necesitan para
construirse; la segunda, las que usan la tabla con selección o el armazón nuevo, y las que tocan
un archivo que en la primera es de otro lote.

- `adm1-por-decidir` va en la tanda 2 porque usa la cabecera, las pestañas y la tabla de
  propuestas que deja `adm1-asignaciones` (tanda 1), y el armazón de `adm1-galpon`.
- `adm1-registrar` va en la tanda 2 porque toca `AdminDonacionesController.java` y
  `AdminDonacionesEnvios.java`, que en la tanda 1 toca `adm1-donaciones`, y porque «registrada»
  muestra Donaciones con el aviso breve (DA-06).
- `adm1-ranking` va en la tanda 2 para revisarse con el armazón del galpón; no depende de otro lote.

| Lote | Tanda | Plantillas | Arquetipo y decisiones | Ficha |
|---|:-:|---|---|---|
| `adm1-galpon` | 1 | Ninguna pantalla: `layout-privado.html` (solo lo del admin) y el menú del admin | P22 y la mitad de P26; DA-04, DA-17, DA-18 y DA-27 | No |
| `adm1-asignaciones` | 1 | `admin-asignaciones`, `-aprobada`, `-corrida`, `-descartada` y `-error` | A03 directo; P21; DA-18, DA-21, DA-25 y DA-26 | No |
| `adm1-camiones` | 1 | `admin-camiones`, `-agregado`, `-agregar-error`, `-deshabilitado` y `-habilitado` | Deriva de A03; la flota, sin arquetipo, con `componentes/flota` | No |
| `adm1-donaciones` | 1 | `admin-donaciones`, `-detalle` y `-detalle-vencida` | Deriva de A03; DA-07 | No |
| `adm1-por-decidir` | 2 | `admin-inicio` | A03 directo; los gráficos, sin arquetipo; DA-04, DA-25, DA-29 y DA-30 | Sí |
| `adm1-registrar` | 2 | `admin-donaciones-registrar`, `-registrar-error` y `-registrada` | Deriva de A12, en el galpón; DA-06 | No |
| `adm1-ranking` | 2 | `admin-ranking` | Sin arquetipo; DA-28 y DA-31 | Sí |

Suma: 0 + 5 + 5 + 3 + 1 + 3 + 1 = 18, las 18 de §5.

**Plantillas y hojas del área que no son pantallas de ADM1:**

| Plantilla u hoja | En ADM1 |
|---|---|
| `layout-privado.html` | `adm1-galpon`, solo lo que dibuja para el admin: el armazón de la huerta de quien dona y de la organización no cambia |
| `estilos.html` y `docs/interfaz/componentes.md` | Compartidos (regla 4): `adm1-galpon`, la muestra y la fila del cajón y del galpón; `adm1-asignaciones`, las de la tabla con selección |
| `static/css/componentes/navegacion.css` y `privado.css` | `adm1-galpon`, solo lo del galpón |
| `static/css/componentes/tabla.css` | `adm1-asignaciones`, solo la variante con selección: la tabla de hoy no cambia para quien dona ni para las listas del admin |
| Las demás hojas y fragmentos de `componentes/` (entre ellas `flota`, `mapa`, `filtros`, `listado`, `pestanas`, `segmentos`, `sello` y `datos`) y `static/js/` | Fuera: son del catálogo; se usan sin editarlos |
| `iconos.html` | Fuera: los íconos del cajón salen de `iconos :: comunes` (`i-bandeja`, `i-caja`, `i-cuenta`, `i-gente`, `i-camion`, `i-podio` e `i-puntos`), como hizo PUB1 con `i-brote` |
| `admin-secciones.css` | Compartida: es la hoja de las 39 pantallas del admin que no son el inicio, también las de ADM2. Cada lote saca solo las reglas que usan nada más que sus pantallas; no se borra |
| `admin-inicio.css` | Compartida: la importa `admin-secciones.css`, así que la ven todas las pantallas del admin. `adm1-por-decidir` saca las reglas que usa nada más que el inicio y deja los gráficos (DA-30); no se borra ni sale de `HOJAS_HEREDADAS` |
| `entidad-comun.css` | Fuera: la importa `admin-inicio.css` y es de las pantallas de la organización (ORG); no se edita |
| Las 22 pantallas de ADM2 (`admin-donantes*`, `admin-importaciones`, `admin-entidades*`, `admin-administradores*`, `admin-actividad`, `admin-notificaciones*` y `admin-perfil`) | Fuera: ven el armazón y el menú nuevos con su contenido de hoy; quien revisa lo anota como menor |
| `persona/datos-persona.html`, `datos-donante.html`, `viaje-datos.html`, `piezas-entidad.html` y `datos-publicos.html` | Fuera: son de DON, ORG y lo público |
| `cuenta-menu.html`, `interaccion.html`, `envio.html`, `cerrar-sesion.html`, `paginador.html` y `pie.html` | Fuera: son comunes a los cuatro roles; no se editan |
| `maqueta/AdminPersonasMaqueta.java` | Compartida con ADM2: `adm1-por-decidir`, solo los datos del inicio |
| `maqueta/AdminOperacionMaqueta.java` y `AdminOperacionDonaciones.java` | Compartidas (regla 4): cada lote, solo los datos de sus pantallas |

### 14.3 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| ADM1-01 | `adm1-galpon` | P22 · El cajón del galpón: las secciones en la tierra y la actual en papel, con su cuenta chica (la cuenta de la persona queda en la barra de arriba, DA-27); con su muestra en `/estilos` y su fila en el catálogo | Guía 417-427; DA-18 | Parcial (`b5a6879f8`, `ed7631670`, `e9fbc76d2`, `2b7ef8b38`, `e90cd4bf1` y `8b0e09344`): el cajón con sus seis secciones, la actual en papel, su muestra a todo el ancho en `/estilos` y su fila en el catálogo. Falta la cuenta chica en las pantallas: tiene estilo y muestra, pero `OpcionNavegacion` no trae el número y no se tocan los puertos ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 1) |
| ADM1-02 | `adm1-galpon` | P26, la mitad del galpón · El armazón del admin: el papel claro, los títulos un escalón más chicos y el contenido denso; desde 56 em el cajón siempre abierto a la izquierda, con la barra superior fina arriba (marca, campana y cuenta); hasta 56 em, la barra del celular | 15, 16-25 y 98-104; DA-18 y DA-27 | Hecho (`b5a6879f8`, `ed7631670`, `e9fbc76d2`, `2b7ef8b38` y `e90cd4bf1`): `body[data-armazon="galpon"]`, la barra de arriba en todos los anchos (marca, campana y cuenta), el cajón fijo a la izquierda desde 56 em y la barra del celular con Más. La barra de arriba conserva el alto de la huerta y, desde 56 em, queda fija también en ventanas bajas (desvío de R11-33, [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 2). `ArmazonFijoTest` pasa sus 12 (en `2a5e83682`) |
| ADM1-03 | `adm1-galpon` | El menú del admin: Por decidir, Donaciones, Donantes, Organizaciones, Camiones y Ranking; en el celular, Por decidir, Donaciones, Camiones y Más, que abre todas. Por decidir lleva a `/admin-inicio` y queda marcada también en `/admin-asignaciones` y sus respuestas | §5.1; DA-04, DA-17 y DA-25 | Hecho (`b5a6879f8`): Por decidir, Donaciones, Donantes, Organizaciones, Camiones y Ranking; en el celular, Por decidir, Donaciones, Camiones y Más. Por decidir lleva a `/admin-inicio` y queda marcada en `/admin-asignaciones` y sus respuestas. Los menús de los otros roles no cambiaron |
| ADM1-04 | `adm1-asignaciones` | P21 · La tabla del galpón con selección: la casilla sola de 48 × 48 px (el patrón de DA-21), la de elegir todas, la fila elegida, las columnas que se esconden en el celular y la barra de las elegidas con su cuenta; con su muestra en `/estilos` y su fila en el catálogo | Guía 395-405; 15, 20; DA-18 y DA-21 | Hecho (`0fda83499`, `4bc65d917`, `a8c064a75`): `.tabla[data-forma="seleccion"]` con `.tabla-casilla` de 48 × 48 px, la fila elegida, `data-columna="ancha"` y `.tabla-elegidas`. Sin JavaScript (esta ola no edita `static/js`): elegir todas es un enlace con `?elegir=todas` que dibuja el servidor y la cuenta es un contador de CSS, que no se anuncia al marcar ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 3) |
| ADM1-05 | `adm1-asignaciones` | Asignar pasa a ser la mesa de «Por decidir»: la cabecera y la barra de pestañas de A03 (en un fragmento que usa también `/admin-inicio`), Propuestas en la tabla con selección, Conflictos de a una, Destino y Decididas; las cuatro respuestas, con su aviso breve | A03; DA-25 | Hecho (`0fda83499`, `e88438454`, `4bc65d917`, `a8c064a75` y `e90cd4bf1`): la cabecera y las pestañas de A03 en fragmentos (`admin-asignaciones :: cabecera` y `:: pestanas`), Propuestas en la tabla con selección, Conflictos, Destino y Decididas, y las cuatro respuestas con el aviso breve con signo. Desde `e90cd4bf1` las pestañas llevan la fecha y las cuentas de Solicitudes y Por vencer del mismo origen que `/admin-inicio`. La columna «La donación vence» de A03 no se dibuja: el dato no llega ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 4) |
| ADM1-06 | `adm1-asignaciones` | Aprobar o descartar varias propuestas juntas, una por una contra el puerto de hoy, con el aviso de cuántas se decidieron | DA-26 | Hecho (`0fda83499`, `4bc65d917` y `e90cd4bf1`): varias `propuesta` a `-aprobada` o `-descartada`, una por una contra `decidirPropuesta`; si una da 409, para y el aviso dice cuántas se decidieron; en la maqueta de error, la que falló queda en la tabla sin casilla, con el sello «problema», y se descarta desde el aviso. Otro error a mitad del lote lleva a la página de error sin la cuenta ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 4) |
| ADM1-07 | `adm1-camiones` | Camiones y sus cuatro respuestas: la cabecera del galpón, la barra del listado, las vistas por estado con su cuenta y la flota con su ficha; el mapa chico con los marcadores de 48 px | Deriva de A03; la flota, sin arquetipo | Hecho (`57b0fe52d`, `c0a520425`, `e90cd4bf1` y `8b0e09344`): la cabecera del galpón con las cifras de la flota, el error con el `.aviso` del catálogo, los avisos con `admin-donaciones :: hecho`, el alta con error con el patrón de A12 y el mapa chico con `data-mapa-margenes="24 96 120 40"`. `/admin-camiones` y `/admin-camiones-agregar-error` salen de la línea de base de §10.4 |
| ADM1-08 | `adm1-donaciones` | Donaciones, su detalle y el detalle de una vencida, con «Vencida» en el par neutro | Deriva de A03; DA-07 | Hecho (`341f726ee`, `b13b05eed`, `1af6d4b85`, `e90cd4bf1` y `8b0e09344`): la `.tabla` del catálogo con cada fila al detalle, en lugar de la bandeja con ficha de R11-34 (decisión de agente, [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 5); «Vencida» con el par neutro en la lista y en el detalle (DA-07). `/admin-donaciones?donacion=` dibuja el detalle (200) para los enlaces de Decididas y de Donantes |
| ADM1-09 | `adm1-por-decidir` | `/admin-inicio` es «Por decidir»: la cabecera y las pestañas de A03, con Propuestas abierta (la primera página de las propuestas sin conflicto en la tabla con selección) | A03; DA-04 y DA-25 | Hecho (`0dec8c01c`, `b64f154fa` y `e90cd4bf1`): `/admin-inicio` es «Por decidir» con la cabecera y las pestañas del fragmento de Asignar, Propuestas abierta en la tabla con selección y «Aprobar las N» como única acción llena. La cuenta de Propuestas sale de la vista de asignaciones, no de `Decisiones.propuestas` |
| ADM1-10 | `adm1-por-decidir` | Debajo de la bandeja, según su ficha: «Cómo está todo» como resumen denso y los dos gráficos con su tabla | Sin arquetipo: ficha; DA-08, DA-29 y DA-30 | Hecho (`0dec8c01c` y `b64f154fa`): «Cómo está todo» como un resumen con `.datos` y su fecha, las salidas juntas debajo en un `.racimo`, y los dos gráficos con su tabla en `admin-inicio.css` con los nombres de token de la guía. «Entregas para mirar hoy» no se dibuja (DA-29, [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 6) |
| ADM1-11 | `adm1-registrar` | Registrar donación y su error, en el galpón; «registrada» pasa a aviso breve en Donaciones y, en la maqueta, su ruta sigue | Deriva de A12; DA-06 | Hecho (`24549de7c`, `2a5e83682`, `e90cd4bf1` y `8b0e09344`): Registrar en la composición de A12, cada bien en una tarjeta con h3, el botón fijo en el celular y «Volver sin registrar»; «registrada» es el aviso breve en Donaciones y, en la maqueta, su ruta muestra Donaciones con el aviso. Fuera de la maqueta, el error de datos sigue volviendo con un aviso breve y no con el resumen arriba ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 7) |
| ADM1-12 | `adm1-ranking` | Ranking, según su ficha | Sin arquetipo: ficha; DA-08, DA-28 y DA-31 | Hecho (`fa55bb12b` y `8b0e09344`): la tabla del catálogo sin podio, el puesto en número, el caption para lectores, la pastilla «Mes», «Volver al último mes» y el paginador de meses debajo de la tabla, y el error con el `.aviso` del catálogo |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5), con el
límite de las hojas compartidas de §14.2.

### 14.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
  `adm1-galpon` y `adm1-asignaciones` llevan `/estilos`. Antes de comitear, `adm1-galpon` corre
  además `-Pvisual` sobre `/admin-inicio`, `/admin-donaciones` y `/admin-donantes` (ADM2), y los
  tests `ArmazonFijoTest` y `MenusQueSeCierranTest`; esas rutas no entran en su revisión ni en la
  hoja de contacto.
- **Mientras dura la tanda 1:** `adm1-asignaciones`, `adm1-camiones` y `adm1-donaciones` ven el
  armazón y el menú de hoy. No es hallazgo de esos lotes.
- **Al cierre:** `-Pvisual` sobre las 61 rutas de ADM1 de `paginas.txt` (las que empiezan con
  `/admin-inicio`, `-asignaciones`, `-donaciones`, `-camiones` y `-ranking`) y sobre `/estilos`. Se
  compara con la línea de base de §10.4 que dejaron DON (§12.5) y PUB1. Corren también los otros
  cinco tests visuales, con la línea de base de §10.5, §11.3, §12.5 y del cierre de PUB1. No se
  levantan el stack ni el recorrido con login: los hace Z, y ningún lote cambia un adaptador `rest`
  (plan de la corrida, punto 5).
- **Lo que cambia en la línea de base:** `/admin-camiones` y `/admin-camiones-agregar-error` están en
  la de §10.4 por el marcador del mapa chico (`button.mapa-marcador`, del catálogo). `adm1-camiones`
  intenta sacarlas con lo que el catálogo ya tiene (el atributo `data-mapa-margenes` de
  `static/js/mapa.js`, como DON en Seguimiento). Si siguen fallando solo por el marcador, se anotan
  y no son bloqueantes; si pasan, también se anota. `ArmazonFijoTest` mide `/admin-donantes`,
  `/admin-donaciones`, `/admin-camiones` y `/admin-asignaciones?vista=decididas`: un fallo nuevo
  ahí es bloqueante del lote que lo rompe (DA-27).

### 14.5 Cierre de ADM1 (2026-11-25)

Rama `E7_guia_ui_adm1`, `8b0e09344`; 18 commits de lote y de sus arreglos sobre `5182610a0` (el
manifiesto integrado), uno de cierre (`78518efb4`) y dos de la revisión de pantallas de la ola
(`e90cd4bf1` y `8b0e09344`). Estado de cada ítem en §14.3: 11 hechos y ADM1-01 parcial (la cuenta
chica del cajón no llega a ninguna pantalla). La revisión de pantallas de la ola corrió 2 rondas
de arreglos y no llegó a PASS. Por eso las decisiones del usuario de esta ola (DA-04, DA-06, DA-07,
DA-17, DA-18 y DA-25 a DA-31) constan como no cumplidas, aunque su comportamiento esté en el
código y en los tests. Lo que queda abierto está en [DTI-51](../adr/DEUDA_TECNICA.md#dti-51); lo
que ADM1 cierra de C1, en [DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítems 6 y 13; las decisiones,
en la [bitácora](bitacora.md) (2026-11-25).

Después de la ola (2026-11-25, `7bdee25e1`): un arreglo de los bloqueantes de la ronda 3 (los
ejemplos de los campos del alta de camión enteros, «Volumen en m³» sin estirarse y la opción «Todas»
del filtro de Camiones). Los gates dieron verde (spotless, 926 tests, `-Pvisual` sobre las 19 rutas
de la ola, incluidas `/admin-camiones` y `/admin-camiones-agregar-error`, que salen de la línea de
base, y `agent-check`), y la revisión independiente de `revisar-ui` dio PASS. Sigue sin correr
`ArmazonFijoTest` ni `MenusQueSeCierranTest`: quedan para Z.

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `8b0e09344`: exit 0, 926 tests, sin
  fallos. En `2a5e83682`, antes de la revisión de pantallas: 925 tests, sin fallos (903 en PUB1).
  Spotless e `InterfazFitnessTest` incluidos: ningún tope cambió y el test no se editó.
  `clases-heredadas.txt` pierde 10 líneas (`adm1-por-decidir`) y no suma ninguna.
- `[VERIFIED]` Cercos con `git diff 5182610a0..HEAD`: sin cambios en `rest/`, `puertos/`,
  `tokens.css`, los tests de `visual/`, `InterfazFitnessTest.java`, `static/js/`,
  `templates/componentes/`, `iconos.html`, `entidad-comun.css`, `cuenta-menu.html`, `envio.html` ni
  `pie.html`. No se borró ninguna plantilla ni hoja; el único archivo nuevo es
  `ArmazonDelGalponTest.java`. `admin-donantes.html` (ADM2) se tocó en una ronda de `adm1-donaciones` y
  se revirtió: su enlace y el de «La cubría» en Decididas siguen apuntando a
  `/admin-donaciones?donacion=`, que dibuja el detalle.
- `[VERIFIED]` `-Pvisual` sobre las 62 rutas (las 61 de ADM1 de `paginas.txt` y `/estilos`), en
  `2a5e83682`: pasan las 62. `/admin-camiones` y `/admin-camiones-agregar-error`, de la línea de
  base de §10.4, **ahora pasan** (`data-mapa-margenes="24 96 120 40"` y la hoja de MapLibre antes
  del script, sin editar `mapa.js` ni `mapa.css`). Ninguna ruta pasó de verde a rojo. No se volvió
  a correr después de las dos rondas de la revisión de pantallas.
- `[VERIFIED]` Los otros cinco tests visuales, en `2a5e83682`: 31 tests, 3 fallos y 3 errores,
  todos de la línea de base: `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`, el
  del lema del pie ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítem 5) y los tres de DA-22
  ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 3). `ArmazonFijoTest` pasa sus 12, también
  `enElCelularConTabYShiftTabElFocoNoQuedaTapado`, que estaba en la línea de base de §10.5: sale de
  ella. `DonanteDetallesTest` pasa sus 7. El inestable de `MenusQueSeCierranTest` falló una vez y
  pasó 2 de 2 al repetirlo solo. Ningún fallo nuevo. No se volvieron a correr después de la revisión
  de pantallas.
- Sin stack ni recorrido con login (DA-20): ningún lote cambia un adaptador `rest`. Queda para Z
  comprobar con datos reales Por decidir, aprobar y descartar varias juntas, Camiones, Donaciones
  con «Vencida», Registrar y Ranking.
- Revisión de los lotes: el veredicto de cada una no consta en el cierre. Tuvieron rondas de
  arreglo `adm1-galpon` (2), `adm1-asignaciones` (2), `adm1-donaciones` (2), `adm1-camiones` (1) y
  `adm1-registrar` (1); `adm1-por-decidir` y `adm1-ranking`, ninguna (el segundo commit de
  `adm1-por-decidir` corrige el resumen y el error antes de su revisión). La revisión de pantallas
  de la ola: 2 rondas de arreglos (`e90cd4bf1`: el título separado de la barra del listado, la
  fecha y las cuentas de la mesa del mismo origen que el inicio, los errores de Donaciones y Asignar
  con el `.aviso`, el fragmento `admin-donaciones :: hecho`, la propuesta que no se pudo aprobar y
  la muestra del cajón a todo el ancho; `8b0e09344`: la barra del celular del galpón en `/estilos`
  a 360 px, el alta de camión con error en el patrón de A12, los errores de guardado con el
  `.aviso` y el error de Camiones y Ranking con el enlace en línea), sin PASS.
- Commits de la ola, en hora de autor (2026-11-24 y 25, -03:00): `b5a6879f8` (24, 22:32),
  `0fda83499` (22:24), `341f726ee` (22:07), `57b0fe52d` (22:17), `e88438454` (22:37), `ed7631670`
  (22:45), `e9fbc76d2` (23:15), `b13b05eed` (23:20), `4bc65d917` (23:33), `c0a520425` (25, 00:07),
  `a8c064a75` (00:33), `2b7ef8b38` (00:39), `1af6d4b85` (00:41), `fa55bb12b` (01:11), `24549de7c`
  (01:16), `0dec8c01c` (01:31), `b64f154fa` (01:40), `2a5e83682` (01:58), `78518efb4` (02:34,
  cierre), `e90cd4bf1` (03:18, revisión de pantallas, ronda 1) y `8b0e09344` (03:55, ronda 2).
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); DA-25 a DA-31 son decisiones de pantalla y DA-26 no cambia el puerto. Ninguna spec de
  `docs/specs/active/` es de esta ola, así que no se mueve ninguna.
- Documentos: `mapa-del-sitio.md` suma los cambios de la ola (el menú del admin, Por decidir en
  las dos rutas, decidir varias juntas, Donaciones como tabla y la respuesta de Registrar) y el
  diagrama y la tabla nombran Por decidir; `componentes.md` (además de las filas del cajón, el
  galpón y la tabla con selección que trajeron los lotes) actualiza `data-fijo`, las migas, la
  tarjeta (sale `decision`) y la flota; `patrones-y-microcopy.md`, la regla 26 (también Registrar
  donación) y la regla 27 (decidir varias juntas). `tono-y-comunicacion.md` y
  [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no cambian: el primero no nombra
  pantallas ni menús, y ningún requerimiento cambia de estado. Las fichas de antes de la ola que ya
  no describen el código están en [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 10.

## 15. Ola PUB2: registro, cuenta y páginas de estado

Rehace las 19 plantillas de PUB2 de §5 (14 de lo público y 5 comunes) y las dos que PUB1 dejó
anotadas para esta ola (§13.2): `error/429.html` y `pagina-error.html`, la pieza que comparten las
páginas de error. No lleva fichas: el registro en una sola página y las páginas de estado son
derivación del agente (DA-08). Ningún menú cambia: el público cambió en PUB1 (DA-17). Manifiesto:
[`backlog-guia-al-codigo.pub2.manifiesto.json`](backlog-guia-al-codigo.pub2.manifiesto.json). Se
escribió el 2026-11-24 en la corrida desatendida, mientras corría ADM1: el reparto y DA-32 no los
aprobó el usuario y van con la opción recomendada (§2.2). Se lanza desde la `E7_guia_ui` que deje
ADM1. PUB2 no comparte pantallas con ADM1. Comparte `privado.css`, y de esa hoja toca solo la regla
`.sin-dibujar` (regla 4).

### 15.1 Decisiones de la ola

| ID | Qué decide | Lote |
|---|---|---|
| DA-06 y DA-32 | Recuperar, Pedir el enlace y Activar responden con el aviso en la página, en su misma pantalla y sin el sello de recibido | `pub2-cuenta` |
| DA-06 y DA-32 | «Solicitud enviada» se queda como la página de estado de la solicitud, con «Solicitud recibida» como aviso en la página | `pub2-registro-entidad` |

DA-32 es nueva de esta ola; su opción descartada está en §2.2. DA-08 (sin fichas) y DA-17 (ningún
menú cambia) son reglas del manifiesto (1 y 3), no decisiones de un lote.

### 15.2 Reparto

Una sola tanda, con 4 lotes. Ninguna plantilla la tocan dos lotes. Los archivos que comparten se
reparten por líneas (regla 4): las hojas de la cadena de lo público, `AccesoController.java`,
`paginas.txt` y los tests de varias rutas.

| Lote | Tanda | Plantillas | Arquetipo y decisiones | Ficha |
|---|:-:|---|---|---|
| `pub2-cuenta` | 1 | `publico-recuperar`, `-recuperar-enviado`, `publico-activar`, `-activar-pedir`, `-activar-pedido`, `-activar-lista` y `-activar-error` | Deriva de A12, con la columna del formulario de Ingresar; DA-06 y DA-32 | No |
| `pub2-registro-donante` | 1 | `publico-registro`, `publico-registro-donante` y `-donante-error` | Deriva de A12, dentro del registro en una página del catálogo (`componentes/registro`) | No |
| `pub2-registro-entidad` | 1 | `publico-registro-entidad`, `-corregir`, `-enviada` y `-error` | Sin arquetipo: el registro en pasos, con `componentes/registro` y la columna de A12 (DA-08); DA-06 y DA-32 | No |
| `pub2-estado` | 1 | `error/403`, `error/404`, `error/429`, `error/5xx`, `pagina-error`, `sin-dibujar` y `aviso-persona` | Sin arquetipo: páginas de estado (DA-08) | No |

Suma: 7 + 3 + 4 + 7 = 21. Son las 19 de §5 (14 y 5 comunes), más `error/429` y `pagina-error`.

**Plantillas y hojas del área que no son pantallas de PUB2:**

| Plantilla u hoja | En PUB2 |
|---|---|
| `persona/datos-persona.html` | Fuera: no es una pantalla, son los datos de ejemplo de la persona (§5); los usan las pantallas de `persona/` (DON) |
| `componentes/registro.html` y `static/css/componentes/registro.css` | Fuera: son del catálogo, y los dos registros los usan tal como están. `IngresoConKeycloakRealTest` (de `visual/`, que nadie edita) busca `a.registro-tipo` y `form.registro-formulario` |
| `datos-publicos.html` | Fuera: `avisoError` lo usan también pantallas de PUB1; se usa sin editarlo |
| `navegacion-publica.html`, `pie.html`, `layout-publico.html` y `layout-privado.html` | Fuera: son de PUB1, de C1 y de ADM1; no se editan (DA-17) |
| `cuenta-menu`, `interaccion`, `envio`, `cerrar-sesion`, `paginador`, `iconos` y el resto de `componentes/` | Fuera: son comunes a los cuatro roles o del catálogo; no se editan |
| `datos-donante.html`, `viaje-datos.html`, `piezas-entidad.html` y el resto de `persona/` | Fuera: son de DON y de ORG; no se editan |
| `estilos.html` y `docs/interfaz/componentes.md` | Fuera: PUB2 no suma ni cambia piezas del catálogo, así que no tiene muestra ni fila que tocar |
| `publico-ingresar.css` | Compartida: es la hoja de Ingresar (PUB1) y de las 7 plantillas de `pub2-cuenta`. Ese lote saca solo las reglas de `.cuenta-principal`, que usan nada más que sus pantallas. Las de `main > .pila`, de Ingresar, no cambian. No se borra |
| `publico-registro.css` | Compartida: es la hoja de los dos registros y la importa `publico-ingresar.css`. Cada lote de registro saca solo las reglas que usan nada más que sus pantallas. No se borra |
| `publico-paginas.css` y `publico-listas.css` | Compartidas con PUB1 y `/estilos`. Una regla que usan solo pantallas de PUB2 la saca su lote. `.recibido` y `.publica` las usan `pub2-cuenta` y `pub2-registro-entidad`: ninguno de los dos las borra; las borra el integrador de la tanda 1 (abajo) |
| `publico-error.css` | De `pub2-estado`: es la hoja de las páginas de error y solo dispone |
| `privado.css` | Compartida con todo lo privado y con ADM1: `pub2-estado` toca solo la regla `.sin-dibujar`. `.aviso-codigo` la usan también pantallas del admin, así que queda |

Ninguna de las hojas de PUB2 sale de `HOJAS_HEREDADAS` en esta ola, porque las cinco las usan
también pantallas de otras olas. Si una queda sin reglas heredadas, se anota para Z.

**Las clases heredadas que comparten varios lotes.** En `clases-heredadas.txt` cada lote borra solo
las líneas de las clases que deja sin uso. Una clase heredada que usan plantillas de dos o más lotes
de PUB2, y ninguna de fuera de la ola, no la puede borrar ningún lote en su rama, porque la plantilla
del otro todavía la usa. `[OBSERVED]` Hoy son siete: `publica` y `cuenta` (las cuatro de
`pub2-cuenta` con `.publica` y `publico-registro-entidad-enviada`), `recibido`
(`publico-activar-lista` y `publico-registro-entidad-enviada`), `campos-par` y `casilla`
(`publico-registro-donante` y `publico-registro-entidad`), `clave-fila` (`publico-activar` y los
dos registros) y `lectura-bajada` (tres de `pub2-cuenta`, `publico-registro-entidad-enviada` y
`pagina-error`). Si al integrar la tanda 1 alguna queda sin uso,
`InterfazFitnessTest.clasesDeLasPlantillasEstanEnElCatalogoOEnLasHeredadas` da rojo. Es un choque
entre lotes y lo arregla el integrador de la tanda (su paso 4): borra cada línea que ya no usa
ninguna plantilla y, con cada una, las reglas que quedan sin uso en las hojas compartidas (por
ejemplo `.recibido` en `publico-paginas.css`, `.publica` en `publico-listas.css` y
`.cuenta .formulario` en `publico-registro.css`). Lo dice la regla 4.

### 15.3 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| PUB2-01 | `pub2-cuenta` | Recuperar la contraseña: la columna del formulario de Ingresar, con su campo, una sola acción llena y la salida a Ingresar | Deriva de A12 | Hecho (`d03df922f` y `3f23221bb`): el `.pila` con el h1 y la bajada, el correo con su `.campo`, «Mandame el enlace» como única acción llena y «Volver a Ingresar» como enlace. Con la respuesta se va la bajada que pide el correo (`3f23221bb`) |
| PUB2-02 | `pub2-cuenta` | Activar la cuenta y Pedir el enlace, con sus estados (lento, error, sin conexión y vencido) y el error de datos (`publico-activar-error`), en la misma columna | Deriva de A12 | Hecho (`d03df922f` y `3f23221bb`): la columna de Ingresar, con la contraseña y «Mostrar» en un `.racimo` (sale `.clave-fila`), el resumen del error en el `.aviso` con el foco, el banner de enlace vencido con «Pedir otro enlace» como acción llena y el estado lento con `aria-busy` y sin `disabled`, como Ingresar (`3f23221bb`). El correo de solo lectura se ve igual que uno editable ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 4). Después de la ola, un solo h1, «Activá tu cuenta», en todos los estados, y el campo «Correo electrónico», como Ingresar y Recuperar (arreglo posterior a la ola, §15.5) |
| PUB2-03 | `pub2-cuenta` | Las tres respuestas (`-recuperar-enviado`, `-activar-pedido` y `-activar-lista`) con el aviso en la página, en su misma pantalla y sin el sello de recibido | DA-06 y DA-32 | Hecho (`d03df922f` y `3f23221bb`): las tres muestran su misma pantalla con el `.aviso` `data-tipo="exito"` en el lugar del formulario; en `-activar-lista`, «Tu cuenta está lista» es el título del aviso, el h1 sigue «Activá tu cuenta» e Ingresar es la única acción llena. Las tres rutas siguen y ninguna usa `.recibido`. DA-32 consta como no cumplida: la revisión de pantallas de la ola no llegó a PASS (§15.5) |
| PUB2-04 | `pub2-registro-donante` | Crear una cuenta: la cabeza y las tres tarjetas del tipo de cuenta del catálogo (`registro :: cabeza` y `:: tipos`), con su salida a Ingresar | Deriva de A12 | Hecho (`57ca07271`, arreglos `df357b740`, `f36b98e39` y `40083290f`): `registro :: cabeza` y `:: tipos` como están en el catálogo; la salida a Ingresar, centrada con una regla de disposición de `publico-registro.css` (`40083290f`) |
| PUB2-05 | `pub2-registro-donante` | El registro de quien dona (persona y empresa) y su error: el índice de pasos y el formulario del catálogo, el resumen del error arriba y una sola acción llena | Deriva de A12 | Parcial (`57ca07271`, arreglos `df357b740` y `f36b98e39`): el índice de pasos y `.registro-formulario` con una `.registro-seccion` por paso, la contraseña en un `.racimo`, el resumen del error en el `.aviso` dentro del formulario y el botón en un `.racimo`; lento con `aria-busy`. Nombre y Apellido de la persona en la `.grilla` del catálogo, como Calle y Altura en el de la organización (sale `.campos-par`; arreglo posterior a la ola, §15.5). Falta: con JavaScript, un reenvío con errores sobre `-donante-error` muestra dos resúmenes, porque el del servidor ya no lleva `.resumen` y `publico.js` no lo saca; la revisión del lote lo dejó como bloqueante tras 3 rondas ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 1) |
| PUB2-06 | `pub2-registro-entidad` | El registro de la organización, su corrección y su error, con el mismo formulario; `hoja` y `comoSigue` siguen como fragmentos | Sin arquetipo: el registro en pasos (DA-08) | Parcial (`1dd71c308`, arreglos `21f7d3ae3`, `8e0e7f0be`, `40083290f` y `3f23221bb`): los pasos como `section.registro-seccion` con h2, la dirección en `.grilla` con los campos alineados abajo, «Volver a la solicitud sin guardar» como botón secundario, `comoSigue(paso, nivel)` con h3 dentro de Confirmar y el resumen del error con el mismo marcado que el del donante (`40083290f`); lento con `aria-busy` y sin `disabled` (`3f23221bb`). Falta lo mismo que en PUB2-05: el resumen doble con JavaScript ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 1) |
| PUB2-07 | `pub2-registro-entidad` | Solicitud enviada, la página de estado de la solicitud (R11-41), con sus cinco escenarios y «Solicitud recibida» como aviso en la página | DA-06 y DA-32 | Hecho (`1dd71c308`, arreglos `21f7d3ae3`, `8e0e7f0be`, `40083290f` y `3f23221bb`): sigue como pantalla, con la hoja `publico-registro` y la columna de Ingresar; «Solicitud recibida» (y «Guardamos los cambios» en `corregida`) en el `.aviso` `data-tipo="exito"`, sin `.recibido`; «Cómo sigue» con `huerta :: raiz-pasos`. En `aprobada` se ocultan el aviso, la bajada, «Cómo sigue» y «Mientras tanto», y queda el banner con «Ingresar de nuevo» (`3f23221bb`). DA-32 consta como no cumplida: la revisión de pantallas de la ola no llegó a PASS (§15.5) |
| PUB2-08 | `pub2-estado` | Las páginas de error 403, 404, 429 y 5xx y su pieza `pagina-error`, sin sesión (el marco público) y con sesión (el privado del rol) | Sin arquetipo: página de estado (DA-08) | Hecho (`492548f78`): `pagina-error` con la composición de Términos y privacidad (el `.huerta-rotulo`, el h1, la bajada en un `p` simple y un solo botón lleno), el código para soporte en su propia pila; las cuatro páginas conservan la firma `pagina(...)` y 429 sigue diciendo «Esperá un minuto» |
| PUB2-09 | `pub2-estado` | `sin-dibujar` y `aviso-persona`, con la composición de lectura de la huerta; el lote suma sus dos rutas a `paginas.txt` | Sin arquetipo: página de estado (DA-08) | Hecho (`492548f78`): la misma composición, sin la regla `.sin-dibujar` de `privado.css`; el texto de `aviso-persona` no cambia. Sin ese tope, su h1 corre casi todo el ancho a 1280 px ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 5) |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5), con el
límite de las hojas compartidas de §15.2.

### 15.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla. En
  `pub2-estado` son las rutas `?respuesta=` de `paginas.txt` y dos que el lote suma:
  `/entidad-seguimiento-tesoro`, una sección sin pantalla que muestra `sin-dibujar` (como en
  `SitioControllerTest.unaSeccionSinPantallaMuestraQueNoEstaDibujada`), y
  `/donante-donaciones-nueva?cuenta=persona`, que muestra `aviso-persona` (como en
  `SitioControllerTest.conLaPersonaLasOtrasDelDonanteMuestranSuVersionOElAviso`). La segunda va en el
  bloque de la persona de `paginas.txt`, antes de `/donante-inicio?cuenta=empresa`, porque la cuenta
  queda en la sesión. En `pub2-registro-entidad`, la solicitud enviada entra a `rutasVisuales` solo
  sin escenario; sus cinco rutas (sin escenario y con `con-sesion-pendiente`, `corregida`,
  `sin-datos` y `aprobada`) las afirma un test de `SitioControllerTest` (200 y la vista
  `publico-registro-entidad-enviada`), que es el criterio de DA-32 de ese lote, y las mira
  `-Pvisual` al cierre.
- **Las hojas compartidas, antes de comitear:** un lote que saca reglas de `publico-ingresar.css` o
  de `publico-registro.css` corre además `-Pvisual` sobre `/publico-ingresar` y
  `/publico-ingresar-error`. Uno que saca reglas de `publico-paginas.css` o de `publico-listas.css`
  lo corre también sobre `/publico-contacto`, `/publico-legales` y `/estilos`. Esas rutas no entran
  en su revisión ni en la hoja de contacto. Una falla nueva ahí es bloqueante del lote.
- **Al cierre:** `-Pvisual` sobre las 40 rutas de PUB2 de `paginas.txt`: las 33 de
  `/publico-recuperar`, `-activar` y `-registro`, las 5 de `?respuesta=` y las 2 que suma
  `pub2-estado`. `[VERIFIED]` Hoy el mismo filtro da 38 líneas: faltan las dos que suma el lote.
  Ninguna está en la línea de base de §10.4, así que cualquier falla es bloqueante, con el lote que
  la rompe. Corren también los otros cinco tests visuales, y se comparan con la línea de base de
  §10.5, §11.3, §12.5, §13.5 y del cierre de ADM1 (§14). Un fallo que no esté en esa línea de base
  es bloqueante. No se levantan el stack ni el recorrido con login: los hace Z, y ningún lote cambia
  un adaptador `rest` (plan de la corrida, punto 5). `IngresoConKeycloakRealTest` necesita el
  stack y corre en Z.

### 15.5 Cierre de PUB2 (2026-11-25)

Rama `E7_guia_ui_pub2`, `3f23221bb`. Cuatro lotes en una tanda sobre `8b6608abd` (el manifiesto
integrado): `pub2-cuenta`, `pub2-registro-donante`, `pub2-registro-entidad` y `pub2-estado`, más
la integración de la tanda (`e2232c725`), cuatro commits de arreglos de la revisión de los lotes,
uno de cierre (`0118e666e`) y dos de la revisión de pantallas de la ola (`40083290f` y
`3f23221bb`). Estado de cada ítem en §15.3: 7 hechos, y PUB2-05 y PUB2-06 parciales (el resumen
doble con JavaScript en los dos registros). La revisión de pantallas de la ola corrió 2 rondas de
arreglos y no llegó a PASS. Por eso DA-32, la decisión de esta ola (delegada por el relevamiento
del 2026-11-24, §2.2), consta como no cumplida en sus dos partes, aunque su comportamiento esté en
el código y en los tests. Lo que queda abierto está en [DTI-52](../adr/DEUDA_TECNICA.md#dti-52);
las decisiones, en la [bitácora](bitacora.md) (2026-11-25).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `3f23221bb`, después de las dos rondas
  de la revisión de pantallas: exit 0, 934 tests, sin fallos (log en la carpeta de la corrida,
  `PUB2/gate-doc.log`). En `8e0e7f0be`, antes de esas rondas: exit 0, 934 tests,
  sin fallos (926 en ADM1). `InterfazFitnessTest` pasa sin editarse: ningún tope cambió.
  `clases-heredadas.txt` pierde 23 líneas y no suma ninguna.
- `[VERIFIED]` `-Pvisual` sobre las 40 rutas de PUB2 de `paginas.txt`, en `8e0e7f0be`: el filtro
  da 40 líneas, las 38 de antes más `/entidad-seguimiento-tesoro` y
  `/donante-donaciones-nueva?cuenta=persona`. Pasan las 40 (160 capturas, 4 anchos por ruta;
  `reporte.json` y `excepciones.json` vacíos). Ninguna estaba en la línea de base de §10.4.
  `[VERIFIED]` En `3f23221bb`, después de la revisión de pantallas, la misma pasada sobre las 40
  rutas más las cinco de las hojas compartidas de §15.4 (`/publico-ingresar`,
  `/publico-ingresar-error`, `/publico-contacto`, `/publico-legales` y `/estilos`): exit 0, 8
  tests, pasan las 45 (180 capturas; `reporte.json` y `excepciones.json` vacíos). Cubre también
  el gate de `pub2-cuenta`, que había quedado NOT_VERIFIED por no correr esas rutas.
- `[VERIFIED]` Los otros cinco tests visuales, en `8e0e7f0be`: 31 tests, 3 fallos y 3 errores,
  todos de la línea de base: `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`,
  `AbejasYRenglonesTest.conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras` y el del lema
  del pie ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)), y los dos de `BajoTierraRotulosTest` (DA-22,
  [DTI-50](../adr/DEUDA_TECNICA.md#dti-50)). `MenusQueSeCierranTest.lasPastillasDeFiltroSeCierranTocandoAfueraYConEsc`,
  el inestable conocido, falló esta vez. `ArmazonFijoTest` pasa sus 12 y `DonanteDetallesTest`
  sus 7. Ningún fallo nuevo. No se volvieron a correr después de la revisión de pantallas.
- Sin stack ni recorrido con login (DA-20): ningún lote cambia un adaptador `rest`. Queda para Z
  comprobar con datos reales el registro de quien dona y de la organización (incluida la
  corrección y la solicitud enviada con sus escenarios), Recuperar y Activar, y las páginas de error
  con sesión de cada rol. `IngresoConKeycloakRealTest` también corre en Z.
- Cercos `[VERIFIED]` (`git diff 46c1f4708..HEAD`, en `3f23221bb`): sin cambios en `rest/`,
  `puertos/`, `tokens.css`, los tests de `visual/`, `InterfazFitnessTest.java`,
  `templates/componentes/`, `static/css/componentes/`, `static/js/`, `auth-service/`, los menús
  (`navegacion-publica.html`, `pie.html`, `Pantallas.java`, `CuentaDeEjemplo.java`) ni
  `AGENTS.md`. Ninguna plantilla ni hoja borrada. El código Java de `cliente-liviano/src/main` no
  cambió: solo plantillas, hojas y tests (`SitioControllerTest` y `PerfilPendienteTest`, con
  aserciones igual de fuertes).
- Revisión de los lotes: el constructor de `pub2-registro-entidad` declaró que no leyó §15.2, las
  skills `implement-task` y `construir-ui` ni `tono-y-comunicacion.md`, y aplicó las reglas del
  pedido de la corrida. `pub2-registro-donante` y `pub2-registro-entidad` tuvieron 2 rondas de
  arreglos cada uno y quedaron con bloqueantes tras la tercera revisión: el resumen doble con
  JavaScript, los dos registros con el resumen distinto y el h2 «Cómo sigue» dentro del h2
  «Confirmar». La revisión de pantallas arregló los dos últimos (`40083290f`); el primero sigue
  abierto ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 1). `pub2-cuenta` y `pub2-estado`, sin
  rondas. La revisión de pantallas de la ola: 2 rondas de arreglos (`40083290f`: el mismo resumen
  en los dos registros, `comoSigue(paso, nivel)`, Calle y Altura en la misma línea y la salida a
  Ingresar centrada; `3f23221bb`: el relleno de «Lo que nos enviaste», `aprobada` sin la bajada,
  «Cómo sigue» ni «Mientras tanto», la bajada de Recuperar y Pedir que se va con la respuesta, el
  estado lento con `aria-busy` y sin `disabled` en Activar y en el registro de la organización, y
  el texto de cada campo en el resumen de la organización), sin PASS.
- Commits de la ola, en hora de autor (2026-11-25, -03:00): `8b6608abd` (05:46, manifiesto
  integrado), `57ca07271` (06:04), `d03df922f` (06:12), `492548f78` (06:12), `1dd71c308` (06:13),
  `e2232c725` (06:27, integración de la tanda), `df357b740` (06:46), `21f7d3ae3` (06:49),
  `f36b98e39` (07:04), `8e0e7f0be` (07:05), `0118e666e` (07:27, cierre), `40083290f` (07:52,
  revisión de pantallas, ronda 1) y `3f23221bb` (08:13, ronda 2).
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); DA-32 es una decisión de pantalla. Ninguna spec de `docs/specs/active/` es de esta ola,
  así que no se mueve ninguna.
- Documentos: `mapa-del-sitio.md` ya suma los cambios de la ola (desde `0118e666e`) y sigue
  valiendo; `patrones-y-microcopy.md` suma la regla 28 (la respuesta que dice qué hacer después
  queda en la página); `componentes.md` saca `aviso-acciones` de las clases heredadas (ya no la usa
  ninguna plantilla). `tono-y-comunicacion.md` y
  [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no cambian: el primero no nombra
  pantallas, y ningún requerimiento cambia de estado. Las fichas de antes de la ola que ya no
  describen el código están en [DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 7.
- Arreglo posterior a la ola (2026-11-25), sobre los bloqueantes de la revisión de pantallas
  (ronda 3): Nombre y Apellido del registro de la persona pasan a la `.grilla` del catálogo y
  `.campos-par` sale de `publico-registro.css` y de `clases-heredadas.txt`; Activar lleva un solo
  h1, «Activá tu cuenta», y rotula el campo «Correo electrónico»; salen de `publico-registro.css`
  `.enlace-campo`, `.campo-rotulo` y `.casilla-enlace`, sin uso. El resumen doble con JavaScript
  sigue abierto: solo se cierra en `static/js/publico.js`, fuera de esta ola
  ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 1). Gates `[VERIFIED]`: `mvn clean test -pl cliente-liviano -am`, exit 0, 934 tests sin fallos; `-Pvisual` sobre las 20 rutas del pedido del arreglo más `/publico-registro-donante?tipo=persona`, exit 0, 84 capturas y `reporte.json` vacío.

Después de la ola (2026-11-25, `6c8f677c1`): los gates del paso posterior dieron verde (spotless,
934 tests, `-Pvisual` sobre las 20 rutas de la ola y `agent-check` PASS 270). La revisión
independiente de `revisar-ui` dio CHANGES_REQUIRED, con dos bloqueantes: el resumen doble con
JavaScript ([DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítem 1) y la firma de `comoSigue`, que pasó a
`comoSigue(paso, nivel)` aunque la regla de `pub2-registro-entidad` la fijaba (ítem 9). La rama se
mergeó en `E7_guia_ui` por la regla del gate (gates en verde); los dos quedan para Z o el PR final.

## 16. Ola ADM2: el admin, las personas y la cuenta

Rehace las 22 plantillas de ADM2 de §5: Donantes y sus importaciones, Organizaciones, los dos
registros, Administradores y Actividad, Mi perfil y las Novedades del admin. No lleva fichas: todas
derivan de A03 o de A12 (DA-08), salvo el buzón, que sigue la suya de ORG
([`flujos/guia-org-buzon.md`](flujos/guia-org-buzon.md)). Ningún menú cambia: el del admin cambió
en ADM1 (DA-17). Manifiesto:
[`backlog-guia-al-codigo.adm2.manifiesto.json`](backlog-guia-al-codigo.adm2.manifiesto.json). Se
escribió el 2026-11-25 en la corrida desatendida, mientras corría PUB2: el reparto y DA-33 a DA-35
no los aprobó el usuario y van con la opción recomendada (§2.2). Se lanza desde la `E7_guia_ui` que
deje PUB2. ADM2 no comparte pantallas con PUB2. Con ADM1 comparte `admin-secciones.css`,
`admin-inicio.css` y `AdminPersonasMaqueta.java`, y de ellos cada lote toca solo lo de sus
pantallas (regla 4).

### 16.1 Decisiones de la ola

| ID | Qué decide | Lote |
|---|---|---|
| DA-34 | Donantes y Organizaciones siguen como bandeja con ficha (`.maestro`), en el galpón | `adm2-donantes` y `adm2-organizaciones` |
| DA-35 | La contraseña provisoria se queda como página propia; el alta sin JavaScript, como su página aparte | `adm2-administradores` |
| DA-06 y DA-33 | «Registrado» pasa a aviso breve en Donantes; registrar su donación sigue desde la ficha del donante | `adm2-registrar-donante` |
| DA-06 | «Registrada» pasa a aviso breve en Organizaciones | `adm2-registrar-organizacion` |

DA-33 a DA-35 son nuevas de esta ola; su opción descartada está en §2.2. En `adm2-organizaciones`
(ADM2-05) y `adm2-novedades` (ADM2-09), DA-06 ya se cumple hoy: sus respuestas son un aviso breve
en la lista, y el lote solo les pone el signo del aviso breve del galpón; por eso no llevan una
decisión en el manifiesto. DA-08 (sin fichas),
DA-17 (ningún menú cambia) y DA-21 (la casilla del buzón, que ORG-02 ya dejó en 48 × 48 px) son
reglas del manifiesto (1 y 3) o trabajo ya hecho, no decisiones de un lote.

### 16.2 Reparto

Dos tandas, como en ADM1: la primera trae las listas y la cuenta; la segunda, los dos registros.

- `adm2-registrar-donante` va en la tanda 2 porque toca `AdminDonantesEnvioController.java` y, si
  hace falta, `AdminDonantesController.java`, que en la tanda 1 toca `adm2-donantes`, y porque
  «registrado» muestra Donantes con el aviso breve (DA-06 y DA-33), con los fragmentos que rehace
  ese lote.
- `adm2-registrar-organizacion` va en la tanda 2 por lo mismo con Organizaciones:
  `AdminEntidadesEnvioController.java` y `AdminEntidadesController.java` son de
  `adm2-organizaciones` en la tanda 1, y «registrada» muestra su lista.
- Mi perfil va con Administradores porque comparten `AdministradoresConIngresoTest`,
  `PortadaConSesionMaquetaTest` y el caso
  `SitioControllerTest.lasPantallasDeAdministracionMuestranSusEstadosEnLaMaqueta`.
- Importaciones va con Donantes porque es su pestaña: `admin-importaciones.html` define el
  fragmento `pestanas` que usa `admin-donantes.html`. Por lo mismo, Actividad va con
  Administradores (`admin-actividad :: pestanas`).

| Lote | Tanda | Plantillas | Arquetipo y decisiones | Ficha |
|---|:-:|---|---|---|
| `adm2-donantes` | 1 | `admin-donantes`, `-importando`, `-importado`, `-importar-error` y `admin-importaciones` | Deriva de A03; DA-34 | No |
| `adm2-organizaciones` | 1 | `admin-entidades`, `-aprobada`, `-correccion` y `-rechazada` | Deriva de A03; DA-34 y DA-06 (las respuestas, ya aviso breve) | No |
| `adm2-administradores` | 1 | `admin-administradores`, `-nuevo`, `-alta`, `admin-actividad` y `admin-perfil` | Deriva de A03; `-nuevo`, `-alta` y Mi perfil, de A12; DA-35 | No |
| `adm2-novedades` | 1 | `admin-notificaciones` y `-leidas` | Sin arquetipo: el buzón, con la ficha de ORG; DA-06 | No |
| `adm2-registrar-donante` | 2 | `admin-donantes-registrar`, `-registrado` y `-registrar-error` | Deriva de A12, en el galpón; DA-06 y DA-33 | No |
| `adm2-registrar-organizacion` | 2 | `admin-entidades-registrar`, `-registrada` y `-registrar-error` | Deriva de A12, en el galpón; DA-06 | No |

Suma: 5 + 4 + 5 + 2 + 3 + 3 = 22, las 22 de §5.

**Plantillas y hojas del área que no son pantallas de ADM2:**

| Plantilla u hoja | En ADM2 |
|---|---|
| `admin-secciones.css` | Compartida con ADM1: es la hoja de todas las pantallas del admin salvo el inicio. Cada lote saca solo las reglas que usan nada más que sus pantallas (las de `[data-pantalla="donantes"]` o `#solicitudes`, por ejemplo); las de `.tabla` y las de ADM1 se quedan. No se borra |
| `admin-inicio.css` | Compartida: la importa `admin-secciones.css`. Arriba tiene `.mesa` y `.mesa-cabecera`, que usan solo plantillas de ADM2; las borra el integrador de la tanda (abajo). Los gráficos del inicio se quedan (DA-30) y la hoja no sale de `HOJAS_HEREDADAS` en esta ola |
| `admin-donaciones.html` | Fuera: es de ADM1. Su fragmento `hecho(texto)`, el aviso breve con signo, se usa sin editarlo |
| Las 18 pantallas de ADM1 (`admin-inicio`, `admin-asignaciones*`, `admin-donaciones*`, `admin-camiones*` y `admin-ranking`) | Fuera: no se editan |
| `layout-privado.html`, `cuenta-menu.html`, `Pantallas.java` y `maqueta/CuentaDeEjemplo.java` | Fuera: el armazón y el menú del admin son de ADM1 (DA-17) |
| `componentes/buzon.html` y `static/css/componentes/buzon.css` | Fuera: son del catálogo; ORG llevó al admin la fila de la campana y la casilla de 48 px (ORG-01 y ORG-02). `adm2-novedades` cambia solo la pantalla |
| El resto de `componentes/` y `static/css/componentes/` (`maestro`, `listado`, `segmentos`, `tabla`, `datos`, `migas` y `paginas`), `static/js/` (`copiar.js` incluido), `iconos.html` y `estilos.html` | Fuera: son del catálogo; se usan sin editarlos. ADM2 no suma ni cambia piezas, así que no tiene muestra que tocar |
| `docs/interfaz/componentes.md` | Solo `adm2-novedades`, y solo la frase de la fila `.buzon` que dice que los estados del admin llegan con ADM2 |
| `paginador.html` | Fuera: es común. Hoy lo usan solo plantillas de ADM2; las listas de ADM2 pasan a `componentes/paginas :: paginas`, y si queda sin uso se anota para Z |
| `envio.html`, `interaccion.html`, `cerrar-sesion.html` y `pie.html` | Fuera: son comunes a los cuatro roles; no se editan |
| `persona/datos-persona.html`, `datos-donante.html`, `viaje-datos.html`, `piezas-entidad.html` y `datos-publicos.html` | Fuera: no son pantallas, son datos de ejemplo y piezas de DON, ORG y lo público (§5); no se editan |
| `entidad-comun.css` | Fuera: la importa `admin-inicio.css` y es de ORG; no se edita |
| `maqueta/AdminPersonasMaqueta.java` | Compartida con ADM1 (el inicio): cada lote, solo los datos de sus pantallas |

**Las clases heredadas que comparten varios lotes.** `[OBSERVED]` `mesa` la usan 13 plantillas
de ADM2 y `mesa-cabecera` 12, de los seis lotes, y ninguna de fuera de la ola. Con la composición de
la regla 1 salen de todas, pero ningún lote puede borrar su línea de `clases-heredadas.txt` en su
rama, porque la plantilla de otro lote todavía las usa. Si al integrar una tanda quedan sin uso,
`InterfazFitnessTest.clasesDeLasPlantillasEstanEnElCatalogoOEnLasHeredadas` da rojo. Es un choque
entre lotes y lo arregla el integrador de la tanda (su paso 4): borra las dos líneas y las reglas
que quedan sin uso con ellas (`.mesa` y `.mesa-cabecera` en `admin-inicio.css`, y las de `.mesa` en
`admin-secciones.css`). Lo dice la regla 4.

### 16.3 Ítems

| ID | Lote | Qué | Arquetipo y decisiones | Estado |
|---|---|---|---|---|
| ADM2-01 | `adm2-donantes` | Donantes en el galpón: la cabecera con la cifra y su fecha, «Registrar donante» como única acción llena e «Importar CSV»; la franja de la importación de la sesión como aviso en la página; importar plegado y su error; la barra del listado; la bandeja con ficha, con «Ver sus donaciones» y «Registrar una donación». Cada donación reciente de la ficha lleva a `/admin-donaciones-detalle?donacion=` ([DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítem 5). `/admin-donantes?donante=juan` sale de la línea de base de §10.4 | Deriva de A03; DA-34 | Hecho (`83b7c34f8`, `b2669fedb`, arreglos `275f60a40` y `0431c7005`; revisión de pantallas `dffc1947e`): la cabecera del galpón con la cifra y su fecha, «Registrar donante» como única acción llena (con importar abierto, la llena es «Importar») e «Importar CSV»; la importación de la sesión, «terminada», «cortada» y «no pudimos actualizar» como `.aviso` en la página; el campo del archivo es el `.campo` del catálogo; `componentes/listado` y `componentes/paginas`; la bandeja con ficha (`.maestro`, DA-34), con «Ver sus donaciones», «Registrar una donación» y cada donación reciente a `/admin-donaciones-detalle?donacion=`. La de partida lleva `aria-current` y `data-elegido="partida"`, y en el celular no se abre sola (`dffc1947e`). La ficha ya no es fija desde la tableta (`0431c7005`): bajaba sola con el `sticky`. `/admin-donantes?donante=juan` pasa y sale de la línea de base de §10.4. DA-34 consta como no cumplida: la revisión de pantallas de la ola no llegó a PASS (§16.5) |
| ADM2-02 | `adm2-donantes` | El avance y el resultado de una importación (`-importando` e `-importado`), como páginas de estado, con sus avisos de etapa y las filas con error en la tabla del catálogo | Deriva de A03 | Hecho (`83b7c34f8`, arreglos `275f60a40` y `0431c7005`): el avance con las migas Donantes › Importaciones › archivo, sus avisos de etapa en el `.aviso`, «Ver todas las importaciones» y «Volver a Donantes» como botones chicos; las filas con error en la `.tabla` del catálogo con `componentes/paginas`. La región viva vacía ya no deja espacio de más (`0431c7005`) |
| ADM2-03 | `adm2-donantes` | Importaciones, la pestaña de Donantes (Donantes · Importaciones), con la barra del listado, la tabla y el paginador del catálogo | Deriva de A03 | Hecho (`83b7c34f8`, arreglos `275f60a40` y `0431c7005`; revisión de pantallas `dffc1947e`): las pestañas Donantes · Importaciones (`componentes/segmentos`), `componentes/listado`, la `.tabla` con el enlace de cada importación en el `th scope="row"` (sale la columna «Qué se puede hacer»), `componentes/paginas` y la ayuda en el `.plegable` del catálogo, igual que Actividad. «Coinciden N importaciones.» solo con filtros, y «Mostrando X a Y de N» debajo de la lista (`dffc1947e`) |
| ADM2-04 | `adm2-organizaciones` | Organizaciones en el galpón: la cabecera con «Registrar organización», la barra del listado con Estado y su cuenta, la bandeja con ficha y, en las que piden sumarse, lo que mandaron y sus tres acciones (Aprobar, Pedir corrección y Rechazar) | Deriva de A03; DA-34 | Parcial (`6e073d8fa`, `ec98c14f6`, arreglos `a2da2489d` y `36dbafa92`; revisión de pantallas `dffc1947e`): la cabecera con «Registrar organización» como única acción llena (Aprobar pasa a secundario), `componentes/listado` con Estado, la bandeja con ficha (DA-34) y, en las que piden sumarse, lo que mandaron con `componentes/sello :: signo` y las tres acciones; la de partida con `aria-current`; «Mostrando X a Y de N» en lugar del texto del orden. Falta: el pictograma de la identidad perdió `data-tamano="grande"`, que pide la pieza `.maestro` (`estilos.html`, `maestro.css`), y la ficha angosta del galpón se apila con una consulta de contenedor en `admin-secciones.css` (`dffc1947e`), no con una variante del catálogo ([DTI-53](../adr/DEUDA_TECNICA.md#dti-53), ítem 4). DA-34 consta como no cumplida (§16.5) |
| ADM2-05 | `adm2-organizaciones` | Las tres respuestas (`-aprobada`, `-correccion` y `-rechazada`): la lista con su aviso breve con signo | DA-06 | Hecho (`6e073d8fa`, arreglo `a2da2489d`): las tres respuestas son Organizaciones con `admin-donaciones :: hecho(texto)`, el aviso breve con signo, leído por `admin-entidades :: hoja(accion)` sin cambiar su firma |
| ADM2-06 | `adm2-administradores` | Administradores (Cuentas) y Actividad, con las pestañas Cuentas · Actividad, la tabla y el paginador del catálogo; el alta en su ventana | Deriva de A03 | Hecho (`94df0d9a0`, arreglos `5bd85e8e3` y `d40c3d5d6`; revisión de pantallas `dffc1947e`): las pestañas Cuentas · Actividad debajo de la cabecera, como A03; la `.tabla` y `componentes/paginas`; «Dar de alta un administrador» lleno y el alta en su ventana; en Actividad, «Actualizar» en la cabecera, «Leída el …» en la bajada, los avisos de la consulta como `.aviso` de advertencia y la ayuda en el `.plegable` del catálogo, igual que Importaciones (`dffc1947e`). Los diálogos no tienen captura: los cubren los tests HTMX de `AdministradoresConIngresoTest` |
| ADM2-07 | `adm2-administradores` | El alta sin JavaScript (`-nuevo`) y la contraseña provisoria (`-alta`) como páginas propias, con el patrón de A12; el lote suma las dos rutas a `paginas.txt` | Deriva de A12; DA-35 | Hecho (`94df0d9a0`, arreglos `5bd85e8e3` y `d40c3d5d6`; revisión de pantallas `f1454822d`): `-nuevo` en la columna de A12, con el resumen del error arriba y «Volver sin dar de alta» como salida; `-alta`, página propia (DA-35) con las migas a Administradores (`f1454822d`), el correo en `.datos` y «Volver a Administradores» como única acción llena. Las dos rutas están en `paginas.txt`. DA-35 consta como no cumplida (§16.5) |
| ADM2-08 | `adm2-administradores` | Mi perfil del admin, en una columna, con sus dos errores en el aviso en la página | Deriva de A12 | Hecho (`94df0d9a0`, arreglo `5bd85e8e3`): Mi perfil en una columna, el correo como texto fijo, «Todavía no cargaste tu nombre» como `.aviso` informativo, los dos errores en el `.aviso` de la página con el signo del problema y el h2 «Tu cuenta» para sus tres enlaces |
| ADM2-09 | `adm2-novedades` | Las Novedades del admin en el galpón: la cabecera, los estados de lista del catálogo (vacío, cargando y error) y el aviso de marcar como aviso breve con signo | Sin arquetipo: la ficha del buzón de ORG; DA-06 | Hecho (`cfcb65a52` y `ba4ddb2f0`): la cabecera del galpón con la cifra y la bajada de la bandeja compartida; vacío, cargando (región `aria-busy` con `.cargando` y el esqueleto de tres renglones) y error (`.aviso` con «Volver a cargar», como el resto del galpón; la ficha R11-14 decía «Volver a intentar»); marcar responde con `admin-donaciones :: hecho(texto)` y los tres textos de antes |
| ADM2-10 | `adm2-registrar-donante` | Registrar donante (persona u organización) y su error, en el galpón | Deriva de A12 | Hecho (`e1a960a63` y `f21c0d6a6`): las migas a Donantes, la columna de `--medida-formulario` (`[data-pantalla="registrar-donante"]`), la bajada nueva, «Quién dona» en su tarjeta con «Cambiar» secundario, el resumen del error arriba con un enlace a cada campo, los `.campo` del catálogo, «Registrar donante» como única acción llena y «Volver sin registrar». La dirección (opcional) va abierta bajo un h2, no plegada |
| ADM2-11 | `adm2-registrar-donante` | «Registrado» como aviso breve en Donantes, también con `?escenario=ya-era`; en la maqueta su ruta sigue | DA-06 y DA-33 | Hecho (`e1a960a63`): en la maqueta, `/admin-donantes-registrado` (404 fuera de ella) muestra Donantes con el aviso breve, también con `?escenario=ya-era`; sale «Registrar su donación». Registrar la donación de un donante nuevo pide buscarlo en la lista ([DTI-53](../adr/DEUDA_TECNICA.md#dti-53), ítem 1). DA-33 consta como no cumplida (§16.5) |
| ADM2-12 | `adm2-registrar-organizacion` | Registrar organización y su error, en el galpón | Deriva de A12 | Hecho (`668bc5231`): las migas a Organizaciones, la columna de `--medida-formulario` (`[data-pantalla="registrar-organizacion"]`), los rótulos más llanos («Responsable», «Nombre y apellido», «Correo»), el resumen del error con el enlace a `#calle`, una sola acción llena y «Volver sin registrar» |
| ADM2-13 | `adm2-registrar-organizacion` | «Registrada» como aviso breve en Organizaciones; en la maqueta su ruta sigue | DA-06 | Hecho (`668bc5231`): en la maqueta, `/admin-entidades-registrada` (404 fuera de ella) muestra Organizaciones con el aviso breve «Registraste a Escuela Ejemplo N.º 5. Queda activa…», sin el banner. DA-06 consta como no cumplida en esta parte (§16.5) |

Cada lote termina, además, la migración de las hojas heredadas de sus pantallas (plan §5), con el
límite de las hojas compartidas de §16.2.

### 16.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
  `adm2-donantes` lleva además `/admin-donantes?donante=juan`, la única ruta de ADM2 que queda en
  la línea de base de §10.4 (por un enlace de 22 px de la ficha, [DTI-46](../adr/DEUDA_TECNICA.md#dti-46),
  ítem 4): tiene que pasar, y si no pasa es bloqueante del lote. `adm2-administradores` suma
  `/admin-administradores-nuevo` y `/admin-administradores-alta` a `paginas.txt` y las lleva en
  sus `rutasVisuales`.
- **Antes de comitear, `adm2-donantes`:** `ArmazonFijoTest` y `MenusQueSeCierranTest` con
  `-Pvisual`, porque `ArmazonFijoTest` mide `/admin-donantes` (la cabecera, la barra del listado
  fija y el foco con Tab) y el gate visual del workflow corre solo `VerificacionVisualTest`. Se
  compara con la línea de base de §14.5; un fallo nuevo es bloqueante del lote. Esos tests no
  entran en su revisión ni en la hoja de contacto.
- **Mientras dura la tanda 1:** desde la lista nueva, «Registrar donante» y «Registrar
  organización» llevan al formulario de hoy, que rehacen los lotes de la tanda 2. No es hallazgo
  de `adm2-donantes` ni de `adm2-organizaciones`.
- **En la tanda 2:** `adm2-registrar-donante` y `adm2-registrar-organizacion` parten de lo que
  integró la tanda 1: ven la lista nueva y rehacen el formulario de hoy.
- **Al cierre:** `-Pvisual` sobre las 49 rutas de ADM2 de `paginas.txt`: las que empiezan con
  `/admin-donantes`, `-importaciones`, `-entidades`, `-administradores`, `-actividad`,
  `-notificaciones` y `-perfil`. `[VERIFIED]` Hoy el mismo filtro da 47 líneas: faltan las dos que
  suma `adm2-administradores`. De ellas, solo `/admin-donantes?donante=juan` está en la línea de
  base de §10.4, y tiene que pasar; cualquier otra falla es bloqueante, con el lote que la rompe.
  Corren también los otros cinco tests visuales, y se comparan con la línea de base de §10.5,
  §11.3, §12.5, §13.5, §14.5 y del cierre de PUB2 (§15). Un fallo que no esté en esa línea de base
  es bloqueante. No se levantan el stack ni el recorrido con login: los hace Z, y ningún lote
  cambia un adaptador `rest` (plan de la corrida, punto 5). Queda para Z comprobar con datos reales
  Donantes, la importación, Organizaciones con una solicitud, el alta de un administrador y los dos
  registros.

### 16.5 Cierre de ADM2 (2026-11-25)

Rama `E7_guia_ui_adm2`, `f1454822d`. Seis lotes en dos tandas sobre `2d74fc081` (el manifiesto
integrado): en la tanda 1, `adm2-donantes`, `adm2-organizaciones`, `adm2-administradores` y
`adm2-novedades`; en la tanda 2, `adm2-registrar-donante` y `adm2-registrar-organizacion`, con la
integración de la tanda 2 (`0bdf24657`, que borra `mesa` y `mesa-cabecera` de
`clases-heredadas.txt` con sus reglas), un commit de cierre (`e5fbdc8d6`) y dos de la revisión de
pantallas de la ola (`dffc1947e` y `f1454822d`). Estado de cada ítem en §16.3: 12 hechos y ADM2-04
parcial (el pictograma grande de la ficha y la ficha angosta del galpón, que pide una variante de
`.maestro`). La revisión de pantallas de la ola corrió 2 rondas de arreglos y no llegó a PASS. Por
eso DA-33, DA-34, DA-35 y la parte de DA-06 de registrar una organización constan como no
cumplidas, aunque su comportamiento está en el código y en los tests. Lo que queda abierto está en
[DTI-53](../adr/DEUDA_TECNICA.md#dti-53); las decisiones, en la [bitácora](bitacora.md)
(2026-11-25).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `f1454822d`, después de las dos rondas
  de la revisión de pantallas: exit 0, 942 tests en los reportes de `cliente-liviano`, 0 fallos y
  0 errores (log en la carpeta de la corrida, `ADM2/gate-documentacion.log`). En `0bdf24657`,
  antes de esas rondas: exit 0, 942 tests (934 en PUB2). `InterfazFitnessTest` pasa sin editarse:
  ningún tope cambió.
- `[VERIFIED]` `-Pvisual` en `0bdf24657` sobre las 49 líneas de ADM2 de `paginas.txt` (el filtro da
  49, con las dos que suma `adm2-administradores`), a 360, 360x640, 1280 y 1920 px: exit 0, 55
  rutas capturadas (las 49 y sus escenarios), `reporte.json` y `excepciones.json` vacíos. Pasan las
  49. `/admin-donantes?donante=juan`, la única de ADM2 en la línea de base de §10.4, ahora pasa y
  sale de ella. La pasada de las 49 no se repitió después de la revisión de pantallas
  (`dffc1947e` y `f1454822d`).
- `[VERIFIED]` Los otros cinco tests visuales, en `0bdf24657`: 31 tests, 3 fallos y 3 errores,
  todos de la línea de base: `AbejasYRenglonesTest.lasAbejasCambianDeCapaSoloCuandoNoSeVen`,
  `AbejasYRenglonesTest.conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras` y el del lema
  del pie ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)), y los dos de `BajoTierraRotulosTest` (DA-22,
  [DTI-50](../adr/DEUDA_TECNICA.md#dti-50)). `MenusQueSeCierranTest.lasPastillasDeFiltroSeCierranTocandoAfueraYConEsc`,
  el inestable conocido, falló y pasó 2 de 2 al repetirlo solo. `ArmazonFijoTest` pasa sus 12 y
  `DonanteDetallesTest` sus 7. Ningún fallo nuevo. No se volvieron a correr después de la revisión
  de pantallas.
- Sin stack ni recorrido con login (DA-20): ningún lote cambia un adaptador `rest`. Queda para Z
  comprobar con datos reales Donantes y su ficha, importar, Importaciones, Organizaciones (aprobar,
  pedir corrección y rechazar), los dos registros, Administradores (alta y contraseña provisoria),
  Actividad, Mi perfil y las Novedades.
- Cercos `[VERIFIED]` (`git diff 2d74fc081..f1454822d`): sin cambios en `rest/`, `puertos/`,
  `tokens.css`, los tests de `visual/`, `InterfazFitnessTest.java`, `templates/componentes/`,
  `static/css/componentes/` ni `static/js/`. Ningún menú cambió (DA-17). `clases-heredadas.txt`
  pierde `mesa` y `mesa-cabecera` y no suma ninguna. Ninguna plantilla ni hoja borrada:
  `templates/paginador.html` queda sin uso y `admin-inicio.css` sigue en `HOJAS_HEREDADAS`
  ([DTI-53](../adr/DEUDA_TECNICA.md#dti-53), ítems 2 y 3).
- Revisión de los lotes, según los commits: `adm2-donantes`, `adm2-organizaciones` y
  `adm2-administradores`, construcción y 2 rondas de arreglos cada uno, y quedaron con bloqueantes
  tras la tercera revisión. Donantes y Administradores, por la ayuda plegada de Importaciones y
  Actividad, que se cruzó entre los dos lotes (una con `.plegable` y la otra con el botón chico);
  la revisión de pantallas la cerró con el `.plegable` en las dos (`dffc1947e`). Organizaciones, por
  la ficha angosta en el galpón entre 1100 y 1280 px y por el pictograma sin `data-tamano="grande"`;
  la revisión de pantallas apiló la ficha con una consulta de contenedor (`dffc1947e`) y el
  pictograma sigue chico (ADM2-04 parcial). `adm2-novedades`, construcción y un arreglo (el
  esqueleto de la lista); `adm2-registrar-donante`, construcción y un arreglo (la bajada y una
  aserción de la barra); `adm2-registrar-organizacion`, construcción sin arreglos. El veredicto de
  cada ronda no consta en el repo.
- Revisión de pantallas de la ola: 2 rondas de arreglos, sin PASS. Ronda 1 (`dffc1947e`): la ayuda
  de Actividad al `.plegable`, como Importaciones; la de partida de Donantes y Organizaciones con
  `aria-current` y `data-elegido="partida"`, sin abrirse sola en el celular; «Coinciden» solo con
  filtros y «Mostrando X a Y de N» debajo de la lista en Importaciones y Organizaciones; la ficha de
  Organizaciones apilada con menos de 45 rem. Ronda 2 (`f1454822d`): las migas de la contraseña
  provisoria. Lo que la última revisión dejó abierto no consta en el repo.
- Commits de la ola, en hora de autor (2026-11-25, -03:00): `2d74fc081` (manifiesto integrado),
  `cfcb65a52` (09:25), `ba4ddb2f0` (09:35), `94df0d9a0` (09:37), `6e073d8fa` (09:44), `83b7c34f8`
  (09:50), `ec98c14f6` (09:59), `b2669fedb` (10:02), `a2da2489d` y `5bd85e8e3` (10:31), `275f60a40`
  (10:38), `d40c3d5d6` (10:57), `0431c7005` (11:01), `36dbafa92` (11:04), `668bc5231` (11:31),
  `e1a960a63` (11:35), `f21c0d6a6` (11:42), `0bdf24657` (11:49, integración de la tanda 2),
  `e5fbdc8d6` (12:14, cierre), `dffc1947e` (12:42, revisión de pantallas, ronda 1) y `f1454822d`
  (13:06, ronda 2).
- ADR: ninguno. La ola no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1,
  Gate A); DA-33 a DA-35 son decisiones de pantalla. Ninguna spec de `docs/specs/active/` es de esta
  ola, así que no se mueve ninguna.
- Documentos: [`mapa-del-sitio.md`](mapa-del-sitio.md) suma los cambios de la ola;
  [`patrones-y-microcopy.md`](patrones-y-microcopy.md) suma los dos registros a la regla 26, la
  regla 29 (en el galpón, el aviso breve es solo la respuesta a un envío) y pone al día las reglas
  generales 3 y 5 (el pase `paginador.html` sin uso y la ficha de Donantes, que ya no es fija);
  [`componentes.md`](componentes.md) pone al día las filas de `.paginas`, del paginador provisorio
  y de `.plegable`. [`tono-y-comunicacion.md`](tono-y-comunicacion.md) y
  [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no cambian: el primero no nombra
  pantallas, y ningún requerimiento cambia de estado; RF-15 («en un mismo flujo», en los
  [requerimientos de E6](../requerimientos/requerimientos-e6.md)) sigue, con el paso de más de DA-33
  ([DTI-53](../adr/DEUDA_TECNICA.md#dti-53), ítem 1). Las fichas de antes de la ola que ya no
  describen el código están en [DTI-53](../adr/DEUDA_TECNICA.md#dti-53), ítem 6.

Después de la ola (2026-11-25, `3637d358c`): un arreglo de los bloqueantes de la ronda 3 (la acción
principal antes de «Cancelar» en los diálogos de Administradores, la ayuda plegada de Importaciones
y Actividad, la ficha de Organizaciones con `data-tamano="grande"` y Mi perfil con
`componentes/datos`). Los gates dieron verde (spotless, 942 tests, `-Pvisual` sobre las 23 rutas
de la ola, incluida `/admin-donantes?donante=juan`, de la línea de base, y `agent-check` PASS 271),
y la revisión independiente de `revisar-ui` dio PASS. Lo que queda sigue en
[DTI-53](../adr/DEUDA_TECNICA.md#dti-53).

## 17. Ola Z: el cierre

Z no rehace ninguna pantalla: lo visual de las olas ya está. Retira los 10 alias de §3.2 y los
tokens sin uso: los 11 de DA-14b y los dos de [DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítem 8, que
no son alias. Cambia la lista de pares de contraste, saca del cerco lo que ya no hace falta, baja
los dos topes de `InterfazFitnessTest`, conecta el dato real de cada hueco de §7 cuyo PR esté
mergeado y pone al día la documentación. Es la única ola que intenta el stack propio con login real
y la única, con C1, que pasa `-Pvisual` por todas las rutas de `paginas.txt` (DA-20). Manifiesto:
[`backlog-guia-al-codigo.z.manifiesto.json`](backlog-guia-al-codigo.z.manifiesto.json).

Se escribió el 2026-11-25 en la corrida desatendida, antes de que corriera ADM2: el reparto y DA-36
a DA-38 no los aprobó el usuario y van con la opción recomendada (§2.2). Se lanza desde la
`E7_guia_ui` que deje ADM2. Sus `condiciones` son los huecos cuyo PR esté mergeado en `ENTREGA_7`,
con `ENTREGA_7` ya mergeada en `E7_guia_ui` (regla 3).

### 17.1 Decisiones de la ola

| ID | Qué decide | Lote |
|---|---|---|
| DA-14b | Salen los 10 alias y los 11 tokens que solo nombra `InterfazFitnessTest`; cada uso de un alias pasa al token de la guía del mismo valor | `z-tokens` |
| DA-36 | El tema de Keycloak también deja los alias: su hoja y `test_auth_theme.py` pasan a los nombres de la guía | `z-tokens` |
| DA-37 | De la lista de pares salen los 16 que nombran un token retirado; entran tres con el nombre de la guía para los colores que siguen en uso (raíz sobre suelo y sobre suelo medio, acción primaria sobre una lámina); el del título pasa a `--color-accion-primaria`; ningún mínimo baja | `z-tokens` |
| DA-38 | Sale `entidad-recibidas.css`; se borran las hojas vacías; sale de `HOJAS_HEREDADAS` toda hoja que el cerco ya acepta | `z-cerco` |
| DA-10 | En lo público, quien recibió es la organización; en lo privado, la persona | `z-hg03`, solo si corre |

DA-36 a DA-38 son nuevas de esta ola; su opción descartada está en §2.2. DA-10 es de un lote
condicional: si HG-03 no corre, el workflow la informa como «no cumplida: el lote no corrió», y eso
es lo esperado. DA-09 (HG-01 no se construye) no tiene lote: la cubre la regla 3. DA-12 (PRs
sueltos) y DA-15 (corte por área) tampoco son de un lote: son del plan y de G2.

### 17.2 Reparto

Cinco tandas. Lo más probable es que corran solo tres. Los huecos son lo último en la cola de cuota
y nadie mergea sus PRs antes del tope (plan de la corrida, punto 3). Por eso los lotes
condicionales se omiten y sus tandas quedan vacías.

- `z-tokens` va con `z-hg02` en la tanda 1: toca hojas, `tokens.css`, `/estilos`, el tema y
  `PARES`, y ningún lote HG toca una hoja de estilo (regla 5).
- Los tres lotes HG van en tandas distintas:
  - HG-02 y HG-03 comparten `donante-inicio.html` y `persona/donante-inicio.html`.
  - HG-03 y HG-04 comparten `entidad-seguimiento-sellada.html` y `EntidadConfirmarRest.java`.
  - HG-02 y HG-04 no comparten plantillas, pero los dos suman un campo en `puertos/` y un dato en
    `maqueta/` sin una lista cerrada de archivos.
- `z-cerco` va en la tanda 4, después de todos los cambios de hojas. Mide los topes al final y puede
  borrar una hoja junto con su `<link>` en una plantilla que tocó un lote HG.
- `z-docs` va último, en la tanda 5, para escribir lo que dejaron los lotes anteriores.

| Lote | Tanda | Qué | Condición | Rutas |
|---|:-:|---|---|---|
| `z-tokens` | 1 | Los alias, los tokens sin uso, la lista de pares y el tema de Keycloak | — | `/estilos` |
| `z-hg02` | 1 | HG-02, la hora estimada de llegada | `HG-02` | 4 de quien dona |
| `z-hg03` | 2 | HG-03, el nombre de quien recibió | `HG-03` | 7 de quien dona, la organización y lo público |
| `z-hg04` | 3 | HG-04, el avance de una necesidad en unidades | `HG-04` | 6 de la organización, quien dona y lo público |
| `z-cerco` | 4 | `HOJAS_HEREDADAS`, las hojas vacías, `paginador.html` y los dos topes | — | Ninguna |
| `z-docs` | 5 | `backlog-migracion-componentes.md`, `docs/README.md`, `ESTADO_DOCUMENTACION.md` y `DEUDA_TECNICA.md` | — | Ninguna |

### 17.3 Ítems

| ID | Lote | Qué | Evidencia y decisiones | Estado |
|---|---|---|---|---|
| Z-01 | `z-tokens` | Los 10 alias: cada uso en las hojas pasa al token de su definición y salen sus 10 definiciones. `--color-papel` pasa a `--color-fondo-pagina`; `--color-tinta-suave`, a `--color-texto-suave`; `--color-acento` y `--color-titulo`, a `--color-accion-primaria`; `--color-acento-hondo`, a `--color-accion-primaria-activa`; `--color-acento-claro`, a `--color-salvia`; `--color-suelo-hondo` y `--color-sombra-grano`, a `--color-tinta`; `--color-sombra`, a `--color-hoja`; `--color-destacado`, a `--color-raiz`. La ficha «Color: los nombres de antes» de `/estilos` se queda, porque `EstilosBaseTest` cuenta 14 fichas, y pasa a decir que se retiraron en Z. `componentes.md` dice lo mismo | §3.2; `tokens.css:177-183, 221, 274-275`; `estilos.html:292-305`; `componentes.md:203-212`; `EstilosBaseTest.java:44`. `[VERIFIED]` 2026-11-25: los usan 34 hojas, `estilos.html` y la hoja del tema. DA-14b | Hecho (`0dba3e78f`): salen las 10 definiciones y cada uso pasa al token de la guía del mismo valor; en `privado.css` sale además la redeclaración de `--color-papel` en `[data-armazon="galpon"]`. La ficha de `/estilos` se queda y lista los 8 tokens que reemplazaron a los alias, un token por renglón; `componentes.md` dice que se retiraron en Z |
| Z-02 | `z-tokens` | Los 11 tokens que no usa ninguna hoja ni plantilla: `--color-fondo`, `--color-marca`, `-marca-alterna`, `-marca-brillante` y `-marca-texto`, los cuatro `--color-destacado-*`, `--color-texto-sobre-error` y `--movimiento-retraso-ninguno`. Salen, junto con todo valor base `--p-*` que quede sin referencia. La lista `PARES` pasa de 51 a 38 pares: salen 16 y entran 3 con el nombre de la guía (DA-37) | §3.2; `InterfazFitnessTest.java:57-112`; pares de la guía en `guia/partes/90-script.html:216-245`. El caso de `InterfazFitnessTest.java:664-668` no se toca: es un texto de prueba de la regla. DA-14b y DA-37 | Hecho (`0dba3e78f`): salen los 11 tokens y `--p-espera-0`, el único `--p-*` que quedó sin referencia. `PARES` pasa de 51 a 38 pares, con los 3 nuevos en 4,5 y ningún mínimo más bajo |
| Z-03 | `z-tokens` | Los dos de DTI-47, ítem 8, que no son alias: `--movimiento-animacion-ola` y `--movimiento-animacion-vuelo-carga` salen de `movimiento.css`. Con el primero sale `@keyframes mov-ola`, si nada más lo nombra. `mov-vuelo` se queda, porque lo usa `--movimiento-animacion-vuelo` | `movimiento.css:53, 56, 235`; [DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítem 8. El tercero, `--color-sombra-grano`, es uno de los 10 alias (Z-01) | Hecho (`0dba3e78f`): salen `--movimiento-animacion-ola`, `--movimiento-animacion-vuelo-carga` y `@keyframes mov-ola`, con sus comentarios; `mov-vuelo` se queda. El comentario de `--movimiento-animacion-vuelo` todavía describe la variante de carga retirada ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54), ítem 10) |
| Z-04 | `z-tokens` | El tema de Keycloak: `donatrack.css` usa los nombres de la guía, la copia de `tokens.css` se vuelve a sincronizar y `test_auth_theme.py` nombra los tokens de la guía, con las mismas aserciones y los mismos mínimos | `auth-service/themes/donatrack/login/resources/css/donatrack.css` (9 usos de 6 alias); `scripts/tests/test_auth_theme.py:201-203, 216-219, 236, 262-300`; `scripts/sincronizar_tema.py`. DA-36 | Hecho (`0dba3e78f`): `donatrack.css` y `test_auth_theme.py` con los nombres de la guía, las mismas aserciones y los mismos mínimos; `sincronizar_tema.py --verificar` sale con 0 y `test_auth_theme.py` pasa sus 45 tests |
| Z-05 | `z-hg02` | Con el PR de HG-02 mergeado, la hora estimada de llegada va en el inicio de quien dona (la empresa y la persona), en el detalle en camino y en Seguimiento. Reemplaza a «En camino desde las …», que era el texto de reemplazo. Salen los comentarios HG-02 que explican ese reemplazo | §7; A04 y A07; `donante-inicio.html:7, 92`, `persona/donante-inicio.html:6, 103`, `donante-seguimiento.html:9, 125`, `donante-donaciones-detalle.html:118`. A05 no la muestra hoy y no se le agrega (regla 5) | No hecho: `z-hg02` no corrió, porque ningún PR de HG-02 está mergeado en `ENTREGA_7` (condición HG-02). Sigue «En camino desde las …» ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54), ítem 1) |
| Z-06 | `z-hg03` | Con el PR de HG-03 mergeado, en lo privado la etiqueta lleva el nombre de quien recibió («Recibió Marta el 5 de octubre», `componentes/etiqueta-entrega.html:13`). En lo público sigue la organización. Confirmar recepción cambia solo su comentario: si el PR pide el nombre al confirmar, el campo nuevo del formulario no entra en Z (regla 5) y va al PR final | §7; A01, A04, A10 y A13; `EntidadConfirmarRest.java:250`; los comentarios HG-03 de `donante-inicio.html`, `persona/donante-inicio.html`, `entidad-seguimiento*.html`, `publico-portada.html:12` y `publico-donaciones.html:7`. DA-10 | No hecho: `z-hg03` no corrió, porque ningún PR de HG-03 está mergeado en `ENTREGA_7` (condición HG-03). DA-10 consta como no cumplida: el lote no corrió ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54), ítem 1) |
| Z-07 | `z-hg04` | Con el PR de HG-04 mergeado, el avance de una necesidad va en unidades (llegaron, asignadas y sin asignar). Reemplaza a la cuenta de donaciones. Va con el `.progreso` que ya tienen Necesidades, el inicio de la organización, Organizaciones (de quien dona y de lo público) y Llegó con un problema. En Recepción confirmada va solo en palabras, en el texto de «Tu necesidad», sin sumar la barra (regla 5). El detalle de una donación no entra: no muestra la necesidad (§17.5) | §7; A02, A09, A11 y A13; los comentarios HG-04 de `entidad-necesidades.html`, `entidad-inicio.html`, `donante-entidades.html`, `publico-entidades.html`, `entidad-entrega-problema.html` y `entidad-seguimiento-sellada.html:120`; `PeriodoNecesidadDTO.java:12-19`. `[VERIFIED]` 2026-11-25: `entidad-seguimiento-sellada.html` y `donante-donaciones-detalle.html` no tienen ningún `.progreso` | No hecho: `z-hg04` no corrió, porque ningún PR de HG-04 está mergeado en `ENTREGA_7` (condición HG-04). Sigue la cuenta de donaciones ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54), ítem 1) |
| Z-08 | `z-cerco` | `HOJAS_HEREDADAS`: sale `entidad-recibidas.css`, que ya no existe. Una hoja sin reglas se borra, con todo lo que la nombra. Sale también toda hoja que el cerco acepta como hoja de pantalla nueva | `InterfazFitnessTest.java:268-294` (la lista dice que solo baja) y `:390-409` (`hojasDePantallaNuevasSoloDisponen`); [DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 3; §16.2 (`admin-inicio.css`). `[VERIFIED]` 2026-11-25: la lista tiene 20 hojas y la única que no existe es `entidad-recibidas.css`. DA-38 | Hecho (`3ed0130d0`): `HOJAS_HEREDADAS` pasa de 20 a 8 hojas. Sale `entidad-recibidas.css` y salen las 11 que el cerco ya acepta. Ninguna hoja de la lista estaba sin reglas, así que no se borró ninguna. Siguen `admin-inicio.css` y 7 más, cada una con una declaración prohibida |
| Z-09 | `z-cerco` | `templates/paginador.html` se borra si, después de ADM2, ningún archivo de `cliente-liviano/src` lo nombra. Si lo nombra uno, se queda y se anota | §16.2 (fila de `paginador.html`). DA-38 | Hecho (`3ed0130d0`): `templates/paginador.html` se borró; ningún archivo de `cliente-liviano/src` lo nombraba. `clases-heredadas.txt` no cambia |
| Z-10 | `z-cerco` | Los dos topes de `InterfazFitnessTest` bajan al valor medido con todo lo de Z integrado: clases de un solo uso (hoy 159) y medidas literales (hoy 1192). Si un conteo da más que su tope, es bloqueante: el tope no sube | `InterfazFitnessTest.java:253, 561`; los mensajes de `:386` y `:608` («solo baja»); §10.2 (fila de los topes) | Hecho (`3ed0130d0`): clases de un solo uso de 159 a 37 y medidas literales de 1192 a 532, el valor medido. `[VERIFIED]` al cierre: con 36 y 531 el test falla en los dos |
| Z-11 | `z-docs` | `backlog-migracion-componentes.md`: la fase 3 (B3-02 a B3-06) y el registro de avance dicen qué hojas migraron las olas de la guía, cuáles salieron de `HOJAS_HEREDADAS` y cuáles se borraron, con la ola y el commit | `backlog-migracion-componentes.md:184-252` | Hecho (`2e13dba80`, correcciones `6d0b77d78`): B3-02 y B3-04 constan como «Fuera de HOJAS_HEREDADAS», no como hechos; B3-03 y B3-05 siguen pendientes y B3-06 queda parcial |
| Z-12 | `z-docs` | `docs/README.md`: el árbol de `interfaz/` suma `guia/` y este backlog con sus manifiestos; la línea de la guía suma la deuda de Z | `docs/README.md:127-140, 203` | Hecho (`2e13dba80`): el árbol de `interfaz/` suma `guia/`, este backlog y sus manifiestos; la línea de la guía nombra DTI-54 |
| Z-13 | `z-docs` | `docs/ESTADO_DOCUMENTACION.md`: el mapa de la sección 2 suma `docs/interfaz/guia/` y este backlog; la fila de la guía dice qué hizo Z | `ESTADO_DOCUMENTACION.md:21-153, 428` | Hecho (`2e13dba80`, correcciones `6d0b77d78`): el mapa de la sección 2 suma un nodo `interfaz/` y la fila de la guía dice qué hizo Z. El cierre de la ola suma el stack y la pasada completa |
| Z-14 | `z-docs` | `docs/adr/DEUDA_TECNICA.md`: lo que Z salda va como actualización de su ítem (los alias y los tokens de DTI-46 y DTI-47, DTI-47 ítem 8 y DTI-48 ítem 3). Una DTI nueva junta lo que Z deja: los huecos sin conectar, con su PR, y lo de §17.5 | `DEUDA_TECNICA.md:819-1000` | Hecho (`2e13dba80`, correcciones `6d0b77d78`): las actualizaciones de DTI-46, DTI-47 ítem 8 y DTI-48 ítem 3, más las de DTI-34, DTI-43, DTI-49 a DTI-53, y la DTI nueva, [DTI-54](../adr/DEUDA_TECNICA.md#dti-54). El cierre de la ola le suma los ítems 10 a 13 |

### 17.4 Qué se verifica (DA-20)

- **En cada lote:** los gates y `-Pvisual` sobre sus `rutasVisuales`, una por plantilla.
  - `z-tokens` corre además `python scripts/sincronizar_tema.py --verificar` y
    `python -m unittest discover -s scripts/tests -p test_auth_theme.py`, y pega las dos salidas.
    El gate del workflow no corre los tests de Python.
  - `z-cerco` y `z-docs` no tienen rutas: no cambian nada que se vea.
  - `z-docs` corre `python scripts/validate_docs_links.py`.
- **Ningún color cambia:** Z cambia nombres y no valores. La pasada completa del cierre aplica el
  piso (contraste, desborde y toque) sobre todas las rutas.
- **Al cierre (regla 1):**
  - El stack propio, el seed y el recorrido con login real. Si Docker no levanta, va
    `[DEFERRED_NO_DOCKER]` y el cierre sigue.
  - `IngresoConKeycloakRealTest`, contra ese stack.
  - `-Pvisual` sobre todas las rutas de `paginas.txt`. `[VERIFIED]` El 2026-11-25 eran 345 líneas.
  - Los otros cinco tests visuales.
  - Todo se compara con la línea de base de §10.4 y §10.5, menos lo que sacaron los cierres de
    §11.3 a §16.
  - Una falla nueva en una pantalla que Z no tocó va a los pendientes de su ola y no se arregla en Z
    (regla 5). Una falla que rompió un lote de Z es bloqueante de ese lote.
- **Lo que cambia en la línea de base:** nada, fuera de lo que las olas ya sacaron. Z no suma
  ninguna excepción a la verificación visual (regla 2).

### 17.5 Qué queda fuera de Z

Son los puntos de las DTI de la guía que dicen «en Z», pero piden algo que Z no hace. Van a la lista
de pendientes del PR final y a la DTI nueva de Z-14.

| Qué | Dónde consta | Por qué no entra en Z |
|---|---|---|
| `DonanteInicioPuerto.tablero()`, `DonanteTableroVista` y sus implementaciones, sin uso | [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 6 | Retirarlos borra su test (`DonanteInicioRestTest.java:252`, `elTableroPideUnaPaginaDeTresPorColumnaYLasQueEsperanDestinoComoCifra`), y en Z ningún test se borra (regla 2). Lo decide el usuario |
| «Entregas para mirar hoy» y los otros datos que el puerto del admin no da | [DTI-51](../adr/DEUDA_TECNICA.md#dti-51), ítems 1, 6 y 7 | Piden cambiar un puerto o un adaptador fuera de la guía. Lo decide el usuario |
| La barra de arriba en el celular, el lema del pie y los tests de las abejas y del corte «bajo tierra» | [DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítems 4 y 5; [DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 3 (DA-22) | Piden editar un test de `visual/`, y eso lo decide el usuario |
| El resumen doble con JavaScript de los dos registros y la firma de `comoSigue` | [DTI-52](../adr/DEUDA_TECNICA.md#dti-52), ítems 1 y 9 | Son arreglos de pantallas de PUB2, y Z no rehace pantallas (regla 5). Van al PR final |
| El aviso «Todavía espera destino» en Seguimiento | [DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 2 y 4 | Es una decisión del usuario, pendiente desde DON |
| `raices-pie.svg`, `grano-sombra.svg` y las reglas de `.vacio` pisadas en `aviso.css` | [DTI-47](../adr/DEUDA_TECNICA.md#dti-47), ítem 8 | No son tokens, y la regla de Z retira tokens. Además, `aviso.css` es del catálogo |
| La migración de las hojas heredadas que todavía tienen reglas (B3-02 a B3-06) | `backlog-migracion-componentes.md:246-250` | Es reescribir hojas de pantalla. Z solo saca de la lista las que el cerco ya acepta (DA-38) |
| HG-01, la fecha de la nueva salida | §7; DA-09 | No se construye: queda el texto sin fecha |
| Con HG-04, la barra de avance de la necesidad en la donación asignada y en Recepción confirmada, y la necesidad que cubre en el detalle de una donación | La guía dibuja la barra en A06 y A13 (`guia/partes/50-pantallas.html:455` y `:981`); `donante-donaciones-detalle.html:13` y `entidad-seguimiento-sellada.html:120-127` no la tienen | Es una pieza que esas pantallas no tienen, y Z no suma piezas (regla 5). `z-hg04` conecta el dato solo en palabras en Recepción confirmada. Va al PR final |

### 17.6 Cierre de Z (2026-11-25)

Rama `E7_guia_ui_z`, `6d0b77d78`. Corrieron tres lotes sobre `84012dffa` (el manifiesto integrado):
`z-tokens` (`0dba3e78f`), `z-cerco` (`3ed0130d0`) y `z-docs` (`2e13dba80` y su corrección
`6d0b77d78`). Los tres lotes de los huecos (`z-hg02`, `z-hg03` y `z-hg04`) no corrieron: ningún PR
de HG-02, HG-03 ni HG-04 está mergeado en `ENTREGA_7`, así que sus `condiciones` fueron vacías, como
preveía §17.2. Estado de cada ítem en §17.3: 11 hechos (Z-01 a Z-04 y Z-08 a Z-14) y 3 no hechos
porque su lote no corrió (Z-05 a Z-07). El cierre no sumó commits de arreglo. Lo que queda abierto
está en [DTI-54](../adr/DEUDA_TECNICA.md#dti-54); las decisiones, en la [bitácora](bitacora.md)
(2026-11-25).

- `[VERIFIED]` `mvn clean test -pl cliente-liviano -am` en `6d0b77d78`: exit 0, 942 tests, 0 fallos.
  `InterfazFitnessTest` pasa sus 19 con los topes en 37 y 532; eso deja como verificado lo que la
  revisión de `z-cerco` había replicado en Python sin correr Maven. `-Pvisual` sobre `/estilos`:
  exit 0. `python scripts/sincronizar_tema.py --verificar`: exit 0. `test_auth_theme.py`: 45 tests,
  OK. `node scripts/agent-check.js`: PASS 272. `python scripts/validate_docs_links.py`: exit 1 con
  75 enlaces rotos, los mismos 75 de la línea de base de [DTI-46](../adr/DEUDA_TECNICA.md#dti-46),
  ítem 8; ninguno nuevo.
- `[VERIFIED]` Línea de base de Maven, corrida después de la ola: `z-tokens` no la corrió antes de
  editar. `mvn clean test -pl cliente-liviano -am` sobre una copia de `84012dffa` (el punto de partida
  de los tres lotes): exit 0, 942 tests, 0 fallos (BASELINE_GREEN). Los mismos 942 pasan en el
  arreglo después de la ola, así que Z no rompió ni sacó ningún test.
- `[VERIFIED]` La pasada completa de DA-20, punto 1: `-Pvisual` sobre todo `paginas.txt`, con
  `target/capturas` borrado antes. El archivo tiene 347 líneas y 335 rutas sin contar comentarios ni
  líneas en blanco. Pasan 333. Fallan 2, solo a 360 px y por el área de toque de
  `button.mapa-marcador`: `/publico-mapa?escenario=muchas` y
  `/publico-mapa?escenario=muchas&pagina=2&abierta=ejemplo-12`. Son las 2 que quedaban de las 11 de
  la línea de base de §10.4 (los cierres de §11 a §16 sacaron las otras 9) y son del catálogo
  (`componentes/mapa.css` y `static/js/mapa.js`), que Z no toca. Contraste y desborde: 0 fallas.
  `excepciones.json` vacío: Z no sumó ninguna excepción. Ningún color cambió.
- `[VERIFIED]` Los otros cinco tests visuales: 31 tests, 3 fallos y 3 errores, los mismos de los
  cierres de PUB2 y ADM2, todos de la línea de base de §10.5: los tres de `AbejasYRenglonesTest`
  (la capa de las abejas, las abejas con movimiento reducido y el lema del pie, DTI-47 y DA-22) y los
  dos de `BajoTierraRotulosTest` (DA-22, [DTI-50](../adr/DEUDA_TECNICA.md#dti-50)).
  `MenusQueSeCierranTest.lasPastillasDeFiltroSeCierranTocandoAfueraYConEsc`, el inestable conocido,
  falló y pasó 7 de 7 al repetirlo solo. `ArmazonFijoTest` pasa sus 12 y `DonanteDetallesTest` sus 7.
  Ninguna falla nueva, y nadie editó un test de `visual/`.
- `[DEFERRED_NO_DOCKER]` El stack propio, el seed, el recorrido con login y
  `IngresoConKeycloakRealTest`. El stack del proyecto `donatrack-guia` llegó a levantar sano después
  de varios intentos (un `initdb` de Postgres lento en frío y el `eacces` de RabbitMQ, DIT-11), con el
  cliente en el 8091 por un override fuera del repo, porque un proceso ajeno usa el 8090. El seeder
  quedó 20 minutos esperando y después el motor de Docker Desktop dejó de responder: `docker exec` y
  `docker info` se cuelgan y `docker ps` devuelve 500. No hubo seed, ni recorrido, ni ingreso real.
  No había stack del usuario corriendo y no se detuvo nada. Quedan los contenedores y volúmenes del
  proyecto `donatrack-guia`, y lo que las olas dejaron para Z sigue sin comprobar con datos reales
  ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54), ítem 13).
- Cercos `[VERIFIED]`: ningún tope sube (de 159 a 37 y de 1192 a 532, el valor medido: con 36 y 531
  el test falla en los dos). `HOJAS_HEREDADAS` pasa de 20 a 8, `PARES` de 51 a 38 con los mínimos de
  siempre, y `clases-heredadas.txt`, `LISTAS` y `BARRAS_EXCEPTUADAS` no crecen. Entre `3511e2567` y
  `6d0b77d78`, los únicos tests que cambian son `InterfazFitnessTest.java` y `test_auth_theme.py`, con
  las mismas aserciones. Ningún test borrado ni deshabilitado.
- Revisión de los lotes: `z-tokens`, STANDARD, PASS en la ronda 1. `z-cerco`, STANDARD, PASS en la
  ronda 1, con `[TESTS_NOT_EXECUTED_BY_EVALUATOR]`, que salda el gate de este cierre. `z-docs`, QUICK,
  `LIGHTWEIGHT_CLOSING_CHECK` con PASS después de una corrección (`6d0b77d78`). La revisión de
  pantallas de `/estilos`, a 360 y 1280 px, no encontró bloqueantes. Dejó como menor que la ficha
  «Color: los nombres de antes» ya no muestra los nombres de antes ([DTI-54](../adr/DEUDA_TECNICA.md#dti-54),
  ítem 11).
- Commits de la ola, en hora de autor (2026-11-25, -03:00): `84012dffa` (13:52, manifiesto
  integrado), `0dba3e78f` (14:11, `z-tokens`), `3ed0130d0` (14:30, `z-cerco`), `2e13dba80` (14:47,
  `z-docs`) y `6d0b77d78` (14:51, `z-docs`, correcciones).
- ADR: ninguno. Z no tuvo lotes de contrato e implementa el ADR «Huerta y galpón» (§9.1, Gate A).
  Ninguna spec de `docs/specs/active/` es de esta ola, así que no se mueve ninguna.
- Documentos: [`componentes.md`](componentes.md) (la fila del paginador provisorio) y
  [`patrones-y-microcopy.md`](patrones-y-microcopy.md) (la regla general 3) registran que
  `paginador.html` se borró. [`mapa-del-sitio.md`](mapa-del-sitio.md),
  [`tono-y-comunicacion.md`](tono-y-comunicacion.md) y
  [`requerimientos-e7.md`](../requerimientos/requerimientos-e7.md) no cambian: Z no cambió ninguna
  ruta, ningún texto de pantalla ni el estado de ningún requerimiento.
