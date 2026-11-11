# Spec: SPEC-06 — MVP de autenticación con Keycloak (backlog)

> **Estado:** APPROVED_BY_USER (2026-11-04, tsorren). Falta la revisión del equipo en el PR.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): introduce una tecnología nueva (Keycloak), cambia los mecanismos de seguridad y tiene impacto cross-service.
> **Fecha:** 2026-11-04
> **Base:** `ENTREGA_6` (`8962906f`). Rama del PR: `E6_auth-service`, hacia `ENTREGA_6`. Ojo: no es `E6_auth_service`, que tiene un IdP propio con Spring Authorization Server y queda como referencia.
> **ADR:** [Proveedor de identidad: Keycloak configurado](../../adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) (`proposed`).
> **ADR del vínculo (2026-11-10):** [`personaId` como atributo de Keycloak](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md) (`proposed`), que enmienda D9 y cierra E2-03.
> **Requerimientos:** [requerimientos de E6](../../requerimientos/requerimientos-e6.md), §4 (IdP) y §9–§10 (decisiones).

---

## 1. Objetivo Funcional (Goal)

Que una persona **inicie sesión con email y clave en el cliente liviano** y que **el sitio sepa su rol** (`DONANTE`, `ENTIDAD` o `ADMIN`) para mostrarle sus secciones. Es la primera etapa del servicio de autenticación de E6. Las etapas siguientes quedan ordenadas en el backlog (§4).

En lenguaje llano:
- **Keycloak** es el "portero": guarda las cuentas, comprueba la clave y entrega un **pase firmado** (token JWT) con el id de la persona y sus roles.
- El **sitio** manda a la persona a la pantalla de Keycloak, recibe el pase y lo guarda en la sesión del servidor. El navegador solo tiene una cookie.
- Los **servicios** del backend van a comprobar ese pase en la etapa 2. En el MVP todavía no lo hacen.

**Criterio de hecho del MVP:**
- Un donante se registra.
- Un donante y el admin inicial entran con su email y su clave.
- Cada uno llega a su inicio y ve su menú.
- Ninguno entra a las secciones de otro rol.
- Al salir, para volver a entrar hay que escribir la clave otra vez.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- **Esta spec (docs):** el ADR del proveedor de identidad, este backlog y la sincronía de `requerimientos-e6.md`, `auth-service/README.md`, `docs/README.md`, `docs/specs/README.md`, `docs/ESTADO_DOCUMENTACION.md` y `docs/adr/DEUDA_TECNICA.md`.
- **Etapa 1 (MVP), en sesiones siguientes:**
  - Keycloak en el compose y el realm `donatrack` versionado.
  - El admin inicial y un tema mínimo para la pantalla de login.
  - El cliente liviano como cliente OIDC: login, registro de donante, menú por rol y logout.

### Out-of-Scope

Queda fuera del MVP. Va a la etapa 2 en adelante (§4):
- Gateway y validación del token en los 4 servicios.
- Spring Session JDBC (D5).
- ~~`sub` en `Persona` y "completar perfil"~~ (E2-03: resuelto con el `personaId` en Keycloak, ver el [ADR del vínculo](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md)).
- Registro de entidad con aprobación.
- Google (D3, D4).
- Recuperación de la clave y verificación del email (D8).
- Client credentials y retiro de `X-API-Key` (D25).
- Alta masiva por CSV (D12).
- Tema completo con la identidad Raíces.
- Cambio de rol.
- Quién entra por una jurídica (D30).

También queda fuera:
- Key Broker: E7, como servicio aparte (D29).
- El código de la rama `E6_auth_service` (Spring Authorization Server): no se integra.

## 3. Decisiones Acordadas y Trade-offs

| # | Decisión (usuario, 2026-11-04) | Alternativas descartadas y por qué |
|---|---|---|
| D1 | Keycloak configurado como IdP. Según el equipo, la cátedra confirmó que cuenta como servicio de autenticación | Spring Authorization Server, Keycloak con fachada propia y JWT casero: ver la matriz del [ADR](../../adr/auth-service/20261104-proveedor-de-identidad-keycloak.md) |
| M1 | El MVP es "IdP + sitio": la validación en los servicios va a la etapa 2 | "Solo el IdP": no cumple el criterio de hecho. "IdP + sitio + servicios": rompe Postman y los tests de integración antes de la entrega |
| M2 | En el MVP, la pantalla de login es la de Keycloak con logo y colores mínimos; el tema Raíces completo, en la etapa 2 | Formulario propio con *password grant* (ROPC): OAuth 2.1 lo elimina, el sitio vería la clave y se pierde el SSO |
| M3 | Rama nueva `E6_auth-service` desde `ENTREGA_6`, sin el código de `E6_auth_service` | Traerlo dejaría código muerto. Sus reglas (bloqueo tras 5 intentos en 15 min; clave de 10 a 128 caracteres y distinta del email) pasan al realm |
| M4 | La parte A del MVP (Keycloak) arranca ya; la parte B (el sitio) espera los PR del cliente liviano | Empezar B sobre `ENTREGA_6`: el módulo `cliente-liviano` todavía no existe ahí (PR 920, 924 y 926 abiertos) |
| M5 | El vínculo entre la cuenta y la `Persona` es el atributo `personaId` en Keycloak, no el `sub` guardado en `Persona` (decisión del usuario, 2026-11-10; enmienda D9) | `sub` en `Persona` con `GET /api/personas/me` (D9 original): obliga a Incentivos, Logística y Notificaciones a consultar a Donaciones para cada pertenencia `[INFERRED]`. Ver el [ADR del vínculo](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md) |

## 4. Backlog

**Reglas:**
1. Cada ítem cierra con su test, la documentación que toca y esta tabla actualizada (fecha y commit).
2. Las cadenas de configuración marcadas `[INFERRED]` se confirman exportando el realm desde la consola de Keycloak antes de versionarlas.
3. Los secretos van solo por variables de entorno (RNF-AUTH-05). En el repo, solo `.env.example`.

### Etapa 1 — MVP. Parte A: Keycloak (sin dependencias, desde `ENTREGA_6`)

| ID | Ítem | Depende de | Hecho cuando | Estado |
|---|---|---|---|---|
| A1-01 | **Keycloak en el compose.** Imagen `quay.io/keycloak/keycloak:26.8.1` fijada por digest, con un `Dockerfile` en `auth-service/` que copia el realm y el tema y corre `kc.sh build`. Servicio en `docker-compose.yml`, puerto 8180 (el 8080 es de Donaciones y el 8085 de Adminer). Base `keycloak` con su rol `keycloak_user` en `persistencia/init-db/01-init-schemas-roles.sql` y su clave en `02-set-role-passwords.sh` (ADR de base por servicio). Variables en `.env.example` y `scripts/generar-env-db.sh`: `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_USERNAME` y `KC_BOOTSTRAP_ADMIN_PASSWORD` (admin de la consola, no de DonaTrack), `KC_HOSTNAME`, `KC_HTTP_ENABLED`, `KC_HEALTH_ENABLED`. Healthcheck por el puerto de gestión 9000 dentro del contenedor (sin publicarlo en el host: el 9000 del host es de MinIO); la imagen no trae curl, así que no puede copiar el de los demás servicios | — | `docker compose up` deja `keycloak` en `healthy`. El README de `auth-service/` avisa que, con un volumen previo, hace falta `docker compose down -v` para crear la base | **Implementado 2026-11-05**, commit pendiente. Verificado sin Docker: `scripts/tests/test_auth_keycloak_compose.py` (27 tests), YAML, sintaxis de scripts y lógica del healthcheck. **Verificado por el equipo (2026-11-05):** `docker compose up --build -d keycloak` deja `keycloak` en `healthy` a los ~30 s, con `postgres` en `healthy`, sobre Docker Desktop (Windows) |
| A1-02 | **Realm `donatrack` versionado** en `auth-service/realm/donatrack-realm.json`, importado al arrancar (`--import-realm`). Contenido:<br>• roles `DONANTE`, `ENTIDAD` y `ADMIN`; login por email (`registrationEmailAsUsername`);<br>• perfil de usuario sin nombre ni apellido obligatorios (sirve también para una jurídica);<br>• política de clave `length(10) and maxLength(128) and notEmail` `[INFERRED]`;<br>• fuerza bruta: 5 fallos en 15 min bloquean 15 min (RNF-AUTH-07);<br>• access token de 15 min (D6) y SSO idle de 8 h (D7);<br>• `revokeRefreshToken=true` con `refreshTokenMaxReuse=0` (D7);<br>• cliente confidencial `cliente-liviano` (*authorization code*), con el secreto por variable, `redirectUris` y `post.logout.redirect.uris`;<br>• mapper de roles con el claim `roles` plano y multivalor en el access token, el ID token y el token liviano (RNF-AUTH-08);<br>• `aud` del API; access token sin email ni nombre (RNF-AUTH-09) | A1-01 | Test con Testcontainers (`GenericContainer` con la imagen del Dockerfile, sin dependencia nueva) que pide un token y verifica `sub`, `roles`, `aud` y `exp − iat = 900 s`, sin `email` ni `name` en el access token. Además, 5 fallos bloquean la cuenta y reusar un refresh rotado corta la sesión | **Hecho y verificado 2026-11-06**, commit pendiente. A1-02a (roles, login por email, cliente `cliente-liviano`, mapper `roles`, `aud`, access de 15 min, SSO idle de 8 h, sin datos personales en los tokens) y A1-02b (perfil de usuario sin nombre obligatorio, política de clave, fuerza bruta 5/15/15, rotación de refresh sin reuso). **Verificado:** `RealmTokenContractIT` 20 de 20 en verde con Testcontainers 2.0.5 sobre Docker Desktop (Windows), contra Keycloak 26.8.1 real, más `scripts/tests/test_auth_realm_contract.py` (con prueba de mutación). Confirmado en runtime: el 6.º intento falla con el mismo mensaje que una clave mala, reusar un refresh corta la sesión entera, el refresh vence a las 8 h y el perfil declarativo importa bien. Spotless y la compilación pasan |
| A1-03 | **Admin inicial y rol por defecto.** El usuario ADMIN del realm se importa con `${AUTH_ADMIN_EMAIL}` y `${AUTH_ADMIN_PASSWORD}`; el compose usa `${VAR:?}` para fallar si faltan. El autorregistro da `DONANTE` y el admin no lo hereda (D10; los `default-roles` del realm se aplican a todo usuario nuevo) | A1-02 | Un test verifica que el admin tiene solo `ADMIN`, que un usuario autorregistrado tiene `DONANTE` y que la clave del admin no quedó como el literal `${...}` | **Hecho y verificado 2026-11-06**, commit pendiente: `registrationAllowed`, rol por defecto `default-roles-donatrack` = `DONANTE`, y el ADMIN importado solo con `ADMIN`; `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD` en `.env.example`, `generar-env-db.sh` y el compose con `${VAR:?}`. **Verificado** en `RealmTokenContractIT` (20 de 20): el admin tiene solo `ADMIN` (directo y efectivo), entra con la clave de la variable y el literal `${AUTH_ADMIN_PASSWORD}` no sirve, y el autorregistro con `prompt=create`, sin nombre ni apellido, termina con un token `roles=[DONANTE]`. **Sin probar** `[INFERRED]`: que el import rechace una clave de admin que no cumple la política (no hay test negativo) y que el rol por defecto sin los roles del cliente `account` alcance para el MVP |
| A1-04 | **Tema mínimo `donatrack`** para la pantalla de login y registro: extiende el tema base de Keycloak, con el logo, los colores de `tokens.css` como CSS propio y el idioma español por defecto | A1-01 | Captura de la pantalla de login y de la de registro en 360 y 1280 px | **Hecho 2026-11-06 con paleta y logos provisorios**, commit pendiente: tema `donatrack` que extiende `keycloak.v2`, tokens en dos niveles (D20) con modo claro y oscuro, realm con `loginTheme`, `displayName` y español como único idioma. **Verificado:** `RealmTokenContractIT` 22 de 22 en verde; `scripts/tests/test_auth_theme.py` (contraste WCAG AA calculado en los dos modos, tokens, logos, realm); y capturas de login y registro. Las primeras capturas mostraron tarjeta oscura sobre fondo claro (modo oscuro de Keycloak), enlaces ilegibles en oscuro y botón azul; se corrigieron y el equipo confirmó las capturas siguientes (2026-11-06). **Sin reporte aún:** el chequeo del logo oscuro agregado al IT después de esa corrida. **A reemplazar:** paleta y logos por `tokens.css` y el logo de la diseñadora. Las capturas de 360 px de la primera ronda se sacaron en una ventana de ~600 px |

### Etapa 1 — MVP. Parte B: el sitio

> **Depende de que se mergeen los PR 920 y 924 (hacia `ENTREGA_6`) y 926 (hacia `E6_corrida_creativa`).** En `ENTREGA_6`, `cliente-liviano` todavía no es módulo de Maven. Si la parte B no puede esperar, va en un PR apilado sobre `E6_raices`, y eso se declara en el PR.
>
> **Actualización 2026-11-10:** en esta rama `cliente-liviano` ya es módulo de Maven y la parte B está en el código (filas B1-01 a B1-04). La condición de los PR dejó de bloquear.

| ID | Ítem | Depende de | Hecho cuando | Estado |
|---|---|---|---|---|
| B1-01 | **Cliente OIDC.** `spring-boot-starter-security` y `spring-boot-starter-security-oauth2-client` (nombres de Spring Boot 4.0.7; el test usa `spring-boot-starter-security-oauth2-client-test`). Registro `keycloak` con el `issuer-uri` por variable. Un `GrantedAuthoritiesMapper` arma `ROLE_*` desde el claim `roles` del ID token. Reglas por ruta: las páginas públicas abiertas; `/donante*`, `/entidad*` y `/admin*`, cada una con su rol. Con el perfil `maqueta`, todo abierto, para que la maqueta y sus capturas sigan iguales | A1-02, PR del cliente | Tests con MockMvc y `oidcLogin()`. `SitioControllerTest` y `-Pvisual` siguen en verde | **Implementado en el código (verificado por lectura, 2026-11-10).** `SeguridadConfig` (`oauth2Login`, reglas por área, `GrantedAuthoritiesMapper` desde `roles` y `permisos`, solo fuera del perfil `maqueta`). Tests: `SeguridadTest` (pantallas públicas abiertas, privada sin sesión → ingresar, área ajena → 403, roles y permisos del ID token como autoridades). **Tests no corridos:** la sesión de verificación no tenía Java ni Maven |
| B1-02 | **Ingresar y registrarse.** "Ingresar" lleva a `/oauth2/authorization/keycloak`. "Registrarse" agrega `prompt=create` con un `OAuth2AuthorizationRequestResolver` propio (el endpoint `/registrations` está deprecado). `publico-ingresar` y `publico-registro-donante` quedan solo en la maqueta | B1-01 | Test del redirect: "Registrarse" lleva `prompt=create` | **Implementado en el código (verificado por lectura, 2026-11-10).** `SolicitudDeAutorizacion` agrega `prompt=create` y también `kc_idp_hint=google` (adelanta parte de E2-05); cualquier otro valor se ignora. Test: `SeguridadTest.crearUnaCuentaLePideAKeycloakElRegistro` y `ingresarSinPedidos…`. **Tests no corridos** |
| B1-03 | **Navegación según el rol.** Los menús por rol (`MENUS` de `SitioController`, perfil `maqueta`) pasan a un componente compartido. Un controller real en `grupo5.cliente.web` llena `navegacion`, `cuenta` y `perfil` desde el `OidcUser`. El inicio depende del rol, con precedencia `ADMIN` > `ENTIDAD` > `DONANTE`; el cambio de rol (RI-AUTH-04) queda para la etapa 2 | B1-01 | Un test por rol: cada uno llega a su inicio y ve su menú; el donante recibe 403 en `/admin*` | **Implementado en el código (verificado por lectura, 2026-11-10).** Tests: `cadaAreaLlegaASuInicioYVeSuMenu`, `elInicioDependeDelAreaDeMasPrecedencia`, `unaAreaAjenaResponde403`. Va más allá de la spec: `PerfilPendiente` y `EstadoDeCuenta` (cuenta sin perfil o con solicitud de entidad solo ve sus pantallas). **Tests no corridos** |
| B1-04 | **Cerrar sesión.** Botón con un formulario POST `/logout` (con CSRF) en `layout-privado.html`. `OidcClientInitiatedLogoutSuccessHandler` con `post_logout_redirect_uri` a la portada, para cerrar también la sesión SSO de Keycloak | B1-01 | Test del logout. De punta a punta: después de salir, volver a entrar pide la clave | **Implementado en el código (verificado por lectura, 2026-11-10).** `OidcClientInitiatedLogoutSuccessHandler` con `post_logout_redirect_uri={baseUrl}/`. Tests: `cerrarSesionCierraTambienLaDeKeycloakYVuelveALaPortada` y `cerrarSesionSinElTokenCsrfResponde403`. **Sin probar** el "de punta a punta" (volver a entrar pide la clave): necesita un Keycloak real. **Tests no corridos** |
| B1-05 | **Punta a punta y documentación.** Test con Playwright contra Keycloak en un contenedor, en un perfil propio (no corre en el `mvn test` por defecto). Recorrido: registro de donante, inicio y logout; login del admin. `auth-service/README.md` explica cómo levantarlo, las variables y los usuarios; el README del cliente explica cómo correrlo con Keycloak | B1-02, B1-03, B1-04, A1-04 | El recorrido pasa y queda grabado (video del perfil `visual`) | **Pendiente.** No hay recorrido contra un Keycloak real: los tests del cliente usan MockMvc y un Keycloak simulado (`DirectorioKeycloakTest`). Playwright solo está en el perfil `visual` |

### Etapa 2 en adelante — en orden, sin detalle

| ID | Ítem | Requerimientos |
|---|---|---|
| E2-01 | Gateway (Spring Cloud Gateway) y validación por JWKS en los 4 servicios, con autorización por rol y por pertenencia | RI-GW-01..04, RI-SVC-01..04, D2, D13, D16, [ADR-E6-02](../../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`) |
| E2-02 | Sesión del cliente en Spring Session JDBC, con base propia | D5 |
| E2-03 | ~~`usuarioId` (= `sub`) en `Persona`, `GET /api/personas/me` y "completar perfil" al primer login~~ **Resuelto con otro diseño (decisión 2026-11-10):** el vínculo es el atributo `personaId` del usuario de Keycloak, que viaja en el token; "completar perfil" ya está en el cliente. Queda `GET /api/personas/me` (por el `personaId` del token) y la baja de personas con cuenta. Ver el [ADR del vínculo](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md) | RI-AUTH-01..03, HB-11, D9, D11 |
| E2-04 | Registro de entidad pendiente de aprobación y pantalla de aprobación del admin | RF-AUTH-06, RI-AUTH-05, RF-21, D10 |
| E2-05 | Login con Google y vinculación por contraseña | RF-AUTH-02..04, D3, D4 |
| E2-06 | Recuperación de la clave y verificación del email: SPI de Keycloak → RabbitMQ → notificaciones | RF-AUTH-13, RF-AUTH-16, D8 |
| E2-07 | Client credentials para n8n, jobs y el proveedor externo; retiro de `X-API-Key` y de `X-Actor` | RF-AUTH-17, RI-SVC-05..08, D15, D25, DTI-09, DTI-14 |
| E2-08 | Alta masiva de usuarios desde el CSV | RF-AUTH-14, RF-AUTH-15, D12 |
| E2-09 | Tema completo de Keycloak con la identidad Raíces | D20 (`tokens.css`) |
| E2-10 | Cambio de rol para quien tiene dos | RI-AUTH-04, RF-CL-03 |
| E2-11 | Cuenta por representante de una jurídica | RI-AUTH-06, D30 |
| E2-12a | Keycloak en `docker-compose.preprod.yml` y en el CI. **Hecho 2026-11-10, commit pendiente.** Servicio `keycloak` en preprod (imagen `donatrack/auth-service:preprod`, 8180 solo en loopback, `environment` y healthcheck idénticos a los del compose local) y `KEYCLOAK_DB_PASSWORD` en su Postgres. `auth-service` **no entra a las matrices Java** (no produce un JAR ni está en los `<modules>`): `main.yml` tiene el job `auth-service` (Spotless, contrato del realm con `-Pcontrato-auth` e imagen `pr-<N>`) y `merge.yml` el job `auth-publish` (contrato, candidato `:sha-<7>` y `digest-auth-service`, que entra al manifiesto y a `promote`). El resolver deja de excluirlo; `api-gateway` sigue excluido. **Verificado sin Docker ni GitHub:** `scripts/tests/test_auth_ci_preprod.py` (40 tests, con prueba de mutación) y YAML de los dos workflows. **Sin probar:** que `keycloak` quede `healthy` en el preprod y que los jobs corran en GitHub Actions; se confirma en el primer PR. **Transición:** hasta el primer `promote` con `auth-service`, un PR que no lo toca falla en el preprod porque el manifiesto base no lo trae | — |
| E2-12b | El cliente liviano en el compose (emisor único para el navegador y el backchannel). **Pendiente:** depende de la parte B (B1-01) | B1-01 |

### Estado verificado de la Etapa 2 y plan de iteraciones (2026-11-10)

> Relevado **leyendo el código** de esta rama, sin compilar ni correr tests: la sesión no tenía Java, Maven ni acceso a Maven Central. Lo marcado como "hecho" significa "el código y sus tests existen", no "los tests pasan". Esta rama avanzó más que la tabla de arriba (que describe `ENTREGA_6`): conviene que el equipo corra `mvn clean test` antes de dar por cerrado cualquier ítem.

| ID | Estado verificado | Qué falta |
|---|---|---|
| E2-01 | **Parcial: aplicada en los cuatro servicios y en el gateway, apagada por defecto.** Donaciones valida el JWT: resource server en `common-lib` (`SeguridadAutoConfiguration`, activable con `donatrack.seguridad.habilitada`, **apagado por defecto**), `@PreAuthorize` por rol y pertenencia en sus controllers (revisé la anotación de clase o de método, no cada regla contra la matriz de permisos), rutas públicas en `ReglasDeAccesoDonaciones`. **Gateway (iteración 4, 2026-11-11):** valida el JWT con la misma cadena de `common-lib`, apagado por defecto (`GATEWAY_SEGURIDAD_HABILITADA`); su allowlist es exactamente el conjunto de endpoints `PUBLICO` de los servicios. el equipo corrió sus tests (63, 0 fallas), que confirman que el `Authorization` llega a los servicios. **Incentivos (iteración 3a, 2026-11-10):** resource server y `@PreAuthorize` en sus 6 controllers según la matriz §7, apagado por defecto (`INCENTIVOS_SEGURIDAD_HABILITADA`); el equipo corrió sus tests (366, 0 fallas). **Logística (iteración 3b, 2026-11-10):** lo mismo, según la matriz §6, con pertenencia por entrega, por donación y por ruta; el equipo corrió sus tests (647, 0 fallas, 1 salteado preexistente). **Notificaciones (iteración 3c, 2026-11-10):** lo mismo, según la matriz §8, con pertenencia por id y por dueño de la notificación; el equipo corrió sus tests (230, 0 fallas) | Probar la seguridad encendida contra un Keycloak real, y la etapa 2 del ADR-E6-02 sigue `proposed`. Con la seguridad encendida, los llamadores sin token reciben `401`: Postman, `integration-tests` y k6 ya piden el token de `donatrack-pruebas` (iteración 5); `NotificacionesFeignClient` (vía legada de Incentivos) no |
| E2-02 | **No.** Ningún `pom.xml` tiene Spring Session; los ingresos del cliente quedan en memoria | Todo |
| E2-03 | **Resuelto (decisión 2026-11-10).** El `personaId` es un atributo del usuario de Keycloak que el cliente escribe por la Admin API y que viaja en el access y el ID token (`Actores`, `Miembro`, `UsuarioActual`, `DirectorioKeycloak`, mappers del realm). Solo el admin lo ve y edita, y un IT contra Keycloak real lo comprueba (`RealmTokenContractIT`). Documentado en el [ADR del vínculo](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md) (`proposed`) | `GET /api/personas/me` por el `personaId` del token (lo piden "Mi perfil" y los legales). Baja o anonimización de una persona con cuenta: nada quita el atributo. Confirmar con el equipo las razones del desvío |
| E2-04 | **Hecho en el cliente.** Atributo `solicitudEntidad`, `SOLICITUD_RECIBIDA`, pantalla del admin y aprobación con CSRF (`SolicitudesDeEntidadController`; tests en `SeguridadTest` y `CompletarPerfilTest`) | HB-12: el estado "pendiente" vive en Keycloak, no en Donaciones. Confirmar si alcanza |
| E2-05 | **Parcial.** `kc_idp_hint=google` en el cliente y proveedor Google en el realm, con `GOOGLE_CLIENT_*` opcionales | Vinculación por contraseña (RF-AUTH-03/04) sin verificar |
| E2-06 | **No.** `auth-service` no tiene extensiones (SPI) | Todo |
| E2-07 | **Parcial: aplicada donde hay llamadores (iteración 5, 2026-11-11).** El cliente pide un token `client_credentials` (cuenta `service-account-cliente-liviano`, rol `SERVICIO`) y reenvía el del usuario. Cuenta nueva `donatrack-pruebas` (rol `ADMIN`) para `integration-tests`, k6 y Postman con la seguridad encendida (`run-preprod-tests.sh --auth`, o `PREPROD_AUTH=true` en CI). `X-Actor` ya no es obligatorio: con token el actor es el `sub`. **No hacen falta tokens entre servicios:** en el despliegue estándar no hay llamadas HTTP internas (Incentivos usa RabbitMQ, el proveedor interno de logística va por AMQP, n8n solo llama a mocks externos). **`X-API-Key` se mantiene** en `ApiKeyFilter` y en el flujo 9: lo usa el proveedor externo | El preprod con `--auth` todavía no corrió, ni local ni en CI. Retirar `X-API-Key` exige una cuenta propia para el proveedor externo (D25). Si se usara la vía legada de Incentivos con la seguridad de Notificaciones encendida, necesitaría un token de servicio |
| E2-08, E2-09, E2-10, E2-11 | **No** (tema mínimo, sin alta por CSV ni cambio de rol) | Todo |
| E2-12a | **Hecho** (2026-11-10, ver fila) | Confirmar `healthy` en preprod y los jobs en GitHub |
| E2-12b | **Parcial.** El gateway ya está en `docker-compose.preprod.yml` (iteración 4); el cliente liviano no | El cliente liviano en `docker-compose.preprod.yml`, con emisor único |

**Convivencia de `X-API-Key` y JWT (decisión de la Iteración 0).** No hace falta un modo de transición: la seguridad por token es opt-in (`donatrack.seguridad.habilitada=false` por defecto y el preprod no la enciende), así que Postman, los tests de integración y el flujo 9 siguen con `X-API-Key`. El retiro de `X-API-Key` es parte de E2-07 y solo se hace cuando el preprod corra con la seguridad encendida y un token de servicio.

**Plan de iteraciones ajustado.** Cada iteración cierra con su test, la documentación que toca y esta tabla actualizada. Las que tocan Java llevan el comando de verificación para que el equipo lo corra.

| Iter. | Qué | Tipo | Depende de | Estado |
|---|---|---|---|---|
| 0 | Auditoría del estado real y ajuste del plan | Solo lectura y docs | — | **Hecha** (2026-11-10) |
| 1 | E2-12a: Keycloak en preprod y CI | Infra, verificada sin Docker | — | **Hecha** (2026-11-10) |
| 2 | **Decisión sobre E2-03 / D9** y alineación de la documentación: ADR del vínculo, spec, requerimientos (D9, RNF-AUTH-09, RI-AUTH-01..03, HB-11), ADR de Keycloak, matriz de permisos y README de `auth-service` | Docs | — | **Hecha** (2026-11-10): se alineó con el `personaId` en Keycloak |
| 3a | E2-01 en **Incentivos**: resource server (dependencia del `pom.xml`, la misma que Donaciones), `@PreAuthorize` en los 6 controllers según la matriz §7, `Acceso` y `PermisoIncentivos`, variables en el compose. Apagado por defecto | Java; `mvn clean test -pl incentivos-service -am` | 2 | **Hecha y verificada** (2026-11-10), commit pendiente. **Verificado sin Maven:** `scripts/tests/test_incentivos_seguridad.py` (23 tests con prueba de mutación: las 19 reglas contra la matriz, nombres de parámetro del SpEL, enum de permisos, pom, propiedades y compose), `test_auth_realm_contract.py` (66, valida los nombres de `PermisoIncentivos` contra el realm), `javac` sobre `Acceso` y `PermisoIncentivos` (compilan completos) y sintaxis de los controllers y del test. **Verificado por el equipo (2026-11-10):** `mvn clean test -pl incentivos-service -am` en Windows: 366 tests, 0 fallas, 0 errores, BUILD SUCCESS (compila contra Spring y corren juntos `SeguridadIncentivosTest`, 17 casos, y los tests existentes). **Sin probar:** Spotless (`mvn spotless:check`) y la seguridad encendida contra un Keycloak real |
| 3b | E2-01 en **Logística**: resource server, `@PreAuthorize` en sus 7 controllers según la matriz §6 (`Acceso`, `PermisoLogistica`, `ReglasDeAccesoLogistica` para el mapa público, `DuenioDe`), consultas de dueño de solo lectura en `IEntregasService` e `IRutasService` (`beneficiariaDe`, `donanteDeDonacion`, `organizacionesDe`) y variables en el compose. Apagado por defecto | Java; `mvn clean test -pl logistica-service -am` | 3a | **Hecha y verificada** (2026-11-10), commit pendiente. **Verificado sin Maven:** `scripts/tests/test_logistica_seguridad.py` (29 tests con prueba de mutación de 14 roturas: los 31 endpoints, de los cuales 14 con regla propia, contra la matriz; nombres de parámetro del SpEL, ruta pública, bean de dueño sin 404, enum, servicios, pom, propiedades y compose), `test_auth_realm_contract.py` (valida `PermisoLogistica` contra el realm), `javac` (`Acceso` y `PermisoLogistica` compilan completos; sintaxis del resto, donde atrapó un `void` faltante) y controles mecánicos de formato. **Verificado por el equipo (2026-11-10):** `mvn clean test -pl logistica-service -am` en Windows: 647 tests, 0 fallas, 0 errores, 1 salteado, BUILD SUCCESS (compila contra Spring y corren juntos `SeguridadLogisticaTest`, los 7 tests nuevos de servicio y los `@WebMvcTest` existentes). El test salteado es preexistente (Logística ya tiene varios `@Disabled`, p. ej. en `AlgoritmoAsignadorDeEntregaTest`); ninguno de los tests nuevos deshabilita nada. **Sin probar:** Spotless y la seguridad encendida contra un Keycloak real. **Decisión a revisar:** 3 endpoints no estaban en la matriz (HB-16 y HB-10) y se les dio la regla de su par, marcada `[PROPOSED]` |
| 3c | E2-01 en **Notificaciones**: resource server, `@PreAuthorize` en sus 2 controllers según la matriz §8 (`Acceso`, `PermisoNotificaciones`, `DuenioDe`), alias legado separado en su propio método (solo ADMIN), `NotificacionService.personaDe` para la pertenencia por dueño y variables en el compose. Apagado por defecto | Java; `mvn clean test -pl notificaciones-service -am` | 3a | **Hecha y verificada** (2026-11-10), commit pendiente. **Verificado sin Maven:** `scripts/tests/test_notificaciones_seguridad.py` (25 tests con prueba de mutación de 11 roturas: las 12 reglas contra la matriz, nombres de parámetro del SpEL, bean de dueño sin 404, enum, pom, propiedades y compose), `test_auth_realm_contract.py` (valida `PermisoNotificaciones` contra el realm), `javac` (`Acceso` y `PermisoNotificaciones` compilan completos; sintaxis del resto) y controles mecánicos de formato. **Verificado por el equipo (2026-11-10):** `mvn clean test -pl notificaciones-service -am` en Windows: 230 tests, 0 fallas, 0 errores, BUILD SUCCESS (compila contra Spring y corren juntos `SeguridadNotificacionesTest`, 18 casos, los 2 tests nuevos de `NotificacionServiceTest` y los `@WebMvcTest` existentes). **Sin probar:** Spotless y la seguridad encendida contra un Keycloak real. **Cambio de contrato a revisar:** el alias `/notificaciones/persona/{personaId}` pasó a ser un método propio (`obtenerPorPersonaLegacy`), con el mismo comportamiento |
| 4 | E2-01 en el **gateway**: valida el JWT antes de enrutar (reusa la cadena de `common-lib`, apagada por defecto con `GATEWAY_SEGURIDAD_HABILITADA`), allowlist pública que es exactamente el conjunto de endpoints `PUBLICO` de los servicios (cinco `GET` y `POST /api/contacto`), y el gateway en el preprod (compose, resolver y runner local) | Java e infra; `mvn clean test -pl api-gateway -am` | 3 | **Hecha y verificada** (2026-11-11), commit pendiente. **Verificado sin Maven:** `scripts/tests/test_gateway_seguridad.py` (20 tests con prueba de mutación de 15 roturas: la allowlist es igual a los `PUBLICO` de los servicios y a sus `ReglasDeAcceso`, cada ruta pública está expuesta, pom, yaml, compose local y preprod, resolver y runner), `test_auth_ci_preprod.py` (42; el test que decía que el gateway quedaba afuera del preprod se reemplazó por tres que prueban que se resuelve como cualquier servicio), **ejecución real** de `ReglasDeAccesoGateway` con la allowlist del equipo (14 verificaciones con las clases reales y stubs de dos anotaciones de Spring), `javac` sobre los tests y controles mecánicos de formato. **Verificado por el equipo (2026-11-11):** `mvn clean test -pl api-gateway -am` en Windows: 63 tests, 0 fallas, 0 errores, 0 salteados, BUILD SUCCESS (compila contra Spring; corren juntos `SeguridadGatewayTest`, `ReglasDeAccesoGatewayTest` y el `RutasGatewayTest` existente con las rutas del contacto). Eso confirma que el gateway **reenvía el `Authorization` a los servicios** (lo comprueba `SeguridadGatewayTest`). **Sin probar:** Spotless, el gateway `healthy` en el preprod y la validación encendida contra un Keycloak real. **Decisión a revisar:** la allowlist abre la ruta sin distinguir método, como las reglas de los servicios: `GET /api/contacto` pasa el gateway sin token y lo rechaza Notificaciones por su `@PreAuthorize`. El ADR-E6-02 sigue `proposed` |
| 5 | **Preprod con la seguridad encendida y retiro de `X-Actor`:** cuenta `donatrack-pruebas` (rol `ADMIN`) en el realm; token en `integration-tests`, k6 y Postman; interruptor `PREPROD_SEGURIDAD` en los cinco servicios del preprod, con `run-preprod-tests.sh --auth` (comprueba el `401` sin token y que el token abre una ruta protegida **antes** de correr las pruebas) y `PREPROD_AUTH` en CI; `X-Actor` opcional, con el `sub` del token; el CI corre por primera vez los tests de `scripts/tests` | Java, realm, infra y tests | 3, 4 y E2-12a | **Implementada** (2026-11-11), commit pendiente. **Verificado sin Maven ni Docker:** `scripts/tests/test_pruebas_con_token.py` (33 tests con prueba de mutación de 17 roturas que cruzan archivos), `test_auth_realm_contract.py` (11 nuevos o ajustados: la cuenta de pruebas solo con client credentials, solo `ADMIN`, secreto por variable), `scripts/tests/postman-k6-token.test.js` (144 comprobaciones que **ejecutan** los scripts reales de las 9 colecciones y `token.js`, con prueba de mutación), y ejecuciones reales con dobles: `TokenDePruebas` contra un Keycloak falso (17), `AuditoriaDelActor` (12) y la función `verificar_seguridad_encendida` del runner con un `curl` falso (8 escenarios, incluido que el secreto no aparece en argumentos ni en la salida). Toda la carpeta `scripts/tests` pasa (334 tests), también sin PyYAML. **Sin probar:** que compile y corra en Maven `RealmTokenContractIT` (necesita Docker; 4 tests nuevos), `TokenDePruebasTest` (9 métodos), `AuditoriaDelActorTest` (7), los 2 casos nuevos de `SeguridadDonacionesTest` y `BaseIT`; Spotless; `--auth` de punta a punta con Docker; Postman y k6 reales. **Hallazgos:** el CI no corría ningún test de `scripts/tests` (ahora sí, en `agent-governance.yml`); un chequeo mío de Postman era vacío y escondía que un prefijo de URL suelto filtraría el token ADMIN al proveedor externo (corregido). **Corrección de iteraciones anteriores:** `ProveedorLogisticaHttp` no habla con Logística sino con el proveedor externo. **Decisiones a revisar:** M-08 a M-10 de la matriz |
| 6 | E2-12b y B1-05: cliente en el compose con emisor único y recorrido Playwright contra un Keycloak real | Infra y tests; necesita Docker | 4 | Pendiente |
| 7 | E2-02: Spring Session JDBC con base propia | Java e infra | 6 | Pendiente |
| 8 | E2-06: recuperación de clave y verificación de email (SPI → RabbitMQ → notificaciones) | Java (extensión de Keycloak) | 5 | Pendiente |
| 9 | E2-05: completar Google (vinculación por contraseña) | Realm y cliente | — | Pendiente |
| 10 | Lote final: E2-08 (CSV), E2-10 (cambio de rol), E2-11 (jurídica), E2-09 (tema Raíces) | Varios | 2 | Pendiente |

La decisión de la iteración 2 fue alinear la spec con el `personaId` en Keycloak, así que no hace falta una iteración de migración a `sub` en `Persona`. Queda abierto, para una iteración posterior, `GET /api/personas/me` por el `personaId` del token y la baja de personas con cuenta.

## Invariantes

- Ningún secreto en el repo: clave del admin, secreto del cliente, base y Google van por variables de entorno.
- El access token no lleva datos personales (RNF-AUTH-09): ni email ni nombre. Lleva `sub`, `roles`, `permisos` y `personaId`, que son ids y permisos de negocio.
- Los servicios y el cliente no dependen de claims propios de Keycloak (`realm_access`, `resource_access`): solo de `sub`, `roles`, `permisos`, `personaId`, `aud`, `iss` y `exp`. Los claims propios del sistema son planos.
- El navegador nunca ve un token: solo la cookie de sesión del cliente liviano.
- El perfil `maqueta` del cliente liviano sigue andando sin Keycloak.

## 5. Restricciones (Constraints)

- AGENTS.md §4.3: ningún test se deshabilita ni se debilita.
- La imagen de Keycloak es una imagen nueva en el compose, aprobada por el usuario con este plan (2026-11-04). Cualquier otra dependencia de `pom.xml` fuera de las de §4 pide OK aparte.
- El ADR queda en `proposed`.
- La parte B no empieza sobre `ENTREGA_6` mientras no estén mergeados los PR del cliente liviano.

## Criterios de Aceptación

```gherkin
Escenario: login de un donante (B1-01, B1-03)
  Dado un donante registrado en el realm donatrack
  Cuando ingresa su email y su clave en la pantalla de Keycloak
  Entonces vuelve al sitio en su inicio de donante
  Y ve el menú de donante

Escenario: el donante no entra a lo de otro rol (B1-03)
  Dado un donante con la sesión iniciada
  Cuando pide una página que empieza con /admin
  Entonces recibe 403

Escenario: registro de un donante (A1-03, B1-02)
  Dado una persona sin cuenta
  Cuando elige "Registrarse" y completa email y clave válidos
  Entonces queda con el rol DONANTE y vuelve al sitio con la sesión iniciada

Escenario: el token no lleva datos personales (A1-02)
  Dado el realm importado
  Cuando el cliente liviano obtiene un access token
  Entonces trae sub, roles, aud y una vida de 15 minutos (y permisos y personaId, si la cuenta los tiene)
  Y no trae email ni nombre

Escenario: bloqueo por fuerza bruta (A1-02)
  Dado una cuenta activa
  Cuando falla la clave 5 veces en 15 minutos
  Entonces el sexto intento con la clave correcta también falla, con el mismo mensaje genérico

Escenario: cerrar sesión cierra el SSO (B1-04)
  Dado un usuario con la sesión iniciada
  Cuando elige "Cerrar sesión" y después "Ingresar"
  Entonces Keycloak le pide la clave otra vez
```

## 6. Validación

| Ítem | Validación |
|---|---|
| Esta spec y el ADR | `node scripts/agent-check.js`, `node scripts/tests/run-tests.js` y `python scripts/validate_docs_links.py`, todos con exit 0 |
| A1-01..A1-04 | Test de contrato del realm con Testcontainers; `docker compose up` sano; capturas del tema |
| B1-01..B1-04 | `mvn clean test -pl cliente-liviano -am` |
| B1-05 | Perfil de punta a punta con Playwright y Keycloak en un contenedor |

## 7. Especificación Técnica de Diseño

**Two-Gate ADR (AGENTS.md §9.1):**

| Decisión | Gate A | Gate B | Resultado |
|---|---|---|---|
| D1, Keycloak como IdP | Sí | Sí: tecnología nueva y mecanismo de seguridad | ADR nuevo `proposed` |
| M1 a M4, alcance y orden del MVP | No: alcance de una tarea | — | Sin ADR; quedan en esta spec |
| M5, `personaId` en Keycloak (enmienda a D9) | Sí | Sí: cambia un contrato entre el IdP y los servicios, y mueve un id de negocio al IdP | ADR nuevo `proposed` ([ADR del vínculo](../../adr/auth-service/20261110-vinculo-cuenta-persona-personaid-en-keycloak.md)) |
| Cliente OIDC con *authorization code* y logout RP-initiated | No: implementa D2, D5 y D1 | — | Sin ADR |

**Plan de tests (TDD por ítem):**

- `auth-service` (A1-02, A1-03): módulo de test con `GenericContainer` sobre la imagen de `auth-service/Dockerfile`. Casos:
  - claims del access token y del ID token;
  - vida de 15 minutos;
  - bloqueo tras 5 fallos;
  - reuso del refresh;
  - admin con solo `ADMIN`;
  - autorregistro con `DONANTE`.
  
  Dónde vive el test (un módulo Maven chico o `integration-tests`) se decide en A1-02, sin dependencias nuevas.
- `cliente-liviano` (B1-01..B1-04): `SecurityConfigTest` con MockMvc, `oidcLogin()` y `logout()`. Cubre:
  - acceso por rol;
  - 403 cruzado;
  - el redirect con `prompt=create`;
  - el logout con `post_logout_redirect_uri`;
  - el perfil `maqueta` abierto.
- De punta a punta (B1-05): Playwright en el perfil propio, contra Keycloak en un contenedor.
