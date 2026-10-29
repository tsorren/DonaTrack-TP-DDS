# Spec: SPEC-05 — Rediseño del roadmap de preprod fiel e implementación hasta RM-08

> **Estado:** DRAFT — decisiones elegidas por el usuario el 2026-10-28 (§3.5); falta su lectura y aprobación de la spec completa
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0: cambia la estrategia de persistencia, el pipeline de promoción y suma contratos vivos)
> **Fecha:** 2026-10-28
> **Base:** `E5_requerimientos_e6` (`aec0f0eb`) + cherry-pick del análisis de preprod fiel (`1bb339dd`). Rama de PR: `E5_preprod_fiel_rm08`, hacia `ENTREGA_5`.
> **Documentos que rediseña:** [preprod-fiel-roadmap.md](../../testing/preprod-fiel-roadmap.md) (fase 3), [preprod-fiel-arquitectura-objetivo.md](../../testing/preprod-fiel-arquitectura-objetivo.md) (fase 2).
> **ADR:** [promoción por digest](../../adr/20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md) (`proposed`, se revisa el punto de despliegue) y un ADR nuevo `proposed` de base por servicio (D1).

---

## 1. Objetivo (Goal)

1. Rediseñar tres partes del roadmap a pedido del usuario (2026-10-28):
   - **Persistencia:** pasar de un schema por servicio a **una base de datos por servicio**, y dejar previsto **un MinIO por servicio** a futuro.
   - **RM-06 y RM-07:** que la promoción y el despliegue reproducible **no dependan de Render**.
2. Implementar RM-01 a RM-08 según el roadmap rediseñado, o dejar cada ítem bloqueado con su razón.

## 2. Alcance (Scope)

### In-Scope

- Docs: roadmap, arquitectura objetivo, ADR de promoción (revisión), ADR nuevo de base por servicio, `DEUDA_TECNICA.md`, `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md`, `docs/testing/README.md`, `persistencia/README.md`.
- RM-02 a RM-08, en los archivos que nombra cada uno (§4).
- RM-19 (base por servicio): **solo documentación** (ADR, roadmap, `persistencia/README.md`); el código espera a que mergeen `E4_donaciones_minio` y `E4_persistencia_donaciones`.

### Out-of-Scope

- Cambiar la configuración de Neon, Atlas, CloudAMQP o Render (fuera del repo). Se deja como precondición en RM-01.
- Código de RM-19 (decisión del usuario, 2026-10-28).
- MinIO por servicio: queda como ítem RM-20, sin código (§3, D2).
- Trabajo funcional de E5 y E6 (persistencia de Donaciones, auth, cliente liviano).
- RM-09 en adelante.

## 3. Decisiones de rediseño

Criterios ponderados (1 a 5; ponderación en %). El puntaje es la suma de nota × peso / 100.

### D1 — Aislamiento de datos relacionales: base por servicio

| Criterio | Peso | A. Schema por servicio (hoy) | **B. Base por servicio en el mismo motor** | C. Un motor (contenedor) por servicio |
|---|:-:|:-:|:-:|:-:|
| Aislamiento entre contextos | 25 | 3: roles y `REVOKE`, pero una sola base; un `GRANT` mal puesto cruza dominios | 4: sin `JOIN` posible entre bases; `REVOKE CONNECT … FROM PUBLIC` | 5: aislamiento de proceso |
| Fidelidad con la nube | 20 | 3 | 4: Neon admite varias bases por proyecto `[INFERRED]`, a verificar en RM-01 | 3: varios proyectos de Neon o instancias |
| Huella en CI (runner de 7 GB) | 20 | 5 | 5: un solo proceso | 2: unos 200 MB por motor (ADR `20260902`) |
| Costo de migración | 15 | 5 | 4: cambia el nombre de la base en las URLs y el script de init; Flyway y Hibernate no cambian (se conserva el schema `<servicio>` dentro de cada base) | 2: 3 o 4 servicios nuevos en 3 composes y sus healthchecks |
| Reversibilidad | 10 | 5 | 4 | 3 |
| Paso a un motor por servicio | 10 | 2 | 4: cambiar el host de la URL alcanza | 5 |
| **Total** | 100 | **3,8** | **4,2** | **3,35** |

- **Recomendada: B.** La pedida "base por servicio" con el menor costo. C queda como evolución sin código nuevo en la aplicación: solo cambia el host.
- **Descartadas:** A (es lo que se reemplaza); C por huella en CI y porque la nube no la necesita hoy.
- **Diseño B:** el script de init crea las bases `notificaciones`, `incentivos`, `donaciones` y `logistica` (perfil `postgres` heredado de Logística), cada una con dueño su rol y `REVOKE CONNECT ON DATABASE … FROM PUBLIC`. Dentro de cada base, el schema `<servicio>` sigue igual, así que `currentSchema`, `hibernate.default_schema` y `spring.flyway.schemas` no cambian. Las URLs pasan de `/donatrack?currentSchema=x` a `/x?currentSchema=x`. Los tests de Testcontainers cambian `withDatabaseName("donatrack")` por el nombre del servicio. E6 (auth y cliente liviano) recibe su propia base en vez de un schema (RNF-AUTH-06 y RNF-CL-05 ya lo permiten: "schema o base propia").
- **ADR:** sí (Gate A: decisión nueva sobre persistencia; Gate B: cambia la estrategia de datos). ADR nuevo `proposed` que, si se aprueba, reemplaza a `20260902` (también `proposed`). La relación `superseded` la aprueba un humano.
- **Conflicto en curso `[OBSERVED]`:** `origin/E4_donaciones_minio` (41 commits sobre la base) y `origin/E4_persistencia_donaciones` tocan `persistencia/init-db/01-init-schemas-roles.sql`, los dos composes y suman `persistencia/Dockerfile`. RM-19 choca con esos cambios. Ver la pregunta de aprobación.

### D2 — Objetos: MinIO por servicio (a futuro)

`[OBSERVED]` `origin/E4_donaciones_minio` ya crea un usuario de MinIO por servicio con acceso solo a sus buckets (`persistencia/minio/init-usuarios.sh`). Es el equivalente a "schema por servicio" en objetos.

| Criterio | Peso | A. Usuario y bucket por servicio (rama E4) | B. Una instancia de MinIO por servicio |
|---|:-:|:-:|:-:|
| Aislamiento | 35 | 3 | 5 |
| Huella en CI | 25 | 5 | 3 |
| Costo ahora (E5 vence el 30/10) | 25 | 5 | 2 |
| Paso posterior | 15 | 4: cambiar endpoint y credenciales por servicio | 5 |
| **Total** | 100 | **4,15** | **3,75** |

- **Recomendada:** A ahora (llega con la rama de Donaciones) y B como **RM-20, después de E7**, sin código en esta tarea. B no pide ADR propio si D1 se aprueba: aplica el mismo criterio a otro almacén (Gate A no aplica).

### D3 — RM-06 y RM-07 sin depender de Render

RM-06 (promoción por digest con preprod post-merge) **ya no dependía de Render**: solo usa GHCR y GitHub Actions. Se mantiene igual.

RM-07 dependía de Render en dos precondiciones sin confirmar (plan gratuito con servicio desde imagen y cómo construye hoy).

| Criterio | Peso | A. Deploy hook de Render (original) | **B. Despliegue del manifiesto en un entorno efímero del runner, con smoke y rollback; Render como adaptador opcional** | C. Otro PaaS (Fly.io, Railway) |
|---|:-:|:-:|:-:|:-:|
| Independencia de terceros | 30 | 1: bloqueado por RM-01 | 5 | 2: cuenta nueva y su plan gratuito |
| Reproducibilidad desde el repo | 25 | 4 | 5: el mismo manifiesto levanta el mismo set de digests | 4 |
| Fidelidad con producción | 20 | 5 | 3: mismo artefacto, otra plataforma | 4 |
| Costo y dependencias nuevas | 15 | 4 | 5: solo `docker compose` | 2: CLI, secretos, ADR |
| Rollback ensayado | 10 | 3 | 5: se ensaya en cada corrida | 3 |
| **Total** | 100 | **3,2** | **4,6** | **3,0** |

- **Recomendada: B.** `deploy.yml` (`workflow_dispatch` con el manifiesto, y después de `promote`) levanta el stack con los digests del manifiesto M, corre el smoke (`/actuator/health`, `/v3/api-docs` y los tests `smoke`), despliega M-1 encima y repite el smoke (rollback ensayado). Si existe el secreto `RENDER_DEPLOY_HOOK_LOGISTICA`, un paso extra llama al deploy hook con el digest de Logística; si no existe, se saltea con aviso y no falla.
- **Efecto en el puntaje:** P12 sube a 2 por promoción reproducible con smoke y rollback; el despliegue real en Render queda como mejora condicionada.
- **ADR:** se revisa el ADR `proposed` de promoción (punto de despliegue), sin ADR nuevo. Está sin publicar y sin aprobar, así que se puede corregir.
- **RM-01** se reduce: las preguntas de Render dejan de bloquear. Queda el pedido de aprobaciones y la verificación de Neon para D1.

### D4 — RM-08: cómo capturar y validar los mensajes AMQP

`[OBSERVED]` Los 19 schemas `evento-*` usan JSON Schema draft 2020-12. El validador que ya trae `integration-tests` por transitividad (`com.github.java-json-tools:json-schema-validator:2.2.14`, de `swagger-request-validator`) solo soporta draft-04: no sirve.

| Criterio | Peso | A. `amqp-client` + `networknt/json-schema-validator` + puerto AMQP en 127.0.0.1 (original) | **B. Plugin de management activado en la misma imagen + API HTTP con RestAssured + `networknt`** | C. Captura por API de management y validación con el validador nativo de `scripts/validate-contracts.js` |
|---|:-:|:-:|:-:|:-:|
| Dependencias nuevas | 25 | 2: 2 de test | 4: 1 de test | 5: ninguna |
| Falla dentro de JUnit (gate honesto) | 25 | 5 | 5 | 2: el resultado queda en otro paso |
| Soporte de draft 2020-12 | 20 | 5 | 5 | 3: motor propio, cobertura parcial `[INFERRED]` |
| Reuso en RM-12 y RM-13 | 15 | 5: el mismo cliente | 3: la API devuelve headers (traza de RM-12); la DLQ de RM-13 se mira con `rabbitmqctl` | 2 |
| Superficie del compose | 15 | 4: un puerto en loopback | 3: plugin y un puerto | 3 |
| **Total** | 100 | **4,1** | **4,15** | **3,1** |

- A y B quedan casi empatadas. **Recomendada: B**, por una dependencia menos. `[INFERRED]` La imagen `rabbitmq:3.13-alpine` trae el plugin `rabbitmq_management` sin activar (la variante `-management` solo lo activa); se verifica al implementar. El espía declara una cola propia ligada a los exchanges con `#` y lee con `POST /api/queues/%2F/<cola>/get`. A queda como alternativa si RM-13 necesita publicar mensajes malformados desde el test.
- **Herramienta de diff de OpenAPI** (criterio 3 de RM-08): `oasdiff` en contenedor (`tufin/oasdiff`) en un paso de `main.yml`. Pide aprobación (imagen nueva). Sin ella, el criterio 3 queda bloqueado y P7 queda en 1.

## 4. Roadmap rediseñado (RM-01 a RM-08, más RM-19 y RM-20)

| ID | Cambio respecto de la fase 3 | Archivos |
|---|---|---|
| RM-01 | Las preguntas de Render dejan de bloquear. Se suma: confirmar que Neon admite una base por servicio (D1) | 1 issue (lo abre el equipo) |
| RM-02 | Sin cambios | `BaseIT.java`, `report_test_failures.py` y su test, `main.yml`, `notificaciones-service/pom.xml` |
| RM-03 | Sin cambios | 5 workflows |
| RM-04 | Sin cambios | `run-preprod-tests.sh`, 2 envoltorios, `main.yml`, `docs/testing/integration-tests.md` |
| RM-05 | Fijar `mongo:7.0` por digest y la corrida verde | `docker-compose.preprod.yml` |
| **RM-19 (nuevo; código después de que mergee Donaciones)** | Base por servicio (D1). En esta tarea, solo docs | `persistencia/init-db/*`, 3 composes, 3 `application-postgres.properties`, 4 tests de Testcontainers y sus copias del script, `persistencia/README.md` |
| RM-06 | Sin cambios (no dependía de Render) | `merge.yml`, `main.yml`, 1 script de manifiesto |
| RM-07 | Rediseñado (D3): despliegue del manifiesto en entorno efímero, smoke y rollback; Render opcional | `deploy.yml` nuevo, script de smoke |
| RM-08 | Diseño A de D4 (elegido por el usuario) | `integration-tests/pom.xml`, consumidor espía, `ContractIT.java`, `docker-compose.preprod.yml`, `main.yml` |
| **RM-20 (nuevo, después de E7)** | MinIO por servicio (D2), sin código ahora | — |

Orden de implementación: RM-03 → RM-02 → RM-05 → RM-04 → docs de RM-19 → RM-06 → RM-07 → RM-08.

### 3.5 Decisiones del usuario (2026-10-28)

| Decisión | Elegida | Nota |
|---|---|---|
| D1 | **B.** Base propia por servicio en el mismo motor | — |
| RM-19 | Solo documentar por ahora | El código choca con `E4_donaciones_minio` y `E4_persistencia_donaciones` |
| D2 | A ahora, B (RM-20) a futuro | Sin código |
| D3 | **B.** Ensayo del manifiesto en el runner; Render opcional | — |
| D4 | **A.** `amqp-client` + `networknt/json-schema-validator`, puerto AMQP en `127.0.0.1` | El usuario prefirió A sobre la recomendada B |
| Aprobaciones de RM-08 | `networknt/json-schema-validator` (test), `com.rabbitmq:amqp-client` (test), imagen `tufin/oasdiff` | El plugin de management no se aprobó ni hace falta con A |

## 5. Restricciones (Constraints)

- AGENTS.md §4.3: ningún test se deshabilita ni se debilita; un test que falla por un defecto real queda en rojo con su causa.
- Dependencias de `pom.xml` e imágenes nuevas, solo con OK explícito del usuario.
- ADRs nuevos o revisados quedan en `proposed`.
- Sin push ni PR sin pedido; la rama de PR lleva prefijo `E5_`.
- RM-06 y RM-07 no se pueden ejecutar en local (dependen de GitHub Actions y GHCR): se validan con sintaxis y con un ensayo local del script de manifiesto; el resto queda `[TESTS_NOT_EXECUTED_BY_EVALUATOR]` hasta la primera corrida en CI.

## 6. Validación

| Ítem | Validación local | En CI |
|---|---|---|
| RM-02 | `mvn test` de `report_test_failures` (pytest) y `ContractIT` con `PREPROD_STRICT=true` y el stack apagado: falla | Gate de preprod |
| RM-03 | Conteo: 18 de 18 jobs con `timeout-minutes` | — |
| RM-04 | `./run-preprod-tests.sh --images local` en verde | `--images ghcr` |
| RM-05 | Stack de preprod en verde con `mongo` fijado por digest | `preprod-validation` |
| RM-19 (docs) | `python scripts/validate_docs_links.py` sin enlaces rotos | — |
| RM-06, RM-07 | Script de manifiesto probado en local; YAML válido | Primera corrida post-merge |
| RM-08 | `ContractIT` valida los mensajes capturados; un mensaje sin schema hace fallar el test | `preprod-validation` |
