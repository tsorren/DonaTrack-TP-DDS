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
- Orden de las olas: C0 (piloto, tokens), C1 (piezas, armazones y menús), ORG, DON, PUB1, ADM1,
  PUB2, ADM2 y Z (cierre). Si hay que cortar, la prioridad es ORG, DON, PUB1, ADM1, PUB2, ADM2.

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

| ID | Pregunta | Recomendación | Respuesta del usuario | Fecha | Ola |
|---|---|---|---|---|---|
| DA-01 | Público: ¿el Mapa sale del menú y queda dentro de Entregas? | Sí, como dibuja la guía | Sí («3 si», ronda 2) | 2026-11-22 | C1 |
| DA-02 | Organización: ¿«Donaciones recibidas» y «Seguir entregas» se funden en «Entregas»? | Sí | Sí («3 si», ronda 2) | 2026-11-22 | C1 |
| DA-03a | Quien dona: ¿«Seguimiento» deja de ser una sección del menú? (el seguimiento de cada donación se abre desde su detalle) | Sí | Sí («3 si», ronda 2) | 2026-11-22 | C1 |
| DA-03b | Quien dona, la persona: ¿dónde va Misiones? | Como cuarta entrada del menú de la persona | Pendiente | | C1 |
| DA-03c | Quien dona, la persona: ¿cómo es su inicio, que la guía no dibuja? | Pendiente de proponer | Pendiente | | C1 |
| DA-04 | Admin: ¿«Por decidir» reemplaza a Inicio y a Asignar? | Sí | Pendiente | | C1 |
| DA-05 | «Cómo funciona», «Cómo donar» y «Ayuda»: ¿páginas nuevas o las de hoy con otro nombre? | Las de hoy (Nosotros, Para donantes, Para organizaciones) | Pendiente | | C1 |
| DA-06 | ¿Página propia de respuesta solo al confirmar una recepción, y aviso breve en el resto? | Sí, es la regla de la guía | Pendiente | | Roles |
| DA-07 | Sello para «vencida», «cancelada» y «en revisión» | El par neutro, con su texto | Pendiente | | C1 |
| DA-08 | Pantallas sin arquetipo: ¿ficha aprobada por el usuario o derivación del agente? | Ficha para Misiones, Ranking, inicio de la persona y buzón; el resto, derivación | Pendiente | | Roles |
| DA-09 | HG-01, la nueva salida: ¿función nueva o texto sin fecha? | Texto sin fecha; la función, después de la defensa | Pendiente | | Z |
| DA-10 | HG-03: ¿el nombre de quien recibió se muestra en lo público? | En lo privado sí; en lo público, la organización | Pendiente | | Z |
| DA-11 | ¿Se acepta Opus high en los lotes con pantallas, como fija el workflow? | Sí | Sí («todas las recomendadas», ronda 1) | 2026-11-22 | Todas |
| DA-12 | ¿Los huecos de backend van como PRs sueltos a `ENTREGA_7`? | Sí | Pendiente | | Z |
| DA-13 | Las 6 plantillas de `persona/` que solo sirve la maqueta: ¿se migran con su gemela o se borran? | Se migran; borrarlas es otra decisión | Pendiente | | DON |
| DA-14a | En C0: ¿los 10 nombres de color de hoy pasan a ser alias del token de la guía del mismo valor (sin cambiar ningún píxel) y se borran los 3 tokens que nadie usa? | Sí | Sí («todas las recomendadas», ronda 1). Corregida el 2026-11-22 con la opción B («Opción B»): se borran 3 y no 4, porque `--medida-quiebre-medio` sí se usa (ver §3.2) | 2026-11-22 | C0 |
| DA-14b | En Z: ¿se borran los alias que quedaron sin usos y los 11 tokens que no usa ninguna hoja ni plantilla y solo nombra `InterfazFitnessTest`, cambiando su lista de pares por la de la guía? | Sí | Pendiente | | Z |
| DA-15 | Si el 30/11 no llegó todo: ¿corte por área con la regla G2, o todo o nada? | Corte por área | Corte por área («todas las recomendadas», ronda 1) | 2026-11-22 | Z |

## 3. Ola C0: los tokens (piloto)

C0 es la primera corrida real del workflow `oleada-ui` y sirve de piloto: además de su trabajo,
mide cuánto tarda y cuánto gasta una ola (§3.4).

### 3.1 Ítems

| Ítem | Lote | Qué | Evidencia | Estado |
|---|---|---|---|---|
| C0-01 | `c0-adr` | ADR `proposed` «Huerta y galpón»: la guía de `docs/interfaz/guia/` como sistema visual y un esqueleto por rol. Dice su relación con el ADR de la piel Huerta, sin promover nada. `direccion-de-arte.md` deja de nombrar a Pico como base y apunta a la guía | Cambiar la identidad pide un ADR: `docs/interfaz/README.md:102`. ADR previo: `docs/adr/cliente-liviano/20261110-piel-huerta-con-abejas-como-identidad-del-cliente-liviano.md`. Pico: `docs/interfaz/direccion-de-arte.md:137-138` frente a `cliente-liviano/AGENTS.md:25-26` | Pendiente |
| C0-02 | `c0-tokens` | `tokens.css` suma los 46 tokens nuevos de la guía y cambia el valor de los 6 que cambian (§3.2) | Guía: `docs/interfaz/guia/partes/05-tokens.css`. Tokens de uso de hoy: `cliente-liviano/src/main/resources/static/css/tokens.css:169-314` | Pendiente |
| C0-03 | `c0-tokens` | Los 10 nombres de color en uso que la guía no trae pasan a ser alias del token de la guía del mismo valor; se retiran los 3 tokens sin ninguna referencia y se conserva `--medida-quiebre-medio` (§3.2) | Tablas de §3.2, `[VERIFIED]` | Pendiente |
| C0-04 | `c0-tokens` | Los pares de contraste de los tokens nuevos entran en la lista del cerco; ningún par se quita | Lista: `cliente-liviano/src/test/java/grupo5/cliente/InterfazFitnessTest.java:57-90`; prueba: `:204`. Pares de la guía: `docs/interfaz/guia/partes/90-script.html:216-248` | Pendiente |
| C0-05 | `c0-tokens` | `/estilos` gana una sección «Tokens» (color, letra, espaciado, radio, borde, elevación y movimiento), antes de «Componentes». `/estilos` no tiene hoja propia y el cerco prohíbe `style` sueltos: las muestras de token van en `componentes/muestra.css`. `componentes.md` suma una sección de tokens, nueva | Hoy tiene Composición, Utilidades y Movimiento, sin sección de tokens: `cliente-liviano/src/main/resources/templates/estilos.html:21-147`. Encabezados de `docs/interfaz/componentes.md`: no hay ninguno de tokens | Pendiente |
| C0-06 | `c0-tokens` | La copia de `tokens.css` del tema de Keycloak se vuelve a copiar con el script, y su prueba de contraste pasa | El tema lleva una copia idéntica: `auth-service/README.md:126`. Script: `scripts/sincronizar_tema.py`. Prueba: `scripts/tests/test_auth_theme.py`, en el CI: `.github/workflows/agent-governance.yml:67`. Hoy las dos copias son iguales `[VERIFIED]` | Pendiente |

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
No entran en la revisión de pantallas ni en la hoja de contacto de la ola.

### 3.4 Qué mide el piloto

| Medida | Para qué |
|---|---|
| Duración de punta a punta y por paso | Decidir si las olas siguen en serie o en dos pistas (G0) |
| Tokens por lote y por paso | Proyectar la cuota de las 12 corridas que faltan (13 en total: 9 olas de `construir` y 4 de `disenar`, porque ORG, DON, ADM1 y ADM2 tienen pantallas con ficha para aprobar antes de construir) |
| Rondas de reparación y de revisión de pantallas | Ajustar el tamaño de los lotes |
| `-Pvisual` sobre las 320 rutas con los tokens nuevos | El piso: contraste AA, desbordes y toque de 48 px |
| Las capturas de la muestra de §3.3 | Primera señal para G1 |
| Si el stack del cierre levantó | Saber si el recorrido con login real es viable en esta máquina |

La duración y los tokens los mide la sesión que lanza el workflow (hora de inicio y de fin, y el
uso del plan antes y después). Los agentes no tienen ese dato. El cierre anota la hora de cada
commit de la ola, sacada de `git log`.

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

### 5.1 Los menús (cambian una sola vez, en C1)

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
`cliente-liviano/src/main/resources/static/css/` o `templates/`. Todas entran en C1.

| ID | Pieza | Guía | Destino | Qué se hace |
|---|---|---|---|---|
| P01 | Base y aro de foco | 1-43 | `app.css` | Se retoca |
| P02 | Composición y medida del texto | 44-71, 452 | `composicion.css` | Se retoca |
| P03 | Botones, con «cargando» | 72-114 | `componentes/boton.css` | Se retoca |
| P04 | Campos, opción y casilla | 115-156 | `componentes/campo.css`, `formularios.css` | Se extiende |
| P05 | Subir fotos | 456-461 | `componentes/subida.css` | Se retoca |
| P06 | Sello (D1) y «Espera tu confirmación» | 157-178 | `componentes/sello.css`, `sellos.css` | Se extiende |
| P07 | Lámina y sombra de paja (D7) | 179-190 | `componentes/tarjeta.css` | Se retoca |
| P08 | Foto de una entrega y etiqueta (D3) | 191-223 | `componentes/etiqueta.css` y `componentes/etiqueta.html` `[PROPOSED]` | Componente nuevo; reemplaza al acta de `entidad-comun.css` |
| P09 | Raíz (D2), tres formas y «no pudo llegar» | 224-263, 490-502 | `componentes/huerta.css` y `componentes/huerta.html` | Se extiende |
| P10 | Frutos (D4) | 264-267, 483 | `componentes/huerta.html` (fragmento `cifras`) | Se extiende: un fruto por entrega confirmada |
| P11 | Trama (D5) | 268-273 | `componentes/trama.css` `[PROPOSED]` | Componente nuevo |
| P12 | Avance de una necesidad | 274-279 | `componentes/progreso.css` | Se retoca |
| P13 | Avisos: en la página, breve y campanita | 280-313, 480 | `componentes/aviso.css`, `componentes/campanita.css` | Se extiende |
| P14 | Barra superior e inferior | 317-356, 444 | `componentes/navegacion.css` | Se retoca |
| P15 | Pestañas | 357-368, 447 | `componentes/pestanas.css` | Se retoca |
| P16 | Migas de pan | 369-373 | `componentes/migas.css` `[PROPOSED]` | Componente nuevo |
| P17 | Paginador | 374-379 | `componentes/paginas.css` | Se retoca |
| P18 | Pie de la huerta | 380-384 | `pie.html` | Se retoca |
| P19 | Listas y sus estados | 385-394, 406-416 | `componentes/lista.css`, `componentes/carga.css` | Se retoca |
| P20 | Tabla que se apila | 503-520 | `componentes/tabla.css` | Se retoca |
| P21 | Tabla del galpón, con selección | 395-405 | `componentes/tabla.css` | Se extiende |
| P22 | Cajón del galpón | 417-427 | `componentes/navegacion.css` | Componente nuevo |
| P23 | Lista de datos | `15-pantallas.css:172` | `componentes/datos.css` `[PROPOSED]` | Componente nuevo |
| P24 | Dibujos: recorrido, puerta, mapa y 7 escenas | `20-simbolos.html` | `static/img/` | Nuevos |
| P25 | 17 íconos y 9 signos de estado | `20-simbolos.html` | `iconos.html` | Se extiende |
| P26 | Armazones de la huerta y del galpón | `15-pantallas.css:1-26, 98-104` | `layout-publico.html`, `layout-privado.html` | Se extiende |
| P27 | Movimiento reducido y el trazo | 428-432; `15-pantallas.css:177` | `movimiento.css` | Se extiende |

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
  dos pistas, y si el plazo alcanza.
- **G1, después de C1:** el usuario mira 16 pantallas de hoy sin rehacer (4 por área: lo público,
  quien dona, la organización y el admin) y responde, por área, «se puede mostrar así» o «no se
  puede mostrar así». La lista de las 16 va en el manifiesto de C1.
- **G2, el 30/11:** el corte del PR final a `ENTREGA_7`, con la respuesta a DA-15.
