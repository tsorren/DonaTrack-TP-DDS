# Preprod fiel: criterio, estado actual y brechas de CI/CD e integración

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Fase 1 de 3 · 2026-10-20 · borrador para revisar con el equipo · análisis completo: las 3 fases están hechas**
> Fase 1: criterio y medición (este documento). Fase 2: [arquitectura objetivo con alternativas](preprod-fiel-arquitectura-objetivo.md). Fase 3: [roadmap por entrega y ADR](preprod-fiel-roadmap.md).

## 0. Cómo leer este documento

- **Pregunta:** ¿qué le falta al pipeline de CI/CD y a los tests de integración para que el entorno preproductivo valide con rigor todo el sistema, tal como corre en producción?
- **Base relevada:** `origin/main` en `a808942c` (merge de Entrega 4, igual a `ENTREGA_5`). Las referencias `archivo:línea` apuntan a ese commit.
- **Etiquetas** (AGENTS.md §3): `[OBSERVED]` visto en código o configuración · `[DOCUMENTED]` lo dice un ADR, el enunciado o un documento · `[INFERRED]` deducción · `[PROPOSED]` no existe todavía.
- **Antecedentes:** este informe parte de la [revisión DevOps del 2026-09-02](../auditoria/revision-critica-devops-ci.md) y del [backlog de QA del 2026-09-08](auditoria/06-cobertura-critica-qa-backlog.md). La §4 dice qué hallazgos de esos documentos siguen abiertos.

## 1. Producción de referencia

Un entorno preproductivo es fiel respecto de una producción concreta. Para DonaTrack, esa producción es:

| Componente | Dónde corre en producción | Fuente |
|---|---|---|
| Logística | Render (PaaS, contenedor), perfil `postgres`, plan que se suspende sin tráfico | `[DOCUMENTED]` [arquitectura-sistema.md §7](../entrega-4/arquitectura/arquitectura-sistema.md) · ADR [despliegue de Logística en PaaS](../adr/logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md) (`proposed`) |
| PostgreSQL de Logística | Neon (gestionado, TLS) | idem |
| RabbitMQ | CloudAMQP (gestionado, TLS) | idem · `docker-compose.hybrid.yml:33` |
| Donaciones, Incentivos, Notificaciones, n8n, MinIO | Locales, con Docker Compose | `[DOCUMENTED]` arquitectura-sistema.md §7 |
| Despliegue | Manual. CI publica imágenes en GHCR; no hay despliegue automático | `[DOCUMENTED]` arquitectura-sistema.md §7 · `[OBSERVED]` `merge.yml:200-287` |

Cambios ya comprometidos por el [enunciado del TP Anual 2026](../entregas/Enunciado-TP-Anual-2026.pdf) `[DOCUMENTED]`:

- **E5 (pág. 25):** persistencia políglota. Logística en una base documental con ODM; el resto en un esquema relacional propio con ORM.
- **E6 (págs. 26–27):** cliente liviano desacoplado (MVC) y servicio de autenticación con SSO y Social-Login. Hoy `auth-service` y `cliente-liviano` son placeholders `[OBSERVED]` (`auth-service/README.md:1-13`, `cliente-liviano/README.md:1-12`).
- **E7 (pág. 28):** observabilidad y seguridad.

El criterio de la §2 se mide contra la producción de hoy. Las propiedades P2, P9 y P10 ya anticipan E5–E7.

## 2. Criterio: qué hace fiel a un preprod

Doce propiedades en cuatro ejes. Cada una tiene una condición verificable y una escala: **0** ausente · **1** parcial · **2** cumple.

### Eje A — El entorno se parece a producción

| ID | Propiedad | Se cumple (2) cuando… |
|---|---|---|
| P1 | **Artefacto idéntico** | Preprod corre exactamente las imágenes (por digest) que se publican y despliegan, construidas una sola vez desde el commit integrado. Ninguna imagen de otra versión reemplaza en silencio a la del cambio. |
| P2 | **Topología completa** | Están todos los componentes desplegables (servicios, cada motor de base, broker, almacenamiento de objetos, n8n y, desde E6, auth y cliente), con el mismo medio de comunicación que en producción (AMQP o HTTP). |
| P3 | **Configuración y plataforma equivalentes** | Mismos perfiles y propiedades que producción, salvo endpoints y secretos. Hay TLS donde producción lo usa. Las versiones de infraestructura coinciden con las gestionadas y están fijadas. Los límites de recursos son comparables. |
| P4 | **Datos y esquema** | Las migraciones corren desde cero **y** desde el esquema de la versión anterior. Los datos son sintéticos (nunca PII, AGENTS.md §4.3), con un volumen que ejercita índices y paginación. Cada test está aislado de los demás. |
| P5 | **Dependencias externas controladas** | Cada dependencia externa tiene un doble con contrato verificado y fallas inyectables (latencia, 5xx, timeout). La suite no depende de internet. |

### Eje B — La suite valida el sistema

| ID | Propiedad | Se cumple (2) cuando… |
|---|---|---|
| P6 | **Cobertura funcional** | Cada caso de uso del enunciado y cada endpoint o evento público tiene al menos un escenario feliz y uno negativo contra el entorno, y se verifica el efecto en el consumidor asíncrono. |
| P7 | **Contratos vivos** | REST se valida contra OpenAPI en los 4 servicios. Los eventos AMQP se validan contra su JSON Schema en el broker. Hay un chequeo de compatibilidad contra la versión anterior. |
| P8 | **Resiliencia** | En el entorno integrado se inyectan caídas de broker, base y servicio, mensajes duplicados, desordenados y malformados, y se verifica DLQ, idempotencia y recuperación. |
| P9 | **No funcionales y seguridad** | La carga con umbrales SLO es parte del gate. Hay escaneo de dependencias e imágenes. Hay pruebas de autenticación y autorización (credenciales del broker hoy; tokens desde E6). |

### Eje C — La señal es confiable

| ID | Propiedad | Se cumple (2) cuando… |
|---|---|---|
| P10 | **Observabilidad verificable** | Un test comprueba que el `traceId` se propaga por HTTP y por AMQP entre servicios. Los logs estructurados se analizan como gate. Health y métricas se exponen como en producción. |
| P11 | **Gate honesto y reproducible** | Un test que no corre no cuenta como verde. Ningún paso requerido es tolerado. Hay tiempo máximo por job. El mismo comando da el mismo resultado local y en CI. |

### Eje D — Lo validado es lo que se entrega

| ID | Propiedad | Se cumple (2) cuando… |
|---|---|---|
| P12 | **Promoción y despliegue** | El artefacto validado se promueve sin reconstruirse. El despliegue es reproducible desde el repo. Un smoke post-despliegue corre contra el entorno real. El rollback está ensayado. |

## 3. Estado actual contra el criterio

**Puntaje: 9 de 24.** Ningún eje cumple completo. El eje B (validación) y el eje D (entrega) son los más débiles.

| ID | Puntaje | Evidencia principal |
|---|:-:|---|
| P1 | 1 | `[OBSERVED]` Preprod usa la imagen `pr-<N>` del servicio cambiado. Si no la encuentra, usa la de la rama base con solo un aviso (`main.yml:699-704`). Los servicios no cambiados toman el tag mutable `:<base>`. Al mergear, `merge.yml` vuelve a construir y publica `:sha-<7>` sin pasar por preprod (`merge.yml:150-287`): **la imagen publicada no es la validada**. |
| P2 | 1 | `[OBSERVED]` Están los 4 servicios, PostgreSQL, RabbitMQ, MinIO y n8n (`docker-compose.preprod.yml:3-262`). Falta el proveedor `externo`, que Donaciones tiene configurado por defecto en `localhost:8084` (`donaciones-service/.../application.properties:23,31`) y que preprod no levanta. Faltan la base documental (E5), auth y cliente (E6). Cada servicio corre en una sola instancia. |
| P3 | 1 | `[OBSERVED]` No existe ningún `application-preprod*`: el perfil `preprod` solo cambia el formato de logs (`logback-spring.xml:67-72`). Hay ajustes de velocidad que producción no tiene: outbox a 1 s contra 10 s (`docker-compose.preprod.yml:113`), `SEED_CATALOGO_ENABLED=true` (`:114`) y JVM de arranque rápido (`TieredStopAtLevel=1`, `SerialGC`, `Xmx384m`). No hay TLS, cuando producción lo usa con Neon y CloudAMQP. `rabbitmq:3.13-alpine`, `postgres:16-alpine` y `grafana/k6:latest` usan tags flotantes; MinIO va por digest y n8n con tag fijo. No hay límites de recursos. A favor: los secretos salen de `.env` con `:?` y se generan en CI (`scripts/generar-env-db.sh`). |
| P4 | 1 | `[OBSERVED]` Flyway corre desde cero en cada corrida (`down -v`, sin volumen de PostgreSQL). Los roles están aislados por schema (`persistencia/init-db/01-init-schemas-roles.sql`). Nunca se migra desde el esquema anterior. Donaciones no tiene base en `main` (`donaciones-service/pom.xml` sin JPA). No hay `@AfterEach`/`@AfterAll` en los ITs, y matching y ranking son globales: un test puede afectar a otro `[INFERRED]`. |
| P5 | 1 | `[OBSERVED]` Notificaciones y el planificador externo son simuladores dentro del código de producción (`*AdapterSimulado`, `ProveedorExternoPlanificacionSimulado`), sin fallas inyectables. Los workflows de n8n publican con una llamada HTTP a `httpbin.org` (`n8n/workflow-insignias.json:44`, `workflow-ranking-mensual.json:44`). `[INFERRED]` Como preprod importa y publica esos workflows (`main.yml:726-730`), **CI depende de internet**. No hay WireMock en ningún `pom`. |
| P6 | 1 | `[OBSERVED]` Hay 29 métodos `@Test` en 9 clases IT, que cubren unos 36 de unos 100 endpoints `[INFERRED por conteo]`: Donaciones 13/47, Notificaciones 3/8, Incentivos 8/20, Logística 12/25. El E2E recorre el camino feliz (`FullDistributedDonationE2EIT`). El único camino negativo de negocio es `NO_RECIBIDA` (`LogisticaIntegrationIT.java:77-121`). Los efectos asíncronos se verifican en el consumidor con polling (`PollingUtils`). |
| P7 | 1 | `[OBSERVED]` `ContractIT` valida en vivo con `swagger-request-validator` contra 3 servicios; de Donaciones solo carga la spec (`ContractIT.java:74`). El contrato AMQP está diferido: el test solo comprueba que exista un archivo de schema (`ContractIT.java:262-271`). `validate-contracts.js` es estático y corre solo en `agent-governance.yml:44`. No hay chequeo de compatibilidad hacia atrás. |
| P8 | 0 | `[OBSERVED]` Ningún IT inyecta fallas, duplicados, desorden ni mensajes malformados. Hay tests de componente con DLQ e idempotencia (listeners de Logística, Incentivos y Notificaciones; `OutboxRetrySchedulerTest`), pero no en el entorno integrado. El ADR [tolerancia al desorden](../adr/20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests.md) propone Toxiproxy en preprod (`proposed`). |
| P9 | 0 | `[OBSERVED]` k6 no corre en CI. Localmente, `run-preprod-tests.sh --groups performance` ejecuta solo 1 de los 2 scripts, durante 25 s (`run-preprod-tests.sh:162-171`). No hay OWASP dependency-check, Trivy ni escaneo de imágenes. No hay tests de `X-API-Key` ni `X-Actor`. |
| P10 | 1 | `[OBSERVED]` Preprod emite NDJSON y `analyze_preprod_logs.py` falla el job ante una anomalía `CRITICAL` (`main.yml:793-796`, `analyze_preprod_logs.py:577`). `TracingContractIT` solo comprueba que el header `X-Trace-Id` aparezca en `/v3/api-docs`, no que se propague entre servicios ni por AMQP. Actuator expone solo `health`. No hay métricas. |
| P11 | 1 | `[OBSERVED]` A favor: no hay `|| true` ni `continue-on-error` en preprod, y `report_test_failures.py --fail-on-zero-tests` falla con 0 tests (`main.yml:801`). En contra: **un test salteado cuenta como verde**. `ContractIT` y `LogisticaIntegrationIT` se saltean por `assumeTrue` si `/v3/api-docs` no responde 200 en 500 ms o tira una excepción (`BaseIT.java:35-53`, `ContractIT.java:41,88,140`, `LogisticaIntegrationIT.java:24`). Sonar no bloquea el gate (`main.yml:912`). Ningún job tiene `timeout-minutes`. `RepositoriosJpaTest`, el test de persistencia real de Notificaciones, está excluido de Surefire en el `pluginManagement` del pom raíz (`pom.xml:135-139`). Hay 3 scripts locales con semánticas distintas: `run-preprod-tests.sh` construye desde fuente y corre todos los ITs; CI usa imágenes de GHCR y grupos; `run-hybrid-tests.sh` excluye `LogisticaIntegrationIT`. |
| P12 | 0 | `[OBSERVED]` No hay job de despliegue. Render, Neon y CloudAMQP se configuran a mano, fuera del repo (arquitectura-sistema.md §7, deuda T16). `run-hybrid-tests.sh` prueba contra Render, pero es manual y no forma parte de CI. El rollback sería manual con los tags `sha-<7>` `[INFERRED]`. `main.yml` solo corre en PR: **el estado después del merge nunca se valida en preprod** (`main.yml:44-46`). |

## 4. Hallazgos de auditorías anteriores

| Hallazgo | Fuente | Estado hoy |
|---|---|---|
| INC-01 — CI corría ITs con `mvn test` | revisión DevOps | ✅ Cerrado: `mvn verify` con Failsafe (`main.yml:773,781`) |
| INC-02 / §3.1 — reporte que siempre devolvía 0 | revisión DevOps | ✅ Cerrado: códigos de salida y `--fail-on-zero-tests` (`report_test_failures.py:323-331`). Queda abierto el caso de tests salteados (P11) |
| INC-03 / V-05 — tags `:latest` | revisión DevOps | 🟡 Parcial: n8n fijo y MinIO por digest; siguen flotantes k6, RabbitMQ y PostgreSQL |
| INC-04 / V-04 — API de GHCR fija a `users/` | revisión DevOps | ✅ Cerrado: detecta el tipo de cuenta y usa `orgs/` o `users/` (`merge.yml:365-370`) |
| INC-05 — `node-version: lts/*` y `log4brains` sin versión | revisión DevOps | ❌ Abierto (`deploy-pages.yml:26,67`) |
| V-01 — contenedores como root | revisión DevOps | ✅ Cerrado: usuario 1001 en los Dockerfiles |
| V-02 — interpolación `${{ }}` en `run:` | revisión DevOps | Sin verificar en este relevamiento |
| V-03 — arranque frágil de n8n | revisión DevOps | 🟡 Parcial: importa con `--separate` y sin `|| true`, pero sigue publicando por IDs fijos (`main.yml:726-730`) |
| V-06 — puertos publicados en el host | revisión DevOps | 🟡 Parcial: PostgreSQL, RabbitMQ y MinIO ya no publican puertos; los servicios (8080-8083) y n8n (5678) sí |
| V-07 — esperas secuenciales | revisión DevOps | Sin verificar en este relevamiento |
| QA-GAP-01 — idempotencia y DLQ | backlog QA | 🟡 Solo a nivel componente; no en preprod (P8) |
| QA-GAP-02 — persistencia real con Testcontainers | backlog QA | 🟡 Logística e Incentivos sí; el test de Notificaciones existe pero está excluido (`pom.xml:138`); Donaciones sin base |
| QA-GAP-03 — timeouts Feign con WireMock | backlog QA | ❌ Abierto |
| QA-GAP-04 — concurrencia en Logística | backlog QA | Sin verificar en este relevamiento |
| QA-GAP-05 — E2E de caminos negativos | backlog QA | 🟡 Parcial: `NO_RECIBIDA` solo dentro de Logística |
| QA-GAP-06 — PITest ampliado | backlog QA | ❌ Abierto: solo `AlgoritmoCompatibilidadSemantica` y el perfil nunca corre en CI |

## 5. Brechas priorizadas

El orden pondera dos cosas: si la brecha **produce un verde falso** (el gate aprueba algo roto) y cuánto riesgo de producción deja sin probar. El tamaño se cuenta en archivos o piezas `[INFERRED]`; las soluciones son de la fase 2.

| # | Brecha | Propiedades | Por qué importa | Tamaño |
|:-:|---|---|---|---|
| B1 | **Verdes falsos en el gate**: los tests salteados cuentan como verdes, la imagen base reemplaza en silencio a la del PR, Sonar es opcional y `RepositoriosJpaTest` está excluido | P11, P1 | El gate puede aprobar un PR cuyo contrato o cuya Logística nunca se probó | 4 archivos: `main.yml`, `BaseIT.java`, `report_test_failures.py`, `pom.xml` |
| B2 | **Lo publicado no es lo validado, y el estado post-merge no se prueba** | P1, P12 | La imagen `:sha` del merge nunca pasó por preprod. `[INFERRED]` Dos PRs verdes pueden romper la base al combinarse | 2 workflows |
| B3 | **La producción híbrida no está representada**: Render, Neon y CloudAMQP con TLS, suspensión por inactividad y despliegue manual sin smoke | P2, P3, P12 | Lo que falla solo en la nube (TLS, latencia de internet, arranque en frío, credenciales) se descubre en la defensa | 1 workflow nuevo, 1 compose, `run-hybrid-tests.sh` |
| B4 | **Contratos asíncronos sin validar**, Donaciones sin validación REST en vivo y sin chequeo de compatibilidad | P7 | El grueso de la integración es AMQP (ADRs de topología y broker): un cambio de payload pasa el gate | `ContractIT` y 1 utilidad de consumo AMQP |
| B5 | **Resiliencia sin probar en el entorno integrado** | P8 | Outbox, DLQ, idempotencia y tolerancia al desorden son decisiones de arquitectura (ADRs) que hoy solo se prueban por servicio | 1 contenedor de inyección de fallas y 1 clase IT |
| B6 | **Cobertura funcional baja y sin aislamiento de datos** | P6, P4 | `[INFERRED]` Unos 64 de unos 100 endpoints nunca se ejercitan contra el entorno (conteo de P6). Los datos que quedan de un test pueden afectar el matching y el ranking de otro | Incremental: 1 clase IT por flujo |
| B7 | **Configuración que no es la de producción**: perfil vacío, ajustes de velocidad, JVM de arranque, tags flotantes, sin límites de recursos | P3 | El entorno se comporta distinto que producción en memoria, tiempos y versiones | 1 compose y 4 `application-*.properties` |
| B8 | **No funcionales y seguridad fuera del gate** | P9 | No hay línea base de rendimiento ni detección de dependencias vulnerables. Auth (E6) va a llegar sin un arnés que la pruebe | 1 job y 1 script k6 |
| B9 | **Dependencia de internet y externos sin fallas inyectables** | P5 | `httpbin.org` caído rompe CI sin que el código cambie. No se prueba qué pasa si un proveedor tarda o falla | 2 workflows de n8n y 1 doble HTTP |
| B10 | **Migraciones solo desde cero** | P4 | E5 cambia el modelo de datos de Logística y Donaciones; una migración que no aplica sobre datos existentes pasa el gate | 1 paso de CI |
| B11 | **La trazabilidad no se verifica de punta a punta** | P10 | El invariante de `traceId` (AGENTS.md §4.2) solo se comprueba con un header, sin seguir la traza entre servicios | 1 test |
| B12 | **Paridad local/CI y tiempos máximos** | P11 | Tres scripts dan resultados distintos para la misma pregunta. Un job colgado consume el runner sin límite | 3 scripts y `timeout-minutes` |

## 6. Deuda documental encontrada de paso

No se corrige en esta fase (AGENTS.md §6). Se deja registrada:

- `integration-tests.md` dice que `ErrorHandlingIntegrationIT` cubre 409 y `fieldErrors`, y el código no lo hace. Dice que el umbral de Incentivos es p95<500, y el script usa p95<300. Dice que el E2E valida RabbitMQ, y no hay ninguna aserción AMQP. Dice que `--groups performance` corre k6 completo, y corre un solo script.
- `FullDistributedDonationE2EIT.java:103` comenta "llamada asíncrona Feign", pero el proveedor `donatrack` usa transporte AMQP (`application.properties` de Donaciones, `proveedor.donatrack.transporte=amqp`).
- `DonacionesApiClient.obtenerPersona` llama a `GET /api/personas/{id}`, que `PersonasController` no mapea. Ningún IT lo usa.
- ~~La exclusión de `RepositoriosJpaTest` en `pom.xml:138` no está registrada como deuda en `DEUDA_TECNICA.md`.~~ **Corregido en la fase 3:** ya estaba registrada en el ítem 8 de DTI-16 ([roadmap, §1.1](preprod-fiel-roadmap.md#11-correcciones-a-las-fases-1-y-2)).

## 7. Fases del análisis

Las tres fases están completas.

- **Fase 1 — Criterio y medición:** este documento.
- **Fase 2 — Arquitectura objetivo:** hecha, en [preprod-fiel-arquitectura-objetivo.md](preprod-fiel-arquitectura-objetivo.md). Para cada brecha B1–B12, alternativas con matriz ponderada. Por ejemplo, para B3: compose local con TLS y emulación de la nube, entorno efímero en la nube por PR, o validación de staging en Render después del merge. Restricciones a respetar `[DOCUMENTED]`: compatibilidad con los scripts de evaluación de la cátedra, runners gratuitos de GitHub (7 GB de RAM) y plan gratuito de Render.
- **Fase 3 — Roadmap y decisiones:** hecha, en [preprod-fiel-roadmap.md](preprod-fiel-roadmap.md). Ordena RM-01 a RM-18 por entrega (E5–E7 y después) y proyecta el puntaje en 10, 17, 23 y 24 de 24. Deja un ADR `proposed`, [promoción de artefactos por digest y despliegue reproducible de Logística](../adr/20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md), y dos entradas de deuda, DTI-24 y DTI-25, en [`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md).
