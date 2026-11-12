# api-gateway — Entrada única a los servicios

> **Estado:** etapa 1 del [ADR-E6-02](../docs/adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`): rutas, allowlist escrita y `X-Trace-Id`. **Todavía no valida el JWT**: eso llega en la etapa 2 (F5). Spec: [SPEC-07](../docs/specs/active/SPEC-07-api-gateway-etapa-1.md).

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

`donatrack.gateway.rutas-publicas` en [`application.yml`](src/main/resources/application.yml) lista lo que en la etapa 2 pasa sin token (D26). Hoy no se aplica. Si una pantalla pública suma una llamada, su ruta entra ahí en el mismo cambio. Un test comprueba que cada ruta pública tiene una ruta expuesta.

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

## Correr y probar

```bash
mvn clean test -pl api-gateway -am
```

```bash
docker compose up -d api-gateway
```
