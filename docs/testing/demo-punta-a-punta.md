# Demo punta a punta: el sitio con Keycloak y los servicios por el gateway

> **Estado:** guía de SPEC-06 E2-12b y de SPEC-09 F6. `[OBSERVED]` Lo que describe sale del código y de los
> compose; **todavía no se corrió el stack completo** (la sesión que la escribió no tenía Docker). Si algo
> de acá no pasa como dice, es un hallazgo: avisalo.

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
`PRUEBAS_CLIENT_SECRET`). Si ya tenías Keycloak levantado de antes, el realm no se vuelve a importar con la
base existente: `docker compose down -v` y de nuevo. La primera vez construye las imágenes desde el código,
así que tarda.

**El ingreso del sitio usa Keycloak siempre; la validación del token en los servicios y el gateway viene
apagada.** Para que ellos también exijan token, exportá antes `GATEWAY_SEGURIDAD_HABILITADA`,
`DONACIONES_SEGURIDAD_HABILITADA`, `INCENTIVOS_SEGURIDAD_HABILITADA`, `NOTIFICACIONES_SEGURIDAD_HABILITADA` y
`LOGISTICA_SEGURIDAD_HABILITADA` en `true`. En el preprod es `./run-preprod-tests.sh --auth`.

## El recorrido

| Rol | Cómo entrar | Qué ver |
|---|---|---|
| **Público** | Sin ingresar | La portada, el mapa, las entidades y el contacto (`/publico-*`) |
| **Donante** | "Crear mi cuenta de donante" en la portada | Keycloak pide email y clave; el sitio pide completar el perfil; desde ahí se puede cerrar sesión |
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

Deja los videos en `cliente-liviano/target/ingreso-real/`. Cada corrida crea una cuenta de donante nueva en
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

## Límites conocidos

- Las sesiones del sitio viven en memoria (E2-02, Spring Session, queda para la iteración 7): reiniciar el
  sitio cierra todas.
- Una alternativa sin `socat` sería armar el registro del cliente OAuth2 a mano desde el descubrimiento,
  pidiéndolo por `keycloak:8080` y con `hostname-backchannel-dynamic` en Keycloak. Es más correcta pero toca
  código de seguridad de Spring; se eligió el reenvío por ser la que menos cambia.
