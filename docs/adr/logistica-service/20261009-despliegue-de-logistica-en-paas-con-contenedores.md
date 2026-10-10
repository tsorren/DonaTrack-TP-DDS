# Despliegue de Logística en PaaS con contenedores

- Status: proposed
- Date: 2026-10-09
- Deciders: Decisión Grupal
- Tags: logística, despliegue, docker, paas, render, neon, cloudamqp, entrega-4

## Contexto y Problema

La Entrega 4 exige que el servicio de logística quede desplegado y accesible por web a través de sus URIs. El despliegue puede quedar pausado para reducir consumos hasta la defensa (`docs/entregas/4/Enunciado-4.pdf`, p. 24). E3 ya pedía cada servicio en un contenedor (p. 22).

`[OBSERVED] El código actual ya implementa esta decisión` en todo lo que depende del repo:

* `logistica-service/Dockerfile`: multi-stage (`maven:3.9.6-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`), usuario `1001:1001`, `-XX:MaxRAMPercentage=75.0`, target `ci` que reutiliza el JAR que construye CI.
* `server.port=${PORT:8083}` en `logistica-service/src/main/resources/application.properties`: el puerto lo fija la plataforma.
* Perfil `postgres` (`application-postgres.properties`): datasource por variables de entorno, Flyway, `ddl-auto=validate`, Hikari acotado "para instancias con recursos limitados".
* `logistica-service/src/main/resources/db/migration/V2__orden_visita_rutas.sql:1` dice que la V1 "ya corrió en Render".
* CI publica las imágenes en GHCR (`.github/workflows/merge.yml`). No hay despliegue automático a la nube.

Fuera del repo: el servicio en Render, la base en Neon, el RabbitMQ en CloudAMQP, sus credenciales y la URL pública.
**URL de Logística: https://donatrack-logistica-0op2.onrender.com** (publicada el 2026-10-09). No hay `render.yaml` ni otro manifiesto de nube.

Requisitos del servicio que condicionan la plataforma:

* PostgreSQL para el perfil `postgres` (schema `logistica`).
* RabbitMQ para consumir el comando `entrega.solicitada.<instancia>.v1` y publicar en `logistica.exchange`.
* Un proceso residente: el listener AMQP y la planificación diaria (cron `0 0 2 * * ?`) corren dentro del servicio.

Problema: ¿dónde y cómo se despliega Logística para que sea accesible por URI, pausable y fiel a la imagen que valida CI?

## Atributos de Calidad y Drivers de Decisión

* **Costo y posibilidad de pausar** (E4, p. 24).
* **Acceso web por URI con HTTPS**, sin administrar certificados.
* **Paridad con CI:** desplegar la misma imagen Docker, sin reempaquetar.
* **Proceso residente** para el consumidor AMQP y el cron.
* **Esfuerzo operativo bajo:** sin sistema operativo, TLS ni parches a cargo del grupo.

## Relaciones Arquitectónicas

1. **Aplica:** [20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host](../20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md) (contenedor no root, logs por consola) y [20260426-pipeline-unificado-de-cicd-y-automatizacin-de-calidad](../20260426-pipeline-unificado-de-cicd-y-automatizacin-de-calidad.md) (imágenes construidas por CI).
2. **Depende de:** [20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql](../20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) (schema `logistica`) y [20261007-broker-de-integracion-con-logistica](../20261007-broker-de-integracion-con-logistica.md) (Logística recibe el comando por RabbitMQ).
3. **Vista resumida:** [`docs/entrega-4/arquitectura/arquitectura-sistema.md`](../../entrega-4/arquitectura/arquitectura-sistema.md), §7 y Figura 8.

## Alternativas Consideradas

| Alternativa | Veredicto |
|---|---|
| **A. PaaS con contenedor (Render) + PostgreSQL gestionado (Neon) + RabbitMQ gestionado (CloudAMQP)** | ✅ Elegida. Despliega la misma imagen, da URI con HTTPS y se puede pausar. |
| B. Máquina virtual (IaaS) con Docker Compose | ❌ Corre todo igual que en local, pero el grupo administra sistema operativo, TLS, DNS y parches, y el costo corre mientras esté encendida. |
| C. Serverless de funciones (FaaS) | ❌ Barato y pausable por diseño, pero no hay proceso residente para el listener AMQP ni para el cron, y habría que reempaquetar el servicio. |

Matriz (escala 1 = malo a 5 = muy bueno; total = Σ peso × puntaje / 5, sobre 100; juicio del grupo):

| Criterio (peso) | A | B | C |
|---|---|---|---|
| Costo y posibilidad de pausar (35) | 5 | 2 | 5 |
| Acceso web por URI con HTTPS (20) | 5 | 3 | 5 |
| Reutiliza la imagen Docker sin cambios (20) | 5 | 5 | 1 |
| Proceso residente para consumidor AMQP y cron (15) | 3 | 5 | 1 |
| Esfuerzo operativo (10) | 4 | 1 | 3 |
| **Total** | **92** | 63 | 68 |

## Resultado de la Decisión

Se adopta la alternativa **A**:

1. **Logística** corre en Render a partir de su imagen Docker (target `ci` o `local`), con el perfil `postgres`.
2. **PostgreSQL** gestionado en Neon. El datasource se inyecta por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. Flyway migra al arrancar.
3. **RabbitMQ** gestionado en CloudAMQP. La conexión se inyecta por `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` y `RABBITMQ_PASS`.
4. **Identidad ante el broker de integración** por `LOGISTICA_INSTANCIA_ID` y `LOGISTICA_TOKEN_VUELTA`.
5. **Credenciales y URL fuera del repo**, solo como variables de entorno de cada plataforma.
6. **Pausado** entre defensas, según permite el enunciado.

## Consecuencias Positivas

* El artefacto desplegado es el mismo que valida CI.
* URI pública con HTTPS sin administrar certificados.
* Costo acotado: el servicio se puede pausar.
* No hay que operar sistema operativo, base ni RabbitMQ.

## Consecuencias Negativas

* Pausada, Logística no consume comandos ni corre la planificación de las 02:00.
* `[INFERRED]` Para integrarse con la instancia en la nube, Donaciones tiene que usar el mismo RabbitMQ gestionado. Es diseño: no hay evidencia en el repo.
* Tres plataformas, tres juegos de credenciales. Al estar fuera del repo, el despliegue no se reproduce desde el código ni lo verifica CI.
* El arranque después de una pausa demora la primera respuesta.
* Sin manifiesto de infraestructura como código: un cambio de configuración en la nube no deja rastro en el historial de Git.

## Validación

1. `docker build -f logistica-service/Dockerfile --target local .` construye la imagen que se despliega.
2. `GET <URL>/actuator/health` responde `UP` con la instancia activa. **Pendiente: la URL todavía no está publicada.**
3. Con la instancia activa, los endpoints de `docs/arquitectura/contratos/openapi-logistica.yaml` responden en la URL pública.
4. Al publicar la URL, registrarla en `docs/entrega-4/arquitectura/arquitectura-sistema.md` (§7) y en este ADR.
