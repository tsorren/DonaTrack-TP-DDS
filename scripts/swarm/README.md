# Stack de Docker Swarm de un nodo

Despliegue de la defensa y la prueba final (E7). Decisión: ADR `docs/adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md`.
Compose sigue siendo el entorno de desarrollo, del CI y de la demo pública.

| Archivo | Qué hace |
|---|---|
| `generar-stack.js` | Pasa `docker compose config --format json` del preprod a un archivo que acepta `docker stack` |
| `generar-stack.test.js` | Un test por regla de la transformación, sobre `ejemplo-compose.json` (datos sintéticos) |
| `desplegar.sh` | Exporta el `.env`, genera `target/swarm/stack.json` y despliega con `docker-stack.override.yml` |
| `../../docker-stack.override.yml` | Réplicas, límites de memoria, `hostname` de mongodb, healthchecks y `minio-init` |

## Por qué hay una transformación

`docker stack` no lee el `.env` ni acepta el compose del preprod ni la salida de `docker compose config`
(prueba de Swarm del 2026-11-18). `generar-stack.js`, en todos los servicios:

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

Solo el cliente liviano (`http://localhost:8090`) y Keycloak (`http://localhost:8180`), en todas las interfaces.
El gateway no se publica: con la regex `10.*` de `GATEWAY_LIMITE_PROXIES_CONFIABLES`, cualquiera que llegue por
el *routing mesh* (`10.0.0.2`) podría elegir su cubeta de rate limiting con `X-Forwarded-For`. Los servicios y
la infraestructura tampoco. Postman, k6 o `curl` llegan desde un contenedor en la red del stack, que es
`attachable`:

```bash
docker run --rm --network donatrack_preprod-network curlimages/curl -s http://api-gateway:8086/actuator/health
```

## Arranque observado

Swarm no respeta el orden: los servicios fallan, se reintentan y el stack converge en unos 4 minutos
(prueba del 2026-11-18). Orden en que quedaron sanos: minio, rabbitmq, postgres, gateway y n8n,
notificaciones, donaciones e incentivos, keycloak, cliente liviano, y al final mongodb y logística.

- mongodb necesita `hostname: mongodb` (en el override): su healthcheck inicia el *replica set* contra
  `mongodb:27017`, que en Swarm es la VIP y no enruta a una tarea todavía no sana. Sin eso, mongodb y
  logística no arrancan.
- `minio-init` es una tarea de una vez (`restart_policy: on-failure`): que figure `0/1` y `Complete` es lo esperado.
- Los replicados usan `interval: 5s`, `timeout: 3s`, `retries: 3` y `start_period: 120s`: una réplica
  colgada saldría en unos 15 a 25 s (`[INFERRED]`, sin medir; con `retries: 20` tardaba 160 s y la mitad de los pedidos caía en *timeout*).
  El `start_period` es largo porque con tres reintentos un arranque lento contaría como falla.
- Los límites de memoria son techos, sin reservas. Con 2 réplicas suman más que los 8,7 GiB de la VM, pero
  lo medido con el gateway ×2 fue 3,6 GiB.
