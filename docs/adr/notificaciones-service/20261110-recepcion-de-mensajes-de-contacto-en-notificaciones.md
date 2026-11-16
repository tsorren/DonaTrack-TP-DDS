# Recepción de los mensajes de contacto en Notificaciones

- Status: proposed
- Date: 2026-11-10
- Deciders: tsorren (2026-11-10, eligió Notificaciones sobre la matriz de esta decisión); formulario dentro de DonaTrack, decidido por el usuario el 2026-11-02 (pregunta 17 de las preguntas abiertas de Raíces); límites, clave de envío, estados y plazo de retención, propuestos por el agente
- Tags: notificaciones, contacto, formulario-publico, abuso, idempotencia, privacidad, correo, entrega-6

> **Origen:** HB-21 de [`requerimientos-e6.md`](../../requerimientos/requerimientos-e6.md) y
> [DTI-33](../DEUDA_TECNICA.md#dti-33). La pantalla está en la ficha
> [Contacto, en Raíces](../../interfaz/flujos/raices-contacto.md). Esta tarea solo decide: no toca
> código. La implementación es un hueco aparte, con su propio PR.

## Contexto y Problema

El sitio tiene un formulario de contacto (`/publico-contacto`). Lo usa cualquiera: una entidad o una
empresa a la que algo le falló ("escribinos con este código") o alguien sin cuenta. Hoy es maqueta:
ningún servicio recibe los mensajes.

Qué hay hoy:

- `[OBSERVED]` Ningún servicio tiene mensajes de contacto. Las rutas que expone el gateway están en
  `api-gateway/src/main/java/grupo5/gateway/RutasGateway.java`, y `/api/contacto` no figura (denegar
  por defecto).
- `[OBSERVED]` De los servicios, solo `donaciones-service` valida el token
  (`spring-boot-starter-security-oauth2-resource-server` en su `pom.xml`; en `common-lib` es
  `optional` y no llega a Notificaciones). El cliente liviano maneja la sesión con
  `spring-boot-starter-security-oauth2-client`. HB-13 planea el resource server en los cuatro
  servicios (`requerimientos-e6.md`, fila HB-13).
- `[OBSERVED]` Notificaciones ya manda correos: `NotificacionRouter.enviarA(Correo, String)`
  despacha a `CorreoAdapter.enviarMail(correo, mensaje)` (hoy `CorreoAdapterSimulado`; el DTO de SendGrid ya existe en
  `adapters/dtos/sendgrid/`).
- `[OBSERVED]` Notificaciones guarda una réplica de cada persona con sus medios de contacto
  (`PersonasController`, `/api/notificaciones/personas`), así que conoce el correo de una cuenta.
- `[DOCUMENTED]` La Administración es una persona replicada con id fijo
  (`AdminConstantes.ID_ADMIN`), con su propia bandeja de notificaciones
  ([ADR aceptado del 2026-07-02](../20260702-modelado-del-destinatario-administracion-como-persona-replicada-de-id-fijo.md)).
- `[OBSERVED]` El repo no tiene ningún limitador de envíos (sin `bucket4j`, `RequestRateLimiter` ni
  `resilience4j-ratelimiter`).
- `[DOCUMENTED]` La ficha de Contacto pide: número del mensaje al enviar, respuesta por correo,
  correo obligatorio sin sesión y el de la cuenta con sesión, y que tras un corte "sin conexión" se
  pueda mandar de nuevo sin saber si llegó.

Problema (los tres puntos de DTI-33):

1. **Dueño:** qué servicio recibe `POST /api/contacto` y dónde lee el administrador los mensajes.
2. **Abuso y repetidos:** cómo se limita un formulario público y cómo se reconoce el mismo mensaje
   mandado dos veces.
3. **Respuesta:** cómo se contesta por correo, con el número del mensaje.

## Atributos de Calidad y Drivers de Decisión

| Driver | Peso | Qué mide |
|---|---|---|
| Cohesión del contexto | 20 | Que el contacto encaje en lo que el servicio ya hace |
| Autenticación, hoy y con HB-13 | 20 | Saber quién escribe (con sesión) y que solo `ADMIN` lea |
| Respuesta por correo con número | 15 | Llegar al correo sin canales nuevos |
| Privacidad | 15 | Dónde quedan el correo de alguien sin cuenta y el texto libre, y cómo se borran |
| Costo y piezas nuevas | 15 | Servicios, eventos, endpoints y dependencias que suma |
| El admin lo lee donde ya trabaja | 15 | Que el mensaje no quede en un lugar que nadie mira |

## Relaciones Arquitectónicas

- [Destinatario Administración como persona replicada de id fijo](../20260702-modelado-del-destinatario-administracion-como-persona-replicada-de-id-fijo.md)
  (`accepted`): el aviso de mensaje nuevo va a esa persona.
- [Privacidad de usuarios](../20260519-privacidad-de-usuarios.md) (`accepted`) y
  [protección de PII con crypto-shredding en Notificaciones](./20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md)
  (`proposed`): el correo y el texto del mensaje son datos personales y siguen esa estrategia.
- [Gateway con validación JWT en doble capa y rutas públicas](../20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
  (`proposed`): `POST /api/contacto` es una ruta pública más de la allowlist.
- [DTI-13, migración de la API REST deprecada de Notificaciones a AMQP](../20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md)
  (`proposed`): deprecó los `POST` que usan otros servicios. Esta decisión no los revive. El endpoint
  nuevo lo llama el sitio a través del gateway, igual que la bandeja, y no otro servicio.
- [Matriz de permisos](../../arquitectura/seguridad/matriz-permisos.md) (borrador): suma las filas de
  `/api/contacto` al implementar.

## Alternativas Consideradas

1. **A. Notificaciones recibe, guarda y responde.**
2. **B. Donaciones recibe y guarda; la respuesta sale por un evento hacia Notificaciones.**
3. **C. Mixta: Donaciones recibe (ya valida el token) y publica un evento; Notificaciones guarda y
   responde.**
4. **D. Un servicio de contacto nuevo.**
5. **E. Fuera del backend: un webhook de n8n o un enlace `mailto`.**

Para el abuso:

- **Límite contado en la tabla del servicio** (elegido).
- **Límite en el gateway** con un filtro de rate limiting.
- **CAPTCHA.**

## Resultado de la Decisión

**Alternativa elegida: A. Notificaciones recibe, guarda y responde los mensajes de contacto.**

Matriz (1 a 5, ponderada sobre 100; los puntajes son `[INFERRED]` a partir de lo observado en el
contexto):

| Driver (peso) | A. Notificaciones | B. Donaciones | C. Mixta |
|---|---|---|---|
| Cohesión (20) | 4: la comunicación con las personas ya es suya | 2: suma un tema ajeno al servicio central, ya grande | 3 |
| Autenticación (20) | 4: la gana con HB-13 | 5: ya la tiene | 3: recibe con token, pero el admin lee donde aún no lo hay |
| Respuesta por correo (15) | 5: el correo está en el mismo servicio | 4: necesita un evento nuevo | 5 |
| Privacidad (15) | 4: ya guarda correos y tiene diseño de cifrado | 3: suma correos de personas sin cuenta | 3: el correo pasa por dos servicios |
| Costo (15) | 3 | 3 | 2: dos servicios y un evento nuevo |
| Admin donde trabaja (15) | 4: aviso en su bandeja | 5 | 3 |
| **Total** | **80** | **73** | **63** |

D y E quedan fuera de la matriz:

- D: un servicio nuevo para un formulario cuesta más que cualquiera de las otras opciones.
- E: no da bandeja ni número del mensaje, y el usuario eligió un formulario dentro de DonaTrack.

### 1. Dueño y lectura del administrador

- `POST /api/contacto` en `notificaciones-service`, público, con su ruta literal en la allowlist del
  gateway. Cuerpo:
  - `tema`: uno de los seis de la ficha;
  - `texto`: obligatorio, con un largo máximo;
  - `codigoError`: opcional;
  - `correo`: obligatorio sin sesión;
  - `claveEnvio`: ver el punto 2.
- Responde `201` con el `numero` del mensaje: un entero de una secuencia, corto para leerlo y
  dictarlo ("Su número es 4821"). No es un dato que haya que ocultar: con el número no se lee nada.
- **Con sesión**, se responde al correo de la cuenta, tomado de la réplica de la persona. Hasta HB-13,
  el cliente liviano manda el correo de la cuenta como si no hubiera sesión. Con HB-13, la persona
  sale del token y el campo `correo` se ignora.
- **Aviso al administrador:** cada mensaje crea una notificación para la persona Administración
  ("Nuevo mensaje de contacto Nº 4821, sobre una entrega"), que aparece en su bandeja actual.
- **Lectura**, solo con rol `ADMIN` (HB-13):
  - `GET /api/contacto?estado=&pagina=`: lista paginada, la más nueva primero;
  - `GET /api/contacto/{numero}`: el mensaje con su respuesta, si la tiene.

### 2. Abuso y repetidos

- **Repetidos:** al abrir el formulario, el cliente liviano genera una `claveEnvio` (UUID) en un
  campo oculto. La tabla la guarda con restricción `UNIQUE`. Si la misma clave llega otra vez, el
  servicio no crea otro mensaje y devuelve `200` con el número del primero. Cubre el caso "sin
  conexión: mandalo de nuevo".
  - Requisito para el cliente liviano: la clave tiene que sobrevivir al reenvío. Con JavaScript, el
    formulario sigue en la página. Sin JavaScript, al volver con Atrás el navegador puede pedir el
    formulario de nuevo y generar otra clave. `[INFERRED]` Para evitarlo, el cliente liviano no
    genera una clave nueva si recibe una en el pedido, y no sirve el formulario con `no-store`.
    Si aun así llega otra clave, el mensaje queda duplicado y lo frena el límite por correo.
- **Orden de los chequeos:** primero la clave de envío, después el límite. Un reenvío con una clave
  ya guardada devuelve el número del primer mensaje aunque ese correo ya haya llegado al límite.
- **Límite de envíos:** el servicio cuenta los mensajes en su propia tabla antes de guardar:
  - hasta 5 por hora por correo;
  - hasta 20 por día por IP de origen. `[INFERRED]` El navegador le envía el formulario al cliente
    liviano, que llama al servicio desde el servidor: la IP que ve el gateway es la del cliente
    liviano. Por eso el cliente liviano reenvía la IP del navegador en `X-Forwarded-For`, y el
    servicio cuenta por ese valor.
  - Al pasarlo, responde `429` con un código del catálogo de errores. Los números son el valor
    inicial y van en configuración.
- **Campo trampa:** el formulario lleva un campo oculto para personas. Si llega con texto, el cliente
  liviano muestra el éxito sin llamar al servicio.
- **Tamaño:** `texto` con un largo máximo (2.000 caracteres) y `codigoError` con el formato de los
  códigos de los avisos.

### 3. Respuesta por correo

- `POST /api/contacto/{numero}/respuesta`, solo `ADMIN`, con el texto de la respuesta.
- Notificaciones la manda por `NotificacionRouter` al correo del mensaje, con "Respuesta a tu
  mensaje Nº 4821" al comienzo.
  - `CorreoAdapter.enviarMail` no recibe asunto. Si hace falta uno, se suma al implementar, dentro
    del mismo servicio.
- Estados del mensaje:
  - `RECIBIDO` → `RESPONDIDO`;
  - `ENVIO_FALLIDO` si el proveedor falla. Desde ahí el admin puede reintentar.
- No hay acuse de recibo por correo: la pantalla ya muestra el número, y un correo automático a
  cualquier dirección escrita en un formulario público serviría para mandar correos a terceros.

### Privacidad

- El correo y el texto son datos personales. Siguen la estrategia de cifrado y borrado de
  Notificaciones (ADR `proposed` de crypto-shredding).
- Mensajes de personas sin cuenta: correo y texto se anonimizan a los 180 días de cerrados. Ese plazo
  es una propuesta y lo valida el grupo.
- Los mensajes con sesión siguen la baja de su persona.
- La IP se guarda solo para el límite y se borra a las 24 horas.

## Consecuencias

### Positivas

- La respuesta sale del mismo servicio que guarda el correo: no hay evento ni canal nuevo entre
  servicios.
- El administrador se entera en la bandeja que ya tiene.
- Reenviar tras un corte no duplica el mensaje, y el número sigue siendo el mismo.
- El límite no suma dependencias: es una consulta sobre la tabla del mensaje.

### Negativas

- Notificaciones pasa de solo mandar avisos a recibir mensajes de personas: su contexto crece.
- Hasta HB-13, Notificaciones no valida el token. Leer y responder (`ADMIN`) quedan sin protección del
  servicio, como el resto de su API hoy. El mensaje con sesión no puede probar de quién es.
- El límite por IP depende de que el cliente liviano reenvíe la IP del navegador. Si alguien llama
  al gateway directo, puede inventar ese encabezado: el límite por correo sigue valiendo. Detrás de
  una NAT compartida, varias personas cuentan como una.
- El límite contado en la tabla no frena un ataque de volumen al gateway: solo evita que los mensajes
  se guarden.

## Análisis por alternativa

- **B. Donaciones.** Ya valida el token y es donde trabaja el admin. Pero suma un tema que no es de
  donaciones al servicio más grande, guarda correos de personas sin cuenta en un servicio que no los
  tenía y necesita un evento nuevo para responder. `[REJECTED]` por cohesión y privacidad.
- **C. Mixta.** Toma la autenticación de Donaciones y el correo de Notificaciones, pero el mensaje
  vive en dos servicios: un evento nuevo, dos tablas y dos lugares para borrar. `[REJECTED]` por
  costo.
- **D. Servicio nuevo.** Despliegue, base y pipeline nuevos para un formulario. `[REJECTED]` por
  costo.
- **E. n8n o `mailto`.** Sin número ni bandeja; contradice la decisión del 2026-11-02. `[REJECTED]`.
- **Límite en el gateway.** Frena el volumen antes de llegar al servicio, pero pide una dependencia
  nueva (por ejemplo, `bucket4j`) y aprobación para el `pom.xml`. `[REJECTED]` por ahora. Si el sitio
  se publica y aparece abuso de volumen, se suma con su propio pedido.
- **CAPTCHA.** Depende de un tercero, le pasa datos de quien navega y empeora la accesibilidad (la
  ficha pide AA sin JavaScript). `[REJECTED]`.

## Validación

Al implementar (otro PR):

1. Test de servicio: la misma `claveEnvio` dos veces devuelve el mismo número y un solo registro.
2. Test de servicio: el sexto mensaje en una hora del mismo correo da `429` con su código. Con el
   correo ya en el límite, una clave repetida devuelve `200` con el número del primero, no `429`.
3. Test del controller: sin `correo` ni sesión da `400`; con datos válidos da `201` con el número.
4. Test: un mensaje nuevo crea la notificación para `AdminConstantes.ID_ADMIN`.
5. Test: responder cambia el estado a `RESPONDIDO` y llama a `NotificacionRouter` con el correo del
   mensaje. Si el proveedor falla, el estado pasa a `ENVIO_FALLIDO`.
6. `RutasGatewayTest`: la ruta pública `POST /api/contacto` llega a Notificaciones.
7. OpenAPI de Notificaciones con los cuatro endpoints, validado por `OpenApiContractDriftTest`.

## Trabajo futuro

- Implementar el hueco en Notificaciones: tabla con Flyway, endpoints, OpenAPI, ruta del gateway y
  código de error del límite.
- Conectar la pantalla de Contacto del cliente liviano y una pantalla del admin para leer y responder.
- Con HB-13: la persona sale del token y los endpoints del admin exigen `ADMIN`.
- Confirmar con el grupo el plazo de retención de 180 días.
