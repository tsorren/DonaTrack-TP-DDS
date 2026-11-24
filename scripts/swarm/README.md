# Stack de Docker Swarm de un nodo

Despliegue de la defensa y la prueba final (E7). Decisión: ADR `docs/adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md`.
Compose sigue siendo el entorno de desarrollo, del CI y de la demo pública.

| Archivo | Qué hace |
|---|---|
| `generar-stack.js` | Pasa `docker compose config --format json` del preprod a un archivo que acepta `docker stack` |
| `generar-stack.test.js` | Un test por regla de la transformación, sobre `ejemplo-compose.json` (datos sintéticos) |
| `desplegar.sh` | Exporta el `.env`, genera `target/swarm/stack.json` y despliega con `docker-stack.override.yml` |
| `verificar-replicas.sh` | SW2: despliega con dos réplicas, espera a que converja y deja la evidencia en `target/swarm/evidencia-replicas.md` |
| `../../docker-stack.override.yml` | Réplicas, límites de memoria, `hostname` de mongodb, healthchecks y `minio-init` |

## Por qué hay una transformación

`docker stack config` no lee el `.env` ni acepta el compose del preprod ni la salida de `docker compose config`
(prueba de Swarm del 2026-11-18, `[VERIFIED]`). `docker stack deploy` no se probó con esos archivos: que use la
misma validación es `[INFERRED]` (ADR). `generar-stack.js`, en todos los servicios:

- borra `name` de primer nivel y pone `version: "3.9"`;
- borra `build`, `depends_on`, `container_name`, `pull_policy`, los campos en `null` y `healthcheck.start_interval`;
- deja afuera los servicios con `profiles` (k6);
- pasa `published` a número y borra `host_ip` (Swarm no publica solo en `127.0.0.1`);
- deja publicados solo los puertos de `PUERTOS_PUBLICADOS`: el cliente liviano (8090) y Keycloak (8180, el
  navegador inicia sesión ahí). RabbitMQ (5672), solo con `DONATRACK_PUBLICAR_RABBITMQ=true`;
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
| `DONATRACK_PUBLICAR_RABBITMQ` | `false` | Publica el 5672 (consumidor espía de las pruebas desde el host) |
| `DONATRACK_BORRAR_COLAS_SAC` | `false` | Lugar del paso que borra las siete colas con *single active consumer* (decisión D8). Hoy solo avisa y corta: el paso lo documenta SL2 en `docs/arquitectura/eventos-amqp.md` |

## Qué está publicado

`[PROPOSED]` Solo el cliente liviano (`http://localhost:8090`) y Keycloak (`http://localhost:8180`).
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
