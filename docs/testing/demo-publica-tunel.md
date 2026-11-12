# Demo pública por túnel (E6)

Cómo publicar el stack completo de DonaTrack, con los datos del seeder, en una URL HTTPS que se abre desde
cualquier red, sin servidor ni dominio: el stack corre en una PC con Docker y cuatro *Quick Tunnels* de
Cloudflare lo exponen. Pensado para presentar la entrega; no es un entorno de producción.

## Qué se publica

El navegador abre cuatro orígenes; cada uno tiene su túnel (`docker-compose.tunel.yml`):

| Túnel | Puerto del host | Para qué lo usa el navegador |
|---|---|---|
| `tunel-cliente` | 8090 | El sitio (cliente liviano) |
| `tunel-keycloak` | 8180 | Ingreso y registro; es también el emisor (`iss`) de los tokens |
| `tunel-minio` | 9000 | Fotos con URL prefirmada (donaciones, incentivos, logística) |
| `tunel-gateway` | 8086 | Fotos de las actas de logística |

Bases, RabbitMQ, n8n, Adminer y las consolas no tienen túnel: solo se ven desde la PC.

## Requisitos

- Docker con 8 GB o más para su VM (`docker info` → `Total Memory`). Medido el 2026-11-12: el stack con n8n
  y los datos del seeder usa unos 4 GB; con 4,8 GB el motor de Docker falló a mitad del seeder.
- Bash (Git Bash en Windows), `openssl` y salida a internet.
- La PC prendida y sin suspensión mientras dure la demo.
- Los puertos 8090, 8180, 9000 y 8086 libres: frenar antes el stack de desarrollo (`docker compose down`).

## Cómo se levanta

```bash
scripts/demo-publica.sh
```

El script:

1. Levanta los túneles (proyecto `donatrack-tunel`) y lee sus URL de los logs.
2. La primera vez crea `.env.publica` con claves aleatorias (`scripts/generar-env-db.sh`), una `DEMO_PASSWORD`
   aleatoria y la seguridad encendida en el gateway y los cuatro servicios. No usa el `.env` versionado,
   que tiene valores de desarrollo.
3. Escribe en `.env.publica` las URL públicas: `KC_HOSTNAME`, `CLIENTE_LIVIANO_BASE_URL`, `PUBLICA_URL_MINIO`
   y `PUBLICA_URL_GATEWAY`.
4. Borra el proyecto `donatrack-publica` con sus volúmenes, lo vuelve a construir y levantar con
   `docker-compose.yml` + `docker-compose.seed.yml` + `docker-compose.publica.yml`, y corre el seeder (`todo`).
5. Imprime la URL del sitio.

Las cuentas de la demo están en [demo-seeder.md](demo-seeder.md); su clave es `DEMO_PASSWORD` de `.env.publica`.

## Qué cambia `docker-compose.publica.yml`

- Keycloak lee el esquema y el host de los encabezados `X-Forwarded-*` (`KC_PROXY_HEADERS=xforwarded`): el
  túnel termina el TLS.
- El cliente liviano arma el `redirect_uri` con esos encabezados (`SERVER_FORWARD_HEADERS_STRATEGY=native`) y
  apaga el reenvío local del emisor (`KEYCLOAK_LOCAL_FORWARD_TO` vacío), que solo sirve con `localhost`.
- Las URL públicas de MinIO y de las fotos de logística apuntan a sus túneles.
- El seeder lee `.env.publica` y siembra las fotos con la URL pública del gateway.
- `restart: unless-stopped` en Keycloak, RabbitMQ, el gateway, el cliente y los cuatro servicios. RabbitMQ a veces
  no arranca en Docker Desktop sobre Windows (`.erlang.cookie: eacces`, DIT-11); el script reintenta el `up`.
- Los túneles usan `--protocol http2`: con QUIC se cortaban por inactividad.

## Límites

- **La URL cambia** cada vez que se recrea un túnel (reinicio de la PC o de Docker). La URL queda guardada en el
  realm (redirect), en el emisor de los tokens y en las fotos sembradas: para re-apuntar, volver a correr
  `scripts/demo-publica.sh`, que **borra los datos** y vuelve a sembrar (unos minutos).
- Quick Tunnel: hasta 200 pedidos en vuelo por túnel, sin Server-Sent Events y sin garantía de disponibilidad
  ([docs de Cloudflare](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/)).
- Mientras corre, el cliente liviano y Keycloak se hablan por la URL pública: si la PC pierde internet, el
  ingreso falla aunque el sitio local siga arriba.
- Con la seguridad encendida, las fotos de las actas (`GET /api/entregas/{id}/fotos/{fotoId}`) dan 401: el sitio
  las muestra con `<img>`, que no manda token, y la ruta no está en las rutas públicas del gateway ni de Logística
  (`@PreAuthorize(VER_SU_ENTREGA)`). Para la demo del 2026-11-12 se abrió en un merge local; falta decidirlo en el código.
- Los nombres fijos de algunos contenedores (`container_name`) chocan con el stack de desarrollo: no correr
  los dos a la vez.

## Apagar

```bash
docker compose -p donatrack-publica down -v
docker compose -f docker-compose.tunel.yml down
```
