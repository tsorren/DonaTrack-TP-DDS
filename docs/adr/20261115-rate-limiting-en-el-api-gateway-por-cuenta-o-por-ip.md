# Rate limiting en el api-gateway, por cuenta o por IP

- Status: proposed
- Date: 2026-11-15
- Deciders: pendiente de aprobación del equipo en el PR (propuesta y construcción asistidas por agente)
- Tags: api-gateway, seguridad, rate-limiting, cliente-liviano, entrega-7

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

**Gate A:** decisión nueva (no había límite general) que suma una dependencia (Bucket4j). **Gate B:**
seguridad y contrato visible (una respuesta `429` nueva en todas las rutas).

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
5. **Un token bucket propio, sin librería.** Es poco código, pero Bucket4j ya resuelve la recarga
   gradual, la concurrencia sin bloqueos y, si hiciera falta, un almacenamiento compartido.

## Decisión

Elegida la alternativa 1, con Bucket4j (`com.bucket4j:bucket4j_jdk17-core`), en memoria.

- **Dónde.** `LimiteDeSolicitudes` es un `HandlerFilterFunction` que va primero en cada ruta de
  `RutasGateway`: un pedido sin ficha no llega al servicio. `/actuator/**` (los healthchecks) no pasa
  por ahí.
- **De quién es la cubeta.** De la cuenta (`usuario:<sub>`) si la cadena de seguridad validó el token;
  si no, de la IP de origen (`ip:<ip>`). Un token sin validar no cuenta: cualquiera podría inventar
  un `sub` distinto por pedido.
- **La IP de origen.** La de la conexión, salvo que venga de un proxy confiable
  (`donatrack.gateway.limite-de-solicitudes.proxies-confiables`, una regex): ahí es la primera de
  `X-Forwarded-For`, recortada a 64 caracteres. Vacía por defecto: el encabezado se ignora. En Docker
  Compose se confía en las redes privadas, de donde llega el cliente liviano.
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
- **Memoria.** Una cubeta que pasó una ventana sin uso ya se recargó entera, así que se borra; la
  limpieza corre una vez por ventana.

## Consecuencias

- **Positivas.** Un visitante o una cuenta que abusa recibe `429` sin afectar a los demás, aunque
  todos lleguen por el cliente liviano. Los servicios no ven los pedidos rechazados. Los valores se
  ajustan sin recompilar.
- **Negativas.**
  - Las cubetas viven en la memoria de cada gateway: con más de una instancia, cada una cuenta lo
    suyo (el cupo real se multiplica por la cantidad de instancias). Si se escala el gateway, pasar a
    un backend compartido de Bucket4j (Redis o la base) sin cambiar la interfaz.
  - Con la validación del JWT apagada (el valor por defecto), las cuentas con sesión se cuentan por la
    IP del navegador y no por cuenta.
  - En una máquina de desarrollo, Docker puede presentar las conexiones desde el host (Postman a
    `localhost:8086`) con una IP de la red interna: esas sí pueden elegir su IP con `X-Forwarded-For`.
    En un despliegue real el gateway no debería publicarse fuera de la red interna, o
    `proxies-confiables` debería apuntar solo a la IP del cliente liviano.
  - Una dependencia nueva (Bucket4j, sin dependencias transitivas).
- **A vigilar.** Los cupos son una estimación (una pantalla hace hasta ~10 lecturas). Con las métricas
  de E7 (requerimiento 1) conviene mirar cuántos `429` hay y ajustarlos.

## Referencias

- [ADR-E6-02: api-gateway](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
- [Catálogo de errores](../arquitectura/catalogo-errores.md) (`ERR-CSR-429`)
- [api-gateway/README.md](../../api-gateway/README.md), sección "Rate limiting"
- Inspiración: el rate limiting por IP con Bucket4j de MetaMapa (TP 2025), adaptado al cliente liviano.
