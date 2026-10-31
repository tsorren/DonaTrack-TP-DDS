# Encargo creativo: Encastre

> Plantilla de la skill `relevamiento-creativo`; la lee la skill `corrida-creativa`. Es
> **autocontenida**: la corrida no le pregunta nada al usuario, así que lo que no está acá lo decide
> la corrida con la [dirección de arte](../direccion-de-arte.md) y queda anotado como "a criterio
> de la corrida".
>
> **Estado:** aprobado por el usuario el 2026-10-31 ("Sí, arrancá")

## 1. Qué se diseña

- **Alternativa:** `encastre` (nueva, visible como "Encastre"), con su brief en
  [`identidad/brief-encastre.md`](../identidad/brief-encastre.md). Es la tercera: Glassmorphism y
  Cozy / Medieval no se tocan.
- **Punto de partida:** desde cero, con la opción 2 de "Direcciones finales DonaTrack" (Encastre)
  como guía principal.
- **Pantalla ancla:** `estilos/armazon` (el inicio de una persona donante): es la única pantalla
  real que tienen las alternativas.
- **Hasta dónde llega:** flujo completo (concepto, sistema y el prototipo navegable por rol).
- **Lo que NO hay que repetir:** el inicio actual de Glassmorphism (`estilos/armazon`, panel lateral
  grafito con tarjetas verdes menta) y la mesa del cartógrafo de Cozy (`estilos/armazon` con
  `?alternativa=cozy-medieval`). No sirven acá porque ya son las otras dos alternativas; además,
  Glassmorphism ya es un mosaico de tarjetas: Encastre no puede ser "Glassmorphism en pastel", tiene
  que cambiar la composición y la navegación (dock, foco de a una tarea), no solo los colores.

## 2. En palabras del usuario

> Usá la skill relevamiento-creativo con las imágenes adjuntas como referencia dentro de los links
> y preguntame lo que necesites. Cuando apruebe el encargo, seguí con corrida-creativa sin pedirme
> nada más.
>
> Inspiración de Opción 2: Encastre

Respuestas del relevamiento (2026-10-31): tercera alternativa; amable, clara y concreta; para todos
por igual; se lleva "una cosa a la vez", "app de bolsillo con dock" y "un dato, un bloque de
color"; no quiere plantilla genérica de dashboard, todo pastel sin que nada se destaque ni
ilustraciones de banco de imágenes; guía principal Encastre; libres la composición, la navegación,
la voz y el movimiento; voz cercana y concreta; juego bien presente; empieza por el inicio del
donante; tres vueltas; PR en borrador.

## 3. Mensaje, público y emoción

- **Qué tiene que transmitir:** cada quien sabe qué le toca ahora, y cada donación llega y se nota.
- **Para quién:** todos por igual: público (celular, llega por un enlace), donante (celular y
  escritorio, ratos libres), entidad (celular en la entrega, con una mano) y administración
  (escritorio, muchas horas).
- **Qué tiene que sentir quien la ve:** amable, clara y concreta.
- **Intensidad:** que sorprenda (a criterio de la corrida: el usuario eligió libertad en
  composición, navegación, voz y movimiento, y juego bien presente).

## 4. Referencias

Carpeta: [`docs/interfaz/identidad/referencias-encastre/`](../identidad/referencias-encastre/README.md),
con su README. Son de terceros: se toma el estilo, no se copia nada, y no entran al sitio ni al
entregable.

| Referencia | Qué se toma | Qué no |
|---|---|---|
| `01-encastre-portada` | Mosaico de bloques que encastran, titular con palabra resaltada, cinco paradas, roles como bloques | El mosaico como plantilla |
| `02-encastre-tablero` | Tarjeta de foco (Resolver / Después), cola al costado, avance del turno, franja de circulación que filtra, dock de cápsulas | Grilla de tarjetas iguales |
| `03-tablero-de-bloques-pastel` | Un dato, un bloque; el color como orden | El número gigante con etiqueta chica en todas |
| `04-tablero-lila-con-ilustraciones` | Fondo de un color que envuelve; avance en tres colores | Ilustraciones de banco |
| `05-app-de-cursos-naranja-y-lila` | App de bolsillo, avance en barras, barra de abajo oscura con el activo en círculo | Brillo y degradado |
| `06-celulares-oscuros-con-tarjetas` | Tarjetas apiladas, filtros en píldora, dock con botón destacado | Modo oscuro como base |
| `07-paletas-calidas` | Pastel apagado y cálido, no infantil | — |
| `08-envios-con-ruta-roja` | Un solo color fuerte para lo urgente; el recorrido como línea | El rojo como principal |
| `09-analitica-rosa-y-naranja` | La cifra principal dentro de un bloque saturado | La densidad de analítica |
| `10-tablero-oscuro-de-mapa` | Flota y paradas como datos de primera | Negro con verde flúor |
| `11-recorrido-de-camiones-en-mapa` | Camiones al costado y la ruta al lado | El gris de oficina |

**La guía principal:** `01-encastre-portada` y `02-encastre-tablero`. El sistema completo de la
referencia (tokens, componentes, estados y voz) está resumido en el
[brief](../identidad/brief-encastre.md#restricciones); las piezas que nombra son un punto de
partida, no una lista cerrada: bloque, cifra, cápsula segmentada, etiqueta de estado (color + punto
+ texto), parada de recorrido, tarjeta de foco, dock de cápsulas, hoja (abajo en el celular, al
centro en escritorio) y aviso "Actualizando…".

## 5. Lo que no queremos

Además de "Lo que evitamos" de la dirección de arte (elegido por el usuario):

- Que parezca una plantilla genérica de dashboard (tarjetas iguales, número gigante y etiqueta
  chica).
- Todo pastel y nada se destaca: lo urgente y la acción de ahora tienen que saltar a la vista.
- Ilustraciones de banco de imágenes.

## 6. Libertades y límites

| Qué | Libre | Con límite (cuál) | Fijo |
|---|:-:|---|:-:|
| Composición de cada pantalla | ✓ | | |
| Navegación | ✓ | | |
| Materiales, dibujos propios y movimiento | ✓ | Sin ilustraciones de banco; el movimiento respeta "reducir movimiento" | |
| Voz en títulos y rótulos | ✓ | Solo títulos y rótulos, en la tabla de voz; nunca botones ni enlaces | |
| Tipografía y paleta de la alternativa | ✓ | Contraste AA; partir de la paleta y las fuentes del brief | |
| JavaScript propio como mejora | ✓ | La página anda sin él (SSR primero) | |

Fijo siempre, aunque la tabla diga otra cosa: los datos y las acciones, el piso, el stack y las
otras dos alternativas ([Libertad creativa](../direccion-de-arte.md#libertad-creativa)).

**Fuentes:** Baloo 2 y Nunito (SIL OFL 1.1) quedan alojadas en `static/fuentes/` antes de la
corrida, con su licencia, como Alegreya y Almendra. La corrida no descarga nada.

## 7. Voz

- **Tono:** cercana y concreta, en voseo: frases cortas, cifras reales; los pendientes son tareas,
  no alarmas.
- **Cómo dice algunos títulos y rótulos** (en `cliente-liviano/src/test/resources/voz/encastre.txt`;
  a criterio de la corrida, con esta dirección):

  | Glassmorphism dice | Esta alternativa dice |
  |---|---|
  | Tu misión actual | Lo que te toca ahora |
  | Recorrido de tu última donación | Por dónde va tu donación |
  | Notificaciones recientes | Lo último que pasó |

  El usuario vio de muestra "Hoy te toca atender 5 cosas." y "Todo al día. Buen turno.". Los textos
  de botones y enlaces no cambian (son acciones, fijas por las gemelas).

- **Palabras que no van:** "dashboard", "KPI", "usuario", "error crítico", "¡Ups!".

## 8. Juego

- **Cuánto:** bien presente.
- **Con qué piezas:** misiones con avance, insignias, rango (Colaborador, Sostenedor,
  Transformador) y el turno del depósito como avance de a una tarea, con celebraciones en los logros.
  Las pantallas de trabajo y los errores siguen sobrios.

## 9. La corrida

| Qué | Valor |
|---|---|
| Iteraciones | 3 (concepto, sistema y flujo) |
| Conceptos en la iteración 1 | 3 |
| Rama | `E6_encastre` |
| Al terminar | PR en borrador hacia `ENTREGA_6` |
| Presupuesto | Unos 40 agentes (1 de preparación, unos 11 por iteración y de 4 a 7 entre el cierre, la auditoría y el PR), entre 8 y 10 millones de tokens de los subagentes y de 4 a 6 horas, medido contra la corrida de prueba (15 agentes, 2 millones y 74 minutos para 1 iteración con 2 conceptos). Si se acaba el uso, se retoma desde la última nota de traspaso |
| Modelo de la sesión | Opus (los roles de la corrida declaran el suyo en la skill) |
| Antes de la corrida | La sesión aloja las fuentes y abre el prototipo navegable (`/prototipo/<pantalla>`) a cualquier alternativa: hoy solo sirve a Cozy / Medieval y sin eso la iteración 3 se bloquea |
| Después de la corrida | El video de presentación (sección 10), antes del PR |

## 10. Video de presentación

Pedido por el usuario al revisar el encargo, en sus palabras:

> Al alcanzar el objetivo se debe generar un vídeo MP4 de hasta 4 minutos recorriendo el sitio,
> como presentación para el equipo de desarrollo.

- **Qué recorre:** Encastre por rol, con clics reales en el prototipo navegable: público → donante →
  entidad → administración, y un tramo en celular.
- **Qué lo acompaña:** rótulos en pantalla, uno por tramo (quién es, qué hace, qué decisión de diseño
  se ve). Sin sonido.
- **Duración:** hasta 4 minutos.
- **Dónde queda:** fuera del repo. La sesión que lanzó la corrida se lo manda al usuario; el PR dice
  cómo regenerarlo. Lo sube al PR el usuario, si quiere.
- **Cómo se graba:** a criterio de la sesión (Playwright graba el navegador y ffmpeg arma el MP4;
  los dos ya están en la máquina).

## 11. Aprobación

- **Fecha:** 2026-10-31.
- **Lo que dijo el usuario:** "Sí, arrancá", después de sumar el video de presentación (sección 10).
