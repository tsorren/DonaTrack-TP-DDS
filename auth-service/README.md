# auth-service — Proveedor de identidad (Keycloak)

> **Estado:** decidido, sin implementar. El backlog del MVP está en [SPEC-06](../docs/specs/active/SPEC-06-auth-mvp-keycloak.md).
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Qué es

`auth-service` es el **proveedor de identidad (IdP)** de DonaTrack. Según el [ADR del proveedor de identidad](../docs/adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) (`proposed`, 2026-11-04), es un **Keycloak configurado**. No hay un IdP propio escrito en Java.

En lenguaje llano:
- Keycloak guarda las cuentas, comprueba la clave y entrega un **pase firmado** (token JWT) con el id de la persona (`sub`) y sus roles (`DONANTE`, `ENTIDAD`, `ADMIN`).
- El cliente liviano manda a la persona a la pantalla de login de Keycloak y guarda el pase en su sesión. El navegador solo tiene una cookie.
- Los servicios comprueban el pase con las claves públicas que Keycloak publica (JWKS). Eso llega en la etapa 2 del backlog.

## Qué va a contener esta carpeta

| Archivo | Contenido | Ítem del backlog |
|---|---|---|
| `Dockerfile` | Imagen de Keycloak con versión fija, el realm y el tema | A1-01 |
| `realm/donatrack-realm.json` | Realm versionado: roles, políticas de clave y bloqueo, vida de los tokens, cliente `cliente-liviano`, mapper `roles`. Sin secretos | A1-02, A1-03 |
| `themes/donatrack/` | Tema mínimo de la pantalla de login (logo, colores, español) | A1-04 |

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

Pendiente: lo documenta A1-01 junto con el servicio en `docker-compose.yml`.
