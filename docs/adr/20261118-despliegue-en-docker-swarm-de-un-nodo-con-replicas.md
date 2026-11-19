# Despliegue en Docker Swarm de un nodo, con réplicas

- Status: proposed
- Date: 2026-11-18
- Deciders: el grupo, en las rondas de decisión de E7 (Q08 y Q09, 2026-11-16) y en la revisión de simplificación (E7-S01 a E7-S01d y E7-S02b, 2026-11-18), en [requerimientos-e7.md](../requerimientos/requerimientos-e7.md); redacción asistida por agente. Pendiente de aprobación del equipo en el PR
- Tags: despliegue, docker, swarm, autorestart, salud, replicas, shedlock, rabbitmq, entrega-7

> **Reescrito el 2026-11-18 (E7-S05 A).** La versión del 2026-11-17 decidía todo en Kubernetes
> con kind y Kustomize (Q07 C2). La revisión de simplificación sacó Kubernetes de E7 (E7-S01 A),
> porque el plan llegaba al congelamiento en el 13 % de las simulaciones. Ese texto queda en el
> historial del PR de los ADR de E7, que no estaba mergeado.
>
> **Relación propuesta:** este ADR **supera** al
> [ADR de ShedLock en Incentivos](./incentivos-service/20260901-coordinacion-distribuida-de-schedulers-con-shedlock.md)
> (`proposed`, sin implementar), porque extiende la misma decisión a los tres servicios con tareas
> programadas. La relación la aprueba un revisor humano (AGENTS.md §9.3); ninguno de los dos cambia
> de estado en este PR.

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 2 de la Entrega 7 pide "monitoreo proactivo de la salud de los
servicios" con "políticas de autorestart ante fallos". El enunciado (pág. 23) pide desplegar con
"Docker o similar".

`[OBSERVED]` Punto de partida:

- **Despliegue.** Todo corre con Docker Compose: siete archivos `docker-compose*.yml` en la raíz y
  14 contenedores en el preprod. Los servicios y el gateway tienen `healthcheck`, pero no hay
  política de `restart` en `docker-compose.yml` ni en el preprod. `/actuator/health` no separa
  *liveness* de *readiness*.
- **Tareas programadas.** Donaciones, Incentivos y Logística tienen 11 clases con `@Scheduled`
  (5, 4 y 2): entre ellas los *relays* del Outbox, el cierre de rachas y el ranking mensual, y la
  posición simulada de los camiones. Ningún `pom.xml` declara ShedLock.
- **Mensajería.** 8 clases con `@RabbitListener` (1 en Donaciones, 2 en Incentivos, 1 en
  Logística y 4 en Notificaciones). El oyente del ciclo de vida del donante en Incentivos ya corre
  con `concurrency = "1"` para no desordenar los eventos.
- **Sesiones del cliente liviano.** Declara `spring-session-jdbc`. El preprod ya enciende la sesión
  en la base (`CLIENTE_SESIONES_JDBC_HABILITADO=true`); el valor por defecto es `false`.

Problema: ¿qué mecanismo vigila la salud y reinicia los procesos, cómo se replican los módulos
propios y qué hay que cambiar en el código para que dos réplicas no se pisen?

**Gate A:** decisión nueva (no había política de reinicio ni orquestador). **Gate B:** cambia la
plataforma de despliegue, cambia cómo se ejecutan las tareas programadas y el consumo de colas
(dos dependencias nuevas, tablas nuevas y colas redeclaradas) y tiene impacto en todos los
servicios. Es ARCHITECTURAL (AGENTS.md §7.0).

## Alternativas Consideradas

**Dónde corre todo (E7-S01).**

1. **Volver a Docker** (elegida).
2. Kubernetes recortado, con un punto de control el 22/11.
3. Seguir con todo en Kubernetes (la decisión anterior, Q07 C2).

**Cómo se replica, dentro de Docker (E7-S01c).**

| Opción | Qué es |
|---|---|
| A | Swarm con solo el gateway ×2 (la recomendación del agente) |
| B | Compose con `restart`, un contenedor *autoheal* y una réplica de cada cosa |
| **C** | **Swarm de un nodo, replicando los seis módulos propios** (elegida) |

**Lo que pide replicar los servicios (E7-S01d).**

| Pregunta | Opciones | Elegida |
|---|---|---|
| Tareas programadas | ShedLock con JDBC · partir cada servicio en *api* y *worker* · locks propios con `pg_advisory_lock` | **ShedLock con JDBC** |
| Orden de los eventos | *single active consumer* de RabbitMQ · confiar en la máquina de estados y la idempotencia · ignorarlo | ***single active consumer*** |
| Memoria | réplicas por variable · siempre dos réplicas | **réplicas por variable** |

**La IP del visitante detrás de Swarm (E7-S02b).**

| Opción | Qué es |
|---|---|
| **A** | **Aceptar la IP aproximada y documentarla** (elegida el 2026-11-18, después de la prueba de Swarm) |
| B | Un nginx en modo host delante, como única entrada (elegida antes de la prueba; se revirtió) |
| C | Publicar el cliente y el gateway en modo host (contradice las réplicas de E7-S01c) |

## Resultado de la Decisión

Elegidas **Docker** (E7-S01), **Swarm de un nodo con los seis módulos propios replicados**
(E7-S01c C), **ShedLock**, ***single active consumer*** y **réplicas por variable** (E7-S01d) y la
**IP aproximada documentada** (E7-S02b A).

- **Plataforma.** Un nodo de Swarm (`docker swarm init`) y `docker stack deploy`.
  `[VERIFIED]` Funciona en Docker Desktop para Windows (ver "Evidencia").
- **Qué se replica.** El gateway, el cliente liviano y los cuatro servicios. Postgres, Mongo,
  RabbitMQ, Keycloak, MinIO, Redis, n8n y `otel-lgtm` quedan con una réplica, porque guardan
  estado.
- **Cuántas réplicas.** `[PROPOSED]` Por variable: 1 en desarrollo y 2 en la defensa y la prueba
  final. Cada servicio va a declarar un límite de memoria. `[INFERRED]` Con dos réplicas de cada módulo propio,
  `otel-lgtm` y Redis, el stack ronda los 6 a 6,5 GiB, a partir de los 3,6 GiB medidos con el
  gateway ×2.
- **Autorestart (Q08b).** Swarm reemplaza la tarea que termina o queda `unhealthy`. La política es
  la de Swarm por defecto (`condition: any`), sin tope. El tope lo reemplaza una alerta en Grafana
  de reinicios repetidos (M26, ver el
  [ADR de observabilidad](./20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md)).
- **Salud.** En Swarm, un solo `healthcheck` decide dos cosas: si se reinicia la tarea y si recibe
  tráfico (`[INFERRED]` la VIP del servicio enruta solo a tareas sanas; ver el hallazgo de
  mongodb). La separación de Kubernetes entre *liveness* y *readiness* no tiene equivalente.
  `[PROPOSED]` El `healthcheck` mira `/actuator/health/liveness` (tarea K3):
  - una caída corta del broker o de la base no reinicia todas las réplicas;
  - el costo es que una réplica con una dependencia caída sigue recibiendo tráfico. Se acepta,
    porque las dependencias son compartidas: si RabbitMQ cae, cae para todas las réplicas y no hay
    adónde desviar el pedido.
- **Valores del `healthcheck`.** `[VERIFIED]` Con los del preprod (`interval: 5s`, `timeout: 3s`,
  `retries: 20`), una réplica colgada tardó unos 160 s en marcarse `unhealthy`, y mientras tanto la
  mitad de los pedidos cayó en *timeout*. `[PROPOSED]` En los servicios replicados, del orden de
  `interval: 5s` y `retries: 3`; el `start_period` cubre el arranque. Los valores finales los fija
  el override.
- **Archivo del stack.** `[VERIFIED]` `docker stack config` no lee el `.env` ni acepta el compose
  del preprod (`[INFERRED]` `docker stack deploy` usa la misma validación): rechaza el `depends_on` largo, `published` como texto, `start_interval` y los puertos
  atados a una interfaz (`"127.0.0.1:8180:8080"`, que `docker compose config` traduce a `host_ip`).
  Tampoco acepta la salida de `docker compose config`. Cómo se arma el archivo queda **abierto**
  (tarea del override): generarlo del compose con un paso de transformación (en la prueba, un
  script de Node de unas 20 líneas) o mantener un `docker-stack.yml` aparte. Cualquiera de las dos
  necesita `hostname: mongodb` (ver "Evidencia").
- **Arranque.** Swarm ignora el orden: los servicios fallan y se reintentan hasta que sus
  dependencias responden. `[VERIFIED]` El stack converge solo en unos 4 minutos, salvo mongodb (y
  Logística, que depende de él), que necesita `hostname: mongodb`.
- **Tareas programadas: ShedLock con JDBC (E7-S01d-1).** `[PROPOSED]` En Donaciones, Incentivos y
  Logística:
  - una tabla `shedlock` por servicio, creada con una migración de Flyway;
  - `@SchedulerLock` en cada uno de los 11 métodos, con `lockAtMostFor` por encima de la duración
    esperada de la tarea;
  - dos dependencias nuevas (`shedlock-spring` y `shedlock-provider-jdbc-template`), aprobadas por
    el grupo al elegir la opción (AGENTS.md §6, a confirmar en el PR).
  - **Configuración común en `common-lib` (decidido el 2026-11-18, D7 B del plan de W1).**
    `@EnableSchedulerLock` y el `LockProvider` van en una autoconfiguración de `common-lib` que se
    activa solo si hay un `DataSource`. Es un cambio estructural de `common-lib` (AGENTS.md §7.0),
    con el reactor completo como gate. Se descartó la opción de cada servicio (A), que era la
    recomendada porque no choca con la tarea O2, que también cambia `common-lib`: se eligió no
    repetir el código.
- **Orden de los eventos: *single active consumer* (E7-S01d-2).** `[PROPOSED]` Las colas donde importa
  el orden se van a declarar con `x-single-active-consumer: true`: una réplica consume y la otra
  queda de reserva. Candidatas:
  - las seis del ciclo de vida de una entrega que consume Donaciones (ruta asignada, ruta
    iniciada, en puerta, exitosa, fallida y cancelada);
  - la del ciclo de vida del donante en Incentivos.

  Las demás se apoyan en la idempotencia que ya tienen. **Lista decidida el 2026-11-18 (D8 A del
  plan de W1):** las siete candidatas, con el mismo nombre. Un paso de despliegue documentado borra
  esas colas antes de la versión nueva; en un stack nuevo no hace falta. Se descartaron los nombres
  nuevos (`.v2`) porque tocan tests de integración y otros consumidores.
  - **Alcance del orden.** `[INFERRED]` El *single active consumer* ordena los mensajes **dentro
    de una cola**. Los eventos de una entrega viajan por seis colas distintas, así que el orden
    entre ellas (asignada → iniciada → en puerta → exitosa) no queda garantizado: cada cola puede
    tener su consumidor activo en otra réplica, y aun con una réplica los oyentes corren en
    paralelo. El orden entre colas queda sin cubrir, como hoy: la máquina de estados rechaza un
    evento repetido, pero no uno que llega antes de su predecesor. Si hace falta orden entre tipos
    de evento, la alternativa es una sola cola por entidad; queda **abierta** para la tarea.

  `[INFERRED]` RabbitMQ no deja redeclarar una cola existente con otros argumentos
  (`PRECONDITION_FAILED`): por eso el despliegue borra esas colas antes de la versión nueva (D8 A).
- **Sesiones del cliente liviano.** Con dos réplicas, la sesión vive en la base
  (`CLIENTE_SESIONES_JDBC_HABILITADO=true`, como en el preprod), así que no hace falta afinidad.
- **Entrada y la IP del visitante (E7-S02b A).** Sin proxy delante: el cliente liviano (8090) se
  publica en el *routing mesh*, que reparte entre las réplicas.
  - **Publicar el gateway (8086): abierta.** `[OBSERVED]` Los compose del PR 986 confían en
    `X-Forwarded-For` si el pedido llega de `10.*`. `[INFERRED]` Eso incluye la dirección del mesh:
    con el gateway publicado y esa regex, cualquiera puede elegir su cubeta de rate limiting con
    ese encabezado. `[PROPOSED]` No publicar el gateway en el stack de Swarm (el cliente le habla
    por la red interna; Postman y k6 lo alcanzan desde un contenedor de la red) o limitar
    `GATEWAY_LIMITE_PROXIES_CONFIABLES` a la subred de la aplicación. Lo decide la tarea del
    override (ver el
    [ADR de rate limiting](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)).
  - `[VERIFIED]` El mesh reemplaza la IP de origen por la de su `ingress-endpoint` (`10.0.0.2`).
    Un nginx en modo host tampoco veía la IP real en Docker Desktop (veía `172.19.0.1` para todos
    los pedidos de la PC): por eso se revirtió la opción B.
  - Consecuencia en el rate limiting: todos los visitantes **sin cuenta** comparten la cubeta
    por IP (120 lecturas y 30 escrituras por minuto en total). Las cuentas con token validado
    siguen con su cubeta propia. Detalle en el
    [ADR de rate limiting](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md).
- **Puertos sin `127.0.0.1`.** `[OBSERVED]` El preprod publica Keycloak y RabbitMQ solo en
  `127.0.0.1` (`"127.0.0.1:8180:8080"` y `"127.0.0.1:5672:5672"`). Swarm no admite esa forma: lo
  que se publica queda en todas las interfaces. `[PROPOSED]` Keycloak (8180) se publica igual, porque el navegador inicia sesión ahí
  (`KC_HOSTNAME`). RabbitMQ (5672) se publica solo si corren pruebas desde el host. Grafana se
  publica con su propio usuario y clave (ver el
  [ADR de observabilidad](./20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md)). Los
  demás puertos de infraestructura no se publican.
- **Prueba del autorestart (Q09).** Un script de caos (tarea K6) congela o mata el proceso de una
  réplica (`docker kill -s STOP`, `docker kill`) y espera a que Swarm la reemplace, con el evento
  visible en el panel.
- **Convivencia.** `[PROPOSED]` Compose sigue siendo el entorno de desarrollo, del CI (Gate 4) y de
  la demo pública con túnel. Swarm es el de la defensa y la prueba final. Pasar el CI a Swarm no
  está decidido y tocaría AGENTS.md §11 (PR aparte, §13).

### Evidencia: prueba de Swarm (2026-11-18)

Docker Desktop 29.8.2 sobre Windows 11, un nodo, primero con un stack descartable de nginx y
después con el preprod real y el gateway ×2.

| # | Qué se verificó | Resultado |
|---|---|---|
| 1 | Reinicio de una tarea `unhealthy` | `[VERIFIED]` Swarm la reemplaza, sin *autoheal*: en el canario en unos 23 s, en el gateway a los ~160 s (por `retries: 20`) |
| 2 | Reparto del *routing mesh* | `[VERIFIED]` 10 y 10 pedidos entre las dos réplicas del gateway |
| 3 | IP que ve el gateway | `[VERIFIED]` Por el mesh, `10.0.0.2` para todos. Detrás de nginx en modo host, la de nginx, con `X-Forwarded-For: 172.19.0.1`. Desde otra máquina, sin probar |
| 4 | `.env` y formato del compose | `[VERIFIED]` `docker stack config` no lo lee ni acepta el compose (`[INFERRED]` `deploy` se comporta igual). Funcionó un paso de transformación del JSON de `docker compose config` |
| 5 | Arranque sin `depends_on` | `[VERIFIED]` Converge con reintentos en unos 4 minutos, salvo mongodb |

- **Hallazgo de mongodb.** Su `healthcheck` inicia el *replica set* contra `mongodb:27017` y el
  log da `isSelf could not connect` y `NodeNotFound`. `[INFERRED]` En Swarm ese nombre es la VIP
  del servicio, que no enruta a una tarea todavía no sana. Con `hostname: mongodb`, sano en 22 s.
- **Límites de la prueba.** Solo se ejercitaron `/actuator/health` y el gateway ×2: sin inicio de
  sesión, sin pedidos de negocio y sin segunda réplica del cliente ni de los servicios. ShedLock y
  el *single active consumer* se verifican en sus tareas.

## Análisis por alternativa

**Docker frente a Kubernetes (E7-S01).** Matriz del grupo: plazo y riesgo 35 %, lo que corrige la
cátedra 25 %, cantidad de piezas 20 %, aprendizaje 20 %. Totales: Docker 3,95, Kubernetes
recortado 3,45, todo en Kubernetes 2,55. `[INFERRED]` Si el aprendizaje pesaba 40 % o más, ganaba
Kubernetes recortado. Kubernetes daba *liveness* y *readiness* separados y un Ingress, a costa de
unos 6 días-persona (kind, Kustomize, manifiestos, migración del CI) que el plan no tenía.

**A. Swarm con solo el gateway ×2**
- A favor: el cambio más chico que muestra réplicas y justifica Redis; los servicios no cambian.
- En contra: solo el gateway escala; el resto queda como punto único de falla.

**B. Compose con *autoheal***
- A favor: usa los `healthcheck` ya escritos; riesgo de plazo mínimo.
- En contra: *autoheal* monta el socket de Docker (contra el
  [ADR de aislamiento de contenedores](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md));
  sin réplicas, el rate limiting no necesita Redis. Queda como plan B si Swarm fallaba en la
  prueba (no falló).

**C. Swarm replicando los seis módulos (elegida)**
- A favor: autorestart y réplicas de la plataforma, sin socket de Docker; muestra escalado
  horizontal en todos los módulos propios.
- En contra: obliga a coordinar las tareas programadas y el consumo de colas, y a armar el archivo
  del stack. Matriz del grupo: A 3,85, B 3,75, C 3,15; el grupo eligió C por escalabilidad y
  aprendizaje.

**Tareas programadas.** Partir cada servicio en *api* y *worker* duplica imágenes y despliegues;
`pg_advisory_lock` a mano reimplementa lo que ShedLock ya resuelve (vencimiento del lock si la
réplica cae, `lockAtLeastFor` contra relojes corridos).

**Orden de los eventos.** Confiar solo en la máquina de estados y la idempotencia rechaza bien un
evento repetido, pero no uno que llega antes de su predecesor. Con dos réplicas consumiendo la
misma cola, eso pasa más seguido. El *single active consumer* devuelve a cada cola el orden que
tenía con una réplica; no ordena entre colas distintas (ver "Alcance del orden"). Ignorarlo deja
errores intermitentes que aparecen recién con dos réplicas.

**La IP (E7-S02b).** Matriz con los pesos del grupo (plazo 35 %, cátedra 25 %, piezas 20 %,
exactitud del límite por IP 20 %), recalculada con la evidencia (`[INFERRED]`, puntajes del agente): aceptar la IP aproximada 3,70,
nginx delante 2,85, defensa en un Linux con Docker nativo 2,75. En Docker Desktop, nginx no daba
la IP real, así que su costo (0,5 a 1 día-persona y una imagen más) no compraba nada.

**Principios.** Cada tarea de Swarm es intercambiable con sus réplicas: el estado vive en la base
(sesiones, locks de ShedLock) y no en memoria. La coordinación entre réplicas (ShedLock, *single
active consumer*) es infraestructura y queda en adaptadores y configuración, sin tocar el dominio.

## Consecuencias

- **Positivas.**
  - El autorestart y el reparto salen de la plataforma, sin código propio ni acceso al socket de
    Docker.
  - El gateway con dos réplicas muestra el valor de Redis en el rate limiting.
  - Las tareas programadas y las colas con orden quedan listas para cualquier cantidad de réplicas.
- **Negativas.**
  - Dos definiciones del despliegue (Compose y el stack de Swarm) que pueden desfasarse, más el
    paso que arma una a partir de la otra si se elige generarla.
  - El límite por IP es uno solo para todos los visitantes sin cuenta.
  - Keycloak queda publicado en todas las interfaces.
  - Dos dependencias nuevas (ShedLock), una tabla por servicio y colas redeclaradas.
  - Sin separación entre *liveness* y *readiness*: una réplica con una dependencia caída sigue
    recibiendo tráfico.
  - `[INFERRED]` Con dos réplicas de todo, el stack usa casi toda la memoria de una máquina de
    8 GB para Docker.
- **Abiertas.**
  - Cómo se arma el archivo del stack (generado o aparte).
  - Los valores del `healthcheck` de los servicios replicados.
  - Si el CI pasa a Swarm.
  - Qué IP se ve desde otra máquina de la red (no se probó).
  - Si el gateway se publica, y con qué `GATEWAY_LIMITE_PROXIES_CONFIABLES`.
  - Si hace falta orden entre los eventos de una entrega que viajan por colas distintas.

## Referencias

- [Requerimientos de E7](../requerimientos/requerimientos-e7.md): Q08, Q09, §5 (E7-S01 a E7-S01d, E7-S02b y E7-S05)
- [ADR de ShedLock en Incentivos](./incentivos-service/20260901-coordinacion-distribuida-de-schedulers-con-shedlock.md) (relación "superado por" propuesta)
- [ADR de observabilidad](./20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md) (M26 y la alerta de reinicios)
- [ADR de rate limiting](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md) (dos réplicas del gateway y la cubeta por IP)
- [ADR de aislamiento de contenedores y logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
- [ADR del pipeline de CI](./20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md)
- Docker Swarm: https://docs.docker.com/engine/swarm/ · ShedLock: https://github.com/lukas-krecan/ShedLock · *Single active consumer*: https://www.rabbitmq.com/docs/consumers#single-active-consumer
