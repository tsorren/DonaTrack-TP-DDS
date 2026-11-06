# auth-service — Proveedor de identidad (Keycloak)

> **Estado:** A1-01 (Keycloak en el compose), A1-02 (realm: tokens y políticas de seguridad) y A1-03 (admin y rol por defecto) hechos y verificados: `RealmTokenContractIT` 20 de 20 en verde y `docker compose` en `healthy`. A1-04 (tema de login y registro) implementado con paleta y logo provisorios, todavía sin ver en pantalla. El backlog del MVP está en [SPEC-06](../docs/specs/active/SPEC-06-auth-mvp-keycloak.md).
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
| `realm/donatrack-realm.json` | Realm versionado: roles, políticas de clave y bloqueo, vida de los tokens, cliente `cliente-liviano`, mapper `roles`. Sin secretos. **Hoy tiene** el contrato de tokens (A1-02a), las políticas de seguridad (A1-02b) y el admin con el rol por defecto (A1-03) | A1-02, A1-03 | Hecho y verificado |
| `.dockerignore` | Deja afuera `src`, `target` y `pom.xml` del contexto de build (que es esta carpeta) | A1-02 | Hecho |
| `pom.xml` y `src/test/java/grupo5/auth/RealmTokenContractIT.java` | Módulo Maven de test: levanta la imagen con Testcontainers, hace el login por *authorization code* y verifica los tokens. No está en los `<modules>` del pom raíz (ver abajo) | A1-02 | A1-02 y A1-03: 20 de 20 en verde (2026-11-06). A1-04: 2 casos nuevos, sin ejecutar |
| `themes/donatrack/login/` | Tema de las pantallas de login y registro: `theme.properties` (extiende `keycloak.v2`), `resources/css/donatrack.css` y `resources/img/logo.svg`. **Paleta y logo provisorios** | A1-04 | Hecho, sin ver en pantalla |

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

## Contrato de tokens (A1-02a)

El realm `donatrack` define roles `DONANTE`, `ENTIDAD` y `ADMIN`, login por email y un único cliente, `cliente-liviano`: confidencial, solo *authorization code* (sin *password grant*, sin *implicit*, sin cuenta de servicio). Su secreto y sus URLs salen de `CLIENTE_LIVIANO_SECRET` y `CLIENTE_LIVIANO_BASE_URL`, que el realm lee al importarse (`${...}`).

| Token | Lleva | No lleva |
|---|---|---|
| Access token | `sub`, `roles` (plano, multivalor, solo los de negocio), `aud` = `donatrack-api`, `exp`, `iat`, `iss`; vence a los 15 min | `email`, `name`, `given_name`, `family_name`, `preferred_username` (RNF-AUTH-09) |
| ID token | `sub`, `roles`, `aud` = `cliente-liviano` | los mismos datos personales |

El cliente solo recibe el scope `basic` (que trae `sub`): no se le asignan `profile` ni `email`, así que **ningún token lleva datos personales**. Es más estricto que la SPEC, que lo pide solo para el access token; si B1-03 necesita mostrar el email o el nombre, se decide entonces (por ejemplo, un scope propio solo para el ID token). Con `fullScopeAllowed=false` y los tres roles como único *scope mapping*, el claim `roles` no trae `default-roles-donatrack` ni `offline_access`. La sesión (SSO) vence tras 8 h sin actividad.

## Políticas de seguridad (A1-02b)

| Política | Valor | Dónde se ve |
|---|---|---|
| Clave | `length(10) and maxLength(128) and notEmail` | Admin API y formulario de registro rechazan claves de menos de 10 o más de 128 caracteres, o iguales al email |
| Fuerza bruta | 5 fallos en 15 min bloquean 15 min (temporal, sin escalar) | El 6.º intento con la clave correcta falla con el mismo mensaje que una clave mala: no revela si la cuenta existe ni si está bloqueada |
| Refresh | `revokeRefreshToken=true` y `refreshTokenMaxReuse=0` | Cada refresh entrega uno nuevo; reusar uno viejo da `invalid_grant` y corta la sesión entera |
| Perfil de usuario | Nombre y apellido **opcionales**; el email sigue obligatorio para el usuario | Sirve también para una persona jurídica |

El perfil de usuario va en el realm como un JSON dentro de un string (`components` → `kc.user.profile.config`), copia del perfil por defecto de Keycloak menos el `required` de `firstName` y `lastName`. Keycloak también aplica una espera corta de 60 s si los fallos llegan con menos de 1 s de diferencia (valores por defecto, no están en la SPEC).

## Admin inicial y rol por defecto (A1-03)

| Qué | Cómo queda |
|---|---|
| Admin de DonaTrack | Un único usuario importado con `${AUTH_ADMIN_EMAIL}` y `${AUTH_ADMIN_PASSWORD}`, **solo** con el rol `ADMIN`. No es el admin de la consola de Keycloak (`KC_BOOTSTRAP_ADMIN_*`). |
| Autorregistro | Habilitado (`registrationAllowed`): "Registrarse" crea una cuenta con email y clave, sin nombre ni apellido. |
| Rol por defecto | `default-roles-donatrack` = `DONANTE` + los dos roles de sistema de Keycloak (`offline_access`, `uma_authorization`). Lo reciben las cuentas creadas por el registro o por la Admin API. `ENTIDAD` y `ADMIN` no se autoasignan (D10). |

- **El admin no hereda `DONANTE`:** Keycloak solo asigna el rol por defecto a las cuentas creadas por el registro o por la Admin API; a un usuario importado le da exactamente los roles que se le listan.
- **La clave del admin tiene que cumplir la política del realm** (10 a 128 caracteres y distinta del email). Se espera que el import la valide y que con una clave corta o igual al email **Keycloak no arranque** (`[INFERRED]`: no hay un test negativo que lo pruebe). Los valores de `.env.example` y los de `generar-env-db.sh` la cumplen.
- **`[INFERRED]`:** el rol por defecto no incluye los roles del cliente `account` (`view-profile`, `manage-account`), que Keycloak agrega en un realm nuevo. Sin ellos, la consola de cuenta de Keycloak no es accesible para los usuarios; el MVP no la usa. Cambiar la clave desde la sesión (RF-AUTH-12) puede pedir sumarlos.

## Tema de login y registro (A1-04)

`themes/donatrack/login/` extiende el tema `keycloak.v2` (PatternFly 5) sin templates propios: un `theme.properties`, un CSS y un logo. El realm lo activa con `loginTheme`, y también fija el español como único idioma (`defaultLocale`) y el nombre visible `DonaTrack` (`displayName`, que es lo que Keycloak muestra en el encabezado).

- **Provisorio:** la paleta (verdes y neutros) y el logo son placeholders hasta que llegue el `tokens.css` de la diseñadora (D20). El CSS sigue los dos niveles de D20: **primitivos** (la paleta: `--verde-700`) y **semánticos** (el uso: `--color-accion-primaria`); las reglas usan solo los semánticos. Para cambiar la identidad alcanza con tocar los primitivos o apuntar los semánticos a los de `tokens.css`, y con reemplazar `logo.svg` (mismo nombre, con `viewBox` y `<title>`).
- **Accesibilidad:** la paleta cumple WCAG AA (RNF-UI-09): `test_auth_theme.py` calcula los ratios de contraste de texto, botón, enlaces y error, así que un color nuevo que no llegue a 4.5:1 hace fallar el test.
- **`[INFERRED]`:** los nombres de las variables de PatternFly 5 (`--pf-v5-global--primary-color--100`…) y los selectores (`#kc-header-wrapper`, `.pf-v5-c-login`) no se pudieron comprobar contra la hoja de estilos real. Si algún color no cambia en pantalla, el ajuste va en `donatrack.css`.

**Cómo verlo.** El tema lo activa el realm al importarse, así que con una base ya existente hace falta `docker compose down -v` (borra los datos locales) y después `docker compose up --build -d keycloak`. Si no querés borrar la base, se puede activar a mano en la consola de Keycloak: *Realm settings → Themes → Login theme*.

**Capturas (el test de A1-04).** Con Keycloak arriba, abrí estas dos URL (el `redirect_uri` tiene que coincidir con `CLIENTE_LIVIANO_BASE_URL` de tu `.env`) y sacá una captura de cada una a **360 px** y a **1280 px** (DevTools → modo dispositivo):

- Login: `http://localhost:8180/realms/donatrack/protocol/openid-connect/auth?client_id=cliente-liviano&redirect_uri=http://localhost:8090/login/oauth2/code/keycloak&response_type=code&scope=openid`
- Registro: la misma URL con `&prompt=create` al final.

Qué mirar: el logo se ve completo (también a 360 px), el botón es verde con texto blanco legible, no hay scroll horizontal, los textos están en español y los enlaces muestran el foco al navegar con teclado.

## Cómo probar el contrato

- **Estático, sin Docker:** `python -m unittest scripts/tests/test_auth_realm_contract.py` (y `test_auth_keycloak_compose.py` para el compose). Lee el JSON del realm y las variables.
- **De runtime, con Docker:** `mvn -f auth-service/pom.xml -Pcontrato-auth verify`. Construye la imagen del `Dockerfile`, la arranca con `start-dev` y la base en memoria (no usa Postgres: eso lo cubre `docker compose up`), crea un donante por la Admin API, inicia sesión contra `cliente-liviano` y lee los claims.
- **Si Testcontainers dice `Could not find a valid Docker environment` (Windows):** es del entorno, no del realm. Con Docker Desktop abierto, probá `$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"` antes del `mvn`, y borrá la línea `docker.client.strategy=…` de `%USERPROFILE%\.testcontainers.properties` (Testcontainers cachea ahí la última estrategia que funcionó).
- El módulo hereda del pom raíz (Spotless, JaCoCo) pero **no está en sus `<modules>`**: el `mvn` general no lo compila. Sumarlo al build y al CI queda para E2-12. Si Spotless se queja del formato, `mvn -f auth-service/pom.xml spotless:apply`.

## Cómo levantarlo

1. Variables: `cp .env.example .env` (valores de desarrollo) o `scripts/generar-env-db.sh` (valores aleatorios). Keycloak usa `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `KC_HOSTNAME`, `KC_HTTP_ENABLED`, `KC_HEALTH_ENABLED`, `CLIENTE_LIVIANO_SECRET`, `CLIENTE_LIVIANO_BASE_URL`, `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD`. Si ya tenías un `.env`, **agregale esas variables a mano**: `docker compose` falla con `ver .env.example` en cualquier comando, aunque no levantes Keycloak.
2. Levantar: `docker compose up --build keycloak` (arrastra `postgres`). La consola queda en <http://localhost:8180> con el admin de `KC_BOOTSTRAP_ADMIN_*`, que **no** es el `ADMIN` de DonaTrack.
3. Verificar: `docker compose ps` debe mostrar `keycloak` en `healthy`. La primera vez tarda hasta un minuto.

> **Volumen previo:** el init de Postgres (`persistencia/init-db/`) corre solo cuando el volumen está vacío. Si ya tenías `postgres_data`, la base `keycloak` y su rol no existen y Keycloak no arranca: hace falta `docker compose down -v` (borra también los datos de los demás servicios en local). Por la misma razón, cambiar `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_*` o las variables `CLIENTE_LIVIANO_*` o `AUTH_ADMIN_*` después del primer arranque exige `docker compose down -v`: el realm se importa solo si todavía no existe.

El healthcheck usa el puerto de gestión (9000) **dentro** del contenedor y no lo publica: el 9000 del host es de MinIO. La imagen no trae `curl`; el chequeo abre el socket con `bash` y espera un `200` en `/health/ready`.

`docker-compose.preprod.yml` todavía no incluye Keycloak (E2-12). Su Postgres comparte el script de init y crea la base `keycloak` vacía y sin contraseña, que nadie usa.
