# Spec: SPEC-09 — Cierre de la E6: demo punta a punta

> **Estado:** PROPOSED (2026-11-11). El usuario aprobó el plan de cierre ese día; la aprobación de esta spec es el merge de sus PRs ("rama = gate"). Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): un valor nuevo en la máquina de estados de `Entrega` (cambio de contrato entre Logística, Donaciones y el cliente), una migración de persistencia en Notificaciones e impacto en cuatro módulos.
> **Fecha:** 2026-11-11
> **Base:** `ENTREGA_6` (`0c00c96a`). Rama de control: `E6_cierre_control`. Un PR en borrador hacia `ENTREGA_6` por frente: `E6_sonar_S2083`, `E6_cierre_F2`, `E6_HB22_llegada_a_puerta`, `E6_HB09_tipo_notificacion` y `E6_F5_token_cliente`; después, `E6_F6_demo` y `E6_sonar`.
> **Relación:** sigue a [SPEC-08](SPEC-08-conexion-de-pantallas.md) (lo que quedó pendiente de la conexión) y convive con [SPEC-06](SPEC-06-auth-mvp-keycloak.md), cuyas iteraciones 4 y 5 hace el grupo en otra rama.

---

## 1. Objetivo Funcional (Goal)

Una demo punta a punta: las pantallas de los cuatro roles del cliente liviano contra los servicios
reales, vía el gateway y con ingreso por Keycloak, sin datos de maqueta.

**Criterio de hecho:** cada frente cumple el suyo (sección 6), los PRs tienen auditoría de cierre
`PASS` y el recorrido de la demo corre en el preprod (Gate 4) o queda `[DEFERRED_NO_DOCKER]` con el
job `preprod-validation` del CI en verde.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- **S2083** (primero, PR propio): `web/Pantallas` deja de armar una ruta al disco con el nombre del
  pedido. Es el único issue de seguridad y el que tiene rojo el quality gate del PR 942.
- **F2, pantallas sin controller:** Entidades y necesidades y Mapa de entregadas (público), Misiones
  e insignias (donante). Puerto, adaptador de maqueta, adaptador REST y controller en `web/`. El
  HTML de la maqueta queda igual. Lo que el backend no da, la pantalla lo dice.
- **HB-22, la entrega que llegó a la puerta** (D1): estado nuevo `EN_PUERTA` en `Entrega` de
  Logística, con un ADR `proposed` previo. Donaciones lo replica; el cliente lo muestra en "Esperan
  tu confirmación" (inicio, recibidas y seguimiento de la entidad). Se relevan y ajustan todos los
  que leen `EstadoEntrega` o su payload: Donaciones, eventos AMQP, cliente y `openapi-logistica`.
- **HB-09, qué guarda la notificación** (D2): `tipoEvento` y `referenciaId` opcionales en
  `Notificacion` y `NotificacionDTO`, con una migración de columnas nulas que se llenan desde el
  evento que ya los trae. El cliente usa el tipo para la categoría y el id para el enlace; las
  viejas, sin tipo, van a "Otras novedades".
- **F5 recortado:** el token del usuario también hacia Incentivos, Logística y Notificaciones en
  `ClientesHttp`, con el mismo interceptor que ya va a Donaciones.
- **F6, demo** (W3, cuando la rama de autenticación del grupo esté en `ENTREGA_6`): el cliente en
  `docker-compose.yml` y en el preprod con emisor único, sus URLs apuntando al gateway, y el script
  y la guía del recorrido por los cuatro roles.
- **Sonar** (W3, sobre lo integrado): los issues abiertos de main, del PR 942 y de los PRs de W1,
  y los hotspots de main. Un issue que pide cambiar comportamiento se reporta y no se toca.
- **Índices compartidos** (`docs/ESTADO_DOCUMENTACION.md`, brechas §10, `docs/adr/DEUDA_TECNICA.md`
  y la tabla de iteraciones de SPEC-06): los actualiza solo el integrador (W2), con las notas que
  deja cada frente.

### Out-of-Scope

- SPEC-06 iteraciones 4 y 5 (JWT en el gateway y preprod con la seguridad encendida): las hace el
  grupo en otra rama. Ningún frente toca `api-gateway/**`, `docker-compose*.yml` (salvo F6, en W3),
  `integration-tests/**`, `common-lib/src/main/java/grupo5/common/seguridad/**` ni el realm.
- SPEC-06 iteraciones 7 a 10 y E2-07 (retiro de `X-API-Key` y `X-Actor`, n8n, proveedor de
  logística) (D3).
- Admin: pedir corrección o rechazar una entidad y la búsqueda en el servidor (no hay operación en
  el backend).
- B3 e identidad visual; pasar ADRs a `accepted`; mergear PRs; el PR 942 hacia `main`.
- Un cambio de contrato no aditivo fuera de `EN_PUERTA`: el frente para, lo reporta y sigue.

## 3. Decisiones Acordadas y Trade-offs

### 3.1 D1 — Cómo se sabe que la entrega llegó a la puerta (HB-22)

| Criterio (peso) | a. Marca `llegoEn` en `Entrega` | **b. Estado nuevo `EN_PUERTA`** | c. Lo deduce el cliente por la distancia |
|---|:-:|:-:|:-:|
| Contrato aditivo (0,30) | 5 | 2 | 5 |
| No toca la máquina de estados (0,25) | 5 | 1 | 5 |
| Dato confiable (0,25) | 4 | 5 | 2 |
| Tamaño (0,20) | 4 | 2 | 5 |
| **Total** | 4,55 | 2,50 | 4,25 |

**Elegida: b**, por decisión del usuario (2026-11-11) contra la recomendación de la matriz. Es el
dato más claro para la entidad: un estado de la máquina, no una marca al costado. Consecuencias:

- El frente escribe primero un ADR `proposed` sobre el estado nuevo (AGENTS.md §4.1 y §9.1).
- El cambio no es aditivo: se anota en `cambios-incompatibles-aceptados.txt` y el PR se marca como
  cambio de contrato con el ADR enlazado (AGENTS.md §8.2), como hizo HB-19 con `CANCELADA`.
- c queda descartada: depende de la posición simulada del camión.

### 3.2 D2 — Qué guarda la notificación (HB-09)

| Alternativa | Qué cambia | Riesgo |
|---|---|---|
| **a. `tipoEvento` y `referenciaId` opcionales** | Una migración con columnas nulas, llenas desde el evento | Bajo: aditivo |
| b. Solo el tipo | La mitad | El botón "Confirmar" de la entidad sigue sin destino |
| c. Fuera de E6 | Nada | La demo muestra avisos sin categoría ni enlace |

**Elegida: a.**

### 3.3 D3 — E2-07

**Fuera.** La demo no necesita retirar `X-API-Key` ni `X-Actor`.

### 3.4 Dónde va el barrido de Sonar (Trade-off)

| Criterio (peso) | S1 primero | S2 en paralelo, por archivos | **S3 al final, sobre lo integrado** |
|---|:-:|:-:|:-:|
| Choques con los frentes (0,30) | 3 | 2 | 5 |
| Cubre lo que agregan los frentes (0,25) | 1 | 2 | 4 |
| Tiempo total (0,20) | 2 | 5 | 3 |
| PRs independientes (0,15) | 2 | 4 | 2 |
| Simplicidad (0,10) | 4 | 2 | 5 |
| **Total** | 2,25 | 2,90 | **3,90** |

Excepción: S2083 va primero, en un PR chico, porque es lo único que tiene rojo el gate del PR 942.

### 3.5 Frentes y zonas

Cada frente trabaja en su worktree, sobre una lista cerrada de archivos (zona) sin intersección con
las otras. Lo que un frente necesita en la zona de otro lo deja por escrito para el integrador. Los
números reservados (W1 Preparar, leídos de `origin/ENTREGA_6`):

| Servicio | Migración | OpenAPI | Frente |
|---|---|---|---|
| Logística | V9 (perfil `postgres`; `mongo` no usa Flyway) | 1.9.0 | HB-22 |
| Donaciones | V20 | 1.8.0 | HB-22 |
| Notificaciones | V5 | 1.2.0 | HB-09 |

## 4. Invariantes

- En `maqueta`, el HTML de cada pantalla queda igual, salvo diferencias explicadas.
- Dos frentes nunca tocan el mismo archivo; los índices compartidos, solo el integrador.
- Contratos aditivos, salvo `EN_PUERTA`, que va con ADR `proposed` y registro del cambio.
- La entrega cancelada sigue siendo final; `EN_PUERTA` es un estado activo (cuenta para "una
  entrega activa por donación").
- Toda llamada del cliente lleva el `X-Trace-Id`; fuera de maqueta, también el token del usuario.
- Sin secretos ni datos reales; los ejemplos son sintéticos. Ningún test se debilita.

## 5. Criterios de Aceptación

```gherkin
Escenario: una dirección con saltos de carpeta
  Dado el cliente liviano
  Cuando se pide una pantalla con "../" o una variante en el nombre
  Entonces responde 404 y no lee el disco con ese nombre

Escenario: la entrega llega a la puerta
  Dado una entrega en traslado hacia una entidad
  Cuando el camión llega a su dirección
  Entonces la entrega pasa a EN_PUERTA, Donaciones lo replica
  Y la entidad la ve en "Esperan tu confirmación"

Escenario: una notificación con su enlace
  Dado un evento de entrega con su tipo y el id de la donación
  Cuando Notificaciones crea la notificación
  Entonces guarda el tipo y el id, y el cliente la muestra en su categoría con el enlace

Escenario: una pantalla del público sin controller hasta ahora
  Dado el sitio fuera de maqueta
  Cuando alguien abre Entidades y necesidades o el Mapa de entregadas
  Entonces ve los datos de la API, o la pantalla dice qué dato falta

Escenario: el token del usuario
  Dado una cuenta con sesión fuera de maqueta
  Cuando el cliente llama a Incentivos, Logística o Notificaciones
  Entonces la llamada lleva el token de esa cuenta
```

## 6. Validación (Validation)

Un criterio de hecho por frente:

| Frente | Hecho cuando |
|---|---|
| S2083 | `mvn clean test -pl cliente-liviano -am` en verde, con el test de "../" y variantes; el quality gate del PR 942 sin el issue de seguridad |
| F2 | Gate 2 de `cliente-liviano` y piso visual de las tres pantallas |
| HB-22 | Gate 2 de `logistica-service`, `donaciones-service` y `cliente-liviano`; ADR `proposed` escrito y el cambio anotado en `cambios-incompatibles-aceptados.txt` |
| HB-09 | Gate 2 de `notificaciones-service` y `cliente-liviano`; `contract-diff` aditivo |
| F5 | Gate 2 de `cliente-liviano` |
| Integrar (W2) | `mvn test` del reactor en verde e índices compartidos al día |
| F6 | Gate 4 en verde, o `[DEFERRED_NO_DOCKER]` con `preprod-validation` del CI en verde |
| Sonar | Cada issue arreglado o con su motivo; Gate 2 por módulo |

Para todos: `mvn spotless:check`, `node scripts/agent-check.js` con 0 FAIL y auditoría de cierre
`PASS`. Testcontainers sin Docker → `[DEFERRED_NO_DOCKER]` (AGENTS.md §11.3).

## 7. Especificación Técnica de Diseño (Technical Design)

- **Two-Gate (AGENTS.md §9.1):**
  - HB-22: Gate A verdadero (un estado nuevo en la máquina de `Entrega`) y Gate B verdadero (cambia
    un contrato entre servicios y un patrón central del dominio). Lleva **ADR `proposed`**, que
    escribe el frente HB-22 antes del código.
  - HB-09: Gate A falso; columnas opcionales dentro del modelo ya decidido. Sin ADR.
  - S2083, F2 y F5: implementan decisiones tomadas (SPEC-08, SPEC-06, ADR-E6-02). Sin ADR.
  - F6 y Sonar: se evalúan en W3.
- **Plan TDD por frente:**
  - S2083: el test de "../" y variantes primero (`PantallasTest`).
  - F2: test del adaptador REST con el JSON del contrato (`MockRestServiceServer`), adaptador de
    maqueta con los datos de la plantilla, test del controller (MockMvc, maqueta y `!maqueta`).
  - HB-22: test de la transición `EN_TRASLADO → EN_PUERTA` en el dominio, del consumidor de
    Donaciones y de los adaptadores del cliente que leen el estado; después, la migración.
  - HB-09: test del mapeo del evento a la notificación con tipo e id, del DTO y del adaptador del
    cliente (categoría y enlace, y "Otras novedades" sin tipo).
  - F5: test de que cada cliente HTTP lleva el token fuera de maqueta y no en maqueta.

## 8. Riesgos

- Docker inestable en local: los Gate 2 de servicios corren en serie; Gate 4 se intenta una vez.
- La rama de autenticación del grupo no llega a tiempo: F6 no arranca; si no llega, la demo corre
  sin JWT en el gateway y se declara.
- El PR 969 del grupo toca SPEC-06, SPEC-07 y los compose: nadie de la corrida los toca en W1.
- Choques en `cliente-liviano`: zonas sin intersección; el integrador resuelve y corre el reactor.
- Textos nuevos de pantalla: van a su ficha como "Pendiente de OK" y se aprueban juntos al final.
