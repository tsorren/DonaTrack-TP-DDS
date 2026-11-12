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
| `themes/donatrack/login/` | Tema de todas las pantallas de Keycloak con la identidad Raíces (piel Huerta): `theme.properties` (extiende `keycloak.v2`, sin modo oscuro), `resources/css/tokens.css`, `fuentes/` y `img/logo.svg` (copias idénticas de las del sitio, `scripts/sincronizar_tema.py`), `css/donatrack.css` y `messages/messages_es.properties` | A1-04, E2-09 | Hecho; capturas de 17 pantallas en Firefox real |

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
- **Google:** identity provider `google` con `${GOOGLE_CLIENT_ID}` y `${GOOGLE_CLIENT_SECRET}`. **Arranca apagado**: se enciende con `GOOGLE_HABILITADO=true` junto con las dos credenciales (sin eso el botón no aparece y todo lo demás anda). El primer ingreso usa el flujo propio `donatrack first broker login`: si el email ya tiene cuenta, se confirma el vínculo y se pide **la contraseña de esa cuenta** (D4); no ofrece el camino por email de Keycloak ni vincula sin prueba. Una cuenta sin contraseña no puede vincularse por ahí (RF-AUTH-04, pendiente de D12): recupera la clave y después vincula. El botón dice "Continuar con Google" gracias a `themes/donatrack/login/messages/messages_es.properties`. Ver el [ADR](../docs/adr/auth-service/20261112-vinculacion-de-google-por-contrasena-con-flujo-propio-de-keycloak.md). **Prueba de punta a punta:** `python scripts/e2e_google_vinculacion.py` (simula Google con un segundo realm; en Windows, `py`). Como Keycloak tiene que alcanzar a ese realm desde su contenedor, `KC_INTERNAL_URL` vale `http://localhost:8080` por defecto.

## Correo de cuenta: el SPI `donatrack-amqp` (E2-06)

El correo de recuperar la clave y el de verificar el email **no salen por SMTP**: un SPI de Keycloak (`spi/`) publica el evento `cuenta.enlace-solicitado.v1` en RabbitMQ y `notificaciones-service` manda el correo. Ver el [ADR](../docs/adr/auth-service/20261112-correo-de-cuenta-por-rabbitmq-con-spi-de-keycloak.md) y el [catálogo de eventos](../docs/arquitectura/eventos-amqp.md).

- **Proyecto autónomo** en `auth-service/spi/` (su propio `pom.xml`, sin el pom raíz): el `Dockerfile` lo compila en una etapa de Maven, porque su contexto es solo `auth-service/`. Se prueba con `mvn -f auth-service/spi/pom.xml spotless:check verify` (lo corre el CI).
- **El proveedor se elige al construir la imagen** (`KC_SPI_EMAIL_TEMPLATE__PROVIDER` en el `Dockerfile`), no al arrancar: con `start --optimized` Keycloak se niega a iniciar si ese valor cambia después del build. La conexión a RabbitMQ (`KC_SPI_EMAIL_TEMPLATE__DONATRACK_AMQP__HOST`, `__USER`, `__PASSWORD`) sí es de ejecución y la pasa el compose.
- **`emailTemplate` es un SPI interno** de Keycloak ("puede cambiar sin aviso"). Al actualizar Keycloak hay que reconstruir y correr `scripts/e2e_correo_de_cuenta.py`.
- **Si RabbitMQ no está, la persona no ve un error** (Keycloak muestra la misma página, a propósito, para no revelar qué cuentas existen). La única señal es el log de Keycloak: `KC-SERVICES0026`. Conviene alertar sobre eso.
- El enlace y el email **no se escriben en ningún log del SPI**. Ojo: el logger de eventos de Keycloak sí imprime el `username` (acá, el email) cuando falla el envío.
- **Prueba de punta a punta** contra el stack del compose local levantado, parado en la raíz del repo: `python scripts/e2e_correo_de_cuenta.py` (en Windows, `py scripts\e2e_correo_de_cuenta.py`). Lee las credenciales del `.env` y no hace falta exportar nada. Necesita la API de administración de RabbitMQ (puerto 15672), que la imagen del compose local trae y la del preprod no.

## Ingresar con Google: las credenciales (E2-05)

"Continuar con Google" **no funciona con credenciales vacías o inventadas**: Google responde `Error 401: invalid_client` ("The OAuth client was not found"), porque no conoce ese `GOOGLE_CLIENT_ID`. Hace falta un cliente OAuth propio en Google Cloud (es gratis y no requiere verificar la app):

1. En <https://console.cloud.google.com/> creá un proyecto (o elegí uno).
2. *APIs y servicios → Pantalla de consentimiento de OAuth*: tipo **Externo**, nombre `DonaTrack` y tu email de soporte. Los permisos son solo `openid` y `email` (no sensibles). Mientras la app está en modo **Pruebas**, solo pueden ingresar los emails que agregues en **Usuarios de prueba**: agregá el tuyo, o Google responde "Acceso bloqueado: la app no completó la verificación".
3. *Credenciales → Crear credenciales → ID de cliente de OAuth*: tipo **Aplicación web**. En **URI de redireccionamiento autorizados** poné exactamente `${KC_HOSTNAME}/realms/donatrack/broker/google/endpoint`, o sea `http://localhost:8180/realms/donatrack/broker/google/endpoint` con el `.env` de ejemplo (el `google` del final es el alias del proveedor en el realm). No hace falta ningún origen de JavaScript.
4. Copiá `.env.google.example` a **`.env.google`** (Git lo ignora: lleva un secreto) y pegá el ID y el secreto del cliente. **No pongas el secreto en el `.env` ni en el repo** (RNF-AUTH-05).
5. Levantá el stack sumando ese archivo: `docker compose --env-file .env --env-file .env.google up -d --build`. El realm se importa con la base vacía, así que si ya estaba levantado hacé `docker compose down -v` antes.
   - **Sin borrar los datos**, también se puede cargar en la consola de Keycloak (*Identity providers → google*: ID y secreto del cliente, y activarlo).
6. Probá: en el login aparece "o · Continuar con Google"; al tocarlo, Google pide la cuenta y vuelve a Keycloak. Con un email nuevo se crea la cuenta; con uno que ya tiene cuenta se pide su contraseña (D4).

Para probar **el flujo** sin cuenta de Google (cuenta nueva, vínculo por contraseña), `python scripts/e2e_google_vinculacion.py` lo simula con un segundo realm; lo que no prueba es lo propio de Google: sus pantallas y estas credenciales.

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

## Tema de login y registro (A1-04, E2-09)

`themes/donatrack/login/` extiende el tema `keycloak.v2` (PatternFly 5) sin plantillas propias y le pone **la identidad Raíces, piel Huerta**: papel crema, tarjetas más claras con esquinas de 16 px, títulos en Archivo ancho y pesado, texto en DM Sans, botón principal verde con la sombra plana de hoja y la marca "Dona**Track**". Es el mismo aspecto que el sitio, y cubre **todas** las pantallas que muestra Keycloak: ingreso (con error), registro (con error), recuperar la clave, aviso de enviado, modificar la contraseña, verificar el email, actualizar el perfil, cuenta desactivada, enlace inválido, cerrar sesión, página caducada y las del vínculo con Google.

- **La fuente de verdad es `tokens.css` del sitio (D20).** El tema vive en otra imagen y no puede leer los archivos del sitio, así que lleva una copia **idéntica** de `css/tokens.css`, de las tres tipografías (con sus licencias OFL) y del logo. Cuando el sitio cambia alguno, se vuelve a copiar con `python scripts/sincronizar_tema.py`; `test_auth_theme.py` falla si la copia quedó vieja (`--verificar` lo comprueba sin copiar). `donatrack.css` usa **solo tokens de uso** (`--color-*`, `--fuente-*`, `--radio-*`, `--sombra-papel`), nunca los valores base `--p-*`, y no escribe colores a mano.
- **Sin modo oscuro** (`darkMode=false` en `theme.properties`). El sitio tiene una sola piel; si el login cambiara con el sistema, quien lo tiene en oscuro pasaría de un sitio crema a una pantalla oscura. Keycloak declara entonces `color-scheme: light` y no agrega el script que cambia de modo.
- **La marca** es `displayNameHtml: Dona<em>Track</em>` en el realm (el sanitizador de Keycloak deja pasar el `<em>`), con el ícono del sitio como `::before`. El nombre sigue siendo texto para los lectores de pantalla.
- **Textos en español** que Keycloak 26.8.1 no trae traducidos: `signInWithProvider` ("Continuar con Google", RF-AUTH-02) y `orLabel` ("o").
- **Accesibilidad:** `test_auth_theme.py` calcula el contraste WCAG AA (RNF-UI-09) de cada par de colores que usa el tema (texto, enlaces, botones, avisos de cuatro tipos y el foco) a partir de `tokens.css`, así que un token nuevo que no llegue hace fallar el test.
- **Verificado con capturas reales** (Firefox 160 sobre un Keycloak 26.8.1) de 17 pantallas a 360 y 1280 px, sin desborde horizontal.
- **Límites.** "La cuenta ya existe" (el vínculo con Google) sale como aviso rojo: es el tipo que Keycloak elige para esa pantalla y cambiarlo exige reemplazar su plantilla. La consola de cuenta (`/realms/donatrack/account`) y los correos **no usan este tema**: el sitio no enlaza a la consola y los correos de cuenta los manda `notificaciones-service`.

**Cómo verlo.** Los archivos del tema van dentro de la imagen: alcanza con `docker compose up --build -d keycloak`. La marca en dos colores (`displayNameHtml`) y el tema vienen del realm, que se importa una sola vez: la primera vez que se activa hace falta `docker compose down -v` (borra los datos locales); también se puede activar a mano en la consola de Keycloak: *Realm settings → Themes → Login theme*. Si el navegador muestra el estilo viejo, es su caché: Keycloak sirve las hojas con una URL que no cambia entre reinicios.

**Capturas.** Con Keycloak arriba, abrí estas dos URL (el `redirect_uri` tiene que coincidir con `CLIENTE_LIVIANO_BASE_URL` de tu `.env`), en un ancho de 360 y de 1280 px:

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
