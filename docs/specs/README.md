# Especificaciones Técnicas y Funcionales (Specs) — DonaTrack

> **Catálogo Canónico de Especificaciones de Software (SDD)**  
> **Alineación Normativa:** [`AGENTS.md`](../../AGENTS.md) §2, §7, [`docs/context-index.md`](../context-index.md) y [`informe-agent-friendly-repository-architecture.md`](../agentes/informe-agent-friendly-repository-architecture.md).

---

## 1. Propósito

Este directorio constituye la **fuente de verdad (*System of Record*)** para los requerimientos funcionales y el diseño técnico detallado de tareas medianas y grandes en DonaTrack.

En consonancia con la disciplina **Spec-Driven Development (SDD)**:
* Ninguna tarea `STANDARD` o `ARCHITECTURAL` se implementa sin un spec previo.
* Las decisiones de alcance acordadas con el usuario y los trade-offs analizados quedan plasmados aquí para transferir contexto entre distintas sesiones de agentes o desarrolladores humanos.

---

## 2. Estructura de Directorios

```text
docs/specs/
├── README.md              # Este documento normativo
├── active/                # Especificaciones en curso de diseño o implementación
└── completed/             # Especificaciones implementadas, verificadas y cerradas
```

---

## 3. Ciclo de Vida de una Spec

```text
[Requerimiento] ──► skill: define-spec
                         │
                         ▼
        docs/specs/active/<task-id>-spec.md  (Status: APPROVED_BY_USER)
                         │
                         ▼ (si es ARCHITECTURAL)
                    skill: design-spec
                         │
                         ▼
                    skill: design-review (Status: DESIGN_APPROVED)
                         │
                         ▼
                    skill: implement-task
                         │
                         ▼
                    skill: implementation-review (Status: PASS)
                         │
                         ▼
       docs/specs/completed/<task-id>-spec.md
```

---

## 4. Convención de Nomenclatura

Los archivos deben seguir el formato:
* `docs/specs/active/SPEC-<id>-<nombre-descriptivo>.md`
* Ejemplo: `docs/specs/active/SPEC-01-donaciones-mision-validacion.md`

Al completar la implementación y pasar todos los Quality Gates, el archivo se traslada a `docs/specs/completed/` como registro histórico inmutable del desarrollo.

---

## 5. Índice de Especificaciones Activas

| ID | Título y Enlace | Nivel | Estado | Alcance / Propósito Principal |
|:---:|---|:---:|:---:|---|
| **SPEC-03** | [`SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md`](active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md) | ARCHITECTURAL | `ACTIVE` | Topología AMQP Pub/Sub canónica DDD, colas segregadas, clúster Dead Letter (`notificaciones.dlx`), envelope nativo y Hard Cutover inter-servicios. |
| **SPEC-04** | [`SPEC-04-broker-integracion-logistica.md`](active/SPEC-04-broker-integracion-logistica.md) | ARCHITECTURAL | `APPROVED_BY_USER` | Broker de Integración con Logística (Entrega 4): selección entre ≥ 2 proveedores (AMQP/HTTP), comando `entrega.solicitada.<id>.v1`, `mandatory` + acuse, outbox basado en datos. |
| **SPEC-05** | [`SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md`](active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md) | ARCHITECTURAL | `APPROVED_BY_USER` | Rediseño del roadmap de preprod fiel (base por servicio, despliegue sin Render, contratos vivos con política de aviso en el PR y bloqueo en el post-merge) e implementación de RM-01 a RM-08. |
| **SPEC-06** | [`SPEC-06-auth-mvp-keycloak.md`](active/SPEC-06-auth-mvp-keycloak.md) | ARCHITECTURAL | `APPROVED_BY_USER` | MVP de autenticación con Keycloak (login con email y clave, registro de donante, menú por rol y logout) y backlog de las etapas siguientes (gateway, Google, entidades, CSV). |
| **SPEC-07** | [`SPEC-07-api-gateway-etapa-1.md`](active/SPEC-07-api-gateway-etapa-1.md) | ARCHITECTURAL | `APPROVED_BY_USER` | API gateway, etapa 1 (ADR-E6-02): módulo `api-gateway` con Spring Cloud Gateway WebMVC, rutas sin reescritura (también `/api/logistica/**`), allowlist escrita y `X-Trace-Id`; sin JWT (F5). |
| **SPEC-08** | [`SPEC-08-conexion-de-pantallas.md`](active/SPEC-08-conexion-de-pantallas.md) | ARCHITECTURAL | `PROPOSED` | Conexión de las pantallas del cliente liviano a las APIs (E6): esqueleto compartido, frentes por rol con lista cerrada de archivos, huecos aditivos (HB-21 Contacto, HB-23 camiones deshabilitados) y lo que queda fuera. |
| **SPEC-09** | [`SPEC-09-cierre-e6-demo-punta-a-punta.md`](active/SPEC-09-cierre-e6-demo-punta-a-punta.md) | ARCHITECTURAL | `PROPOSED` | Cierre de la E6 para la demo punta a punta: S2083, pantallas sin controller (F2), estado `EN_PUERTA` con ADR `proposed` (HB-22), tipo y referencia de la notificación (HB-09), token del cliente hacia todos los servicios (F5), demo en el preprod (F6) y barrido de Sonar. |
| **SPEC-10** | [`SPEC-10-seeder-de-demo.md`](active/SPEC-10-seeder-de-demo.md) | ARCHITECTURAL | `PROPOSED` | Seeder de demo: seis meses de historia en todos los servicios más lo que pasa hoy (camión en ruta, entrega en la puerta, pendientes del admin), con la seguridad prendida o apagada. Solo agrega `demo/seeder/` y el overlay `docker-compose.seed.yml`. |
| **R11-p1-importaciones** | [`R11-p1-importaciones-spec.md`](active/R11-p1-importaciones-spec.md) | ARCHITECTURAL | `PROPOSED` | Lote `p1-importaciones` del backlog R11 (R11-20): `GET /api/donantes/archivos` paginado, con búsqueda por nombre y filtros por estado y fecha; nombre y fecha de subida en el archivo (`V22`); pantalla `admin-importaciones`, descarga de las filas con error y el avance que pasa a `ERROR` sin filas (DTI-40, ítem 7). |
| **R11-p1-notificaciones** | [`R11-p1-notificaciones-spec.md`](active/R11-p1-notificaciones-spec.md) | STANDARD (ruta CONTRATO) | `PROPOSED` | Lote `p1-notificaciones` del backlog R11 (R11-14): la bandeja por persona de Notificaciones pagina, busca sin tildes, filtra por tipo y ordena en la base, con el total en `X-Total-Count` (mismo contrato que R11-p0) y el índice `V6`; el cliente pide de a 20 y dibuja sin leer y leídas según la decisión del usuario. |
| **R11-p1-importador** | [`R11-p1-importador-spec.md`](active/R11-p1-importador-spec.md) | STANDARD | `PROPOSED` | Lote `p1-importador` del backlog R11 (R11-B08): el importador lee el CSV de donantes de a tandas de 500 sin cargarlo entero y Notificaciones reintenta el aviso de bienvenida cuando la persona todavía no llegó, antes de mandarlo a la cola de mensajes muertos. Sin cambios de contrato. |

---

## 6. Índice de Especificaciones Completadas

| ID | Título y Enlace | Nivel | Estado | Alcance / Propósito Principal |
|:---:|---|:---:|:---:|---|
| **SPEC-01** | [`SPEC-01-consolidacion-nivel-4-agent-first.md`](completed/SPEC-01-consolidacion-nivel-4-agent-first.md) | ARCHITECTURAL | `COMPLETED` | Consolidación del repositorio a Nivel 4 (Agent-First Pleno): arnés SDD, suite de 7 skills, generated knowledge, benchmark evals y governance checks. |
| **SPEC-02** | [`SPEC-02-harness-skill-review-pr-adversarial-grepai.md`](completed/SPEC-02-harness-skill-review-pr-adversarial-grepai.md) | ARCHITECTURAL | `COMPLETED` | Implementación de la skill canónica `review-pr`, protocolo GrepAI-First, scripts CLI de contexto y ADR de formalización. |
| **R11-p0-api-listados** | [`R11-p0-api-listados-spec.md`](completed/R11-p0-api-listados-spec.md) | ARCHITECTURAL | `IMPLEMENTADA` | Lote `p0-api-listados` del backlog R11 (2026-11-11): los listados de Donaciones paginan, buscan sin tildes, filtran y cuentan en la base; resumen del admin como conteos. Cerrada el 2026-11-12 con C3, C9 y C10 sin verificar ([DTI-40](../adr/DEUDA_TECNICA.md#dti-40)). |


