# API gateway: entrada única, validación del JWT en doble capa y rutas públicas

- Status: proposed
- Date: 2026-11-07
- Deciders: tsorren; falta la aprobación del equipo en el PR
- Tags: gateway, seguridad, jwt, oauth2, cliente-liviano, integracion, trazabilidad, entrega-6

> **Origen:** candidato ADR-E6-02 de los [requerimientos de E6](../requerimientos/requerimientos-e6.md)
> §11 (decisiones D2, D16 y D26, "Listo" para redactar) y E2-01 de
> [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md). El orden de implementación está en el
> [plan del cliente con datos reales y gateway](../requerimientos/plan-e6-cliente-datos-reales-y-gateway.md).
> **Ninguna parte de esta decisión está implementada.**
>
> De dónde sale cada parte:
>
> - D2, D16 y D26 figuran como tomadas (✅) en los requerimientos, §9.
> - Que el cliente genere el `traceId` lo dijo tsorren el 2026-11-06, al relevar el
>   [plan E6 sin autenticación](../requerimientos/plan-e6-trabajo-sin-auth.md); no quedó escrito en
>   ese plan.
> - El enrutamiento sin reescritura (R1) y la implementación en dos etapas son propuestas del
>   agente (`[PROPOSED]`, 2026-11-07).

## Contexto y Problema

`[DOCUMENTED]` RQ-E6-02 pide que todas las pantallas obtengan sus datos por REST **a través del
gateway**, y §5 de los requerimientos fija RI-GW-01 a RI-GW-04: entrada única, validación del JWT,
denegar por defecto con una allowlist pública y propagación del `traceId`.

Qué hay hoy en `ENTREGA_6`:

- `[OBSERVED]` No hay gateway. Cada servicio publica su puerto en `docker-compose.yml`: Donaciones
  8080, Notificaciones 8081, Incentivos 8082 y Logística 8083. Keycloak ya está en el compose
  (SPEC-06, A1-01 a A1-04).
- `[OBSERVED]` Ningún servicio tiene Spring Security. Donaciones usa un `ApiKeyFilter` (`X-API-Key`)
  y lee `X-Actor` (D25 los retira con el token, ADR-E6-05).
- `[OBSERVED]` El cliente liviano no hace llamadas REST: es una maqueta con datos de ejemplo, y no
  depende de `common-lib`. El [ADR del stack del cliente](./cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
  deja la propagación del `traceId` para este ADR.
- `[OBSERVED]` Las rutas de los servicios no siguen un prefijo por servicio, y dos se pisan en el
  prefijo:
  - `/api/logistica` está en Donaciones (`LogisticaProveedorController`: `/proveedores`,
    `/proveedor-preferido`) y en Logística (`PlanificacionController`: `/resultados`,
    `/callback/rutas`, `/planificaciones/{id}`; `PlanificacionManualController`:
    `/planificaciones/ejecuciones`);
  - `/donaciones-independientes` no lleva `/api`, y Notificaciones conserva dos alias
    heredados sin `/api`: `POST /notificaciones` (deprecado, DTI-13) y
    `GET /notificaciones/persona/{personaId}`, que repite la ruta con `/api`
    (`NotificacionController`). El gateway expone solo las rutas con `/api`; los alias quedan afuera.
- `[OBSERVED]` `common-lib` ya trae `TraceResponseHeaderFilter` (lee o genera el `traceId` y lo
  devuelve en `X-Trace-Id`) y `FeignTraceRequestInterceptor` (lo propaga en Feign), según el
  [ADR de observabilidad](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md).

Problema: ¿por dónde entra el cliente a los cuatro servicios, quién valida el token, qué queda
abierto sin login y cómo viaja el `traceId` de punta a punta?

## Atributos de Calidad y Drivers de Decisión

Pesos `[INFERRED]`: seguridad ante puertas laterales 25 · punto único de entrada para el cliente 20 ·
continuidad con el stack y los logs NDJSON 20 · contratos de los servicios sin cambios (AGENTS.md
§8.1) 15 · costo en E6 10 · base para rate limiting y observabilidad en E7 10.

## Relaciones Arquitectónicas

1. **Aplica:** [proveedor de identidad Keycloak](./auth-service/20261104-proveedor-de-identidad-keycloak.md)
   (`proposed`): la firma se valida con las claves JWKS del realm `donatrack`.
2. **Respeta:** [observabilidad NDJSON y trazabilidad MDC](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md):
   mismo header `X-Trace-Id` y mismo formato de log.
3. **Respeta:** [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): el gateway no cambia rutas ni payloads de los servicios.
4. **Completa:** [stack del cliente liviano](./cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`): decide la propagación del `traceId` que ese ADR dejó abierta.
5. **Se apoya en:** [estilo de microservicios con capas y puertos](./20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md)
   (`proposed`): el gateway es un adaptador de entrada sin lógica de negocio.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`). Columnas: seguridad ·
entrada única · continuidad · contratos · costo · base E7.

### Dónde se valida el token (D2)

| Alternativa | Seg. | Entr. | Cont. | Contr. | Costo | E7 | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **V1. Gateway + JWKS en cada servicio (doble capa)** | 5 | 5 | 4 | 5 | 2 | 5 | **4,5** |
| V2. Gateway que valida y servicios que confían | 2 | 5 | 4 | 5 | 4 | 5 | 3,95 |
| V3. JWKS solo en los servicios, sin gateway | 5 | 1 | 4 | 5 | 4 | 1 | 3,5 |
| V4. Introspección contra el IdP en cada request | — | — | — | — | — | — | `[REJECTED]` en D2: una llamada más por request y el IdP como punto único de falla |

### Con qué producto (D16)

| Alternativa | Seg. | Entr. | Cont. | Contr. | Costo | E7 | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **P1. Spring Cloud Gateway** | 5 | 5 | 5 | 5 | 3 | 5 | **4,8** |
| P2. Traefik | 4 | 5 | 2 | 5 | 4 | 4 | 3,95 |
| P3. Kong | 4 | 5 | 2 | 5 | 2 | 5 | 3,85 |
| P4. Nginx | 3 | 5 | 2 | 5 | 4 | 3 | 3,6 |

Traefik, Kong y Nginx validan el JWT con un plugin o un módulo y escriben los logs en otro formato
(D16). Spring Cloud Gateway valida con el soporte nativo de resource server y reusa el `traceId` y
los logs NDJSON.

### Cómo se enrutan las rutas

| Alternativa | Seg. | Entr. | Cont. | Contr. | Costo | E7 | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **R1. Mismas rutas que los servicios, con predicados por ruta completa donde un prefijo se comparte** | 4 | 5 | 5 | 5 | 4 | 4 | **4,55** |
| R2. Prefijo por servicio (`/donaciones/**`, `/logistica/**`…) con `StripPrefix` | 4 | 5 | 5 | 5 | 3 | 4 | 4,45 |
| R3. Renombrar las rutas de los servicios para que cada uno tenga su prefijo | 4 | 5 | 5 | 1 | 2 | 4 | 3,75 |

R1 deja que el cliente use las rutas de [`contratos-rest.md`](../arquitectura/contratos-rest.md)
tal cual. Su costo: `/api/logistica/**` no se puede rutear por prefijo, sino por cada ruta
completa: las dos rutas de Donaciones y las cuatro de Logística (que entran en tres predicados:
`/resultados`, `/callback/**` y `/planificaciones/**`). R2 queda como respaldo si las rutas compartidas crecen. R3 es
un *breaking change* sin beneficio para el usuario.

### Qué queda abierto sin login (D26)

| Alternativa | Cumple UI/UX 1 y 2 | Fiel al boceto | Datos a sanitizar |
|---|:-:|:-:|---|
| **A1. Mínimo del enunciado + entidades y necesidades** | Sí | Sí | Entidades y necesidades (HB-08, hecho) |
| A2. Mínimo del enunciado | Sí | No | Pocos |
| A3. Todo detrás del login | No | No | Mínimos |

## Resultado de la Decisión

Se eligen **V1**, **P1**, **R1** y **A1**. V1, P1 y A1 formalizan D2, D16 y D26; R1 y los puntos 4
a 6 agregan lo que esas decisiones no fijaban.

1. **Entrada única (RI-GW-01).** Un módulo `api-gateway` con Spring Cloud Gateway, con rutas hacia
   los cuatro servicios. El cliente conoce una sola URL base. El tren de Spring Cloud se fija al
   implementar, compatible con el Spring Boot del reactor `[INFERRED]`: no se verificó todavía.
2. **Validación en doble capa (RI-GW-02, RI-SVC-01).** El gateway valida firma (JWKS), `exp`, `iss`
   y `aud`; un token inválido da `401` con el formato de error unificado. Cada servicio vuelve a
   validar con JWKS. Las claves se cachean, así que el IdP caído no corta las requests con tokens
   vigentes.
3. **Denegar por defecto (RI-GW-03).** La allowlist son las rutas de API que usan las pantallas
   públicas de §6.2 de los requerimientos (D26). Las que piden datos, como la landing y la galería
   de donaciones realizadas (destacadas y contadores), el mapa de entregadas, y las entidades y
   necesidades públicas, pasan sin token. Cada una usa un DTO público propio, nunca el interno.
   - Las demás pantallas públicas no pasan por el gateway. Nosotros, las landings de donante y de
     entidad y Legales no piden datos.
   - Iniciar sesión, Registrarse, Activar cuenta y Recuperar contraseña van al IdP (Keycloak), no
     al gateway.
   - Si una pantalla pública suma una llamada, su ruta entra a la allowlist en el mismo cambio.
4. **`traceId` de punta a punta (RI-GW-04).** `[PROPOSED]` El cliente genera un `traceId` por request de la
   página y lo manda en `X-Trace-Id`, así puede mostrarlo en las páginas de error (RF-CL-08) aunque
   el servicio no responda. El gateway lo propaga o lo genera si falta, y escribe logs NDJSON con
   el formato de `common-lib`. Los servicios ya lo leen con `TraceResponseHeaderFilter`.
5. **Rutas sin reescritura (R1).** `[PROPOSED]` El gateway no cambia rutas ni payloads; donde un prefijo es de
   dos servicios, el predicado usa la ruta completa.
6. **Implementación en dos etapas.** `[PROPOSED]` Primero el enrutamiento, el `traceId` y la
   allowlist ya escrita, sin exigir el token. Después, la validación del JWT, cuando el cliente
   tenga login (SPEC-06, B1). La etapa 1 cumple RI-GW-01 y RI-GW-04. RI-GW-02 y RI-GW-03 quedan
   diferidos a la etapa 2 (E2-01). Mientras falte la etapa 2, todas las rutas pasan sin token: si
   eso llega a `main`, se registra como deuda en [`DEUDA_TECNICA.md`](./DEUDA_TECNICA.md).

## Consecuencias

- **Positivas:**
  - El cliente deja de conocer los puertos de los servicios.
  - Una llamada directa al puerto de un servicio sin token da `401` (con la segunda capa).
  - Un mismo `traceId` cruza el cliente, el gateway y el servicio: el `traceId` visible en una
    página de error sirve para buscar el log.
  - El gateway es el lugar para el rate limiting de E7.
- **Negativas / riesgos:**
  - Un componente más en el compose y en el CI (E2-12).
  - La validación se escribe dos veces: en el gateway y en cada servicio (HB-13).
  - Las rutas por ruta completa de `/api/logistica/**` hay que mantenerlas a mano: una ruta nueva
    bajo ese prefijo no se enruta sola.
  - Con la etapa 1 sola, la allowlist no protege nada: es un estado de transición, no de entrega.
- **Neutras:** los contratos REST y AMQP de los servicios no cambian.

## Validación

- Test de rutas del gateway: cada ruta de `contratos-rest.md` llega a su servicio, incluidas las
  de `/api/logistica/**` de los dos servicios.
- Sin token: las rutas de la allowlist responden y las demás dan `401` (etapa 2).
- Token vencido, de otro `iss` o sin el `aud` del API: `401` en el gateway y también en el servicio
  llamado por su puerto.
- `X-Trace-Id` enviado por el cliente aparece en los logs del gateway y del servicio, y vuelve en
  la respuesta.

## Pros y Contras de las Alternativas

### V1. Doble capa

- Bien, porque cubre las puertas laterales (puerto directo, llamadas internas).
- Bien, porque deja un punto único de entrada para el cliente y para E7.
- Mal, porque es la opción de más trabajo en E6.

### V2. Gateway que valida y servicios que confían

- Bien, porque valida en un solo lugar.
- Mal, porque una llamada directa a un puerto entra sin control.

### V3. Solo JWKS en los servicios

- Bien, porque no suma componentes.
- Mal, porque no cumple RQ-E6-02 ("a través del gateway") ni RI-GW-01.

### P1. Spring Cloud Gateway

- Bien, porque valida el JWT con el resource server nativo de Spring.
- Bien, porque reusa el `traceId` y los logs NDJSON de `common-lib`.
- Mal, porque es un módulo Java más para construir y mantener.

### P2, P3 y P4. Traefik, Kong y Nginx

- Bien, porque se configuran sin código. Traefik y Nginx son livianos.
- Mal, porque validan el JWT con un plugin o un módulo.
- Mal, porque escriben los logs en otro formato y rompen la trazabilidad NDJSON.
- Kong suma, además, su propia base o su modo declarativo.

### R1. Mismas rutas que los servicios

- Bien, porque el cliente usa `contratos-rest.md` tal cual y el gateway es transparente.
- Mal, porque las rutas bajo `/api/logistica/**` se mantienen a mano.

### R2. Prefijo por servicio

- Bien, porque cada ruta nueva se enruta sola.
- Mal, porque las rutas del cliente dejan de coincidir con `contratos-rest.md`.

### R3. Renombrar las rutas de los servicios

- Bien, porque deja un prefijo limpio por servicio.
- Mal, porque es un *breaking change* para n8n, el proveedor de logística y los tests.

### A1. Mínimo del enunciado + entidades y necesidades

- Bien, porque cumple UI/UX 1 y 2 y es fiel al boceto ("emergencias" en la landing).
- Mal, porque obliga a DTOs públicos propios (HB-08, ya hecho).

### A2. Mínimo del enunciado

- Bien, porque deja menos datos a la vista.
- Mal, porque la landing pierde las necesidades que atraen donantes.

### A3. Todo detrás del login

- Bien, porque no expone nada.
- Mal, porque no cumple UI/UX 1 y 2 (landing y mapa públicos).

## Links

- [Requerimientos de E6](../requerimientos/requerimientos-e6.md) §3, §5, §6.2 y §9 (D2, D16, D26)
- [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md), E2-01 y E2-12
- [Plan del cliente con datos reales y gateway](../requerimientos/plan-e6-cliente-datos-reales-y-gateway.md)
- [Contratos REST](../arquitectura/contratos-rest.md)
