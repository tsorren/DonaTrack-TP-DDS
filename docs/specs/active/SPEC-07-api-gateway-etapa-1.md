# Spec: SPEC-07 — API gateway, etapa 1 (rutas, allowlist escrita y traceId)

> **Estado:** APPROVED_BY_USER (2026-11-07, tsorren: diseño G1 + L1). Falta la revisión del equipo en el PR.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): suma un módulo y una dependencia estructural (Spring Cloud Gateway).
> **Fecha:** 2026-11-07
> **Base:** `ENTREGA_6` (`662c69e1`). Rama del PR: `E6_api_gateway_etapa1`, hacia `ENTREGA_6`.
> **ADR:** [ADR-E6-02, API gateway](../../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`). Esta spec implementa la etapa 1, sin decisiones nuevas de arquitectura.
> **Plan:** fila F1 del [plan del cliente con datos reales y gateway](../../requerimientos/plan-e6-cliente-datos-reales-y-gateway.md).

---

## 1. Objetivo Funcional (Goal)

Que el cliente liviano tenga **una sola URL base** para los cuatro servicios (RI-GW-01) y que un mismo `traceId` cruce el cliente, el gateway y el servicio (RI-GW-04).

**Criterio de hecho:**
- Cada ruta de [`contratos-rest.md`](../../arquitectura/contratos-rest.md) con `/api` llega a su servicio sin reescritura, incluidas las de `/api/logistica/**`, que son de dos servicios.
- Una ruta fuera de la tabla da `404`.
- El `X-Trace-Id` del cliente llega al servicio y vuelve una sola vez en la respuesta. Si no viene, el gateway genera uno de 32 hex.
- El gateway escribe un log por pedido con el formato de `common-lib` (NDJSON con los perfiles `json`, `preprod` o `prod`).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- Módulo `api-gateway`: Spring Cloud Gateway Server WebMVC 5.0.1 (tren 2025.1.1, ya en el BOM) + `common-lib`.
- Tabla de rutas en Java (`RutasGateway`): es la allowlist de rutas expuestas.
- Allowlist pública escrita en `application.yml` (`donatrack.gateway.rutas-publicas`), sin aplicar.
- Servicio `api-gateway` en `docker-compose.yml`, puerto 8086.

### Out-of-Scope

- Validación del JWT y aplicación de la allowlist: etapa 2 (F5, E2-01).
- Gateway en el preprod y el cliente apuntando al gateway en el compose: E2-12 y F6. El CI ya lo construye y publica su imagen solo, porque arma la lista de servicios desde `*/pom.xml` con `Dockerfile`. `scripts/resolver-imagenes-ghcr.sh` lo saltea, como a `auth-service`, porque no está en `docker-compose.preprod.yml`.
- Destacadas de la landing: no tienen endpoint todavía (HB-01). Los contadores (`GET /api/donaciones/contadores`, HB-02) ya están en `rutas-publicas`.
- Alias sin `/api` de Notificaciones y `/donaciones-independientes`: el ADR los deja afuera.

## 3. Decisiones Acordadas y Trade-offs


### Alternativas de tecnología

Pesos: reuso de `common-lib` 30 · encaje con el stack 25 · costo 20 · base para JWT en F5 15 · dependencias 10. Puntajes `[INFERRED]`.

| Opción | Reuso | Stack | Costo | F5 | Deps | Total |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **G1. SCG Server WebMVC + `common-lib`** | 5 | 5 | 4 | 5 | 4 | **4,65** |
| G3. Proxy propio con `RestClient` | 4 | 4 | 2 | 3 | 5 | 3,6 |
| G2. SCG Server WebFlux | 1 | 2 | 3 | 5 | 4 | 2,5 |

- G1 reusa sin cambios `TraceResponseHeaderFilter`, la adopción del `X-Trace-Id` en Brave (`XTraceIdPropagationFactory`), `logback-spring.xml` y el error unificado. Todo eso es servlet.
- G2 se descarta: `common-lib` trae `starter-web`, que fuerza el modo servlet, y su filtro y el MDC no corren en Reactor.
- G3 se descarta: reinventa el reenvío de headers, el multipart y el streaming.

### Cómo se escribe la allowlist

| Opción | Una sola fuente | Generar el trace | Legible | Total |
|---|:-:|:-:|:-:|:-:|
| **L1. Tabla de rutas en Java DSL + rutas públicas en `application.yml`** | 5 | 5 | 4 | **4,7** |
| L2. Rutas en `application.yml` | 4 | 2 | 5 | 3,6 |

L2 se descarta porque el yml no tiene un filtro con valor dinámico para el `X-Trace-Id`, así que habría que sumar código igual.

## 4. Invariantes

- El gateway no reescribe rutas ni payloads (R1 del ADR-E6-02).
- Lo que no está en la tabla de rutas da `404`.
- El `X-Trace-Id` vuelve una sola vez en la respuesta, con el mismo valor que recibió el servicio.
- Toda ruta de `rutas-publicas` tiene una ruta expuesta.

## 5. Restricciones (Constraints)


- No cambia rutas ni payloads de los servicios (ADR de evolución de contratos, `accepted`).
- No toca `common-lib` ni `cliente-liviano/`.
- Mientras falte la etapa 2, todas las rutas pasan sin token. Si esto llega a `main` sin F5, se registra como deuda (ADR-E6-02, punto 6).

## Criterios de Aceptación

```gherkin
Escenario: ruta compartida bajo /api/logistica
  Dado el gateway con los cuatro servicios configurados
  Cuando el cliente pide GET /api/logistica/proveedores
  Entonces el pedido llega a Donaciones con la misma ruta
  Y cuando pide POST /api/logistica/resultados, llega a Logística

Escenario: ruta fuera de la tabla
  Dado el gateway
  Cuando el cliente pide POST /notificaciones (alias heredado)
  Entonces responde 404 y el pedido no llega a ningún servicio

Escenario: traceId del cliente
  Dado un pedido con X-Trace-Id de 32 hex
  Cuando pasa por el gateway
  Entonces el servicio recibe ese X-Trace-Id
  Y la respuesta lo trae una sola vez

Escenario: traceId generado
  Dado un pedido sin X-Trace-Id
  Cuando pasa por el gateway
  Entonces el servicio recibe un X-Trace-Id de 32 hex
  Y la respuesta trae el mismo valor
```

## 6. Validación


| Gate | Comando | Qué cubre |
|---|---|---|
| 1–2 | `mvn clean test -pl api-gateway -am` | `RutasGatewayTest`: 18 rutas a su servicio, 5 rutas fuera de la tabla (`404`), trace propagado y generado, cada ruta pública con ruta expuesta |
| 2 | `mvn test` (reactor) | Que el módulo nuevo no rompa los demás |
| 4 | `docker compose up api-gateway` y `curl :8086/api/entregas/mapa` | `[DEFERRED_NO_DOCKER]` si no hay Docker |

## 7. Especificación Técnica de Diseño

### Recorrido de un pedido


1. `TraceResponseHeaderFilter` (de `common-lib`) toma el `traceId`: el que adoptó Brave del `X-Trace-Id`, o uno nuevo. Lo deja en el MDC y en la respuesta.
2. La ruta lo copia al pedido saliente (`propagarTraceId`) y lo manda a la URL del servicio (`donatrack.gateway.servicios.*`).
3. A la respuesta se le saca el `X-Trace-Id` del servicio para que no quede duplicado, y se escribe el log `[GATEWAY]` con el método, la ruta, el servicio y el estado.

**Two-Gate ADR (AGENTS.md §9.1):**

| Decisión | Gate A | Gate B | Resultado |
|---|---|---|---|
| Spring Cloud Gateway, rutas sin reescritura, etapas | No: las decide el ADR-E6-02 (P1, R1, punto 6) | — | Sin ADR nuevo |
| Variante WebMVC (G1) y allowlist en Java + yml (L1) | No: implementan P1 sin cambiar límites ni contratos | — | Quedan en esta spec |

**Plan de tests (TDD):** `RutasGatewayTest`, con `@SpringBootTest` y cuatro servicios falsos (`HttpServer` del JDK, sin dependencias nuevas). Cada servicio falso responde con su nombre, la ruta recibida y el `X-Trace-Id` recibido. Un caso por escenario de los criterios de aceptación, más las 18 rutas de la tabla.
