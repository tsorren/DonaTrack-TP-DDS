# Vuelta de los proveedores de logística por mensajería, con identidad

- Status: proposed
- Date: 2026-10-08
- Deciders: Decisión Grupal
- Tags: arquitectura, integracion, broker, amqp, rabbitmq, seguridad, identidad, contratos, entrega-4

## Contexto y Problema

El broker de integración con logística ([20261007](./20261007-broker-de-integracion-con-logistica.md)) decide a qué proveedor le manda cada entrega y se la manda por AMQP o por HTTP. Esa es la **ida**. Falta definir la **vuelta**: cómo se entera Donaciones de que la entrega avanzó (ruta asignada, ruta iniciada, entrega exitosa o fallida) para cambiar el estado de la donación.

Para un proveedor AMQP (incluida nuestra Logística) la vuelta ya existía: publica eventos en `logistica.exchange` y Donaciones los consume. Para un proveedor HTTP hacía falta algo más, y la primera versión del broker (Etapas 5 y 6, commits `48f334ad` y `c38be9e8`) agregó un **callback HTTP** en Donaciones, `POST /api/logistica/proveedores/{proveedorId}/avisos`, protegido con una API key por proveedor y con verificación de que la donación le pertenece.

Al revisar esa versión contra el enunciado aparecieron dos problemas:

1. **El callback contradice la letra del enunciado.** `[DOCUMENTED]` Enunciado de la Entrega 4 (`docs/entregas/4/Enunciado-4.pdf`), pág. 22, requerimientos de implementación, ítem 3: *«El servicio de logística no debe invocar los servicios de donaciones ni incentivos sino dejar disponible la información»*. Un proveedor que llama a un endpoint de Donaciones lo invoca. La pág. 24 llama «servicio de logística» también al «otro servicio potencial», así que no se puede asegurar que la regla alcance solo al nuestro. El plan del broker había citado la regla únicamente para justificar que el broker viva en Donaciones y no la había contrastado con el callback.
2. **La vuelta por mensajería no tenía identidad.** `[OBSERVED]` Los eventos de vuelta no llevan el proveedor y `LogisticaEventListener` no verificaba quién los publicó. Cualquiera que pudiera publicar en `logistica.exchange` podía avanzar el estado de una donación. Era una limitación que el callback no tenía (verificaba la pertenencia) y que pasaba a ser central si la vuelta es solo por mensajería (`DEUDA_TECNICA.md`, DTI-14, ítem 7).

Al probar con eventos reales apareció además un tercer problema `[OBSERVED]`: nuestra Logística publicaba sus eventos con el nombre de su clase en el header `__TypeId__` (`grupo5.logistica.dto.eventos.EventoRutaAsignada`), que Donaciones no conoce, por lo que todos fallaban en la conversión del mensaje. Ya ocurría antes del broker y nada lo detectaba porque el E2E del Gate 4 avanza los estados llamando directo a Donaciones.

## Atributos de Calidad y Drivers de Decisión

* **Cumplimiento del enunciado:** los proveedores no invocan a Donaciones; «dejan disponible la información».
* **Integridad:** un proveedor solo puede avanzar el estado de las donaciones que el broker le asignó.
* **Simplicidad:** un único camino de vuelta, con una sola lógica idempotente.
* **Desacoplamiento:** coherente con la mensajería asincrónica de [20260911](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md).
* **Costo de implementación y operación acotado al plazo del TP**, sin dependencias nuevas ([AGENTS.md](../../AGENTS.md) §6).
* **Evolución posible hacia un entorno real,** sin rehacer el contrato.

## Relaciones Arquitectónicas

1. **Refina:** [20261007-broker-de-integracion-con-logistica](./20261007-broker-de-integracion-con-logistica.md), decisión 7 (camino de vuelta). Ese ADR remite a este para el detalle.
2. **Aplica:** el requerimiento de implementación 3 del enunciado de la Entrega 4 (pág. 22), y [20260919-convencion-canonica-identificadores-y-contratos-amqp](./20260919-convencion-canonica-identificadores-y-contratos-amqp.md) (el tipo del mensaje es el alias de la routing key).
3. **Se apoya en:** [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) (un TopicExchange por productor; `logistica.exchange` es el de Logística).
4. **Deuda relacionada:** [`DEUDA_TECNICA.md`](./DEUDA_TECNICA.md), DTI-14, ítem 7.

## Alternativas Consideradas

### Cómo vuelve la información

| Alternativa | Veredicto |
|---|---|
| **A. Todo proveedor informa publicando en `logistica.exchange`; HTTP solo de ida** | ✅ **Elegida.** Cumple el enunciado, deja un único camino de vuelta y reutiliza el procesador idempotente ya probado. |
| Callback HTTP: el proveedor avisa a un endpoint de Donaciones (API key por proveedor) | ❌ Se implementó y se quitó. Es la opción más natural para un tercero HTTP, pero contradice la letra del requerimiento 3. |
| B. Polling: Donaciones consulta el estado al proveedor | ⏳ No se implementó. Cumple el enunciado al pie de la letra y es lo más realista para un tercero que solo hable HTTP, pero exige un método de consulta por adapter, un traductor de los estados de cada proveedor a nuestros eventos, un planificador y un estado final en la solicitud (estimado: una jornada). Es la evolución natural. |
| C. Mover el callback a nuestra Logística, que publica los eventos | ❌ Respeta la regla, pero toca otro servicio y la verificación de pertenencia queda del lado equivocado. |
| D. Mantener el callback defendiendo que la regla aplica solo a nuestro servicio | ❌ La p. 24 usa «servicio de logística» también para el proveedor potencial; la defensa depende de una interpretación. |
| E. Quitar también el HTTP de ida y dejar solo AMQP | ❌ Pierde el segundo transporte, que es lo que muestra el patrón Adapter. |

### Cómo se identifica quién publicó un evento de vuelta

| Alternativa | Veredicto |
|---|---|
| Nada (confiar en quien publique en el exchange) | ❌ Cualquier publicador puede avanzar el estado de una donación. |
| Un usuario de RabbitMQ por proveedor, la propiedad `user_id` del mensaje (RabbitMQ la valida contra el usuario de la conexión) y topic permissions de publicación | ⏳ **La solución sólida para un entorno real**, porque la identidad la garantiza el broker. No se implementó: exige administrar usuarios y permisos de RabbitMQ y que cada emisor setee `user_id`. Queda como evolución. |
| El proveedor en la routing key (`ruta.asignada.<proveedorId>`) | ❌ Cambia el contrato y los bindings; sin permisos por routing key solo lo declara el emisor. |
| Un campo `proveedorId` en el payload | ❌ Lo declara el propio emisor: solo no prueba nada. |
| Una cola o exchange por proveedor (la identidad es el canal) | ❌ Más infraestructura; el ADR del broker ya descartó un exchange por proveedor para los comandos. |
| **Id y token por proveedor en headers del mensaje (`X-Proveedor-Id`, `X-Proveedor-Token`), verificados en Donaciones** | ✅ **Elegida.** No exige configurar RabbitMQ, reutiliza la comparación de claves del `ApiKeyFilter` y es una protección acotada al alcance del TP. |
| Firma HMAC del cuerpo con un secreto por proveedor | ⏳ Más robusta que un token (no expone el secreto en el mensaje), pero exige serializar de forma canónica, proteger contra repeticiones y gestionar secretos. Queda como evolución. |

## Resultado de la Decisión

1. **La vuelta es siempre por mensajería.** Todo proveedor, AMQP o HTTP, informa publicando en `logistica.exchange` los eventos existentes: `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa` y `entrega.fallida`. HTTP se usa solo de ida (Donaciones → proveedor). Un proveedor no invoca a Donaciones por HTTP. Es un **requisito de contrato** para todo proveedor, igual que la idempotencia por `donacionIndependienteId`.
2. **El tipo del mensaje es el alias de la routing key.** El header `__TypeId__` vale `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa` o `entrega.fallida`, no el nombre de la clase del emisor. Donaciones solo resuelve esos alias y descarta con un error de conversión cualquier otro valor.
3. **Cada evento lleva la identidad de su emisor** en dos headers:

   | Header | Valor |
   |---|---|
   | `X-Proveedor-Id` | Id del proveedor (el de `donatrack.logistica.proveedores`) |
   | `X-Proveedor-Token` | Token propio de ese proveedor, acordado con Donaciones (`donatrack.logistica.proveedor.<id>.token-vuelta`) |

4. **Verificación en Donaciones** (`LogisticaEventListener` → `VerificadorOrigenEventos` → `ProcesadorEventosLogistica`):
   * se compara el token con el configurado para ese id mediante una comparación que no depende de cuánto coincidan (hash SHA-256 y `MessageDigest.isEqual`, clase `ClaveSegura`, compartida con el `ApiKeyFilter`);
   * se comprueba en el registro de solicitudes del broker que cada donación del evento esté asignada a ese proveedor; en `ruta.iniciada` se verifican todas las de la lista;
   * solo entonces se aplica el cambio de estado, con la lógica idempotente existente (`(tipo, businessId, donación)`).
5. **Falla cerrado.** Un evento sin id, con un id de caracteres no permitidos, de un proveedor sin token configurado, con token ausente o incorrecto, o sobre una donación ajena se **descarta con un aviso en el log**. El token nunca se loguea.
6. **Nuestra Logística se identifica igual que cualquier proveedor:** `logistica-service` firma sus cuatro eventos con `logistica.instancia-id` y `logistica.token-vuelta`, y publica `__TypeId__` con el alias de la routing key.
7. **Configuración.** Donaciones: `LOGISTICA_TOKEN_DONATRACK` y `LOGISTICA_TOKEN_EXTERNO`. Logística: `LOGISTICA_TOKEN_VUELTA`. Los composes de desarrollo y pruebas traen un valor sintético marcado como de uso exclusivo para desarrollo; el compose de la demo los exige por variable de entorno. Nunca van en el código ni en los tests.

## Consecuencias Positivas

* Se cumple la letra del requerimiento 3: ningún proveedor invoca a Donaciones.
* Un único camino de vuelta, con una sola lógica idempotente; se borraron el controller del callback, su servicio, sus DTOs y la clave por proveedor del filtro.
* Un proveedor solo puede avanzar las donaciones que el broker le asignó, y un evento sin identidad válida no cambia nada.
* La identidad es explícita y probada con eventos reales de nuestra Logística de punta a punta.

## Consecuencias Negativas

* **Un proveedor que solo hable HTTP no puede integrarse hoy:** tiene que poder publicar en nuestro RabbitMQ (credenciales, acceso de red y permisos). La evolución es el polling (alternativa B).
* **El token es una protección mínima.** Viaja dentro del mensaje, así que lo ve cualquiera que pueda consumir de `logistica.exchange`; no hay rotación, ni firma del cuerpo, ni protección contra repeticiones, y vive en variables de entorno. En un entorno real corresponde un usuario de RabbitMQ por proveedor con `user_id` y topic permissions de publicación (`DEUDA_TECNICA.md`, DTI-14, ítems 5 y 7).
* **Un evento descartado no se reintenta ni va a una cola de errores:** se pierde con un aviso en el log (DTI-14, ítem 8 sobre eventos de vuelta que fallan al aplicarse).
* **El contrato exige más a los proveedores:** alias de `__TypeId__`, dos headers y publicar en nuestro exchange. Se documenta en `docs/arquitectura/eventos-amqp.md`, sección B3.
* **Se modificó código de otra área** (`logistica-service`: el publicador de eventos y su mapeo de alias), porque sin eso Donaciones descartaría los eventos de nuestra propia Logística.
* **Lectura del enunciado sin confirmar.** Se eligió la opción más conservadora para no depender de la interpretación del ítem 3; si la cátedra confirmara que la regla no alcanza a un proveedor externo, el callback podría recuperarse del historial de Git.

## Validación

1. Tests de `VerificadorOrigenEventos` (id, token, token de otro proveedor, id con caracteres raros, proveedor sin token, donación ajena, donación sin solicitud, lista con una donación ajena, solicitud fallida del mismo proveedor), de `ClaveSegura`, de `LogisticaEventListener` (evento válido delega; evento inválido no procesa nada) y del filtro de API key de administración.
2. Tests de `logistica-service`: `LogisticaEventPublisher` agrega los headers a los cuatro eventos y no manda un token vacío; el `classMapper` publica los cuatro alias de la routing key.
3. Demo con la colección `postman/flujo-9-broker-logistica.json` y `docker-compose.demo.yml`:
   * carpeta **F:** un evento sin token y un evento de `externo` sobre una donación de `donatrack` se descartan y la donación no cambia; el evento de `externo` con su token se acepta;
   * carpeta **H:** con flota cargada y la planificación disparada, los eventos reales de nuestra Logística (`ruta.asignada`, `ruta.iniciada` y `entrega.exitosa`) llegan firmados y la donación pasa por `ListaParaEntregar`, `EnTraslado` y `Entregada`.
4. Gate 4 (`integration-tests`): 26 de 26 tras el cambio.
5. `[A VERIFICAR]` `entrega.fallida` de punta a punta, y la lectura del ítem 3 del enunciado con la cátedra.
6. Implementación de referencia: [`bitacora-broker-logistica.md`](../entrega-4/donaciones/bitacora-broker-logistica.md) (D50, D51 y D52) y [`plan-broker-logistica.md`](../entrega-4/donaciones/plan-broker-logistica.md), §2.5.
