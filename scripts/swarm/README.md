# Stack de Docker Swarm de un nodo

Despliegue de la defensa y la prueba final (E7). Decisión: ADR `docs/adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md`.
Compose sigue siendo el entorno de desarrollo, del CI y de la demo pública.

| Archivo | Qué hace |
|---|---|
| `generar-stack.js` | Pasa `docker compose config --format json` del preprod a un archivo que acepta `docker stack` |
| `generar-stack.test.js` | Un test por regla de la transformación, sobre `ejemplo-compose.json` (datos sintéticos) |
| `desplegar.sh` | Exporta el `.env`, genera `target/swarm/stack.json` y despliega con `docker-stack.override.yml` |
| `verificar-replicas.sh` | SW2: despliega con dos réplicas, espera a que converja y deja la evidencia en `target/swarm/evidencia-replicas.md` |
| `probar-rate-limit.sh` | R4: corre el escenario de k6 del rate limiting contra el stack desplegado y deja `target/swarm/evidencia-rate-limit.md` |
| `humo-jwt.sh` | S3 y S4: prueba de humo de la cadena JWT con el Keycloak real (401, 200 y 403) y de los encabezados de seguridad del gateway en cada réplica; evidencia en `target/swarm/evidencia-humo-jwt.md` |
| `caos.sh` | K6: rompe una réplica (congelada o muerta), espera a que Swarm la reemplace y deja la evidencia en `target/swarm/` |
| `../../docker-stack.override.yml` | Réplicas, límites de memoria, `hostname` de mongodb, healthchecks y `minio-init` |

## Por qué hay una transformación

`docker stack config` no lee el `.env` ni acepta el compose del preprod ni la salida de `docker compose config`
(prueba de Swarm del 2026-11-18, `[VERIFIED]`). `docker stack deploy` no se probó con esos archivos: que use la
misma validación es `[INFERRED]` (ADR). `generar-stack.js`, en todos los servicios:

- borra `name` de primer nivel y pone `version: "3.9"`;
- borra `build`, `depends_on`, `container_name`, `pull_policy`, los campos en `null` y `healthcheck.start_interval`;
- deja afuera los servicios con `profiles` (k6);
- pasa `published` a número y borra `host_ip` (Swarm no publica solo en `127.0.0.1`);
- deja publicados solo los puertos de `PUERTOS_PUBLICADOS`: el cliente liviano (8090), Keycloak (8180, el
  navegador inicia sesión ahí) y Grafana en otel-lgtm (3000). RabbitMQ (5672), solo con `DONATRACK_PUBLICAR_RABBITMQ=true`;
- pone las redes como `overlay` y `attachable`.

Un override no puede sacar puertos ni campos del archivo base: por eso lo hace la transformación.
Tests: `node --test scripts/swarm/*.test.js`.

## Uso

```bash
# Una vez: Swarm de un nodo, jars e imágenes propias, e imágenes de infraestructura locales
docker swarm init
mvn clean package -DskipTests
docker compose -f docker-compose.preprod.yml build
docker compose -f docker-compose.preprod.yml pull --ignore-buildable   # sin esto, el deploy falló con "context deadline exceeded"
# (en la prueba se bajaron solo las imágenes de infraestructura; --ignore-buildable saltea las propias)

# Desplegar (o actualizar: se corre de nuevo)
DONATRACK_REPLICAS=2 scripts/swarm/desplegar.sh

# Solo generar target/swarm/stack.json y validarlo
scripts/swarm/desplegar.sh --solo-generar
docker stack config -c target/swarm/stack.json -c docker-stack.override.yml

# Estado y borrado
docker service ls --filter name=donatrack
docker stack rm donatrack
docker swarm leave --force        # si no se va a usar más
```

`target/swarm/stack.json` tiene los secretos del `.env` interpolados: está en `target/`, que ignora git.
No se commitea ni se comparte.

| Variable | Por defecto | Qué hace |
|---|---|---|
| `ENV_FILE` | `.env` de la raíz | `.env` que se exporta y se interpola |
| `DONATRACK_REPLICAS` | 1 | Réplicas del gateway, el cliente liviano y los cuatro servicios (2 en la defensa) |
| `DONATRACK_SEGURIDAD` | `true` | El gateway y los cuatro servicios validan el JWT de Keycloak (E7-S04 A). `false` la apaga. Pisa un `PREPROD_SEGURIDAD` del `.env` |
| `DONATRACK_PUBLICAR_RABBITMQ` | `false` | Publica el 5672 (consumidor espía de las pruebas desde el host) |
| `DONATRACK_BORRAR_COLAS_SAC` | `false` | Lugar del paso que borra las siete colas con *single active consumer* (decisión D8). Hoy solo avisa y corta: el paso lo documenta SL2 en `docs/arquitectura/eventos-amqp.md` |

## Qué está publicado

`[PROPOSED]` Solo el cliente liviano (`http://localhost:8090`), Keycloak (`http://localhost:8180`) y Grafana (`http://localhost:3000`).
`[INFERRED]` Quedan en todas las interfaces: la prueba verificó que Swarm rechaza los puertos atados a
`127.0.0.1`, no en qué interfaces escucha.

`[PROPOSED]` El gateway no se publica (opción del ADR). Motivo:

- `[DOCUMENTED]` Según el ADR, los compose del PR 986 confían en `X-Forwarded-For` si el pedido llega de `10.*`
  (`GATEWAY_LIMITE_PROXIES_CONFIABLES`). Esa variable no está en esta rama.
- `[VERIFIED]` en la prueba: el *routing mesh* cambia la IP de origen por `10.0.0.2`.
- `[INFERRED]` Con el gateway publicado, cualquiera podría elegir su cubeta de rate limiting con ese encabezado.

Los servicios y la infraestructura tampoco se publican. Postman, k6 o `curl` llegan desde un contenedor en la red del stack, que es
`attachable`:

```bash
docker run --rm --network donatrack_preprod-network curlimages/curl -s http://api-gateway:8086/actuator/health
```

## Cadena JWT encendida (S3)

`[PROPOSED]` En Swarm la validación del JWT está **encendida por defecto** (decisión E7-S04 A): `desplegar.sh`
exporta `PREPROD_SEGURIDAD=true` antes de generar el stack. El compose del preprod la sigue leyendo como
`${PREPROD_SEGURIDAD:-false}`, así que el preprod de Compose y el CI no cambian (siguen con `--auth` / `PREPROD_AUTH`).

Con la seguridad encendida, quien llame al gateway o a un servicio sin token recibe `401`: Postman, k6 e
`integration-tests` necesitan `PRUEBAS_CLIENT_SECRET` (SPEC-06 E2-07). El cliente liviano ya manda el token.

```bash
scripts/swarm/humo-jwt.sh                                     # contra el stack que corre
RED=<red-del-preprod> scripts/swarm/humo-jwt.sh               # contra el preprod de Compose (docker network ls)
```

Requisitos: el stack sano (por ejemplo, después de `verificar-replicas.sh`), `curlimages/curl` local (o
`HELPER_IMAGEN`) y `PRUEBAS_CLIENT_SECRET` y `CLIENTE_LIVIANO_SECRET` en el `.env` con el que se importó el realm.

Desde un contenedor en la red del stack pide dos tokens `client_credentials` a `keycloak:8080`: ADMIN
(`donatrack-pruebas`) y SERVICIO (`cliente-liviano`). Por cada servicio, una ruta solo para ADMIN, directo al
servicio y por el gateway:

| Servicio | Ruta | Sin token | ADMIN | SERVICIO |
|---|---|---|---|---|
| Donaciones | `GET /api/personas` | 401 | 200 | 403 |
| Incentivos | `GET /api/incentivos/admin/resumen` | 401 | 200 | 403 |
| Logística | `GET /api/entregas` | 401 | 200 | 403 |
| Notificaciones | `GET /api/notificaciones` | 401 | 200 | 403 |

Además, `GET /api/necesidades/publicas` por el gateway y sin token tiene que dar 200 (la allowlist sigue abierta).
Son 25 comprobaciones de la cadena; S4 les suma los encabezados y las réplicas (abajo). El `200` prueba que el `iss` del token pedido por la red interna coincide con el que
validan los servicios (`${KC_HOSTNAME}/realms/donatrack`). El `403` lo da el `@PreAuthorize` del servicio:
el gateway solo exige un token válido. Los secretos y los tokens viajan por stdin y no quedan en la salida
ni en el informe. Sale con 1 si falla alguna comprobación.

`[VERIFIED]` con un `docker` falso que simula el stack (`scripts/tests/test_swarm_humo_jwt.py`): pasa con la
cadena encendida y falla con la seguridad apagada, con un servicio que no valida el token o si Keycloak no
entregue los tokens. `[VERIFIED]` Contra el Keycloak real del stack (2026-11-24): 25 de 25.

## Encabezados y réplicas del gateway (S4)

`scripts/swarm/humo-jwt.sh` también revisa, en las mismas respuestas, los encabezados de seguridad que pone el
gateway (S1, `api-gateway/README.md`): cada uno **una sola vez** y con su valor.

| Encabezado | Valor |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer` |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` |
| `Strict-Transport-Security` | `max-age=31536000` |
| `Cache-Control` | `no-store`, solo en `/api/**` |

Dónde los busca:

- En las 12 respuestas de las rutas por el gateway: 401, 200 y 403 de cada servicio. Que el 401 los lleve
  prueba que el filtro corre antes que la cadena de Spring Security.
- En la ruta pública (`/api/necesidades/publicas`) y fuera de `/api` (`/actuator/health`). En esta última no
  se exige `Cache-Control`.
- En **cada réplica** del gateway, pedida por su IP en la red del stack: sin token `401` y con el token ADMIN
  `200`, las dos con los encabezados. Por el nombre del servicio, el balanceo elige la réplica y una
  réplica mal configurada podría no aparecer. Si no encuentra contenedores de `${STACK}_api-gateway` (por
  ejemplo, contra el preprod de Compose), lo anota como aviso y no falla.

Los pedidos directos a los servicios no se revisan: los encabezados son del gateway.

Con una réplica son 44 comprobaciones; con `DONATRACK_REPLICAS=2`, 48. Para la defensa:

```bash
DONATRACK_REPLICAS=2 scripts/swarm/verificar-replicas.sh   # despliega con dos réplicas y espera a que converja
scripts/swarm/humo-jwt.sh
```

La lista de encabezados del script la compara un test con la de `EncabezadosDeSeguridad.java`
(`scripts/tests/test_swarm_humo_jwt.py`): si cambia un valor en el gateway, hay que cambiarlo en los dos.

`[VERIFIED]` con el `docker` falso: falla si falta un encabezado, si se repite, si `/api` no trae `no-store`
o si una réplica del gateway no valida el token. `[VERIFIED]` Contra el stack real (2026-11-25): 44 de 44 con una réplica y 48 de 48 con dos (`DONATRACK_REPLICAS=2`).

## Verificación con réplicas (SW2)

```bash
scripts/swarm/verificar-replicas.sh                  # despliega con DONATRACK_REPLICAS=2 y verifica
scripts/swarm/verificar-replicas.sh --sin-desplegar  # verifica el stack que ya corre
```

Requisitos: los mismos de "Uso" (Swarm iniciado, jars, imágenes propias e imágenes de infraestructura locales) y
`curlimages/curl` local (`docker pull curlimages/curl`), o `HELPER_IMAGEN` con otra imagen que tenga `sh` y `curl`.
Espera hasta `ESPERA_MAX` segundos (900 por defecto). Sale con 1 si falla alguna comprobación.

Qué comprueba:

1. Cada servicio llega a sus réplicas en `Running`. Swarm pasa una tarea con healthcheck a `Running` recién
   cuando está sana, así que es lo mismo que "sana". `minio-init` cuenta como lista cuando su tarea quedó `Complete`.
2. Los seis módulos propios declaran `DONATRACK_REPLICAS` réplicas, cada contenedor contesta `/actuator/health`
   por su IP en la red del stack, y el nombre del servicio (la VIP del *routing mesh*) también.
3. La sesión del cliente liviano pasa de una réplica a la otra: la réplica A crea la sesión en
   `/oauth2/authorization/keycloak`, la fila aparece en `cliente.spring_session`, y la réplica B, con esa cookie,
   no manda una cookie nueva (Spring Session solo la manda si no encontró la sesión).
4. Anota cuántas tareas reemplazó Swarm por servicio y el primer motivo, y una muestra de `docker stats` por servicio.

Avisa, sin fallar, si falta ShedLock (SL1) o el *single active consumer* (SL2): con dos réplicas el stack arranca,
pero las tareas `@Scheduled` corren dos veces y las colas pierden el orden. SW2 se cierra con la evidencia de que
arranca; la defensa con dos réplicas necesita SL1 y SL2.

El informe no lleva secretos y se puede pegar en el PR.

`[VERIFIED]` en un Swarm de un nodo con imágenes sustitutas (servidores HTTP con los mismos nombres, puertos,
healthchecks y campos del compose del preprod, y un Postgres falso para la sesión), con el override real:
converge con 2 réplicas, las comprobaciones 1, 3 y 4 dan bien, una réplica con el health en rojo aparece reemplazada
con el motivo `unhealthy container`, y un servicio que no sana sale por tiempo con `FALLA`. La comprobación de la VIP
no se pudo verificar ahí: ese kernel no tiene IPVS. `[INFERRED]` Con las imágenes reales y en Docker Desktop no se
corrió todavía: los tiempos, la memoria y los reemplazos reales salen de esa corrida.

## Rate limiting con dos réplicas (R4)

```bash
scripts/swarm/verificar-replicas.sh   # el stack con dos réplicas, si no está desplegado
docker pull grafana/k6
scripts/swarm/probar-rate-limit.sh
```

Corre [`tests/performance/k6/gateway-rate-limit-replicas.js`](../../tests/performance/k6/gateway-rate-limit-replicas.js)
en un contenedor de k6 dentro de la red del stack. Es el escenario que eligió el grupo en E7-S02c (opción A): demuestra
tres cosas y no calibra los cupos.

1. **El tráfico normal pasa.** Diez clientes, cada uno con su IP, hacen un pedido por segundo durante 30 s, por el
   nombre del servicio (la VIP). Ninguno recibe `429`.
2. **Una ráfaga recibe `429`.** Un cliente hace el cupo más 40 pedidos seguidos. Los últimos reciben `429`.
3. **El cupo se comparte entre las réplicas.** Un cliente hace el doble del cupo alternando entre las dos réplicas,
   por la IP de cada una. Pasa un cupo (más lo que se recarga mientras dura la tanda, con margen de 5) y las dos
   réplicas devuelven `429`. Si cada réplica contara por su lado pasarían todos; el informe lo dice y sugiere mirar
   si Redis contesta (`[REDIS-NO-DISPONIBLE]` en los logs del gateway, *fail-open*).

Los pedidos son `GET /api/categorias` (una lectura, sin efectos) y el cupo es el de lecturas por IP: el script lo lee
de `GATEWAY_LIMITE_LECTURAS_IP` y `GATEWAY_LIMITE_VENTANA` del servicio, o usa los valores por defecto (120 por
minuto). Cada cliente se distingue por `X-Forwarded-For`: k6 corre en la red del stack, que el gateway trata como
proxy confiable (`GATEWAY_LIMITE_PROXIES_CONFIABLES` incluye `10.*`). Las IPs se sortean en cada corrida dentro de
198.19.0.0/16 (rango de pruebas), así que se puede repetir sin esperar a que se vacíen las cubetas.

Cuenta como error, y falla, un `5xx`, un `401` (la seguridad del gateway encendida) o un pedido sin respuesta. Tarda
unos 35 s y sale con el código de k6: 0 si se cumplen las tres.

`[VERIFIED]` En un Swarm de un nodo con un gateway sustituto de dos réplicas (cubetas con recarga gradual, en un
almacén compartido o una por réplica) y k6 1.4.2: con el almacén compartido se cumplen las tres (pasaron 121 de 240,
con `429` en las dos réplicas); con una cubeta por réplica la tercera falla (pasaron 240) y el informe muestra el
diagnóstico. Ahí no hay IPVS, así que las demostraciones 1 y 2 fueron contra una réplica (`GATEWAY_URL`), no por la
VIP. `[INFERRED]` Con el gateway real los pedidos son más lentos, la tanda dura más y el máximo esperado crece con
la recarga: el escenario lo calcula con la duración medida.

`[VERIFIED]` Con el stack real (24/11, Docker Desktop, dos réplicas): con el timeout de Redis por defecto (100 ms) el
gateway no llegaba a resolver `redis` en el DNS de Swarm (`Unable to connect to redis/<unresolved>:6379`), quedaba
en *fail-open* y fallaban la ráfaga y el cupo compartido. Además, las dos réplicas cayeron por `unhealthy` en medio
de la prueba. Por eso el override le pone `GATEWAY_REDIS_TIMEOUT=1s` al gateway: con eso las tres demostraciones
pasan (298 pedidos normales sin `429`, 37 `429` en la ráfaga, 125 de 240 en el cupo compartido) y no hubo caídas.
`[INFERRED]` Las caídas bajo carga en *fail-open* no se repitieron con Redis conectado; no se investigó si el
*fail-open* con carga tumba al gateway.

## Prueba del autorestart (K6)

```bash
scripts/swarm/caos.sh incentivos-service congelar   # docker kill -s STOP: proceso colgado
scripts/swarm/caos.sh incentivos-service matar      # docker kill: proceso muerto
```

Requisitos: el stack desplegado y estable (`scripts/swarm/verificar-replicas.sh --sin-desplegar` en verde),
idealmente con `DONATRACK_REPLICAS=2`, y `curlimages/curl` local (o `HELPER_IMAGEN`). Sirve cualquiera de los seis
módulos propios. Espera hasta `ESPERA_MAX` segundos (300 por defecto). Sale con 1 si falla alguna comprobación y
con 2 si los argumentos o el entorno no sirven.

Qué hace y qué comprueba:

1. Antes: el servicio tiene todas sus réplicas en `Running`. Si no, no rompe nada.
2. Rompe uno de sus contenedores. `congelar` simula un proceso colgado: sigue vivo para Docker, pero no contesta, y
   lo detecta el `healthcheck` (`/actuator/health/liveness`, K3). `matar` simula una caída: el contenedor termina y
   Swarm lo reemplaza por su política de reinicio (`condition: any`).
3. Mide cuándo Swarm sacó la réplica rota y cuándo el servicio volvió a `N/N` con un contenedor nuevo.
4. Mientras tanto pide `/actuator/health/liveness` por el nombre del servicio (la VIP) una vez por segundo, desde
   un contenedor auxiliar en la red del stack. Con `congelar`, hasta que el `healthcheck` marca la réplica, la VIP
   todavía le puede mandar pedidos: esos fallos se informan como aviso, no como falla. Con el servicio recuperado,
   liveness tiene que contestar 10 de 10.

Si el script se corta con la réplica congelada, la descongela al salir. El informe queda en
`target/swarm/evidencia-caos-<servicio>-<modo>.md`, sin secretos, y se puede pegar en el PR. El reinicio también
tiene que aparecer en Grafana, en la alerta M26 (reinicios inesperados de servicios).

`[VERIFIED]` Corrida del 2026-11-24 con `incentivos-service` y 2 réplicas (Docker Desktop 29.7.2, Windows), con el
override (`interval` 5 s, `retries` 3):

| Modo | Swarm sacó la réplica | Otra vez en 2/2 | Pedidos fallidos durante el caos |
|---|---|---|---|
| `congelar` | a los 28 s (`unhealthy container`) | a los 65 s | 5 de 35, todos antes de los 22 s |
| `matar` | al instante | a los 27 s | 0 de 18 |

Con `congelar`, los fallos son el tiempo en que la réplica colgada todavía figuraba sana y la VIP le mandaba pedidos;
después del `healthcheck`, la otra réplica atendió todo. En los dos modos, la mayor parte del tiempo es el arranque de
la JVM nueva hasta pasar su `healthcheck`.

## Arranque observado

Swarm no respeta el orden: los servicios fallan, se reintentan y el stack converge en unos 4 minutos
(prueba del 2026-11-18). Orden en que quedaron sanos: minio, rabbitmq, postgres, gateway y n8n,
notificaciones, donaciones e incentivos, keycloak y cliente liviano. mongodb y logística no convergieron
con ese stack: mongodb quedó sano recién con `hostname: mongodb`, y la hora en que quedó sana logística
después del arreglo no se midió.

- mongodb necesita `hostname: mongodb` (en el override): su healthcheck inicia el *replica set* contra
  `mongodb:27017`, que en Swarm es la VIP y no enruta a una tarea todavía no sana. Sin eso, mongodb y
  logística no arrancan.
- `minio-init` es una tarea de una vez (`restart_policy: on-failure`): que figure `0/1` y `Complete` es lo esperado.
- Los replicados usan `interval: 5s`, `timeout: 3s`, `retries: 3` y `start_period: 120s`: una réplica
  colgada saldría en unos 15 a 25 s (`[INFERRED]`, sin medir; con `retries: 20` tardaba 160 s y la mitad de los pedidos caía en *timeout*).
  El `start_period` es largo porque con tres reintentos un arranque lento contaría como falla.
- Los límites de memoria son techos, sin reservas. Con 2 réplicas suman más que los 8,7 GiB de la VM, pero
  lo medido con el gateway ×2 fue 3,6 GiB.
