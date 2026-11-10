# Proveedor de identidad: Keycloak configurado

- Status: proposed
- Date: 2026-11-04
- Deciders: tsorren, en la sesión de planificación del MVP de autenticación (2026-11-04); falta la aprobación del equipo en el PR
- Tags: auth-service, identidad, oauth2, oidc, sso, keycloak, seguridad, entrega-6

> **Origen:** decisión D1 de los [requerimientos de E6](../../requerimientos/requerimientos-e6.md) (pendiente hasta este ADR) y [SPEC-06](../../specs/active/SPEC-06-auth-mvp-keycloak.md), que trae el backlog del MVP. Cubre la parte D1 del candidato ADR-E6-01 de esos requerimientos. Social-Login (D3, D4), emails de cuenta (D8) y acceso de los importados (D12) siguen en ese candidato. **Ninguna parte de esta decisión está implementada.**

## Contexto y Problema

`[DOCUMENTED]` La Entrega 6 pide un **servicio de autenticación** que gestione los tokens y un **SSO con Social-Login** (RQ-E6-04 y RQ-E6-05 de los requerimientos de E6, págs. 26–27 del enunciado). La entrega es el 13 de noviembre de 2026 ([calendario](../../entregas/README.md)).

`[DOCUMENTED]` Ya están decididas, sea cual sea el producto:

- El navegador solo tiene una cookie de sesión y el cliente liviano guarda los tokens (D5).
- El access token dura 15 minutos (D6) y el refresh rota con detección de reuso (D7).
- La validación usa JWKS en el gateway y en cada servicio (D2).
- Los roles son `DONANTE`, `ENTIDAD` y `ADMIN`, y viajan en un claim `roles` plano (D14, RNF-AUTH-08).
- ~~El negocio guarda el `sub` del IdP (D9).~~ *Enmienda 2026-11-10:* el vínculo es el `personaId` en el IdP, según el [ADR del vínculo](20261110-vinculo-cuenta-persona-personaid-en-keycloak.md).

Falta elegir **quién hace de proveedor de identidad (IdP)**: el componente que guarda las cuentas, comprueba la clave y firma los tokens.

`[OBSERVED]` El 2026-10-29 se publicó la rama `E6_auth_service` (`349ca0a5`). Tiene un modelo de dominio para un IdP propio con Spring Authorization Server: `Usuario`, estados de la cuenta, bloqueo tras 5 intentos y política de clave. Declara JPA y Flyway, pero no tiene repositorios, migraciones, pantallas ni tests.

`[DOCUMENTED]` La pregunta P1 a la cátedra era si un Keycloak configurado cuenta como "servicio de autenticación".

`[INFERRED]` Según el equipo, la cátedra respondió que sí. Lo declaró tsorren el 2026-11-04; no hay un documento de la cátedra en el repo y este ADR no cita el canal. **La decisión depende de esa respuesta** (ver la matriz).

Problema: ¿qué IdP cumple la consigna y llega a la entrega con el menor riesgo de seguridad?

## Atributos de Calidad y Drivers de Decisión

* **Cumplimiento de la consigna:** que la cátedra lo cuente como servicio de autenticación y SSO.
* **Esfuerzo hasta el MVP:** quedan 9 días hasta la entrega, que además suma gateway y cliente liviano.
* **Diseño visible:** clases, capas y tests propios que la cátedra pueda evaluar.
* **Riesgo de seguridad:** cuánto código de seguridad propio hay que escribir y mantener.
* **Encaje con lo que sigue:** Google, emails de cuenta por notificaciones (D8), alta masiva por CSV (D12) y Key Broker (E7).
* **Operación:** memoria, arranque y mantenimiento en el compose y en CI (runner de 7 GB).

## Relaciones Arquitectónicas

1. **Cierra la D1** de los requerimientos de E6. No reemplaza a ningún ADR.
2. **Se relaciona** con el ADR de [DTI-07](../notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) y con el de [crypto-shredding](../notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) (los dos `proposed`). Esos ADRs ponían el Key Broker dentro de `auth-service`. Con Keycloak, el Key Broker pasa a ser **un servicio aparte**, que se construye en E7 (D29). Este ADR no cambia su diseño: cambia solo dónde corre.
3. **Respeta** el ADR de [base por servicio](../20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md): Keycloak tiene su propia base `keycloak` en el motor PostgreSQL compartido.

## Alternativas Consideradas

Notas de 1 a 5; el puntaje es la suma de nota × peso / 100. Las notas son juicio `[INFERRED]` sobre la evidencia relevada.

| Criterio | Peso | A. Spring Authorization Server (servicio propio) | **B. Keycloak configurado** | C. Keycloak + `auth-service` como fachada |
|---|:-:|:-:|:-:|:-:|
| Cumplimiento de la consigna | 25 | 5 | 3 → **5** con la confirmación | 4 |
| Esfuerzo hasta el MVP (5 = menos) | 25 | 3 | 5 | 2 |
| Diseño visible | 15 | 5 | 2 | 4 |
| Riesgo de seguridad (5 = menos) | 15 | 3 | 5 | 4 |
| Encaje con lo que sigue | 10 | 4 | 3 | 4 |
| Operación | 10 | 4 | 2 | 2 |
| **Total** | 100 | **4,00** | 3,55 → **4,05** | 3,30 |

**Diferencia con la matriz de los requerimientos de E6** (SAS 3,95 y Keycloak 3,10):

* Aquella daba 15% al encaje con el Key Broker. Después, la D29 pasó el Key Broker entero a E7, así que ese criterio se volvió "encaje con lo que sigue" y bajó a 10%.
* "Diseño visible" bajó de 30% a 15%. El riesgo de que la cátedra no lo cuente pasó a medirse en un criterio nuevo, "cumplimiento de la consigna" (25%). Según el equipo `[INFERRED]`, la cátedra respondió que sí cuenta.
* Se sumó "riesgo de seguridad": un login propio es código de seguridad que el equipo tiene que escribir y probar.
* "Esfuerzo" ahora considera la base que ya tiene la rama `E6_auth_service`: SAS sube de 2 a 3.
* "Alta en lote desde el CSV" (10%) y "Estándar OAuth2/OIDC" (10%) salieron de la matriz: el CSV no entra en el MVP (D12 sigue abierta) y las dos opciones cumplen el estándar, así que no distinguían.
* "Operación" de Keycloak baja de 3 a 2: además de la imagen y el arranque, ahora cuenta la memoria del runner de CI (7 GB) y el tema FreeMarker que hay que mantener.

Con la confirmación de la cátedra, B y A quedan **casi empatadas** (4,05 contra 4,00). La decisión prioriza el esfuerzo y el riesgo de seguridad.

**Sin esa confirmación, A gana (4,00 contra 3,55).** Si la cátedra dijera que Keycloak no cuenta, este ADR se rechaza y se vuelve a A.

## Análisis por alternativa

**A. Spring Authorization Server.**
- *A favor:* diseño propio y visible. El mismo servicio puede alojar el Key Broker y publicar los eventos de D8 sin extensiones. Reusa el dominio de la rama `E6_auth_service`.
- *En contra:* hay que construir la persistencia, la pantalla de login, el registro, la federación con Google, la recuperación de clave y la detección de reuso del refresh. Es mucho código de seguridad para 9 días.

**B. Keycloak configurado.**
- *A favor:* login, registro, recuperación, bloqueo por fuerza bruta, rotación del refresh, JWKS y Google vienen hechos y probados. La configuración se versiona como un realm exportado.
- *En contra:*
  - Se ve menos diseño propio.
  - La pantalla de login se tematiza con plantillas FreeMarker.
  - La imagen es pesada y tarda en arrancar.
  - D8 pide una extensión Java de Keycloak (SPI, *Service Provider Interface*) para publicar en RabbitMQ.
  - El dominio de la rama `E6_auth_service` queda sin usar.

**C. Keycloak con una fachada propia.**
- *A favor:* combina un login maduro con diseño propio (registro, aprobaciones, CSV).
- *En contra:* dos piezas que operar y sincronizar por la Admin API. Es la de más trabajo total y duplica funciones que Keycloak ya trae.

**Descartadas sin puntaje:**
- `[REJECTED]` **JWT casero sin OAuth2/OIDC:** no da SSO ni permite federar Google, y contradice D2 y D5.
- `[REJECTED]` **Formulario de login propio en el cliente liviano con *password grant* (ROPC: el sitio recibe la clave y se la pasa al IdP) contra Keycloak:** OAuth 2.1 elimina ese flujo, el sitio vería la clave y se pierde el SSO. El login se hace en la pantalla del IdP, con *authorization code* (el IdP le devuelve al sitio un código de un solo uso que el sitio canjea por los tokens).

## Resultado de la Decisión

Se propone **B: Keycloak configurado como IdP de DonaTrack**.

1. **Producto:** Keycloak 26.x, con la versión fijada por digest en el compose. Lleva un `Dockerfile` propio en `auth-service/` con el realm, el tema y, más adelante, el SPI de D8.
2. **Configuración versionada:** el realm `donatrack` se exporta a `auth-service/realm/` y se importa al arrancar. Los secretos (cliente OAuth2, admin inicial, Google) entran por variables de entorno (RNF-AUTH-05).
3. **Contrato hacia el resto del sistema:**
   - Access token JWT firmado con clave asimétrica. Las claves públicas se publican por JWKS (una URL con la lista de claves para verificar firmas).
   - Claim `roles` plano, `aud` del API.
   - Sin datos personales en el access token: ni email ni nombre (RNF-AUTH-09).
   - Los servicios no dependen de claims propios de Keycloak (`realm_access`), sino de claims planos definidos por el sistema (`roles`, `permisos`, `personaId`). *Enmienda 2026-11-10:* cambiar de IdP exige recrear esos mappers y exportar el atributo `personaId` (RNF-AUTH-08; [ADR del vínculo](20261110-vinculo-cuenta-persona-personaid-en-keycloak.md)).
4. **Login:** el cliente liviano es un cliente OIDC confidencial con *authorization code*. El registro entra con `prompt=create` y el cierre de sesión es *RP-initiated logout* (el sitio le pide al IdP que cierre también su sesión, para que el próximo ingreso pida la clave).
5. **`auth-service/`** contiene la configuración de Keycloak, no código Java de un IdP propio. El Key Broker de E7 será un servicio aparte.

## Consecuencias Positivas

* Las funciones de seguridad más delicadas vienen de un producto mantenido: hash de claves, bloqueo, rotación del refresh y JWKS con rotación de claves.
* El MVP solo necesita configuración y la integración en el cliente liviano (ver el backlog de SPEC-06).
* Google (D3) es configurar un proveedor de identidad, sin código.

## Consecuencias Negativas

* **Menos diseño propio visible.** Se compensa con el realm versionado, los tests de contrato de los tokens y la integración en el cliente.
* **Imagen nueva** en el compose (más memoria y más arranque). Entra en `docker-compose.yml`; sumarla a preprod y a CI es una etapa posterior.
* **El rol por defecto choca con D10.** Keycloak le da los `default-roles` del realm a todo usuario nuevo. Para que el autorregistro dé `DONANTE` sin que lo hereden el admin del seed ni, más adelante, las entidades, hay que configurarlo a propósito y probarlo (A1-03 de SPEC-06).
* **Sin verificación de email en el MVP:** alguien puede registrarse con un email ajeno. Se acepta hasta cerrar D8 (RF-AUTH-16).
* **D8** necesita una extensión Java (SPI) de Keycloak que publique en RabbitMQ.
* **Cambiar el realm versionado** obliga a recrear la base de Keycloak en local, porque el import se saltea si el realm ya existe.
* **El trabajo de la rama `E6_auth_service` no se integra.** Queda como referencia, y sus reglas pasan a la configuración del realm.

## Validación

1. Un test con Testcontainers levanta Keycloak con el realm versionado, pide un token y verifica `sub`, `roles`, `aud`, `exp` de 15 minutos y la ausencia de email y nombre.
2. Cinco intentos fallidos en 15 minutos bloquean la cuenta.
3. Reusar un refresh ya rotado corta la sesión (D7). `[INFERRED]` Keycloak lo hace con `revokeRefreshToken` y `refreshTokenMaxReuse=0`; lo confirma el test.
4. De punta a punta: login en el cliente liviano, inicio según el rol y logout que obliga a escribir la clave otra vez.

## Trabajo Futuro / Links

* Backlog: [SPEC-06](../../specs/active/SPEC-06-auth-mvp-keycloak.md).
* ADR-E6-01 (resto): Social-Login (D3, D4), emails de cuenta (D8) y acceso de los importados (D12).
* Key Broker como servicio aparte: [DTI-07](../DEUDA_TECNICA.md), E7.
