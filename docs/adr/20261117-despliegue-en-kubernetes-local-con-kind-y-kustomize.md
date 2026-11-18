# Despliegue en Kubernetes local con kind y Kustomize

- Status: proposed
- Date: 2026-11-17
- Deciders: el grupo, en las rondas de decisión de E7 (Q07 a Q09, 2026-11-16, en [requerimientos-e7.md](../requerimientos/requerimientos-e7.md)); redacción asistida por agente. Pendiente de aprobación del equipo en el PR
- Tags: despliegue, kubernetes, kind, kustomize, autorestart, salud, entrega-7

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 2 de la Entrega 7 pide "monitoreo proactivo de la salud de los
servicios" con "políticas de autorestart ante fallos". El enunciado (pág. 23) pide desplegar con
"Docker o similar".

`[OBSERVED]` Hoy todo corre con Docker Compose: siete archivos `docker-compose*.yml` en la raíz
(base, demo, hybrid, preprod, publica, seed y tunel), 14 contenedores en el preprod. Los servicios
y el gateway tienen `healthcheck`, pero **no hay política de `restart`** en `docker-compose.yml` ni
en el preprod (solo en `publica`, `seed` y `tunel`). `/actuator/health` no separa *liveness* de
*readiness*, y el de Incentivos incluye a RabbitMQ: un corte breve del broker lo marcaría caído.

Problema: ¿qué mecanismo vigila la salud y reinicia los servicios y la infraestructura, y dónde
corre el sistema durante E7?

**Gate A:** decisión nueva (no había política de reinicio ni orquestador). **Gate B:** cambia la
plataforma de despliegue, suma tecnología (Kubernetes, kind, Kustomize), cambia cómo se recolectan
los logs (ver el [ADR del stack de observabilidad](./20261117-stack-de-observabilidad-con-prometheus-tempo-loki-y-grafana.md))
y tiene costo alto de reversión. Es ARCHITECTURAL (AGENTS.md §7.0).

## Alternativas Consideradas

1. **A. Compose con `restart: unless-stopped`, `healthcheck` y un contenedor *autoheal*.** Docker
   reinicia los procesos que caen y *autoheal* reinicia los que quedan `unhealthy`.
2. **B. Solo `restart: on-failure` de Docker.**
3. **C1. Compose para desarrollo y CI, Kubernetes como segundo destino**, con vuelta a A si el
   cluster no estaba estable a mitad de la entrega (la recomendación del agente).
4. **C2. Todo en Kubernetes** (probes y `restartPolicy`), con Compose conviviendo mientras se migra
   (elegida).
5. **D. Un supervisor propio en un script.**

Y dentro de C2:

| Pregunta | Opciones | Elegida |
|---|---|---|
| Cluster local (Q07b) | kind · Kubernetes de Docker Desktop · minikube · k3d | **kind** |
| Compose (Q07c) | convive y el CI migra al final · se retira ya · queda para siempre | **convive; el CI migra solo si Kubernetes pasa todas las pruebas** |
| Manifiestos (Q07d) | YAML con Kustomize · Helm chart propio · `kompose` y ajuste a mano | **Kustomize** |

## Resultado de la Decisión

Elegida **C2**: el sistema completo (los cuatro servicios, el gateway, el cliente liviano y la
infraestructura) corre en un cluster **kind**, descripto con **Kustomize**.

- **Organización.** `[PROPOSED]` Una base de Kustomize con un namespace por función (el grupo
  decidió solo el de observabilidad, en Q05; el resto lo propone este ADR): la aplicación, la
  infraestructura (Postgres, Mongo, RabbitMQ, Keycloak, MinIO, n8n y Redis; n8n y Redis se suman
  a los cinco de Q08a porque también corren en el cluster, tarea K2) y la observabilidad
  (Prometheus, Tempo, Loki y Grafana; Q05 reinterpretada). Las diferencias de entorno van en
  *overlays*, no en copias. Los nombres de carpetas y namespaces los fija la tarea K1.
- **Salud (Q08c).** Liveness y readiness separados, con los grupos de salud de Spring Boot
  (`/actuator/health/liveness` y `/actuator/health/readiness`). El reinicio mira **solo
  liveness**; la disponibilidad de dependencias (RabbitMQ, la base) va en readiness, que saca al pod
  del Service sin reiniciarlo.
- **Reinicio (Q08b).** `restartPolicy: Always` (el valor por defecto de un Deployment), **sin tope**:
  Kubernetes espera cada vez más entre intentos (*CrashLoopBackOff*). El tope lo reemplaza una
  alerta en Grafana cuando un pod se reinicia varias veces seguidas (métrica M26).
- **Alcance de la vigilancia (Q08a).** Los cuatro servicios, el gateway, el cliente y la
  infraestructura, cada uno con sus probes.
- **Recursos.** `[PROPOSED]` Cada contenedor declara `requests` y `limits` (Q11c lo pidió para el
  gateway; este ADR lo extiende a todos por la memoria justa del cluster local); el gateway corre con dos réplicas
  (Q11c, ver el [ADR de rate limiting](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)).
- **Entrada.** Un Ingress delante del gateway y del cliente liviano (tarea K5). Con el Ingress
  delante, el gateway tiene que confiar en `X-Forwarded-For` solo si viene del Ingress o del cliente
  liviano; lo resuelve la tarea S4 (ver el ADR de rate limiting).
- **Prueba del autorestart (Q09).** Un script de caos borra un pod (`kubectl delete pod`) o mata su
  proceso y espera a que vuelva, con el evento visible en el panel.
- **Convivencia (Q07c).** Compose sigue siendo el entorno de desarrollo, del CI (Gate 4) y de la
  demo pública con túnel durante E7. El CI pasa a Kubernetes al final, solo si el cluster pasa todas
  las pruebas; ese cambio toca AGENTS.md §11 y va en un PR aparte con revisión humana (AGENTS.md §13).

### Por qué C2 y no C1

El grupo eligió C2 por el valor de aprender Kubernetes (Q07). `[INFERRED]` Además, las probes y el
`restartPolicy` son la forma estándar de cumplir el requerimiento 2, sin un contenedor extra con
acceso al socket de Docker (que necesita *autoheal*). El agente recomendaba C1 por plazo. `[PROPOSED]` El plan de
Q15 propone acotar el riesgo con un **punto de control el 22/11** (el grupo todavía no lo decidió): si el cluster no levanta todos los servicios (tarea K5), la
integración y la demo pasan a Compose y Kubernetes queda como segundo destino, que es C1
([plan de Q15](../requerimientos/requerimientos-e7.md)).

## Análisis por alternativa

**A. Compose + restart + autoheal**
- A favor: usa los `healthcheck` ya escritos; una sola imagen nueva; riesgo de plazo mínimo.
- En contra: *autoheal* necesita montar el socket de Docker (un privilegio alto, contra el
  [ADR de aislamiento de contenedores](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md));
  no separa liveness de readiness; no hay réplicas con balanceo.

**B. Solo `restart: on-failure`**
- A favor: cero dependencias.
- En contra: no cubre un proceso colgado con el contenedor vivo, que es el fallo que la salud
  detecta; no cumple el "monitoreo proactivo".

**C1. Compose + Kubernetes como segundo destino**
- A favor: el CI y la demo no dependen del cluster; el riesgo de plazo queda acotado.
- En contra: dos definiciones del despliegue que se desfasan; Kubernetes queda como accesorio y el
  aprendizaje es menor. Sigue disponible como plan B en el punto de control.

**C2. Todo en Kubernetes (elegida)**
- A favor: probes de liveness y readiness, reinicio con espera creciente, réplicas detrás de un
  Service y un Ingress; es la herramienta de la industria para este requerimiento; los namespaces
  separan aplicación, infraestructura y observabilidad.
- En contra: 14 contenedores a describir en pocas semanas, junto con los otros frentes de E7;
  consumo de memoria alto en las máquinas del grupo (`[INFERRED]` 16 GB para el sistema completo con
  observabilidad, según el plan de Q15); el CI y los scripts de preprod siguen en Compose hasta el final.

**D. Supervisor propio**
- A favor: control total de la política de reinicio, sin herramientas nuevas.
- En contra: reimplementa lo que Docker y Kubernetes ya hacen, con un proceso más que también puede
  caerse y que nadie vigila.

**Herramientas dentro de C2.**
- *kind* corre los nodos como contenedores Docker: reproducible, se crea y se borra con un comando
  y `[INFERRED]` funciona en GitHub Actions. Docker Desktop no es reproducible entre máquinas; minikube es más
  pesado; k3d es menos difundido.
- *Kustomize* viene en `kubectl`, trabaja sobre YAML sin plantillas y resuelve los entornos con
  *overlays*. Helm suma un lenguaje de plantillas que el sistema no necesita; `kompose` deja YAML que
  igual hay que reescribir.

**Principios.** Cada pieza tiene una responsabilidad de despliegue propia (un Deployment o
StatefulSet por proceso, un namespace por función: alta cohesión y bajo acoplamiento). Separar
liveness de readiness evita que una falla de una dependencia se propague como reinicios en cascada,
lo mismo que el aislamiento de *bounded contexts* evita que un servicio caído arrastre a los demás.

## Consecuencias

- **Positivas.**
  - El autorestart y el monitoreo de salud salen de la plataforma, sin código propio ni acceso al
    socket de Docker (el único acceso al nodo es el del recolector de logs, ver el ADR del stack de
    observabilidad).
  - El gateway con dos réplicas muestra el valor de Redis en el rate limiting.
  - La observabilidad y la aplicación se separan por namespace.
- **Negativas.**
  - Hasta que el CI migre, hay dos definiciones del despliegue (Compose y Kubernetes) que pueden
    desfasarse. Conviene un chequeo que compare puertos, variables e imágenes antes de migrar el CI
    `[INFERRED]`.
  - Los logs ya no los recolecta el demonio de Docker: el recolector lee los de los pods desde el
    nodo. Esa decisión vive en el ADR del stack de observabilidad.
  - `[INFERRED]` Hace falta una máquina con 16 GB para el sistema completo; quien no la tiene trabaja con partes.
  - Riesgo de plazo asumido por el grupo; el punto de control del 22/11 que lo acota es una
    propuesta del plan de Q15, pendiente de decisión del grupo.
- **Abiertas.**
  - La [consulta a la cátedra](../requerimientos/requerimientos-e7.md) (Q17, punto 1) sobre si se
    acepta kind como entorno de despliegue y defensa sigue sin respuesta.
  - La demo pública y el túnel siguen en Compose y no se dibujan en el diagrama de despliegue (Q14b).

## Referencias

- [Requerimientos de E7](../requerimientos/requerimientos-e7.md): Q05, Q07 a Q09, Q11c, Q14b y Q15
- [ADR del stack de observabilidad](./20261117-stack-de-observabilidad-con-prometheus-tempo-loki-y-grafana.md)
- [ADR de rate limiting](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)
- [ADR de aislamiento de contenedores y logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
- [ADR del pipeline de CI](./20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md)
- kind: https://kind.sigs.k8s.io/ · Kustomize: https://kubectl.docs.kubernetes.io/references/kustomize/
