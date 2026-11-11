# auth-service — Proveedor de identidad (Keycloak)

> **Estado:** A1-01 (Keycloak en el compose), A1-02 (realm: tokens y políticas de seguridad) y A1-03 (admin y rol por defecto) hechos y verificados: `RealmTokenContractIT` 20 de 20 en verde y `docker compose` en `healthy`. A1-04 (tema de login y registro) hecho con paleta y logos provisorios: el IT de 22 casos pasó y las capturas, repetidas tras corregir el modo oscuro y el botón, las confirmó el equipo. El bloque A está completo. El backlog del MVP está en [SPEC-06](../docs/specs/active/SPEC-06-auth-mvp-keycloak.md).
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
| `pom.xml` y `src/test/java/grupo5/auth/RealmTokenContractIT.java` | Módulo Maven de test: levanta la imagen con Testcontainers, hace el login por *authorization code* y verifica los tokens. No está en los `<modules>` del pom raíz (ver abajo) | A1-02 | 22 de 22 en verde (2026-11-06); desde entonces el IT suma un chequeo del logo oscuro, sin ejecutar |
| `themes/donatrack/login/` | Tema de las pantallas de login y registro: `theme.properties` (extiende `keycloak.v2`), `resources/css/donatrack.css` y `resources/img/logo.svg` y `logo-oscuro.svg`. **Paleta y logos provisorios** | A1-04 | Hecho y confirmado con capturas |

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

El realm `donatrack` define roles `DONANTE`, `ENTIDAD` y `ADMIN`, el rol técnico `SERVICIO` y login por email. El cliente `cliente-liviano` es confidencial: *authorization code* para las personas y cuenta de servicio (*client credentials*) para el token técnico, sin *password grant* ni *implicit*. Su secreto y sus URLs salen de `CLIENTE_LIVIANO_SECRET` y `CLIENTE_LIVIANO_BASE_URL`, que el realm lee al importarse (`${...}`).

| Token | Lleva | No lleva |
|---|---|---|
| Access token | `sub`, `roles` (plano, multivalor, solo los de negocio), `permisos` y `personaId` si la cuenta los tiene, `aud` = `donatrack-api`, `exp`, `iat`, `iss`; vence a los 15 min | `email`, `name`, `given_name`, `family_name`, `preferred_username` (RNF-AUTH-09) |
| ID token | `sub`, `roles`, `permisos`, `personaId`, `aud` = `cliente-liviano` | los mismos datos personales |
| Token de servicio | `roles` = `SERVICIO`, `aud` con `donatrack-api` | `permisos`, `personaId` |

El cliente solo recibe el scope `basic` (que trae `sub`): no se le asignan `profile` ni `email`, así que **ningún token lleva datos personales**. Es más estricto que la SPEC, que lo pide solo para el access token; si B1-03 necesita mostrar el email o el nombre, se decide entonces (por ejemplo, un scope propio solo para el ID token). Con `fullScopeAllowed=false` y los cuatro roles como único *scope mapping* de realm, el claim `roles` no trae `default-roles-donatrack`. La sesión (SSO) vence tras 8 h sin actividad.

## Permisos, personaId y cuenta de servicio (E6)

- **Catálogo:** el cliente `donatrack-api` (bearer-only, sin flujos) tiene como roles de cliente los doce permisos y las cuatro plantillas de la [matriz de permisos](../docs/arquitectura/seguridad/matriz-permisos.md) (§2 y §3); las plantillas son roles compuestos. `scripts/tests/test_auth_realm_contract.py` compara el realm con las tablas de la matriz.
- **Claim `permisos`:** mapper de roles de cliente de `donatrack-api`, sin prefijo. Como `cliente-liviano` tiene `fullScopeAllowed=false`, sus `clientScopeMappings` traen los doce permisos (todos, porque una persona puede tener permisos sueltos) y ninguna plantilla: el claim lleva los permisos efectivos y no el nombre de la plantilla.
- **`personaId` y `solicitudEntidad`:** atributos declarados en el perfil de usuario, que solo el admin ve y edita. `personaId` viaja en los tokens; `solicitudEntidad` no. El `personaId` es **el vínculo entre la cuenta y la `Persona`** de Donaciones (la `Persona` no guarda el `sub`): decisión y consecuencias en el [ADR del vínculo](../docs/adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md) (`proposed`).
- **Cuenta de servicio:** `service-account-cliente-liviano`, declarada en el realm para que reciba **solo** `SERVICIO` (sin el rol por defecto, que le daría `DONANTE`) y `manage-users`, `view-users` y `query-users` de `realm-management`. La Admin API además exige que esos roles estén en el scope del cliente, por eso también van en sus `clientScopeMappings`. No puede leer los roles del realm ni los proveedores de identidad. Como tampoco ve los clientes, `donatrack-api` tiene el id fijo `7f3c2a9e-5b1d-4e8a-9c6f-2d4b8a1e0c53`: con él asigna plantillas y permisos desde `/users/{id}/role-mappings/clients/{id}/available`.
- **Registro (D11, R14):** la cuenta se crea en Keycloak y el sitio pide "Completá tu perfil". Toda cuenta creada por la Admin API recibe el rol por defecto (`DONANTE`): una cuenta de entidad pendiente tiene `DONANTE` pero ningún permiso.
- **Google:** identity provider `google` con `${GOOGLE_CLIENT_ID}` y `${GOOGLE_CLIENT_SECRET}` (opcionales en el compose: vacías, todo anda menos "Continuar con Google"). El primer ingreso usa el flujo `first broker login`, que pide la clave para vincular una cuenta existente (D4).

## Cuenta de pruebas `donatrack-pruebas` (E2-07)

Cliente confidencial con **solo client credentials** y una cuenta de servicio con el rol `ADMIN` y nada más (sin el rol por defecto, sin roles de `realm-management`). La usan `integration-tests`, k6 y Postman para llamar a los servicios cuando la seguridad está encendida (`run-preprod-tests.sh --auth`). Su token lleva `roles` = `ADMIN` y `aud` con `donatrack-api`, y no tiene `permisos` ni `personaId`. Su secreto sale de `PRUEBAS_CLIENT_SECRET`.

- **Es la credencial más poderosa del realm.** Ser `ADMIN` de DonaTrack pasa todas las reglas de los servicios, aunque no da ningún poder sobre Keycloak: la Admin API le responde `403` (lo comprueba `RealmTokenContractIT`).
- **En un entorno real, `PRUEBAS_CLIENT_SECRET` tiene que ser un valor aleatorio que nadie use.** El realm importa el cliente siempre; lo que lo vuelve inofensivo es que nadie conozca el secreto.
- Con `fullScopeAllowed` en `false`, el rol solo llega al token porque el realm lo declara en `scopeMappings`. Un test del contrato lo fija.
- No hay cuentas de servicio para Incentivos ni Donaciones porque no hacen llamadas HTTP internas en el despliegue estándar (ver M-08 de la [matriz de permisos](../docs/arquitectura/seguridad/matriz-permisos.md)).

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
| Rol por defecto | `default-roles-donatrack` = `DONANTE` + `uma_authorization` en el JSON. **En runtime sigue `offline_access`:** Keycloak crea el rol por defecto con él antes de leer el JSON, y el import solo suma composites. No se usa porque `cliente-liviano` no tiene el scope `offline_access` (lo comprueba el IT); sacarlo del rol exige un paso por la Admin API después del import. Lo reciben las cuentas creadas por el registro o por la Admin API. `ENTIDAD` y `ADMIN` no se autoasignan (D10). |

- **El admin no hereda `DONANTE`:** Keycloak solo asigna el rol por defecto a las cuentas creadas por el registro o por la Admin API; a un usuario importado le da exactamente los roles que se le listan.
- **La clave del admin tiene que cumplir la política del realm** (10 a 128 caracteres y distinta del email). Se espera que el import la valide y que con una clave corta o igual al email **Keycloak no arranque** (`[INFERRED]`: no hay un test negativo que lo pruebe). Los valores de `.env.example` y los de `generar-env-db.sh` la cumplen.
- **Consola de cuenta:** en runtime el rol por defecto incluye `manage-account` y `view-profile`, así que el usuario usa la Account API de Keycloak (el IT prueba que no puede cambiar su `personaId`).

## Tema de login y registro (A1-04)

`themes/donatrack/login/` extiende el tema `keycloak.v2` (PatternFly 5) sin templates propios: un `theme.properties`, un CSS y dos logos (`logo.svg` y `logo-oscuro.svg`). El realm lo activa con `loginTheme`, y también fija el español como único idioma (`defaultLocale`) y el nombre visible `DonaTrack` (`displayName`, que es lo que Keycloak muestra en el encabezado).

- **Provisorio:** la paleta (verdes y neutros) y los logos son placeholders hasta que llegue el `tokens.css` de la diseñadora (D20). El CSS sigue los dos niveles de D20: **primitivos** (la paleta: `--verde-700`) y **semánticos** (el uso: `--color-accion-primaria`); las reglas usan solo los semánticos. Para cambiar la identidad alcanza con tocar los primitivos o apuntar los semánticos a los de `tokens.css`, y con reemplazar los logos (mismos nombres, con `viewBox` y `<title>`).
- **Modo claro y modo oscuro.** Keycloak 26 activa solo el tema oscuro de PatternFly (clase `pf-v5-theme-dark` en `<html>`) cuando el sistema operativo está en oscuro. El CSS redefine ahí los semánticos de fondo, superficie, texto, enlace y error, y cambia al logo oscuro (mismo dibujo, texto claro: con el texto oscuro el nombre desaparecería sobre el fondo oscuro).
- **Accesibilidad:** la paleta cumple WCAG AA (RNF-UI-09) **en los dos modos**: `test_auth_theme.py` calcula 4.5:1 para texto y 3:1 para el botón contra la tarjeta, así que un color nuevo que no llegue hace fallar el test.
- **Botón principal.** Tiene una regla directa porque las variables globales de PatternFly no le llegaban: en las primeras capturas seguía azul. Esas variables sí alcanzan para otros componentes (enlaces, foco, error), pero si algún componente conserva el azul, se le agrega una regla directa igual.
- **`[INFERRED]`:** el nombre de la clase del modo oscuro (`pf-v5-theme-dark`) y las variables de PatternFly se tomaron de PatternFly 5 y de lo que muestran las capturas; no se pudieron comprobar contra la hoja de estilos de Keycloak.

**Cómo verlo.** Los archivos del tema van dentro de la imagen: alcanza con `docker compose up --build -d keycloak`. Solo hace falta `docker compose down -v` (borra los datos locales) la primera vez que se activa el tema, porque `loginTheme` viene del realm y el realm se importa una sola vez; también se puede activar a mano en la consola de Keycloak: *Realm settings → Themes → Login theme*.

**Capturas (el test de A1-04).** Con Keycloak arriba, abrí estas dos URL (el `redirect_uri` tiene que coincidir con `CLIENTE_LIVIANO_BASE_URL` de tu `.env`):

- Login: `http://localhost:8180/realms/donatrack/protocol/openid-connect/auth?client_id=cliente-liviano&redirect_uri=http://localhost:8090/login/oauth2/code/keycloak&response_type=code&scope=openid`
- Registro: la misma URL con `&prompt=create` al final.

Sacá una captura de cada una a **360 px** y a **1280 px** en modo claro, y una de cada una a 360 px en modo oscuro. En Chrome, Edge u Opera (DevTools): `Ctrl+Shift+M` para el modo dispositivo con ancho 360 px, y *⋮ → More tools → Rendering → Emulate CSS media feature prefers-color-scheme* para alternar entre claro y oscuro. Ojo: una ventana angosta del navegador no equivale a 360 px de viewport.

Qué mirar: el logo se ve completo y con buen contraste en los dos modos, el botón es verde con texto blanco legible, los enlaces ("Registrarse", "Volver a la identificación") se leen bien, el fondo de la página y la tarjeta son coherentes entre sí, no hay scroll horizontal, y los textos están en español.

## Cómo probar el contrato

- **Estático, sin Docker:** `python -m unittest scripts/tests/test_auth_realm_contract.py` (y `test_auth_keycloak_compose.py` para el compose). Lee el JSON del realm y las variables.
- **De runtime, con Docker:** `mvn -f auth-service/pom.xml -Pcontrato-auth verify`. Construye la imagen del `Dockerfile`, la arranca con `start-dev` y la base en memoria (no usa Postgres: eso lo cubre `docker compose up`), crea un donante por la Admin API, inicia sesión contra `cliente-liviano` y lee los claims.
- **Si Testcontainers dice `Could not find a valid Docker environment` (Windows):** es del entorno, no del realm. Con Docker Desktop abierto, probá `$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"` antes del `mvn`, y borrá la línea `docker.client.strategy=…` de `%USERPROFILE%\.testcontainers.properties` (Testcontainers cachea ahí la última estrategia que funcionó).
- El módulo hereda del pom raíz (Spotless, JaCoCo) pero **no está en sus `<modules>`**: el `mvn` general no lo compila (a propósito: no produce un JAR, sino la imagen de Keycloak; el CI lo trata aparte, ver [Keycloak en preprod y en el CI](#keycloak-en-preprod-y-en-el-ci)). Si Spotless se queja del formato, `mvn -f auth-service/pom.xml spotless:apply`.

## Probar el ingreso

1. En el `.env` (de `.env.example`) están `CLIENTE_LIVIANO_SECRET`, `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD`. Con `DONACIONES_SEGURIDAD_HABILITADA=true` en el `.env`, Donaciones valida el token (por defecto es `false` y acepta todo).
2. Levantar Keycloak y Donaciones: `docker compose up --build -d keycloak donaciones-service` (arrastran Postgres y RabbitMQ).
3. Levantar el sitio, con el mismo secreto del `.env`: `CLIENTE_LIVIANO_SECRET=<el del .env> mvn -pl cliente-liviano spring-boot:run`. Queda en <http://localhost:8090>; sin el perfil `maqueta` pide ingresar, y `DONACIONES_URL` y `KEYCLOAK_ISSUER_URI` valen `http://localhost:8080` y `http://localhost:8180/realms/donatrack` si no se cambian.
4. Cuentas de prueba: el admin es `AUTH_ADMIN_EMAIL` con `AUTH_ADMIN_PASSWORD`. El donante y la entidad se crean con "Crear una cuenta" en Keycloak (pide completar el perfil) y la entidad espera la aprobación del admin en "Entidades".

**Keycloak importa el realm solo la primera vez.** Si el realm `donatrack` ya existe en la base, un `donatrack-realm.json` nuevo no se aplica. Para reimportarlo: `docker compose down -v` (borra todos los datos locales) y volver a levantar, o borrar solo el realm en la consola (<http://localhost:8180>, *Manage realms → donatrack → Action → Delete*) y reiniciar con `docker compose restart keycloak`.

## Cómo levantarlo

1. Variables: `cp .env.example .env` (valores de desarrollo) o `scripts/generar-env-db.sh` (valores aleatorios). Keycloak usa `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `KC_HOSTNAME`, `KC_HTTP_ENABLED`, `KC_HEALTH_ENABLED`, `CLIENTE_LIVIANO_SECRET`, `CLIENTE_LIVIANO_BASE_URL`, `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD`. Si ya tenías un `.env`, **agregale esas variables a mano**: `docker compose` falla con `ver .env.example` en cualquier comando, aunque no levantes Keycloak.
2. Levantar: `docker compose up --build keycloak` (arrastra `postgres`). La consola queda en <http://localhost:8180> con el admin de `KC_BOOTSTRAP_ADMIN_*`, que **no** es el `ADMIN` de DonaTrack.
3. Verificar: `docker compose ps` debe mostrar `keycloak` en `healthy`. La primera vez tarda hasta un minuto.

> **Volumen previo:** el init de Postgres (`persistencia/init-db/`) corre solo cuando el volumen está vacío. Si ya tenías `postgres_data`, la base `keycloak` y su rol no existen y Keycloak no arranca: hace falta `docker compose down -v` (borra también los datos de los demás servicios en local). Por la misma razón, cambiar `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_*` o las variables `CLIENTE_LIVIANO_*` o `AUTH_ADMIN_*` después del primer arranque exige `docker compose down -v`: el realm se importa solo si todavía no existe.

El healthcheck usa el puerto de gestión (9000) **dentro** del contenedor y no lo publica: el 9000 del host es de MinIO. La imagen no trae `curl`; el chequeo abre el socket con `bash` y espera un `200` en `/health/ready`.

## Keycloak en preprod y en el CI

`docker-compose.preprod.yml` incluye Keycloak como servicio `keycloak` (imagen `donatrack/auth-service:preprod`, puerto 8180 solo en loopback, mismo `environment` y healthcheck que el compose local). Su Postgres recibe `KEYCLOAK_DB_PASSWORD`. Ningún otro servicio depende de Keycloak todavía: lo valida el gateway en E2-01.

`auth-service` no produce un JAR, así que el CI no lo trata como los servicios Java:

| Pipeline | Qué hace con `auth-service` |
|---|---|
| PR (`main.yml`), job `auth-service` | Corre si cambió `auth-service/**` o un archivo global. Spotless del módulo, contrato del realm (`mvn -f auth-service/pom.xml -Pcontrato-auth verify`, necesita Docker) y, si pasa, publica `ghcr.io/<repo>/auth-service:pr-<N>`. El quality gate lo exige solo si cambió. |
| Preprod del PR | Si cambió, exige su imagen `pr-<N>`; si no, la fija por digest del manifiesto de la rama base. |
| Merge (`merge.yml`), job `auth-publish` | Corre el contrato sobre el estado mergeado, publica el candidato `:sha-<7>` y guarda `digest-auth-service`. El manifiesto candidato lo incluye, el preprod post-merge lo valida y `promote` lo etiqueta como a los demás. |

> **Transición:** hasta que el primer merge de esta etapa llegue a `promote`, el manifiesto de la rama base no trae `auth-service` y un PR que no lo toca falla en el preprod con `auth-service no está en …`. Se resuelve solo con ese merge; no hay un camino alternativo a propósito (RM-06: sin digest validado no hay imagen).

`scripts/tests/test_auth_ci_preprod.py` verifica sin Docker que el compose de preprod, el resolver y los dos workflows estén de acuerdo. Lo que no cubre es que `keycloak` quede `healthy` en el preprod y que los workflows corran en GitHub: se confirma en el primer PR.
