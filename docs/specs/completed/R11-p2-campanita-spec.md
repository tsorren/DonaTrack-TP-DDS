# Spec: R11-p2-campanita — La campanita con desplegable y los avisos de la administración

> **Cerrada el 2026-11-12** (cierre documental de la oleada P2, rama `E6_backlog_r11_p2`): construida e integrada, con la bandeja real del admin desde los arreglos de la ronda 1, y verificada con el stack propio; pasa a `completed/`. ADR `proposed`, escrito después del código: [aviso de importación terminada por AMQP](../../adr/20261112-aviso-de-importacion-terminada-por-evento-amqp-a-la-bandeja-compartida-de-la-administracion.md). Desvíos de la construcción: el fragmento `buzon(...)` no se borró (lo usa la bandeja de la persona en la maqueta) y el estilo del desplegable es un componente propio (`componentes/campanita.css`), no `privado.css`. Lo que quedó pendiente está en [DTI-43](../../adr/DEUDA_TECNICA.md#dti-43), ítem 9. Lo que sigue sobre "PROPOSED" queda superado.
>
> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario para P1 y P2; ver "Aprobación del diseño" al final).
> **Estado:** PROPOSED (2026-11-12). La aprobación es la del usuario al revisar juntos los diseños
> de la oleada P2; ningún ADR se promueve.
> **Nivel:** STANDARD por su forma (ningún endpoint REST cambia; se suma un evento AMQP y tres
> valores a un enum), ruta CONTRATO del [manifiesto](../../interfaz/backlog-revision-2026-11-11.manifiesto.json):
> cambia el contrato de mensajería entre Donaciones y Notificaciones, así que lleva esta spec antes
> del código y su ADR `proposed` al cerrar la oleada (guía de orquestación, punto 4).
> **Fecha:** 2026-11-12
> **Módulos impactados:** `donaciones-service`, `notificaciones-service`, `cliente-liviano`
> **Base:** `e9331b3a2`.
> **Relación:** ítems R11-29 y R11-30 del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md);
> pantallas en la [ficha de flujo del lote](../../interfaz/flujos/r11-p2-campanita.md). Usa la bandeja
> paginada de [R11-p1-notificaciones](R11-p1-notificaciones-spec.md) y las páginas de
> importación de [R11-p1-importaciones](R11-p1-importaciones-spec.md). El evento sigue la
> topología de [SPEC-03](../active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md)
> (`donaciones.exchange` → `notificaciones.donaciones`).

---

## 1. Objetivo funcional

1. **R11-29.** La campanita de la cabecera privada abre un desplegable con las 5 notificaciones sin
   leer más nuevas (título y hace cuánto) y "Ver todas", en lugar de llevar directo a la bandeja.
2. **R11-30.** La administración tiene campanita con número real y bandeja con datos reales. Cuando
   Donaciones termina (o corta) una importación de donantes, la administración recibe un aviso.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que incluye)

- **Donaciones:**
  - Evento nuevo `importacion.terminada.v1` en `donaciones.exchange`, publicado una vez por
    importación al final de `ImportadorService.procesarImportacionAsincronica`, en los dos caminos
    (terminó o se cortó), después de guardar el estado final del archivo.
  - `IDonacionesEventPublisher.publicarImportacionTerminada`, la constante de ruteo y su entrada en
    el `DefaultClassMapper` de `RabbitMQConfig`, como los demás eventos.
  - Sin migración: todo lo que lleva el evento ya está en `archivo` (`V23` es la de `subido_en`).
- **Notificaciones:**
  - `ROUTING_KEY_IMPORTACION_TERMINADA = "importacion.terminada.v1"` en
    `ROUTING_KEYS_DONACIONES_CONSUMIDAS` (la misma cola `notificaciones.donaciones`, con su DLQ), su
    `EventoImportacionTerminadaV1` en el mapeo de clases y un `@RabbitHandler` en
    `DonacionEventListener`.
  - Un `EventoNotificable` nuevo (`ImportacionTerminada`) que arma **una** notificación para la
    persona de la Administración (`AdminConstantes.ID_ADMIN`) con el tipo, el mensaje de la tabla de
    la ficha (§6) y `referenciaId` = el id del archivo.
  - `TipoEvento` suma `IMPORTACION_TERMINADA`, `IMPORTACION_CORTADA` y `MENSAJE_CONTACTO`.
  - `NotificacionService.notificarPersona` (el aviso de contacto) guarda `MENSAJE_CONTACTO` como
    tipo. Los avisos de contacto ya guardados quedan sin tipo.
  - Sin migración: `tipo_evento` es `VARCHAR(40)` con el nombre del enum (`V5`).
  - Contratos: `openapi-notificaciones.yaml` de `1.3.0` a `1.4.0` (los tres valores nuevos de
    `TipoEvento`, que valen en `tipo` y `excluirTipo`); esquema nuevo
    `docs/arquitectura/contratos/schemas/evento-importacion-terminada-v1.schema.json`; el catálogo de
    eventos y la matriz productor-consumidor que ya listan `donante.registrado.v1`.
- **Cliente liviano:**
  - `layout-privado.html`: la campanita de los tres roles pasa de `<a>` a `<details>` con su
    `<summary>` (la campana y el número, como hoy) y el desplegable. Adentro, siempre, "Ver todas las
    notificaciones" a la bandeja del rol; con JavaScript, la lista se pide al abrir. `paneles.js`
    suma la campanita a sus menús (`MENUS`) para cerrarla al tocar afuera, con Esc y al perder el
    foco.
  - La hoja de la cabecera (`privado.css`, el bloque `.armazon-avisos`) suma el desplegable: ancho
    fijo en la computadora, ancho completo con 16 px de margen en 360 px.
  - Dos direcciones nuevas por área (ver §3.3): la lista del desplegable y abrir un aviso.
  - `AvisosRest` cuenta las sin leer del admin con la persona de la Administración.
  - La bandeja del admin (`admin-notificaciones`) con datos reales: un adaptador como los del
    donante y la organización, sobre el fragmento paginado del buzón, con las categorías del admin
    (ficha §4). La lista de ejemplo pasa de la plantilla a la maqueta. Con esto, el fragmento viejo
    `buzon(...)` queda sin uso y se borra (lo que dejó dicho R11-p1-notificaciones §3.10).
  - Los títulos nuevos (ficha §6) en la misma tabla de títulos por tipo.
  - Propiedad `cliente.notificaciones.administracion` con el id de la persona de la Administración,
    por defecto `00000000-0000-0000-0000-000000000001`.

### Out-of-Scope (lo que no incluye)

- Una bandeja por cuenta de admin, o avisar solo a quien subió el archivo (ficha, Decisiones 1). No
  se agrega "quién subió" al archivo.
- La franja "Se está importando…" de Donantes para cualquier admin: sigue con la memoria de la
  sesión (R11-p1-importaciones). El aviso de la campanita cubre al admin que no la subió.
- Avisos del admin que ningún servicio genera hoy (entidades para aprobar, donaciones por vencer,
  problemas de logística, asignaciones, ranking).
- Que el número de la campanita se actualice solo, sin cambiar de pantalla.
- "Marcar todas como leídas" o tildes dentro del desplegable.
- Una pantalla de mensajes de contacto para el admin (el aviso de contacto no tiene destino propio).
- `GET /api/notificaciones` (el listado general, solo ADMIN): sigue en `p2-listas-restantes`.
- El cambio de "entidad" a "Organización" en los textos (`p1-nombres`). No se renombran rutas, clases
  ni campos.

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Sin matriz 5D, como pide el lote: el contrato en §3.1 a §3.3 y cada decisión, con la alternativa elegida y las descartadas, en §3.4.

### 3.1 Mensajería (Donaciones → Notificaciones)

**Antes:** cuando termina una importación, Donaciones no publica nada.

**Después:** `importacion.terminada.v1`, exchange `donaciones.exchange` (topic), consumido por
Notificaciones en `notificaciones.donaciones`. Headers como el resto: `message_id` (UUID nuevo por
publicación; la deduplicación de Notificaciones es por él, tabla `evento_procesado`), `__TypeId__` =
la routing key y el `traceId`.

| Campo | Tipo | Obligatorio | Qué es |
|---|---|---|---|
| `archivoId` | uuid | sí | El archivo importado; va a `referenciaId` |
| `resultado` | string | sí | `TERMINADA` (estado `PROCESADO`), `TERMINADA_CON_ERRORES` (`PROCESADO_CON_ERRORES`) o `CORTADA` (`ERROR`) |
| `totalFilas` | integer ≥ 0 | no | Las filas contadas; nulo si se cortó antes de contarlas |
| `procesadas` | integer ≥ 0 | sí | Filas leídas (= `creadas` + `actualizadas` + `conError`) |
| `creadas` | integer ≥ 0 | sí | Donantes nuevos |
| `actualizadas` | integer ≥ 0 | sí | Donantes actualizados |
| `conError` | integer ≥ 0 | sí | Filas con error |
| `subidoEn` | date-time (hora de Buenos Aires, sin zona, como `subido_en`) | no | Cuándo se subió; nulo en archivos anteriores a `V23` |
| `fecha` | date-time (`yyyy-MM-dd'T'HH:mm:ss'Z'`, como los demás eventos) | sí | Cuándo terminó |

**No lleva** el nombre del archivo ni ningún dato de los donantes (§6, R1). Los nombres en
español del resultado son del evento, no del enum interno `EstadoArchivo`, para que un cambio de
estados en Donaciones no rompa a Notificaciones.

Lo que hace Notificaciones con el evento:

| `resultado` | `tipoEvento` | Destinatario | `referenciaId` |
|---|---|---|---|
| `TERMINADA`, `TERMINADA_CON_ERRORES` | `IMPORTACION_TERMINADA` | `ID_ADMIN` | `archivoId` |
| `CORTADA` | `IMPORTACION_CORTADA` | `ID_ADMIN` | `archivoId` |

El mensaje es el de la ficha §6, con las cifras con punto de miles y `subidoEn` como "el 12/11 a las
18:30" (sin `subidoEn`: "La importación de donantes terminó…").

### 3.2 Notificaciones (REST)

Ningún endpoint cambia de ruta, parámetros ni cuerpo.

| | Antes (`1.3.0`) | Después (`1.4.0`) |
|---|---|---|
| `TipoEvento` | `DONANTE_REGISTRADO`, `DONANTE_INACTIVO`, `MISION_CUMPLIDA`, `SUBIO_CATEGORIA`, `DONACION_ASIGNADA`, `DONACION_RECIBIDA`, `DONACION_EN_CAMINO`, `ENTREGA_FALLIDA`, `DONACION_VENCIDA` | Los mismos más `IMPORTACION_TERMINADA`, `IMPORTACION_CORTADA` y `MENSAJE_CONTACTO` |
| `tipo` / `excluirTipo` de la bandeja | Un valor fuera del enum da 400 `ERR-CSR-003` | Igual; los tres nuevos son válidos |
| `NotificacionDTO.tipoEvento` del aviso de contacto | `null` | `MENSAJE_CONTACTO` (los nuevos) |
| `POST /api/notificaciones/eventos` (discriminador `tipo` de `EventoNotificableDTO`) | Los nueve tipos | Sin cambios: los tres nuevos no se aceptan por REST, solo nacen del evento AMQP y del contacto. El Javadoc de `TipoEvento` ("los nombres son los del discriminador") se ajusta para decirlo |

Lo que usa el cliente, todo existente:

| Uso | Llamada |
|---|---|
| Número de la campanita | `GET /api/notificaciones/persona/{personaId}/no-leidas/conteo` |
| Las 5 del desplegable | `GET /api/notificaciones/persona/{personaId}?leida=false&size=5&page=0&orden=recientes` (total de sin leer en `X-Total-Count`) |
| Abrir un aviso | `PATCH /api/notificaciones/{id}/leida?leida=true` (devuelve la notificación con `tipoEvento` y `referenciaId`, de donde sale el destino) |
| Bandeja del admin | `GET /api/notificaciones/persona/{ID_ADMIN}?…` con los filtros de la bandeja; `marcar-todas-leidas` de `ID_ADMIN` |

El admin puede leer y marcar la bandeja de `ID_ADMIN` con los permisos de hoy: `Administrador`
`puedeOperarSobre` cualquier persona.

### 3.3 Cliente liviano (direcciones)

| Dirección | Antes | Después |
|---|---|---|
| La campanita | `<a href="/{area}-notificaciones">` | `<details>`; el vínculo "Ver todas" adentro va a la misma bandeja |
| `GET /{area}-notificaciones-recientes` (`area` = `donante`, `entidad`, `admin`) | No existía | Fragmento del desplegable (con `HX-Request`): la cifra, hasta 5 filas y "Ver todas". Sin `HX-Request`, 303 a `/{area}-notificaciones?ver=sin-leer`. Si Notificaciones no contesta, el fragmento de error con 200 (no rompe la pantalla de abajo) |
| `POST /{area}-notificaciones-abrir` (`id`, CSRF) | No existía | Marca el aviso como leído y responde 303 al destino, calculado en el servidor desde la notificación que devuelve el `PATCH` (nunca desde un parámetro del navegador). Si el `PATCH` falla, 303 a la bandeja. Con HTMX, `HX-Redirect` |
| `GET /admin-notificaciones` | Lista de ejemplo de la plantilla | Bandeja real de `ID_ADMIN`, paginada como las otras dos; en la maqueta, la lista de ejemplo desde la maqueta |

Destinos por tipo (los del donante y la organización son los de su bandeja, sin cambios):

| Tipo | Destino |
|---|---|
| `IMPORTACION_TERMINADA` | `/admin-donantes-importado?archivo=<referenciaId>` |
| `IMPORTACION_CORTADA` | La página que hoy elige `admin-importaciones` para ese archivo (con filas: la de resultado o avance; sin filas: `admin-donantes-importar-error`) |
| `MENSAJE_CONTACTO`, sin tipo o sin `referenciaId` | `/{area}-notificaciones` |

Cada fila del desplegable es un formulario `post` a `-abrir` con aspecto de fila (botón de ancho
completo), para que ande sin JavaScript una vez abierto el `<details>` y lleve CSRF.

### 3.4 Decisiones y alternativas

1. **Bandeja de la Administración compartida, sobre `ID_ADMIN`.** Ya es el destinatario del aviso de
   contacto y está sembrada en los dos servicios. Descartado: una persona por cuenta de admin en
   Notificaciones (copiar cuentas del realm, decidir a quién avisar) y avisar a quien subió (el
   archivo no lo guarda; sería migración y cambio de contrato de la subida).
2. **Evento AMQP y no llamada REST** a `POST /api/notificaciones/eventos`. Es el mecanismo que
   Donaciones ya usa para avisar a Notificaciones, con su cola, su DLQ y su deduplicación, y la
   importación corre en un hilo aparte donde una llamada síncrona no le agrega nada. No es un canal
   nuevo: misma exchange, misma cola.
3. **El evento cuenta el hecho, Notificaciones decide a quién avisar.** El evento no lleva
   destinatario; Notificaciones lo dirige a `ID_ADMIN`. Si mañana hay bandejas por cuenta, cambia
   solo el consumidor.
4. **El desplegable se pide al abrir** (`hx-trigger` en el `toggle` del `<details>` cuando queda
   abierto, cada vez). Descartado: pedirlo con cada pantalla (una llamada más siempre) y sondear.
5. **El destino se calcula en el servidor** desde la respuesta del `PATCH`. Descartado: mandar el
   destino en el formulario (habría que validarlo contra redirecciones abiertas).
6. **Resultado del evento con nombres propios** (`TERMINADA`, `TERMINADA_CON_ERRORES`, `CORTADA`)
   y no `EstadoArchivo`, para no atar el contrato a un enum interno.
7. **Id de la Administración en el cliente como propiedad con valor por defecto**, no una tercera
   copia de la constante en código ni un endpoint nuevo `/api/notificaciones/administracion`
   (cambio de contrato para algo que ya resuelve la ruta por persona).

## 4. Invariantes y compatibilidad con los clientes actuales

- **Ninguna lista completa:** la campanita pide como máximo 5 avisos y el número como conteo; la
  bandeja del admin, de a 20 (regla general 1).
- **Un aviso por importación:** se publica una vez, con el estado final ya guardado; si el guardado
  del final falla y el archivo pasa a `ERROR`, se publica `CORTADA`. Si también falla ese guardado,
  no se publica nada (el error ya queda en el registro).
- **Idempotencia:** el mismo mensaje entregado dos veces deja una sola notificación (inbox por
  `message_id`, como los demás).
- **Trazabilidad:** el evento lleva `traceId` (el publicador genera uno si el hilo de la importación
  no tiene).
- **Donante y organización:** sus bandejas, conteos y marcar no cambian; la campanita sigue
  mostrando su número igual. El único cambio visible es que la campana abre el desplegable.
- **Avisos viejos:** los que no tienen tipo siguen titulándose "Novedad" y llevan a la bandeja.
- **Lectores del enum:** el cliente trata `tipoEvento` como texto y un valor que no conoce como
  "Novedad", así que los tres valores nuevos no rompen una versión anterior del cliente. Incentivos y
  Logística no leen notificaciones.
- **Orden de despliegue:** si Donaciones publica antes de que Notificaciones tenga el binding, el
  mensaje se descarta en la exchange (no hay cola para esa clave) y no se rompe nada; se pierde ese
  aviso. Los dos se despliegan juntos en la misma oleada.
- **Maqueta:** sigue andando sin servicios; la campanita y la bandeja del admin toman los ejemplos
  de la maqueta.

## 5. Criterios de Aceptación

```gherkin
Escenario C1: Termina una importación sin errores
  Dado un archivo de donantes que Donaciones procesa hasta el final sin filas con error
  Cuando el archivo queda en PROCESADO
  Entonces Donaciones publica una sola vez importacion.terminada.v1 con resultado TERMINADA y las cifras del archivo
  Y el mensaje no tiene el nombre original del archivo

Escenario C2: Termina con filas con error
  Dado un archivo que queda en PROCESADO_CON_ERRORES con 50 filas con error
  Cuando termina la importación
  Entonces el evento lleva resultado TERMINADA_CON_ERRORES y conError 50

Escenario C3: Se corta
  Dado un archivo cuya importación lanza una excepción y queda en ERROR
  Cuando termina el procesamiento
  Entonces el evento lleva resultado CORTADA con las filas que se leyeron

Escenario C4: Notificaciones arma el aviso de la administración
  Dado un importacion.terminada.v1 con resultado TERMINADA_CON_ERRORES
  Cuando Notificaciones lo consume
  Entonces guarda una notificación para ID_ADMIN con tipoEvento IMPORTACION_TERMINADA y referenciaId igual al archivoId
  Y el mensaje dice la fecha de subida y las cuatro cifras, sin el nombre del archivo

Escenario C5: Mensaje repetido
  Dado un importacion.terminada.v1 ya consumido
  Cuando llega otra vez con el mismo message_id
  Entonces no se guarda una segunda notificación

Escenario C6: Aviso de contacto con tipo
  Dado un mensaje de contacto nuevo
  Cuando Notificaciones avisa a la administración
  Entonces la notificación lleva tipoEvento MENSAJE_CONTACTO

Escenario C7: Filtro por los tipos nuevos
  Cuando se pide GET /api/notificaciones/persona/{ID_ADMIN}?tipo=IMPORTACION_TERMINADA
  Entonces responde 200 solo con esos avisos y X-Total-Count con su total

Escenario C8: La campanita del admin tiene número
  Dado que ID_ADMIN tiene 2 notificaciones sin leer
  Cuando un admin abre cualquier pantalla de su área con el ingreso real
  Entonces la campanita dice "Notificaciones, 2 sin leer"

Escenario C9: El desplegable trae como máximo 5
  Dado una cuenta con 12 sin leer
  Cuando se abre la campanita
  Entonces el cliente pide a Notificaciones leida=false y size=5, una sola vez
  Y el desplegable muestra 5 filas con título y hace cuánto, "5 de 12 sin leer" y "Ver todas las notificaciones"

Escenario C10: Sin sin leer
  Dado una cuenta sin notificaciones sin leer
  Cuando se abre la campanita
  Entonces se ve "No tenés notificaciones sin leer." y "Ver todas las notificaciones"

Escenario C11: Notificaciones no contesta
  Dado que Notificaciones no responde
  Cuando se abre la campanita
  Entonces el desplegable muestra "No pudimos traer tus notificaciones." y "Ver todas las notificaciones"
  Y la pantalla de abajo sigue igual

Escenario C12: Abrir un aviso de importación
  Dado un aviso IMPORTACION_TERMINADA sin leer en el desplegable del admin
  Cuando el admin lo toca
  Entonces el aviso queda leído
  Y el navegador llega a /admin-donantes-importado?archivo=<id>

Escenario C13: Abrir un aviso sin destino
  Dado un aviso MENSAJE_CONTACTO o sin tipo
  Cuando se toca en el desplegable
  Entonces queda leído y se llega a la bandeja del rol

Escenario C14: El destino no viene del navegador
  Cuando se envía POST /donante-notificaciones-abrir con un parámetro destino=https://otro.sitio
  Entonces la respuesta redirige al destino calculado desde la notificación, nunca a otro sitio

Escenario C15: Sin JavaScript
  Dado el navegador sin JavaScript
  Cuando se abre la campanita
  Entonces se ve "Ver todas las notificaciones" y lleva a la bandeja

Escenario C16: Cierre del desplegable
  Dado el desplegable abierto
  Cuando se toca afuera o se aprieta Esc
  Entonces se cierra y el foco vuelve a la campanita, sin marcar nada

Escenario C17: Bandeja real del admin
  Dado el ingreso real de un admin
  Cuando abre admin-notificaciones
  Entonces ve los avisos de ID_ADMIN de a 20, con el paginador y las categorías Importaciones, Mensajes de contacto y Otras novedades
  Y no ve ningún aviso de ejemplo

Escenario C18: Pantallas de la oleada
  Cuando se abren donante-inicio y admin-inicio en 360, 1280 y 1920 px con la campanita abierta
  Entonces el desplegable no se sale de la pantalla, no tapa la campanita y no hay nada superpuesto
```

Pruebas: C1 a C3 en el test de `ImportadorService` con el publicador simulado; C4 a C7 en
Notificaciones (servicio y listener, más el test de contrato del esquema); C8 a C17 en los tests de
controller y adaptador del cliente con `MockRestServiceServer`; C18 con capturas. Un test de
integración entre Donaciones y Notificaciones por RabbitMQ (Gate 3) si el stack de la oleada lo
permite; si no, `[DEFERRED_NO_DOCKER]`.

## 6. Riesgos

| # | Riesgo | Qué se hace |
|---|---|---|
| R1 | El nombre del archivo puede traer datos personales | El evento y el mensaje no lo llevan; la página de destino lo muestra solo al admin |
| R2 | Bandeja compartida: un admin abre un aviso y otro ya no lo ve como nuevo | Lo dice la bajada de la bandeja (ficha §6); la bandeja por cuenta queda para después |
| R3 | `persistirYPublicar` dispara el envío por los medios de la persona: `ID_ADMIN` tiene correo sembrado, así que cada importación manda un correo a esa dirección, como ya pasa con el contacto | Se acepta: un correo por importación, no por fila. Si molesta en la demo, se apaga el medio, no el aviso |
| R4 | La importación corre con `@Async`, sin transacción: si la app se cae entre guardar y publicar, el aviso se pierde | Igual que el resto de los eventos de Donaciones (no hay outbox de notificaciones). El estado del archivo queda bien y se ve en Importaciones |
| R5 | El `toggle` del `<details>` y HTMX: si el fragmento llega cerrado el desplegable, o se abre dos veces rápido | Se pide en cada apertura y se descarta la respuesta si el `<details>` ya está cerrado; `hx-sync` de reemplazo |
| R6 | La campanita se dibuja en las páginas de error y Contacto con sesión (R11-05): el desplegable no puede romperlas | El fragmento se pide recién al abrir; si falla, muestra su error dentro del desplegable |
| R7 | Cambia `layout-privado.html`, que tocan otros lotes de la oleada | El cambio se limita al bloque de la campanita; el integrador resuelve choques en ese bloque |
| R8 | Tres valores nuevos del enum y un cliente anterior | El cliente ya trata un tipo desconocido como "Novedad" (§4) |

## Aprobación del diseño p2-campanita (2026-11-12)

Aprobado por el usuario el 2026-11-12 con las opciones recomendadas: pidió aprobar en forma automática todas las opciones recomendadas de P1 y P2 para que la corrida termine sin su intervención.

1. ¿Bandeja compartida para todos los admins (si uno abre un aviso, queda leído para todos)? Recomiendo sí para esta entrega, porque reusa lo que ya existe y la bajada de la pantalla lo dice. La bandeja por cuenta quedaría en el backlog.
   **Resuelto: la opción recomendada.**
2. ¿Cada importación debería mandar además un correo a admin@donatrack.com, como ya pasa con el aviso de contacto (riesgo R3)? Recomiendo aceptarlo: es un correo por importación, no por fila. Si molesta en la demo, se apaga ese medio de contacto y no el aviso.
   **Resuelto: la opción recomendada.**
3. ¿La franja 'Se está importando…' de Donantes debería verla cualquier admin, no solo el que subió el archivo? Recomiendo dejarla como está, en la memoria de la sesión, y que el aviso de la campanita cubra al resto. Cambiarla pide guardar quién subió el archivo.
   **Resuelto: la opción recomendada.**
4. ¿Los textos de los avisos de importación ('Terminó una importación', 'Una importación se cortó', con fecha de subida y cifras) están bien? Recomiendo aceptarlos así y ajustar palabras sueltas al ver las capturas.
   **Resuelto: la opción recomendada.**

## Cierre (2026-11-12): revisión y lo que quedó

Resumen del cierre de la oleada P2 para el lote `p2-campanita` (`f608af8bb` y arreglos `aba332675`). La revisión la hizo el
revisor del workflow `backlog-r11`, separado del constructor (`[INFERRED]` por cómo lo orquesta el
workflow: lee la rama y los informes, no el razonamiento del constructor). Este bloque lo redactó
el agente del cierre documental de P2 con lo que consta en los commits y en
[DTI-43](../../adr/DEUDA_TECNICA.md#dti-43), ítem 9, donde está lo pendiente.

```text
=== REVIEW CONTRACT ===

Mode: INDEPENDENT_REVIEW (revisión de integración del workflow backlog-r11)
Task Level: ARCHITECTURAL

V1 Regression risks:
  Ver DTI-43, ítem 9.

V2 Architecture/invariant violations:
  NONE_DETECTED en el cierre.

V3 Contracts/integration:
  [OBSERVED] Evento importacion.terminada.v1 nuevo y OpenAPI de Notificaciones 1.4.0; ADR
  `proposed` escrito después del código (desvío acordado).

V4 Tests/coverage:
  [VERIFIED] run-preprod-tests.sh en el cierre (smoke 4/4; contrato, integración y E2E 26/26,
  stack propio) y recorrido con login real de la administración. Sin caso de contrato del
  evento nuevo en integration-tests (DTI-43, ítem 9).

V5 Scope violations:
  NONE_DETECTED.

V6 Evidence integrity:
  NONE_DETECTED.

V7 SonarCloud pre-flight:
  [TESTS_NOT_EXECUTED_BY_EVALUATOR] No consta; lo mide el Quality Gate del PR.

V8 Security/privacy:
  NONE_DETECTED.

V9 Documentation graph integrity:
  [OBSERVED] Spec movida a completed/ con README, ESTADO_DOCUMENTACION, specs/README y
  DEUDA_TECNICA en el cierre documental de P2.
```
