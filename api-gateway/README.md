# api-gateway — Entrada única a los servicios

> **Estado:** etapas 1 y 2 del [ADR-E6-02](../docs/adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`): rutas, `X-Trace-Id` y validación del JWT. **La validación viene apagada por defecto** (`GATEWAY_SEGURIDAD_HABILITADA=false`): con ella apagada, todas las rutas pasan sin token. Specs: [SPEC-07](../docs/specs/active/SPEC-07-api-gateway-etapa-1.md) (etapa 1) y [SPEC-06](../docs/specs/active/SPEC-06-auth-mvp-keycloak.md), iteración 4 (etapa 2).

## Qué hace

- El cliente usa una sola URL base (`http://localhost:8086`) y las mismas rutas de [`contratos-rest.md`](../docs/arquitectura/contratos-rest.md). El gateway no las reescribe.
- Spring Cloud Gateway Server WebMVC + `common-lib`: mismo `traceId`, mismos logs y mismo formato de error que los servicios.

## Rutas expuestas

La tabla de [`RutasGateway`](src/main/java/grupo5/gateway/RutasGateway.java) es la allowlist de lo que llega a los servicios. Lo que no figura da `404`.

| Servicio | Rutas |
|---|---|
| Donaciones | `/api/{categorias,donaciones,donantes,entidades,items-normalizados,necesidades,personas,asignaciones,subcategorias}/**`, `/api/logistica/proveedores`, `/api/logistica/proveedor-preferido` |
| Notificaciones | `/api/notificaciones/**`, `/api/contacto` y `/api/contacto/**` (HB-21) |
| Incentivos | `/api/incentivos/**` |
| Logística | `/api/{camiones,choferes,entregas,rutas}/**`, `/api/logistica/resultados`, `/api/logistica/callback/**`, `/api/logistica/planificaciones/**` |

- `/api/logistica` es de dos servicios: va por ruta completa. Una ruta nueva bajo ese prefijo hay que sumarla a mano.
- Afuera: los alias sin `/api` de Notificaciones y `/donaciones-independientes`.

## Rutas públicas

`donatrack.gateway.rutas-publicas` en [`application.yml`](src/main/resources/application.yml) lista lo que pasa sin token (D26), con el formato `"MÉTODO /ruta"`. Se aplica solo con la seguridad encendida. Si una pantalla pública suma una llamada, su ruta entra ahí **y** en el servicio, en el mismo cambio.

- **Se abre la ruta, no el método.** El gateway usa la misma cadena de seguridad de `common-lib` que los servicios, que decide por ruta. El método lo decide el `@PreAuthorize` del servicio (la segunda capa). Por eso `POST /api/contacto` es público y `GET /api/contacto` (el listado del ADMIN) pasa el gateway sin token y lo rechaza Notificaciones con `401`.
- **Las dos capas coinciden por test.** `scripts/tests/test_gateway_seguridad.py` exige que esta lista sea exactamente el conjunto de endpoints `@PreAuthorize(PUBLICO)` de los servicios, y que cada ruta tenga una ruta expuesta.
- Una entrada mal escrita (método desconocido, ruta sin `/`) corta el arranque.

## Validación del JWT

Con `GATEWAY_SEGURIDAD_HABILITADA=true` el gateway valida firma (JWKS), `exp`, `iss` y `aud` antes de enrutar. Sin token, o con uno inválido, responde `401` con el formato de error unificado; las rutas públicas pasan. El `Authorization` llega intacto al servicio, que vuelve a validarlo (doble capa). Las claves de Keycloak se cachean, así que un IdP caído no corta los pedidos con tokens vigentes.

- El issuer y las claves llegan por `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` y `_JWK_SET_URI`. No se definen en `application.yml`: con valor vacío Spring igual intenta armar el decoder y falla.
- Una ruta que no está en la tabla de arriba da `401` sin token y `404` con token.

- `POST /api/contacto` (HB-21): el cliente liviano manda la IP del navegador en `X-Forwarded-For`. Si el gateway agrega otra detrás, Notificaciones igual cuenta por la primera.

## `X-Trace-Id`

- Si el cliente lo manda con 32 hex en minúscula, el gateway lo adopta, lo manda al servicio y lo devuelve.
- Si falta, el gateway genera uno con el mismo formato.
- Cada pedido deja un log `[GATEWAY]` con el método, la ruta, el servicio y el estado. Con los perfiles `json`, `preprod` o `prod` sale en NDJSON.

## Configuración

| Variable | Por defecto |
|---|---|
| `PORT` | `8086` |
| `DONACIONES_SERVICE_URL` | `http://localhost:8080` |
| `NOTIFICACIONES_SERVICE_URL` | `http://localhost:8081` |
| `INCENTIVOS_SERVICE_URL` | `http://localhost:8082` |
| `LOGISTICA_SERVICE_URL` | `http://localhost:8083` |
| `GATEWAY_SEGURIDAD_HABILITADA` | `false` |
| `GATEWAY_JWT_AUDIENCIA` | `donatrack-api` |

## Correr y probar

```bash
mvn clean test -pl api-gateway -am
```

```bash
docker compose up -d api-gateway
```
