# API gateway: enrutar `/donaciones-independientes` tal como la publica Donaciones

- Status: proposed
- Date: 2026-11-26
- Deciders: tsorren; falta la aprobación del equipo en el PR
- Tags: gateway, contratos, donaciones, cliente-liviano, entrega-7

> **Origen:** hallazgo del recorrido con login sobre el stack local `donatrack-guia` (2026-11-25),
> registrado en DTI-54 ítem 13 y antes en DTI-48 ítem 8 (rama `E7_guia_ui`). El usuario pidió el
> arreglo con ADR el 2026-11-25 y eligió la alternativa B el 2026-11-26.
>
> **Relación con ADR-E6-02:** este ADR modifica **solo** la exclusión de `/donaciones-independientes`
> del [ADR del gateway](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
> (ADR-E6-02). El resto de ADR-E6-02 sigue vigente: no lo reemplaza, así que ADR-E6-02 no pasa a
> `superseded`.

## Contexto y Problema

- `[VERIFIED]` 2026-11-25, stack `donatrack-guia` con los datos del seed: con la sesión de una
  donante, `/donante-inicio`, `/donante-donaciones` y `/donante-seguimiento` dan 404. El gateway
  registra «No resource found» para `donaciones-independientes`.
- `[OBSERVED]` El cliente liviano llama a `GET /donaciones-independientes`, `GET …/conteos`,
  `GET …/{id}` y `PATCH …/{id}/estado` por el gateway (`DonacionesApi`).
- `[OBSERVED]` `RutasGateway` no enruta esa ruta. ADR-E6-02 la deja afuera junto con los alias sin
  `/api` de Notificaciones: «el gateway expone solo las rutas con `/api`; los alias quedan afuera».
  SPEC-07 y los tests del gateway (`RutasGatewayTest`, `SeguridadGatewayTest`) exigen el 404.
- `[OBSERVED]` `/donaciones-independientes` **no es un alias**. Es la única ruta del recurso en
  Donaciones (`DonacionesIndependientesController`, `@RequestMapping("/donaciones-independientes")`).
  No tiene gemela con `/api` ni equivalente bajo `/api/donaciones`. Los alias de Notificaciones sí
  repiten una ruta que el gateway ya enruta con `/api`.
- `[INFERRED]` Por eso falla toda pantalla que lea las donaciones independientes por el gateway:
  las de quien dona, las de la organización y las del admin.

Problema: ¿cómo llega el cliente a las donaciones independientes por el gateway sin romper el
contrato de Donaciones ni R1 de ADR-E6-02 (rutas sin reescritura)?

## Atributos de Calidad y Drivers de Decisión

Pesos `[INFERRED]`: compatibilidad hacia atrás (AGENTS.md §8.2) 25 · respeta R1 de ADR-E6-02 20 ·
seguridad (doble capa, sin ruta abierta) 20 · costo y superficie del cambio 20 · coherencia del
prefijo `/api` 15.

## Relaciones Arquitectónicas

1. **Modifica en parte:** [ADR-E6-02](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
   (`proposed`): deja de excluir `/donaciones-independientes`. Aplica su R1 sin cambios.
2. **Respeta:** [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): el contrato de Donaciones no cambia.
3. **Respeta:** [rate limiting en el gateway](./20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md)
   (`proposed`): la ruta nueva pasa por el mismo `LimiteDeSolicitudes` que las demás.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`). Columnas: compatibilidad ·
R1 · seguridad · costo · prefijo `/api`.

| Alternativa | Compat. | R1 | Seg. | Costo | `/api` | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **B. Enrutar la ruta existente en el gateway** | 5 | 5 | 5 | 5 | 2 | **4,55** |
| A. Alias `/api/donaciones-independientes` en el servicio, el gateway y el cliente | 4 | 5 | 5 | 2 | 5 | 4,15 |
| C. Reescribir `/api/donaciones-independientes/**` a `/donaciones-independientes/**` en el gateway | 4 | 1 | 5 | 3 | 4 | 3,4 |
| D. Renombrar la ruta del servicio a `/api/donaciones-independientes` | 1 | 5 | 5 | 2 | 5 | 3,4 |
| E. El cliente llama directo al servicio | — | — | — | — | — | `[REJECTED]`: RQ-E6-02 pide todo por el gateway |

## Resultado de la Decisión

Se elige **B**.

1. `RutasGateway.rutaDonaciones` suma `/donaciones-independientes` y `/donaciones-independientes/**`,
   sin reescritura (R1).
2. La ruta **no** entra a la allowlist pública (`rutas-publicas`): sin token da `401` en el gateway,
   porque la cadena de `common-lib` exige autenticación para toda ruta que no sea pública
   (`anyRequest().authenticated()`). Donaciones vuelve a validar con su `@PreAuthorize` (segunda capa).
3. Los alias sin `/api` de Notificaciones siguen afuera: esos sí repiten una ruta que el gateway ya
   enruta.
4. El criterio de ADR-E6-02 queda así: el gateway expone las rutas tal como las publica cada
   servicio. Deja afuera solo los alias que duplican una ruta ya enrutada.

## Consecuencias

- **Positivas:**
  - Las pantallas que leen las donaciones independientes funcionan por el gateway, sin cambiar el
    cliente ni el servicio.
  - Solo cambia el módulo `api-gateway`: se reconstruye una imagen.
  - El seeder, `integration-tests` y Postman, que llaman al servicio por la misma ruta, no cambian.
- **Negativas / riesgos:**
  - Queda un recurso de Donaciones sin el prefijo `/api` en la entrada pública. Es la misma
    inconsistencia que ADR-E6-02 ya había registrado en Donaciones; ahora también se ve en el gateway.
  - Si el equipo decide unificar el prefijo, la alternativa A sigue disponible como evolución
    aditiva: alias con `/api`, migrar el cliente y después deprecar la ruta vieja.
- **Neutras:** los contratos REST y AMQP de los servicios no cambian. La OpenAPI de Donaciones y la
  matriz de permisos ya describen la ruta como está.

## Validación

- `RutasGatewayTest`: el listado, `conteos`, `{id}` y `PATCH {id}/estado` llegan a Donaciones con
  la misma ruta.
- `SeguridadGatewayTest`: sin token, `/donaciones-independientes` da `401`. Una ruta fuera de la
  tabla, con token, sigue dando `404`: se prueba con un alias de Notificaciones.
- Con el stack local: `/donante-inicio` con la sesión de una donante deja de dar 404.

## Pros y Contras de las Alternativas

### B. Enrutar la ruta existente

- Bien, porque cumple R1 al pie de la letra: la misma ruta que publica el servicio.
- Bien, porque toca un solo módulo y ningún contrato.
- Mal, porque deja una ruta sin `/api` en la entrada pública.

### A. Alias con `/api`

- Bien, porque deja el prefijo coherente con el resto de Donaciones.
- Mal, porque toca tres módulos y duplica cuatro paths en la OpenAPI.
- Mal, porque cambia unos quince tests del cliente y deja dos rutas vivas, o abre un ciclo de
  deprecación.

### C. Reescritura en el gateway

- Bien, porque el cliente ve `/api` sin tocar el servicio.
- Mal, porque R1 de ADR-E6-02 prohíbe que el gateway cambie las rutas.

### D. Renombrar la ruta del servicio

- Bien, porque deja un prefijo limpio.
- Mal, porque es un *breaking change* para el seeder, `integration-tests`, Postman y el cliente.

### E. Cliente directo al servicio

- Mal, porque no cumple RQ-E6-02 ni RI-GW-01 (entrada única).

## Links

- [ADR-E6-02](./20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)
- [SPEC-07](../specs/active/SPEC-07-api-gateway-etapa-1.md)
- [Contratos REST](../arquitectura/contratos-rest.md)
- [OpenAPI de Donaciones](../arquitectura/contratos/openapi-donaciones.yaml)
- [Matriz de permisos](../arquitectura/seguridad/matriz-permisos.md)
