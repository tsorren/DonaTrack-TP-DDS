# auth-service — Proveedor de identidad (Keycloak)

> **Estado:** A1-01 hecho (Keycloak levanta en el compose con un realm vacío); el realm real (A1-02, A1-03) y el tema (A1-04) están pendientes. El backlog del MVP está en [SPEC-06](../docs/specs/active/SPEC-06-auth-mvp-keycloak.md).
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Qué es

`auth-service` es el **proveedor de identidad (IdP)** de DonaTrack. Según el [ADR del proveedor de identidad](../docs/adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) (`proposed`, 2026-11-04), es un **Keycloak configurado**. No hay un IdP propio escrito en Java.

En lenguaje llano:
- Keycloak guarda las cuentas, comprueba la clave y entrega un **pase firmado** (token JWT) con el id de la persona (`sub`) y sus roles (`DONANTE`, `ENTIDAD`, `ADMIN`).
- El cliente liviano manda a la persona a la pantalla de login de Keycloak y guarda el pase en su sesión. El navegador solo tiene una cookie.
- Los servicios comprueban el pase con las claves públicas que Keycloak publica (JWKS). Eso llega en la etapa 2 del backlog.

## Qué contiene esta carpeta

| Archivo | Contenido | Ítem del backlog | Estado |
|---|---|---|---|
| `Dockerfile` | Keycloak 26.8.1 fijado por digest; copia el realm y el tema, corre `kc.sh build` y arranca con `start --optimized --import-realm` | A1-01 | Hecho |
| `realm/donatrack-realm.json` | Realm versionado: roles, políticas de clave y bloqueo, vida de los tokens, cliente `cliente-liviano`, mapper `roles`. Sin secretos. **Hoy es un stub** (`realm` y `enabled`) para que la imagen compile | A1-02, A1-03 | Pendiente |
| `themes/donatrack/` | Tema mínimo de la pantalla de login (logo, colores, español). **Hoy solo tiene un `.gitkeep`** | A1-04 | Pendiente |

Más adelante, la extensión (SPI) que publica los emails de cuenta en RabbitMQ (D8, E2-06).

Los secretos (clave del admin inicial, secreto del cliente OAuth2, base y Google) entran **solo por variables de entorno**. En el repo, solo `.env.example`.

## Qué no es

- **No es el Key Broker.** La custodia de claves para crypto-shredding ([DTI-07](../docs/adr/DEUDA_TECNICA.md)) será un servicio aparte en la Entrega 7 (decisión D29 de los [requerimientos de E6](../docs/requerimientos/requerimientos-e6.md)). Mientras tanto, `notificaciones-service` sigue con `LocalKeyBrokerAdapter`.
- **No integra la rama `E6_auth_service`** (IdP propio con Spring Authorization Server). Esa rama queda como referencia: sus reglas de bloqueo y de clave pasan a la configuración del realm.

## ADRs relevantes

| ADR | Descripción | Estado |
|-----|-------------|--------|
| [Proveedor de identidad: Keycloak configurado](../docs/adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) | D1 de E6: Keycloak como IdP, contrato de tokens y alternativas descartadas | `proposed` |
| [DTI-07 — Dependencia diferida de auth-service para Key Broker](../docs/adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) | Solución interina `LocalKeyBrokerAdapter`; con Keycloak, el Key Broker pasa a un servicio aparte | `proposed` |
| [Protección de PII: Crypto-Shredding y desacoplamiento de mensajes](../docs/adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) | Arquitectura de referencia del Derecho al Olvido | `proposed` |

## Cómo levantarlo

1. Variables: `cp .env.example .env` (valores de desarrollo) o `scripts/generar-env-db.sh` (valores aleatorios). Keycloak usa `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `KC_HOSTNAME`, `KC_HTTP_ENABLED` y `KC_HEALTH_ENABLED`. Si ya tenías un `.env`, **agregale esas variables a mano**: `docker compose` falla con `ver .env.example` en cualquier comando, aunque no levantes Keycloak.
2. Levantar: `docker compose up --build keycloak` (arrastra `postgres`). La consola queda en <http://localhost:8180> con el admin de `KC_BOOTSTRAP_ADMIN_*`, que **no** es el `ADMIN` de DonaTrack.
3. Verificar: `docker compose ps` debe mostrar `keycloak` en `healthy`. La primera vez tarda hasta un minuto.

> **Volumen previo:** el init de Postgres (`persistencia/init-db/`) corre solo cuando el volumen está vacío. Si ya tenías `postgres_data`, la base `keycloak` y su rol no existen y Keycloak no arranca: hace falta `docker compose down -v` (borra también los datos de los demás servicios en local). Por la misma razón, cambiar `KEYCLOAK_DB_PASSWORD` o `KC_BOOTSTRAP_ADMIN_*` después del primer arranque exige `docker compose down -v`.

El healthcheck usa el puerto de gestión (9000) **dentro** del contenedor y no lo publica: el 9000 del host es de MinIO. La imagen no trae `curl`; el chequeo abre el socket con `bash` y espera un `200` en `/health/ready`.

`docker-compose.preprod.yml` todavía no incluye Keycloak (E2-12). Su Postgres comparte el script de init y crea la base `keycloak` vacía y sin contraseña, que nadie usa.
