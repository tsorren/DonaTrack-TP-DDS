# Spec: R11 p1-importador — El importador lee el CSV por partes y no pierde avisos de bienvenida

> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario; ver la sección final de aprobación). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Estado:** PROPOSED (espera la aprobación del usuario antes de construir).
> **Nivel:** STANDARD (AGENTS.md §7.0): no cambia ningún contrato público (REST ni eventos), no
> agrega migraciones ni dependencias y no cambia la comunicación entre servicios. Cambia el puerto
> interno `CargadorDonantes` de Donaciones y el manejo de un error en Notificaciones.
> **ADR:** ninguno (AGENTS.md §9.1, Gate A): leer un archivo por partes y reintentar un mensaje
> antes de mandarlo a la cola de mensajes muertos que ya existe no son decisiones nuevas de
> arquitectura.
> **Fecha:** 2026-11-12
> **Base:** `ebe332642` (cierre documental de P0 de R11).
> **Módulos:** `donaciones-service` (importador y lector del CSV), `notificaciones-service` (el
> aviso por donante importado).
> **Ítem:** R11-B08 del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md),
> lote `p1-importador`. Salda [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40) ítem 4 y la parte de
> Notificaciones del ítem 6.

---

## 1. Objetivo Funcional (Goal)

Que la importación de 20.000 donantes dé la cuenta completa (también las empresas), que lea el
archivo por partes en lugar de tenerlo entero en memoria y que ningún aviso de bienvenida se pierda.

### Por qué entra

`[DOCUMENTED]` El [ensayo de 20.000 filas](../../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12)
de P0 alcanzó en tiempo (124,7 a 142,4 s), en respuesta del sitio (p95 menor a 0,6 s) y en memoria
(Donaciones por debajo de 420 MiB). No alcanzó por dos defectos, no por velocidad:

1. **Las empresas no se importaban.** `[OBSERVED]` Las 2.980 empresas del archivo (15 %) fallaron
   con `ERR-VAL-104`: el importador armaba la persona jurídica sin representantes y el dominio la
   rechaza. `[OBSERVED]` Ya está arreglado en `867cd738e` (en la base de este lote):
   `ImportadorService.transformarAPersonaInputDTO` manda el representante de relleno que arma
   `PersonaMapper` (`ImportadorService.java:215-225`, `:247-258`), con su prueba
   (`ImportadorServiceTest`, `conEmpresaNueva_laCreaConSuRepresentanteDeRelleno`). **Falta probarlo
   contra la base real**: la prueba usa un doble de `IPersonasService`. Este lote no lo vuelve a
   arreglar; lo cuida (C5) y lo mide al repetir el ensayo (C8).
2. **Se perdió un aviso de bienvenida.** `[INFERRED]` Un `donante.registrado` llegó a
   Notificaciones antes que el `persona.sincronizada` de esa persona: `EventoMapper.buscarPersona`
   (`EventoMapper.java:208-215`) lanza `ValidationException(RECURSO_NO_ENCONTRADO)` y el mensaje se
   perdió sin pasar por la cola de mensajes muertos. `[OBSERVED]` La carrera es posible: Donaciones
   publica `persona.sincronizada` desde un hilo aparte (`NotificacionesAsyncService.sincronizarPersona`,
   `@Async`), y `donante.registrado` desde el hilo del importador (`DonantesService.publicarDonanteRegistrado`),
   los dos a la misma cola `notificaciones.donaciones`. El contenedor de Notificaciones
   (`RabbitMQConfig.rabbitListenerContainerFactory`) no tiene reintento configurado.

Además, aunque el ensayo alcanzó, `[DOCUMENTED]` el usuario pidió que toda lista tenga página o
loteo (regla general 1 del backlog, DTI-40 ítem 4): hoy `[OBSERVED]` `LectorDonantesCSV.cargarDonantes`
devuelve una `List<FilaPadron>` con las 20.000 filas y `ImportadorService` la recorre después
(`ImportadorService.java:93-103`, `:129-134`).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que entra)

1. **Lectura por partes.** El importador lee el CSV del almacenamiento de a tandas de 500 filas
   (`TAMANIO_TANDA_PROGRESO`, el mismo número del avance) y procesa cada tanda antes de leer la
   siguiente. En ningún momento hay en memoria más de una tanda de filas.
2. **El total se cuenta antes de empezar, sin guardar filas.** Primero una pasada que solo cuenta
   las líneas no vacías después del encabezado (el objeto del almacenamiento se abre dos veces), y
   después la pasada que procesa. Así `totalFilas` sigue llegando antes de la primera fila
   procesada, como hoy, y la barra de avance del cliente no cambia.
3. **El avance se guarda por tanda**, igual que hoy: las filas con error de la tanda y después los
   contadores (`guardarAvance`). No cambia el orden ni la cantidad de escrituras.
4. **El aviso de bienvenida no se pierde.** En Notificaciones, cuando `donante.registrado` llega y
   la persona todavía no está copiada, el mensaje se reintenta unas pocas veces con espera
   creciente y corta (propuesta: 4 intentos, a los 0,5, 1 y 2 s). Si la persona sigue sin llegar, el
   mensaje va a la cola de mensajes muertos que ya existe (`notificaciones.donaciones.dlq`, por la
   `x-dead-letter-exchange` de `notificaciones.dlx`), con un registro que dice el `personaId` y el
   `messageId` (sin nombre ni credenciales). El reintento vale solo para ese caso (la persona que
   falta); los demás errores siguen como hoy.
5. **Pruebas** de lo anterior (sección 5).
6. **Repetir el ensayo** de 20.000 filas con el archivo de la guía, sin convertir nada, al cerrar
   la oleada P1, y anotar los resultados en la [guía](../../testing/ensayo-importacion-20000.md).

### Out-of-Scope (lo que no entra)

- **Transacción por tanda o altas en bloque.** Cada fila sigue con su alta propia
  (`crearPersona`, `crearDonante`): una fila mala no deshace las otras 499. El ensayo mostró que el
  tiempo alcanza.
- **Buscar los duplicados de una tanda en una sola consulta.** Hoy es una búsqueda por fila; no
  hace falta para el rendimiento medido.
- **Cambiar el orden de publicación en Donaciones** (publicar `persona.sincronizada` antes que
  `donante.registrado`, en el mismo hilo). Es `PersonasService` y `NotificacionesAsyncService`,
  fuera de los archivos del lote. Va como duda.
- **Agrupar o espaciar los avisos de bienvenida.** En el ensayo las colas volvieron a 0 solas y
  Notificaciones quedó en 315,5 MiB: no se vio saturación. Cambiar a un aviso por tanda cambiaría el
  evento `donante.registrado.v1`.
- **La subida del archivo** (multipart hasta 50 MB, límites de espera de R11-04), la pantalla de la
  importación (`p1-importaciones`, R11-20) y darles usuario a los importados (RF-AUTH-14, D12).
- **Las credenciales en texto dentro del aviso** (`DonantesService.publicarDonanteRegistrado`): es
  de RF-AUTH-14.
- El teléfono vacío que agrega el constructor de `Persona` (DTI-40 ítem 6, primera parte).

## 3. Decisiones Acordadas y Trade-offs

Sin matriz 5D (lo pide el lote). Cada decisión, con la alternativa descartada:

- **Alternativa elegida: contar primero y procesar después (dos lecturas).** Mantiene `totalFilas`
  antes de la primera fila y la barra del cliente igual. Descartada: dejar `totalFilas` nulo hasta
  el final, porque el admin perdería el "x de y" durante toda la importación.
- **Tanda de 500, la misma del avance.** Una sola medida para leer, procesar y guardar. Descartado
  un tamaño propio de lectura: no mejora nada medido y suma una perilla.
- **Alta fila por fila dentro de cada tanda.** Descartada la transacción por tanda: una fila mala
  desharía 499 buenas, y el ensayo ya alcanzó en tiempo.
- **Reintento acotado en Notificaciones, solo para la persona que falta, y después la cola de
  mensajes muertos que ya existe.** Descartados: crear la persona con los datos del evento (no trae
  el correo, así que el aviso no podría salir), guardar el aviso en espera hasta que llegue la
  persona (más estado y más casos) y una cola de espera con vencimiento (cambia la topología y pide
  ADR).
- **El orden de publicación en Donaciones no se toca** en este lote (no son sus archivos); queda
  como duda para el usuario.

## 4. Invariantes (reglas que no se rompen)

- Las filas mal formadas siguen contando como error de formato con su número de línea (el
  encabezado es la línea 1) y las líneas en blanco no cuentan.
- Al terminar, `creadas + actualizadas + conError = totalFilas`.
- El BOM del encabezado se sigue quitando, y el stream se cierra al terminar o al fallar.
- Ningún registro muestra datos de la fila (nombre, documento, correo, fecha de nacimiento).
- La deduplicación por `messageId` de Notificaciones (`evento_procesado`) sigue valiendo: un
  reintento del mismo mensaje no crea dos avisos.

## 5. Criterios de Aceptación

```gherkin
Escenario: El archivo se procesa de a partes
  Dado un CSV de 1.500 filas válidas en el almacenamiento
  Y un lector que falla al pedir la tercera tanda
  Cuando corre la importación
  Entonces las primeras 1.000 filas quedan aplicadas y guardadas
  Y el archivo termina en ERROR con 1.000 procesadas

Escenario: El aviso de bienvenida espera a la persona
  Dado un donante.registrado cuya persona todavía no se copió en Notificaciones
  Cuando la persona llega antes del último reintento
  Entonces queda un solo aviso de bienvenida para esa persona

Escenario: El aviso sin persona no se pierde
  Dado un donante.registrado cuya persona nunca llega
  Cuando se agotan los reintentos
  Entonces el mensaje queda en notificaciones.donaciones.dlq

Escenario: La cuenta del ensayo
  Dado el CSV de la guía de 20.000 filas, sin convertir
  Cuando se importa en un stack propio
  Entonces termina con 19.400 creadas, 400 actualizadas y 200 con error
```

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C1 | El lector nunca entrega más de 500 filas juntas y nunca arma la lista entera. | Prueba del lector con un CSV generado en memoria de 100.000 líneas: recibe 200 tandas de 500 como máximo, con los números de línea seguidos. |
| C2 | La cuenta previa coincide con lo que se procesa. | Prueba del lector con líneas en blanco, mal formadas y BOM: el total contado es igual a la cantidad de filas entregadas. |
| C3 | Una tanda se procesa antes de leer la siguiente. | Prueba del importador con un lector que falla al pedir la tercera tanda: las dos primeras quedaron aplicadas y guardadas, el archivo termina en `ERROR` y sus contadores dicen 1.000 procesadas. |
| C4 | Las pruebas que ya existen del importador y del lector siguen pasando con las mismas aserciones; solo cambia cómo el doble entrega las filas. | `mvn clean test -pl donaciones-service -am` en verde; el diff de las pruebas no borra ni afloja aserciones. |
| C5 | Una empresa del CSV se crea con su representante de relleno, también leyendo el archivo de verdad. | La prueba `conEmpresaNueva_laCreaConSuRepresentanteDeRelleno` sigue verde, y una prueba nueva lee una línea `JURIDICA` con el lector real y llega a `crearPersona` con un representante. |
| C6 | Un `donante.registrado` que llega antes que su persona genera el aviso cuando la persona llega dentro de los reintentos. | Prueba de Notificaciones: el primer intento no encuentra la persona, el segundo sí; queda un solo aviso. |
| C7 | Si la persona nunca llega, el mensaje termina en `notificaciones.donaciones.dlq` y no se pierde; los demás eventos de la cola no se reintentan. | Prueba del contenedor (o del recuperador) con la persona ausente: rechazo sin reencolar después del último intento; otro error, por ejemplo un campo inválido, se comporta como hoy. |
| C8 | El ensayo de 20.000 filas da la cuenta con el archivo de la guía. | Corrida en un stack propio, al cerrar P1: `PROCESADO_CON_ERRORES` con `totalFilas` 20.000, `creadas` 19.400, `actualizadas` 400 y `conError` 200 (criterio 1 de la guía). |
| C9 | El ensayo sigue cumpliendo los criterios 2 a 5 de la guía y no pierde avisos. | Misma corrida: duración hasta 5 minutos, p95 del sitio menor a 1 s, ningún contenedor sin memoria y Donaciones por debajo de 700 MiB (anotando si baja respecto de P0), colas en 0 sin intervención, `notificaciones.donaciones.dlq` en 0 y un aviso de bienvenida por cada donante creado. |

Gates: Gate 1 y Gate 2 de los dos módulos (`mvn clean test -pl donaciones-service,notificaciones-service -am`);
SonarCloud pre-flight con `docs/IA/07-errores-frecuentes-sonarcloud-ia.md`. C8 y C9 necesitan
Docker: si no se puede levantar un stack propio, van como `[DEFERRED_NO_DOCKER]` y no se dan por
verificados.

## 6. Contrato antes y después

No cambia ningún contrato público. El OpenAPI de Donaciones y los eventos quedan como están.

| Qué | Antes | Después |
|---|---|---|
| `POST /api/donantes/archivos` (multipart `archivo`) | 202 con `ArchivoDonantesResponse`; procesa aparte | Igual |
| `GET /api/donantes/archivos/{id}` | `estado`, `totalFilas` (nulo hasta leer el padrón), `procesadas`, `creadas`, `actualizadas`, `conError`, `ultimos`; el avance cambia cada 500 filas | Igual. `totalFilas` sigue nulo solo mientras se cuenta |
| `GET /api/donantes/archivos/{id}/filas-con-error?page&size` | Página de filas con error, total en `X-Total-Count`, crece por tandas de 500 | Igual |
| Evento `donante.registrado.v1` | Uno por donante nuevo o reactivado | Igual (mismos campos, mismo `messageId`) |
| Evento `persona.sincronizada.v1` | Uno por alta o cambio de persona | Igual |
| Cola `notificaciones.donaciones` | Sin reintento; con cola de mensajes muertos declarada | Misma cola y misma cola de mensajes muertos; reintento acotado solo cuando falta la persona |
| Puerto interno `CargadorDonantes` | `List<FilaPadron> cargarDonantes(InputStream)`: el archivo entero | Entrega las filas de a tandas y cuenta las filas sin guardarlas. Lo usan solo `ImportadorService` y sus pruebas |
| Migraciones | — | Ninguna |

**Cambio de comportamiento que hay que aceptar.** `[INFERRED]` Hoy, si la lectura del archivo
falla a mitad, no se aplicó ninguna fila y el archivo queda en `ERROR`. Después, las tandas ya
procesadas quedan aplicadas y contadas, y el archivo queda en `ERROR` con esos números. Volver a
importar el mismo archivo es seguro: las filas ya importadas entran como actualizadas.

## 7. Compatibilidad con los clientes actuales

- **Cliente liviano** (`AdminDonantesController`, sondeo cada 3 s del avance): no cambia nada. Lee
  los mismos campos con el mismo ritmo de cambio (cada 500 filas).
- **Seeder de demo** e **integration-tests**: usan las mismas rutas; no cambian.
- **Incentivos** también escucha `donante.registrado`: no cambia, porque el evento es el mismo.
- **Gateway:** no cambia.

## 8. Riesgos

| Riesgo | Qué se hace |
|---|---|
| Mientras se espera un reintento, el consumidor de `notificaciones.donaciones` queda frenado y la cola crece durante una importación grande. | Reintentos pocos y cortos, solo para la persona que falta. C9 mide el pico de la cola y que vuelva a 0. Si se ve crecer, se baja a 3 intentos o se usa una cola de espera (eso sí sería un cambio de topología, con su ADR). |
| La carrera sigue existiendo del lado de Donaciones. | El reintento la cubre. Arreglar el orden de publicación queda como duda para el usuario (no son archivos de este lote). |
| Las dos pasadas leen el archivo dos veces del almacenamiento. | Es lectura secuencial de unos pocos MB; el objeto no cambia entre las dos (clave propia, nadie lo reescribe). Si la segunda pasada lee otra cantidad, el archivo termina con la cuenta real y se registra la diferencia. |
| Cada empresa importada deja una persona humana "Representante Legal" sin documento. | `[OBSERVED]` No choca con `uq_persona_documento` (es parcial: `WHERE nro_documento IS NOT NULL`, `V4__personas.sql:13-14`), así que no frena el alta. `[INFERRED]` Con el archivo de la guía son unas 2.980 personas de relleno que pueden verse en el listado de personas del admin. Va como duda. |
| La lectura a mitad deja una importación parcial (sección 6). | Se acepta: reimportar es seguro. El estado `ERROR` y los contadores lo muestran. |


## Aprobación del diseño p1-importador (2026-11-12)

Aprobado por el usuario de forma automática: pidió que, en P1 y P2, cada duda se resuelva con la opción recomendada por el diseñador, sin parada. Cada duda queda resuelta así:

1. Carrera del lado de Donaciones: persona.sincronizada se publica desde un hilo aparte (@Async) y puede salir después de donante.registrado. Es el origen del aviso perdido, pero el arreglo está en PersonasService y NotificacionesAsyncService, fuera de los archivos de este lote. Recomiendo dejar el reintento de Notificaciones en este lote y anotar el arreglo del orden como deuda en DTI-40 ítem 6, para otro lote. **Resuelto: la opción recomendada.**
2. Cada empresa importada crea una persona 'Representante Legal' sin documento: unas 2.980 con el archivo de la guía, y pueden aparecer en el listado de personas del admin. Recomiendo aceptarlo para la demo y anotarlo como deuda (que el CSV traiga el representante, o que el listado oculte a los representantes), sin tocar la regla del dominio que exige un representante. **Resuelto: la opción recomendada.**
3. Mientras dura un reintento, el consumidor de la cola notificaciones.donaciones queda frenado. Recomiendo 4 intentos cortos y medir en el ensayo el pico de la cola y cuánto tarda en volver a 0 (C9); si se ve crecer, bajar a 3 intentos antes de pensar en una cola de espera. **Resuelto: la opción recomendada.**
4. Si la lectura se corta a mitad del archivo, ya no se descarta todo: las tandas procesadas quedan importadas y el archivo termina en ERROR con sus contadores. Recomiendo aceptarlo, porque volver a importar el mismo archivo es seguro: las filas ya importadas entran como actualizadas. **Resuelto: la opción recomendada.**
5. No vi los registros de Notificaciones del ensayo: no sé si el aviso se perdió por la ValidationException o porque la publicación de persona.sincronizada falló y se tragó el error. Con el contenedor por defecto, una ValidationException se reencolaría, no se perdería. Recomiendo que el constructor lo confirme con una prueba antes de elegir cómo engancha el reintento. **Resuelto: la opción recomendada.**

## Ajuste de la ronda 1 de revisión (2026-11-12)

`[OBSERVED]` La revisión encontró que el reintento con esperas (0,5, 1 y 2 s con `Thread.sleep`)
no podía resolver el caso: `persona.sincronizada` y `donante.registrado` llegan por la misma cola
`notificaciones.donaciones` al mismo listener, y la fábrica del contenedor
(`RabbitMQConfig.rabbitListenerContainerFactory`) tiene un solo consumidor. Mientras el hilo
esperaba, la persona quedaba detrás, en la cola o en el prefetch, y el aviso terminaba siempre en
la DLQ.

Cambio sobre el punto 4 del alcance y la duda 3: **no se espera dentro del consumidor.** Cuando
falta la persona, el error sale y el contenedor reencola el mensaje; pasan primero los mensajes que
ya estaban, entre ellos la persona. Las entregas se cuentan por `messageId` y a la décima
(`ReintentoPersonaFaltante.INTENTOS`) el mensaje se rechaza sin reencolar y va a
`notificaciones.donaciones.dlq`; sin `messageId` va directo a la DLQ. C6 se prueba con los dos
eventos por el mismo listener en el orden de la cola (`ReintentoPersonaFaltanteTest`). El riesgo de
la tabla sobre el consumidor frenado ya no aplica. Queda abierto que el tope cuenta entregas y no
tiempo; si en el ensayo (C9) llega algún aviso a la DLQ, el arreglo es el orden de publicación en
Donaciones (duda 1).
