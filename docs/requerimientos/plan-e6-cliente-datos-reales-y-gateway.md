# Plan E6 — Cliente liviano con datos reales, gateway y base para la autenticación

> **Estado:** propuesta (`[PROPOSED]`), 2026-11-07. Sin fecha fija: las fases se ordenan por
> dependencias y valor. Complementa el [plan E6 sin autenticación](plan-e6-trabajo-sin-auth.md)
> (tareas 11, 12 y 23) y [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md) (B1, E2-01 y
> E2-12); no repite sus tareas, las ordena alrededor del cliente.
> Decisión de arquitectura asociada: [ADR-E6-02, API gateway](../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md) (`proposed`).

## 1. Para qué sirve

Las pantallas del enunciado están dibujadas (roadmap de vistas F1 a F7), pero todas muestran datos
de ejemplo. Este plan dice cómo seguir con el cliente **sin esperar al diseño gráfico ni a UX/UI**:
datos reales de las APIs (RQ-E6-02, RQ-E6-03), un gateway como única entrada (RI-GW-01 a 04) y lo
necesario para que el login (SPEC-06) entre sin rehacer los controllers.

Qué quiere decir "sin depender del diseño":

- Ninguna fase cambia el marcado, las clases ni las hojas de estilo.
- Lo único que se toca en una plantilla es **de dónde lee los datos**: pasa de leer los datos de
  ejemplo escritos en Thymeleaf a leer el modelo que arma el controller.
- El piso visual (`-Pvisual`) es la red: con el adaptador de la maqueta, cada pantalla se tiene que
  ver igual que antes del cambio.

La idea central: lo que cada plantilla consume (sus variables y la forma de cada dato) es un
**contrato de vista**. Si ese contrato se escribe como records del cliente y se llena desde puertos,
el diseño y la integración avanzan en paralelo. El diseño cambia el marcado; la integración cambia
quién llena los records.

## 2. Punto de partida `[OBSERVED]`

En `origin/ENTREGA_6` (`e43e50a9`):

| Pieza | Estado |
|---|---|
| Datos de las pantallas | `SitioController` (`@Profile("maqueta")`) sirve `/<pantalla>` y carga en el modelo solo menús, cuentas y pasos. Los datos de ejemplo están en las plantillas: `datos-donante.html`, `datos-publicos.html` y `viaje-datos.html` (315 líneas), que incluyen 12 pantallas; además, 9 pantallas escriben listas de ejemplo en expresiones de Thymeleaf |
| Paquetes del cliente | `maqueta` y `vistas` (4 records de navegación). `web` y `puertos` figuran en [`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md) como estructura objetivo, pero no existen |
| Llamadas REST | Ninguna: sin `RestClient`, sin URLs de servicios, sin Spring Security, sin `common-lib` |
| Pantallas | `ENTREGA_6`: público 18, donante 9, entidad 14 y admin 1 plantillas. La rama del [PR 935](https://github.com/tsorren/DonaTrack-TP-DDS/pull/935) (`E6_raices_calida` → `E6_raices`, borrador) tiene 26, 10, 18 y 32: el admin entero vive ahí |
| Gateway | No existe. Cada servicio publica su puerto (8080 a 8083); Keycloak ya está en el compose (SPEC-06, A1-01 a A1-04) |
| Rutas | `/api/logistica` es prefijo de Donaciones y de Logística; `/donaciones-independientes` no lleva `/api` (ver el ADR-E6-02) |
| `traceId` | `common-lib` lo lee o lo genera y lo devuelve en `X-Trace-Id`; el cliente no lo usa |

Huecos de backend (§8 de los [requerimientos](requerimientos-e6.md)) y las pantallas que destraban:

| HB | Estado en `ENTREGA_6` | Pantallas (RF) |
|---|---|---|
| HB-03, HB-04 filtros de donaciones independientes | Hecho (#931) | Mis donaciones (RF-05), Donaciones recibidas (RF-11) |
| HB-08 DTOs públicos de entidades y necesidades | Hecho (#937): `/api/entidades/publicas`, `/api/necesidades/publicas` | Entidades y necesidades (RF-22), Beneficiarios (RF-06) |
| HB-09 notificaciones por evento | Hecho (#936) | Notificaciones (RF-08, RF-13) |
| HB-15 paginado y búsqueda de entidades | Hecho (#934) | Beneficiarios (RF-06), RF-22 |
| HB-07 mapa de entregadas | El endpoint existe; con la D24 revisada solo quedaba el Javadoc (tarea 22, hecha) | Mapa de entregadas (RF-02) |
| HB-01, HB-02 destacadas y contadores | HB-02 y HB-01 hechos (2026-11-08) | Landing (RF-01) |
| HB-05, HB-06, HB-16 camión y entregas activas | Hechos (2026-11-08): HB-16 con `GET /api/entregas/seguimiento/donantes/{id}`, HB-05 y HB-06 con `posicionCamion` en el seguimiento | Seguir entregas del donante y de la entidad (RF-09, RF-14) |
| HB-10 fotos multipart | Pendiente | Confirmar recepción (RF-12) |
| HB-14 progreso del CSV | En el PR #956 | Importar CSV (RF-20) |
| HB-11, HB-12, HB-13 | Esperan la autenticación | Mi perfil, Aprobar entidades, autorización |
| HB-17 a HB-20 | Hechos: HB-17 a HB-19 mergeados (#951, #957, #958); HB-20 en el [PR #960](https://github.com/tsorren/DonaTrack-TP-DDS/pull/960) | Remito y problemas, necesidades, nombre visible |
| HB-21 | Decidido (2026-11-10): Notificaciones, [ADR `proposed`](../adr/notificaciones-service/20261110-recepcion-de-mensajes-de-contacto-en-notificaciones.md); el código va en un hueco aparte (DTI-33) | Contacto |

Sin hueco: las pantallas del admin (RF-15 a RF-19: donaciones, asignaciones, camiones, ranking) y
misiones e insignias (RF-07) ya tienen sus endpoints (§6.1 de los requerimientos).

## 3. Criterios

Los mismos pesos para todas las matrices `[INFERRED]`, puntajes de 1 (peor) a 5 (mejor):

| Criterio | Peso | Qué mide |
|---|:-:|---|
| Independencia del diseño | 25 | Avanza sin cambiar marcado ni estilos, y sin esperar decisiones de diseño |
| Valor para el enunciado | 20 | Acerca RQ-E6-02 y RQ-E6-03: pantallas con datos reales |
| Riesgo de choque | 15 | Pocos archivos en común con ramas abiertas (PR 935, SPEC-06 B1, compose) |
| Costo | 15 | Archivos y tests que suma |
| Reversibilidad | 10 | Se deshace o se cambia sin rehacer otras partes |
| Preparación para la autenticación | 15 | El login (B1) y el token (E2-01) entran sin rehacer lo hecho |

## 4. Decisiones

En cada matriz, la opción recomendada va primero y en negrita.

### A. Dónde se cortan la vista y los datos

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **A1. Un puerto por API con dos adaptadores (maqueta y REST), y controllers en `web` que llenan los records de `vistas`** | 5 | 5 | 3 | 3 | 4 | 5 | **4,3** |
| A2. Controllers reales que llaman a `RestClient` directo, con la maqueta aparte | 3 | 5 | 4 | 3 | 3 | 3 | 3,55 |
| A3. La maqueta pasa a ser stubs HTTP de las APIs reales y queda un solo adaptador, el REST | 4 | 5 | 2 | 2 | 2 | 4 | 3,4 |

- **A1** es la estructura que ya fijan `cliente-liviano/AGENTS.md` y el
  [ADR del stack del cliente](../adr/cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
  ("adaptadores de ejemplo, perfil `maqueta`, o REST"), y la tarea 11 del plan sin autenticación.
  La maqueta sigue sirviendo las capturas y los escenarios (`?escenario=`); el adaptador REST se
  prueba solo.
- A2 deja dos caminos para cada pantalla, y el de la maqueta se desactualiza.
- A3 obliga a reescribir toda la maqueta como respuestas JSON antes de ver un dato real, y los
  escenarios (lento, error) pasan a configurarse en los stubs.
- **Complemento de A1:** cuando una pantalla junta varias APIs (por ejemplo, el inicio del donante:
  donaciones, notificaciones y misiones), un servicio de consulta en `web` compone los puertos. No
  es una alternativa a A1, es cómo se usa.

### A'. Dónde viven los datos de ejemplo

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **M1. Pasan de las plantillas a records que devuelve el adaptador de la maqueta; la plantilla solo lee el modelo** | 5 | 5 | 2 | 2 | 4 | 4 | **3,85** |
| M2. Quedan en la plantilla para la maqueta, y la plantilla usa el modelo si existe (`th:with` con `?:`) | 4 | 4 | 3 | 4 | 3 | 3 | 3,6 |
| M3. Una plantilla por modo (maqueta y real) | 3 | 4 | 4 | 1 | 3 | 3 | 3,05 |

- **M1** es la que más toca: cada plantilla con datos de ejemplo cambia dónde lee. Pero lo hace una
  vez por pantalla, con el piso visual como prueba, y deja una sola fuente de datos.
- M2 es más barata al principio, pero cada plantilla queda con dos fuentes, y un error en el modelo
  real se tapa con el dato de ejemplo.
- M3 duplica más de 50 plantillas `[REJECTED]`.
- **Riesgo de M1:** las mismas plantillas son las que toca el diseño. Se migra una pantalla por vez
  y se avisa antes, para no pisar una rama de diseño abierta (ver §6).

### B. Con qué se llama a las APIs

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **B1. Interfaces HTTP de Spring (`@HttpExchange`) sobre `RestClient`** | 5 | 5 | 5 | 4 | 5 | 5 | **4,85** |
| B2. `RestClient` escrito a mano en cada adaptador | 5 | 5 | 5 | 3 | 5 | 5 | 4,7 |
| B3. OpenFeign | 5 | 5 | 4 | 3 | 4 | 4 | 4,3 |

- B1 y B2 están en `spring-web`, que el cliente ya trae con `spring-boot-starter-web`: no suman
  dependencias. B1 declara cada endpoint en una interfaz y se escribe menos.
- Los dos aceptan un interceptor para `X-Trace-Id` y, después, para el token (§E).
- B3 suma una dependencia al cliente (AGENTS.md §6 pide aprobación) sin ganar nada sobre B1.

### C. El gateway: cuándo y con qué

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **C1. Ya, en dos etapas: enrutamiento, `X-Trace-Id` y allowlist; el JWT, con el login** | 5 | 5 | 3 | 4 | 4 | 4 | **4,3** |
| C3. Sin gateway por ahora: el cliente usa la URL de cada servicio | 5 | 3 | 5 | 5 | 4 | 2 | 4,05 |
| C2. Completo desde el inicio, con el JWT | 5 | 3 | 3 | 2 | 4 | 5 | 3,75 |

- C1 es la etapa 1 del [ADR-E6-02](../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md).
  El cliente apunta a una sola URL desde el principio y nada se mueve al sumar el JWT.
- C2 deja sin datos a las pantallas privadas hasta que exista el login (B1).
- C3 no cumple RQ-E6-02 ("a través del gateway").
- **Margen que da C1:** el gateway no reescribe rutas, así que el cliente puede tener una URL base
  por API (`cliente.api.donaciones.url`, etc.) que en el compose apuntan todas al gateway. Mientras
  el gateway no esté, una rebanada de F2 puede probarse contra el servicio directo. F2 no espera a
  F1.

### D. En qué orden se conectan las pantallas

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **D1. Por pantalla (rebanada vertical), primero las que ya tienen endpoint** | 5 | 5 | 4 | 4 | 5 | 3 | **4,4** |
| D3. Por rol completo (todo el público, después el donante…) | 5 | 4 | 3 | 3 | 4 | 4 | 3,95 |
| D2. Por API (todos los adaptadores de Donaciones, después Logística…) | 5 | 3 | 4 | 4 | 4 | 3 | 3,9 |

- D1 da una pantalla terminada por paso, y cada paso se revisa solo.
- D3 deja el público para el final, porque su landing espera HB-01 y HB-02.
- D2 tarda en mostrar una pantalla entera.

### E. La base para la autenticación, antes de Keycloak

¿Quién es el usuario mientras no hay login?

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **E1. Un puerto `CuentaActual` con tres adaptadores: maqueta (`?cuenta=`), id por parámetro y, con B1, el usuario del login** | 5 | 4 | 4 | 4 | 5 | 5 | **4,5** |
| E3. Esperar el login para las pantallas privadas | 5 | 1 | 5 | 5 | 5 | 5 | 4,2 |
| E2. Un id fijo en `application.properties` | 5 | 3 | 5 | 5 | 4 | 2 | 4,05 |

- **E1:** los controllers piden la cuenta al puerto y no se enteran de dónde sale. El id por
  parámetro lo decidió tsorren el 2026-11-06, al relevar el plan sin autenticación: la pertenencia
  y `/me` llegan con la etapa 2. Esa decisión no quedó escrita en ese plan, que solo deja la
  autorización por pertenencia fuera de su alcance (§6).
- **E3:** cuesta nada, pero deja sin datos a las pantallas privadas hasta F4.
- **E2:** es lo más simple. Pero un solo usuario no alcanza para probar un donante y una entidad a
  la vez, y el valor queda en la configuración, que hay que borrar al llegar el login.

Las otras costuras que se dejan listas sin Keycloak:

1. **Interceptor del cliente HTTP** que agrega `X-Trace-Id` (lo genera el cliente, ADR-E6-02) y,
   desde B1, `Authorization: Bearer` con el token de la sesión (*token relay*, D15). Antes de B1
   no agrega nada.
2. **Errores HTTP → páginas de error** (RF-CL-08, tarea 12): `401` → login, `403` → sin permiso,
   `404` y `5xx` con el `traceId`. Las páginas de error ya están dibujadas en el PR 935 (F7 del
   roadmap); falta que una respuesta real las dispare.
3. **Rutas privadas por prefijo** (`/donante*`, `/entidad*`, `/admin*`), como las pide B1-01: los
   controllers de `web` respetan esos prefijos, así las reglas de Spring Security entran sin mover
   rutas.

### F. Cómo se prueban los adaptadores REST

| Alternativa | Dis. | Valor | Choque | Costo | Rev. | Auth | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **F1. `MockRestServiceServer` (spring-test) con respuestas JSON de ejemplo** | 5 | 4 | 5 | 4 | 5 | 4 | **4,5** |
| F2. WireMock | 5 | 4 | 5 | 3 | 4 | 4 | 4,25 |
| F3. Testcontainers con los servicios reales | 5 | 5 | 4 | 2 | 4 | 4 | 4,15 |

- F1 no suma dependencias y corre en el `mvn test` del módulo.
- F2 suma una dependencia de test (pide aprobación) y da lo mismo para este uso.
- F3 no reemplaza a F1: es el Gate 3/4. Sirve para comprobar que los JSON de ejemplo de F1 se
  parecen a lo que el servicio devuelve de verdad.

## 5. Fases

Tamaños `[INFERRED]`, contados en archivos y tests; no son mediciones.

| Fase | Qué | Depende de | Tamaño | Validación | Choque |
|---|---|---|---|---|---|
| **F0** | **Esqueleto de integración, sin plantillas:** paquetes `web` y `puertos`; elegir el adaptador de cada puerto por propiedad (`maqueta` o `rest`), así una pantalla migrada y una sin migrar conviven; `RestClient` con una URL base por API; interceptor de `X-Trace-Id`; `CuentaActual`; errores HTTP → páginas de error | Nada | ~10 archivos Java · ~12 tests | `mvn clean test -pl cliente-liviano -am` | Bajo: paquetes nuevos. `SitioController` sigue sirviendo las pantallas sin migrar (una ruta literal de `web` le gana a su `/{pantalla}`). Hoy es `@Profile("maqueta")`: F0 lo deja activo también con los adaptadores `rest`, o las pantallas sin migrar darían 404 |
| **F1** | **Gateway, etapa 1** (hecha en [SPEC-07](../specs/active/SPEC-07-api-gateway-etapa-1.md), 2026-11-07)**:** módulo `api-gateway`, rutas sin reescritura (incluidas las de `/api/logistica/**`), allowlist escrita, `X-Trace-Id` y logs NDJSON; servicio en el compose | ADR-E6-02 | ~8 archivos · compose · ~6 tests de rutas | Módulo + `docker compose up` | Medio: el compose lo editan también A1 y E2-12 |
| **F2** | **Rebanadas con endpoint existente** (qué conecta y qué pierde cada pantalla: [brechas de datos por pantalla](brechas-de-datos-por-pantalla-e6.md))**:** por pantalla, (1) los datos de ejemplo pasan a records del adaptador de la maqueta, con el piso visual igual; (2) el adaptador REST. Orden: Beneficiarios y Entidades públicas (HB-08, HB-15), Mis donaciones y Donaciones recibidas (HB-03, HB-04), Notificaciones (HB-09), Misiones (RF-07), Necesidades (RF-10), Mapa de entregadas (RF-02); después, el admin (RF-15 a RF-19) | F0; el admin, además, que el PR 935 llegue a `ENTREGA_6` | Por pantalla: ~3 archivos Java, 1–3 plantillas (solo de dónde leen), ~3 tests | Tests del módulo + piso visual con el adaptador de la maqueta + la pantalla contra el compose | Alto en las plantillas: avisar antes de cada rebanada |
| **F3** | **Huecos pendientes y sus rebanadas:** HB-01 y HB-02 (landing), HB-05 y HB-06 (seguir entregas; HB-16 hecho el 2026-11-08), HB-10 (confirmar recepción), HB-14 (CSV); después, HB-17 a HB-20 | Tareas 4 a 9 del plan sin autenticación, cada una en su servicio | Lo que dice ese plan, más una rebanada de F2 por pantalla | Gate 2 del servicio + la rebanada | Por servicio, como en ese plan |
| **F4** | **Login en el cliente:** SPEC-06 B1-01 a B1-05; `CuentaActual` con el adaptador del login y el interceptor con el token | A1 hecho; F0 | Lo que dice SPEC-06 | Lo que dice SPEC-06 | Medio: `SitioController` y los layouts |
| **F5** | **Gateway, etapa 2, y JWKS en los servicios:** E2-01, HB-13; los ids de "mis datos" salen del token (E2-03, HB-11) y se retira el id por parámetro | F1, F4 | E2-01 y E2-03 | Los casos de "Validación" del ADR-E6-02 | Alto: los cuatro servicios |
| **F6** | **De punta a punta:** el cliente en el compose (E2-12) y el recorrido de la demo (tarea 23) | F1, F2; con login, F4 y F5 | 1 script · 1 doc · compose | Gate 4 | Medio: compose y CI |

```text
F0 ──┬──> F2 (rebanadas) ──> F3 (huecos)
     └──> F4 (login) ──┐
F1 (gateway, etapa 1) ─┴──> F5 (JWT y JWKS) ──> F6 (punta a punta)
```

Prioridades:

1. **F0 y F1, en paralelo:** no comparten archivos y destraban todo lo demás.
2. **F2:** la primera pantalla con datos reales es la prueba de que la costura funciona. Conviene
   empezar por una pantalla chica (Beneficiarios) antes de seguir con las demás.
3. **F3 y F4, en paralelo:** F3 corre por servicio; F4 lo tiene quien lleve SPEC-06 B1.
4. **F5 y F6:** cierran la seguridad y la demo.

## 6. Riesgos y coordinación

- **PR 935 y diseño en las mismas plantillas.** Las rebanadas de F2 cambian de dónde leen las
  plantillas que el diseño sigue puliendo. Con M1 se migra una pantalla por vez y se avisa antes;
  si hay una rama de diseño abierta sobre esa pantalla, la rebanada espera o se hace sobre esa rama.
- **El admin no está en `ENTREGA_6`.** Sus 32 plantillas viven en la rama del PR 935, que va a
  `E6_raices`. Sus rebanadas esperan a que ese trabajo llegue a `ENTREGA_6`.
- **Compose compartido.** F1, A1 (Keycloak) y E2-12 editan `docker-compose.yml`: una rama por vez.
- **Contrato del cliente que se desactualiza.** Los JSON de ejemplo de F1 (§F) pueden quedar viejos
  si un servicio cambia su respuesta. La política de evolución de contratos pide cambios aditivos, y
  la F6 lo comprueba contra los servicios reales.
- **Gateway sin JWT.** Mientras falte la etapa 2, la allowlist no protege nada. Si F1 llega a
  `main` sin F5, se registra como deuda (ADR-E6-02).

## 7. Lo que este plan no cubre

- Estilos, tokens, identidad visual y la migración de componentes (B3).
- Los cambios internos de cada servicio: están en el plan sin autenticación y en SPEC-06.
- El diseño de la sesión del cliente (ADR-E6-03) y de la autorización por pertenencia (ADR-E6-04).

## 8. Evidencia y límites

- `[OBSERVED]` El punto de partida sale de `git ls-tree` y `git grep` sobre `origin/ENTREGA_6`
  (`e43e50a9`), de `gh pr view` (PR 927, 930 a 933, 935 y 936) y de la lectura de los
  requerimientos de E6, el plan sin autenticación, SPEC-06 y `cliente-liviano/AGENTS.md`. No se
  corrió ningún test ni se levantó el compose.
- `[INFERRED]` Los pesos, los puntajes y los tamaños son estimaciones del agente.
- `[PROPOSED]` Las recomendaciones, las fases y su orden.
