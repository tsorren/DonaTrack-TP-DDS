# Demo punta a punta: el sitio con Keycloak y los servicios por el gateway

> **Estado:** guía de SPEC-06 E2-12b y de SPEC-09 F6. `[OBSERVED]` Lo que describe sale del código y de los
> compose. El equipo informó (2026-11-12) que corrigió los problemas del cliente en el compose y comprobó que la
> sesión queda en PostgreSQL (una fila en `cliente.spring_session`); **el recorrido con Playwright y el cambio
> entre dos instancias todavía no se probaron**. Si algo de acá no pasa como dice, es un
> hallazgo: avisalo.

El cliente liviano (`cliente-liviano`) corre en el compose junto con Keycloak, el gateway y los cuatro
servicios. Las personas ingresan por Keycloak y el sitio habla con los servicios **solo por el gateway**
(`http://api-gateway:8086`).

## Qué se levanta y dónde

| Pieza | URL desde tu máquina | Para qué |
|---|---|---|
| Sitio (`cliente-liviano`) | http://localhost:8090 | La demo |
| Keycloak | http://localhost:8180 | Ingreso y consola (`KC_BOOTSTRAP_ADMIN_*` del `.env`) |
| Gateway | http://localhost:8086 | Entrada única a los servicios |
| Servicios | `8080` a `8083` | Directo, para depurar |

## Levantarlo

```
docker compose up -d --build --wait
```

El `.env` versionado trae valores de desarrollo para todo (`CLIENTE_LIVIANO_SECRET`, `AUTH_ADMIN_*`,
`PRUEBAS_CLIENT_SECRET`, `CLIENTE_DB_PASSWORD`). **Si ya tenías el stack levantado de antes, hacé
`docker compose down -v`:** el realm de Keycloak y la base `cliente` (con su rol) solo se crean con el volumen
de Postgres vacío, y sin ellas el sitio no arranca. La primera vez construye las imágenes desde el código, así
que tarda.

**El ingreso del sitio usa Keycloak siempre; la validación del token en los servicios y el gateway viene
apagada.** Para que ellos también exijan token, exportá antes `GATEWAY_SEGURIDAD_HABILITADA`,
`DONACIONES_SEGURIDAD_HABILITADA`, `INCENTIVOS_SEGURIDAD_HABILITADA`, `NOTIFICACIONES_SEGURIDAD_HABILITADA` y
`LOGISTICA_SEGURIDAD_HABILITADA` en `true`. En el preprod es `./run-preprod-tests.sh --auth`.

## El recorrido

| Rol | Cómo entrar | Qué ver |
|---|---|---|
| **Público** | Sin ingresar | La portada, el mapa, las entidades y el contacto (`/publico-*`) |
| **Donante** | "Crear mi cuenta de donante" en la portada | Keycloak pide email y clave; el sitio muestra las tarjetas del tipo de cuenta (persona, empresa, entidad); con "Persona donante" se llega al formulario del perfil; desde ahí se puede cerrar sesión |
| **Entidad** | "Sumar mi entidad" | Mismo ingreso; la cuenta queda esperando que el admin la apruebe |
| **Admin** | "Ingresar" con `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD` del `.env` | Llega a su área (`/admin-inicio`) |

Cerrar sesión cierra también la de Keycloak: al volver a entrar pide la clave.

### Recorrido automático (B1-05)

Dos de esos pasos están automatizados con Playwright: el donante que se registra y cierra sesión, y el admin
que ingresa y cierra sesión (en ambos, volver a entrar tiene que pedir la clave). Contra el stack levantado:

```
INGRESO_REAL_URL=http://localhost:8090 \
INGRESO_REAL_ADMIN_EMAIL=admin@donatrack.local \
INGRESO_REAL_ADMIN_PASSWORD=<AUTH_ADMIN_PASSWORD del .env> \
mvn test -pl cliente-liviano -Pvisual -Dtest=IngresoConKeycloakRealTest
```

**La cuenta admin sale de `AUTH_ADMIN_EMAIL` y `AUTH_ADMIN_PASSWORD` del `.env` solo cuando se importa el realm**, o sea con la base de Postgres vacía. Si cambiaste esos valores después, la cuenta conserva la clave vieja y el recorrido del admin se queda en el login de Keycloak ("Usuario o contraseña incorrectos"): hacé `docker compose down -v` y volvé a levantar, o probá la clave a mano en http://localhost:8180/realms/donatrack/account. Deja los videos en `cliente-liviano/target/ingreso-real/`. Si un paso falla, el mensaje dice dónde se quedó el navegador (URL, título y texto visible) y deja una captura en esa misma carpeta. Cada corrida crea una cuenta de donante nueva en
Keycloak. Sin `INGRESO_REAL_URL` el test no corre. Necesita los navegadores de Playwright instalados
(`mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"`).

## El "emisor único": por qué `localhost:8180` funciona dentro del contenedor

El navegador abre Keycloak en `http://localhost:8180`, y esa URL es también el *issuer* de los tokens
(`KC_HOSTNAME`). Dentro del contenedor del sitio, en cambio, `localhost` es el propio sitio. El sitio usa una
sola propiedad (`issuer-uri`) para descubrir la configuración, cerrar la sesión en Keycloak, llamar a su
Admin API y armar el enlace a "Mi cuenta", así que una URL distinta para el servidor no alcanza.

`cliente-liviano/docker-entrypoint.sh` lo resuelve sin tocar la configuración de Spring: con
`KEYCLOAK_LOCAL_FORWARD_TO=keycloak:8080`, abre dentro del contenedor el puerto del issuer, solo en
`127.0.0.1`, y lo reenvía a Keycloak con `socat`. Corta si el issuer no es `http://localhost` o
`http://127.0.0.1`, o si su puerto es menor que 1024. Sin la variable (producción) no hace nada.

Si el sitio no arranca o el ingreso falla: `docker compose logs cliente-liviano` tiene que mostrar
`Emisor único: 127.0.0.1:8180 -> keycloak:8080`. Si no aparece, falta `KEYCLOAK_LOCAL_FORWARD_TO`.

## Recuperar la clave (y ver el correo)

En el login, "¿Olvidaste tu contraseña?" pide el email. Keycloak no manda el correo: publica un evento en RabbitMQ y `notificaciones-service` lo manda con su adaptador de correo **simulado**, que no entrega nada de verdad. En el compose local ese adaptador imprime el contenido del mensaje (con el enlace) en el log de Notificaciones, y solo ahí; el preprod no lo hace, porque el enlace da acceso a la cuenta:

```
docker compose logs notificaciones-service | grep "SENDGRID-MOCK"
```

Abrí el enlace del log (vale 30 minutos y se puede usar una vez), elegí la clave nueva y volvé a entrar. Una cuenta que no existe ve la misma pantalla, pero no genera ningún correo. Si el correo no llega, mirá el log de Keycloak: con RabbitMQ caído la persona no ve ningún error, y la señal es `KC-SERVICES0026`. La prueba automática de todo este recorrido es `scripts/e2e_correo_de_cuenta.py`.

## La sesión en PostgreSQL

El navegador solo lleva un id de sesión (cookie `SESSION`, no `JSESSIONID`); los tokens quedan en el servidor,
en las tablas `spring_session` y `spring_session_attributes` del esquema `cliente` de la base `cliente`, a la
que solo entra `cliente_user`. Se enciende con `CLIENTE_SESIONES_JDBC_HABILITADO=true` (el compose lo hace);
apagada, la sesión vive en memoria y el sitio no necesita base de datos, que es lo que usan la maqueta y los
tests. La sesión vence a los 30 minutos sin uso (`CLIENTE_SESIONES_INACTIVIDAD_MINUTOS`).

Cada pestaña y cada instancia leen la misma sesión, así que con dos instancias detrás de un mismo dominio una
persona puede pasar de una a otra sin volver a ingresar (RNF-CL-04). En el compose el puerto 8090 está fijo y no
se puede escalar con `--scale`. **El cambio de instancia no está automatizado ni probado todavía.** Una forma
de probarlo a mano sería levantar una segunda copia del sitio en otro puerto, con la misma red y las mismas
variables, iniciar sesión en una y copiar la cookie `SESSION` al navegador de la otra.

## Límites conocidos

- La sesión del sitio (E2-02, D5) vive en PostgreSQL, en la base `cliente`: reiniciar el sitio **no** cierra
  las sesiones. Con una instancia, se prueba iniciando sesión, corriendo `docker compose restart cliente-liviano`
  y recargando la página: sigue adentro. Con dos instancias, ver la sección siguiente.
- Una alternativa sin `socat` sería armar el registro del cliente OAuth2 a mano desde el descubrimiento,
  pidiéndolo por `keycloak:8080` y con `hostname-backchannel-dynamic` en Keycloak. Es más correcta pero toca
  código de seguridad de Spring; se eligió el reenvío por ser la que menos cambia.
