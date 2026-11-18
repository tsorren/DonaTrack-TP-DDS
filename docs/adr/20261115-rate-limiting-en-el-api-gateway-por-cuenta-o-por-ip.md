# Rate limiting en el api-gateway, por cuenta o por IP

- Status: proposed
- Date: 2026-11-15
- Actualizado: 2026-11-17 (contadores en Redis y *fail-open*, decisiones Q11 a Q11c del grupo); 2026-11-18 (revisión de simplificación: los cuatro cupos quedan, E7-S02; detrás del *routing mesh* de Swarm, sin proxy, E7-S02b; un escenario de k6, E7-S02c)
- Deciders: pendiente de aprobación del equipo en el PR (propuesta y construcción asistidas por agente)
- Tags: api-gateway, seguridad, rate-limiting, cliente-liviano, entrega-7

> **Movido al PR de los ADR de E7 (A0) el 2026-11-17.** Nació en el PR 986, junto con el código, y
> se mudó para que los cuatro ADR de E7 se revisen juntos (Q16b). `[OBSERVED]` El código que nombra
> (`LimiteDeSolicitudes`, `CubetasEnRedis`, `ERR-CSR-429`, la página `error/429`) llega con el
> PR 986, que se integra **después** de este ADR; donde el texto describe ese código en presente,
> describe la implementación del PR 986, no lo que hay hoy en `ENTREGA_7`. Código existente no es
> aprobación: si el revisor rechaza el ADR, el PR 986 cambia.

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 3 de la Entrega 7 pide "un sistema de limitación de solicitudes (Rate
Limiting) que regule el tráfico entrante, protegiendo los recursos del sistema frente a abusos o
sobrecarga". El [ADR-E6-02](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
ya había dejado el gateway como "el lugar para el rate limiting de E7", y
[requerimientos-e6.md](../requerimientos/requerimientos-e6.md) lo anotó como pendiente de E7.

`[OBSERVED]` Hasta ahora el único límite es el del contacto público en Notificaciones (HB-21, por IP,
`ERR-EST-903`). El resto de las rutas no tenía tope.

`[OBSERVED]` Lo que condiciona el diseño:

1. **Todo el tráfico web llega desde el cliente liviano.** El navegador le habla al cliente liviano y
   este llama al gateway desde el servidor. Para el gateway, todos los visitantes tienen la misma IP:
   la del contenedor del cliente liviano.
2. **Las llamadas con sesión llevan el token del usuario** (`tokenDelUsuario` en `ClientesHttp`); las
   públicas (portada, mapa, organizaciones, contacto) van sin token.
3. **La validación del JWT está apagada por defecto** (`GATEWAY_SEGURIDAD_HABILITADA=false`). Con ella
   apagada, un token no prueba nada.
4. **El mapa público consulta cada 30 s** ([ADR del mapa](./20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)):
   con 200 mapas abiertos son ~7 consultas por segundo a Logística, en total.

Problema: ¿dónde y por qué clave se cuentan los pedidos, para frenar a quien abusa sin frenar a los
demás visitantes que comparten la IP del cliente liviano?

`[DOCUMENTED]` El grupo decidió el 16/11 ([requerimientos-e7.md](../requerimientos/requerimientos-e7.md),
E7-Q11 a Q11c) que los contadores vivan en un Redis compartido, que el gateway deje pasar los pedidos
si Redis no responde (*fail-open*) y que el gateway corra con dos réplicas.

**Gate A:** decisión nueva (no había límite general) que suma dependencias (Bucket4j y Lettuce) y un
contenedor (Redis). **Gate B:** seguridad, contrato visible (una respuesta `429` nueva en todas las
rutas) y un componente de infraestructura nuevo del que depende el gateway.

## Alternativas Consideradas

1. **En el gateway, una cubeta por cuenta (el `sub` del token validado) o, sin token validado, por IP
   de origen**; el cliente liviano informa la IP del navegador en `X-Forwarded-For` y el gateway le
   cree solo a proxies configurados (elegida).
2. **En el gateway, solo por IP de la conexión** (como MetaMapa). Con el cliente liviano delante,
   todos los visitantes compartirían una cubeta: uno solo que abusa deja sin servicio a todos.
3. **En cada servicio**, con un interceptor propio. Repite la lógica cuatro veces y el pedido ya
   consumió un hilo del servicio cuando se rechaza; contradice al ADR-E6-02.
4. **El `RequestRateLimiter` de Spring Cloud Gateway.** Es del gateway reactivo (WebFlux) y necesita
   Redis; DonaTrack usa el gateway sobre WebMVC (`spring-cloud-starter-gateway-server-webmvc`).
5. **El filtro `Bucket4jFilterFunctions` del gateway WebMVC.** `[OBSERVED]` Lo trae
   `spring-cloud-gateway-server-webmvc` 5.0.1 (capacidad, período, resolvedor de clave, código de
   estado, encabezado y *timeout*).
6. **Un token bucket propio, sin librería.** Es poco código, pero Bucket4j ya resuelve la recarga
   gradual, la concurrencia sin bloqueos y, si hiciera falta, un almacenamiento compartido.

### Dónde viven los contadores (Q11)

| Opción | Disponibilidad | Exactitud con 2 réplicas | Costo |
|---|---|---|---|
| **Redis compartido** (elegida, Q11 B) | depende de Redis; se cubre con *fail-open* | un solo cupo | un contenedor y dos dependencias |
| En memoria de cada gateway | no depende de nada | el cupo se duplica | ninguno |
| La base de datos (Postgres) | depende de la base | un solo cupo | escrituras en cada pedido sobre una base que no es del gateway |

### Si Redis no responde (Q11b)

| Opción | Disponibilidad del sitio | Protección durante la caída |
|---|---|---|
| **Deja pasar (*fail-open*) con aviso** (elegida) | intacta | ninguna |
| Rechaza todo (*fail-closed*) | Redis pasa a ser punto único de falla | total |
| Cae a un contador local | intacta | parcial e inexacta, y más código |

## Resultado de la Decisión

Elegida la alternativa 1, con Bucket4j (`com.bucket4j:bucket4j_jdk17-core` y
`bucket4j_jdk17-lettuce`) y los contadores en un Redis compartido, con *fail-open*.

- **Dónde.** `LimiteDeSolicitudes` es un `HandlerFilterFunction` que va primero en cada ruta de
  `RutasGateway`: un pedido sin ficha no llega al servicio. `/actuator/**` (los healthchecks) no pasa
  por ahí.
- **De quién es la cubeta.** De la cuenta (`usuario:<sub>`) si la cadena de seguridad validó el token;
  si no, de la IP de origen (`ip:<ip>`). Un token sin validar no cuenta: cualquiera podría inventar
  un `sub` distinto por pedido.
- **La IP de origen.** La de la conexión, salvo que venga de un proxy confiable
  (`donatrack.gateway.limite-de-solicitudes.proxies-confiables`, una regex): ahí es la primera de
  `X-Forwarded-For`, recortada a 64 caracteres. Vacía por defecto en `application.yml`: el
  encabezado se ignora. Los compose del PR 986 la llenan con las redes privadas, de donde llega el
  cliente liviano.
- **Detrás de Swarm (E7-S02b A).** El cliente liviano se publica en el *routing mesh*, sin proxy
  delante; si el gateway también se publica queda abierto (ver "Riesgo de falsificación" y el
  [ADR de despliegue en Docker Swarm](./20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md)).
  - `[VERIFIED]` En la prueba de Swarm, el mesh presentó todos los pedidos al gateway con la IP de
    su `ingress-endpoint` (`10.0.0.2`). `[INFERRED]` El cliente liviano, publicado igual, recibe esa
    misma IP y la reenvía (no se probó con el cliente), así que **todos los visitantes sin cuenta
    comparten la cubeta por IP**: 120 lecturas y 30 escrituras por minuto en
    total, o sea unas 12 pantallas por minuto si cada una hace ~10 lecturas. Las cuentas con token
    validado no se ven afectadas: cada una tiene su cubeta. Para los visitantes anónimos, en Swarm,
    la alternativa elegida se comporta como la 2 (solo por IP), que se descartó por eso; el grupo lo
    aceptó en E7-S02b A a cambio de no sumar un proxy que, en Docker Desktop, tampoco daba la IP
    real.
  - **Riesgo de falsificación.** `[OBSERVED]` La regex que ponen los compose del PR 986
    (`GATEWAY_LIMITE_PROXIES_CONFIABLES`) confía en `10.*`. `[INFERRED]` Eso incluye la dirección
    del mesh: quien llegue al puerto publicado del gateway puede elegir su cubeta con
    `X-Forwarded-For`. `[PROPOSED]` En el stack de Swarm, no publicar el gateway (el cliente liviano
    le habla por la red interna) o limitar `proxies-confiables` a la subred de la red de la
    aplicación, que excluye la del mesh. Lo decide la tarea del override de Swarm.
- **El cliente liviano manda la IP del navegador** en `X-Forwarded-For` en todas las llamadas
  (`ClientesHttp`), sin pisar la que ya pone el contacto.
- **Cupos.** Lecturas (`GET`, `HEAD`, `OPTIONS`) y escrituras por separado, por ventana de 1 minuto,
  con recarga gradual (greedy): por cuenta, 300 lecturas y 60 escrituras; por IP, 120 y 30. Todo se
  cambia por variables de entorno (`GATEWAY_LIMITE_*`) y se apaga con `GATEWAY_LIMITE_HABILITADO`.
- **La respuesta.** `429` con el `ErrorResponse` común (`ERR-CSR-429`, `DEMASIADAS_SOLICITUDES`, nuevo
  en `ErrorCatalog`), `Retry-After` en segundos y el `X-Trace-Id`. Las respuestas que pasan llevan
  `X-RateLimit-Remaining`. El rechazo deja un log `[GATEWAY] [RATE-LIMIT]` con el tipo de cubeta, sin
  la IP ni el `sub`.
- **El cliente liviano** muestra su página `error/429` ("Hubo demasiados pedidos seguidos") en vez de
  la de falla nuestra (502). El contacto sigue mostrando su mensaje de límite propio.
- **Dónde viven las cubetas.** En Redis (`CubetasEnRedis`, cliente Lettuce, algoritmo
  compare-and-swap de Bucket4j), con la clave `rate-limit:<usuario|ip>:…|<lectura|escritura>`.
  Todas las réplicas del gateway cuentan contra el mismo cupo. Redis corre sin persistencia (`--save ""`,
  sin AOF): los contadores son descartables. Cada clave vence 10 s después de que su cubeta se
  recargó entera.
- ***Fail-open*.** Si Redis falla o no contesta dentro de `GATEWAY_REDIS_TIMEOUT` (100 ms por
  defecto), el pedido pasa sin consumir ficha y queda un `WARN` `[REDIS-NO-DISPONIBLE]`, uno por
  ventana como máximo. El gateway se conecta con el primer pedido, así arranca aunque Redis esté
  caído; si no conecta, no reintenta hasta pasado `GATEWAY_REDIS_REINTENTO` (5 s), y mientras tanto
  los pedidos pasan sin esperar. Ya conectado, Lettuce se reconecta solo y, desconectado, rechaza
  los comandos al instante. En Compose el gateway no espera a Redis (`depends_on`) y no expone su
  salud: una caída de Redis no reinicia el gateway.

## Análisis por alternativa

**1. Por cuenta o por IP, en el gateway (elegida)**
- A favor: separa a los visitantes aunque todos lleguen por el cliente liviano, siempre que la IP
  real llegue al cliente (`[INFERRED]` en un Linux con Docker nativo sí; en Docker Desktop la
  prueba de Swarm vio una sola IP para todos los pedidos de la PC; detrás del *routing mesh*, no); un solo punto,
  antes de los servicios; la cuenta solo cuenta con un token validado, así que no se puede inventar.
- En contra: depende de que el cliente liviano informe la IP y de configurar bien los proxies
  confiables; con la validación del JWT apagada, todo se cuenta por IP; en Swarm, los anónimos
  comparten una cubeta.

**2. Solo por IP de la conexión**
- A favor: lo más simple; no confía en ningún encabezado.
- En contra: detrás del cliente liviano todos comparten la misma IP y una cubeta: un abuso deja sin
  servicio al resto.

**3. En cada servicio**
- A favor: protege también el tráfico interno.
- En contra: repite la lógica en cuatro servicios; el pedido rechazado ya ocupó un hilo del
  servicio; contradice al ADR-E6-02, que puso al gateway como lugar del límite.

**4. `RequestRateLimiter` de Spring Cloud Gateway**
- A favor: viene hecho y usa Redis.
- En contra: es de la variante reactiva (WebFlux), y DonaTrack usa la de WebMVC.

**5. `Bucket4jFilterFunctions` del gateway**
- A favor: viene en el jar que ya se usa y se configura por ruta.
- En contra `[INFERRED]` (no se comparó contra su código): la implementación necesita dos cupos por
  cubeta según el método, la clave por cuenta validada o por IP de un proxy confiable, el *fail-open*
  con aviso limitado y el `429` con el `ErrorResponse` común y `Retry-After` (Q13 ya anotaba que ese
  encabezado probablemente había que agregarlo a mano). El filtro propio usa la misma librería
  Bucket4j; si en la revisión el filtro del gateway resulta cubrir todo eso, conviene reemplazarlo.

**6. Token bucket propio**
- A favor: sin dependencias.
- En contra: hay que escribir y probar la recarga, la concurrencia y el almacenamiento compartido,
  que Bucket4j ya resuelve.

**Principios.** El límite es una preocupación transversal y vive en un filtro del borde, no en el
dominio (separación de responsabilidades). El almacenamiento de las cubetas está detrás de una
abstracción (`CubetasEnRedis`, y `CubetasEnMemoria` en los tests), así que cambiar Redis por otro
almacenamiento no toca el filtro (inversión de dependencias).

## Consecuencias

- **Positivas.** Una cuenta que abusa recibe `429` sin afectar a las demás, aunque todas lleguen
  por el cliente liviano. Con la IP real (`[INFERRED]` un Linux con Docker nativo sin mesh), lo
  mismo vale para un visitante anónimo; detrás del *routing mesh* de Swarm, no (ver "Negativas"). Los servicios no ven los pedidos
  rechazados. Los valores se
  ajustan sin recompilar.
- **Negativas.**
  - Mientras Redis está caído no hay límite: un abuso en ese lapso llega a los servicios (riesgo
    aceptado en Q11b). La alerta del panel (métrica M25) es de las tareas R3 y O4.
  - Cada pedido limitado suma un viaje a Redis (hasta el *timeout* si Redis está lento).
  - Un contenedor más (Redis) en Compose y en el stack de Swarm.
  - Detrás del *routing mesh*, el límite por IP es uno solo para todos los visitantes sin cuenta
    (ver "Detrás de Swarm").
  - Con la validación del JWT apagada (el valor por defecto), las cuentas con sesión se cuentan por la
    IP del navegador y no por cuenta.
  - En una máquina de desarrollo, Docker puede presentar las conexiones desde el host (Postman a
    `localhost:8086`) con una IP de la red interna: esas sí pueden elegir su IP con `X-Forwarded-For`.
    En un despliegue real el gateway no debería publicarse fuera de la red interna, o
    `proxies-confiables` debería apuntar solo a la IP del cliente liviano.
  - Dependencias nuevas: Bucket4j y su módulo de Lettuce; `lettuce-core` lo versiona el BOM de
    Spring Boot.
- **Divergencia con Q12, resuelta en E7-S02 A** `[DOCUMENTED]`. El grupo había fijado seis clases
  de límite (público por IP, lectura y escritura autenticadas, subidas pesadas, seguimiento del
  camión aparte y GraphQL). La revisión de simplificación se quedó con los cuatro cupos de este PR
  y con sus valores:
  - el seguimiento del camión (2 consultas por minuto) entra en el cupo de lectura, así que queda
    contado, como pide el ADR-E6-07;
  - las subidas pesadas entran en escrituras;
  - GraphQL entra en lecturas, y su costo lo frenan los límites dentro del servicio (ver el
    [ADR de GraphQL](./donaciones-service/20261117-graphql-de-solo-lectura-en-donaciones.md)).

  Cuenta por cuenta solo con el token **validado**, una condición de seguridad que Q12 no
  nombraba. Los tiempos de Redis (100 ms, 5 s y el vencimiento de 10 s) son valores de la
  implementación; Q11b solo pidió un *timeout* corto.
- **Prueba con k6 (E7-S02c A).** Un solo escenario que muestra tres cosas: el tráfico normal pasa,
  una ráfaga recibe `429`, y el cupo se cuenta entre las dos réplicas del gateway (Redis). Los
  valores no se calibran.
- **A vigilar.** Los cupos son una estimación (una pantalla hace hasta ~10 lecturas). Con las métricas
  de E7 (M24, requerimiento 1) conviene mirar cuántos `429` hay y ajustarlos.

## Referencias

- [ADR-E6-02: api-gateway](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
- [Requerimientos de E7](../requerimientos/requerimientos-e7.md), E7-Q11 a Q11c y §5 (E7-S02 a E7-S02c)
- [Catálogo de errores](../arquitectura/catalogo-errores.md) (`ERR-CSR-429`, que suma el PR 986)
- [api-gateway/README.md](../../api-gateway/README.md), sección "Rate limiting" (llega con el PR 986)
- [ADR de despliegue en Docker Swarm](./20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md) (dos réplicas del gateway y la IP detrás del mesh) y [ADR de observabilidad](./20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md) (M24 y M25)
- Inspiración: el rate limiting por IP con Bucket4j de MetaMapa (TP 2025), adaptado al cliente liviano.
