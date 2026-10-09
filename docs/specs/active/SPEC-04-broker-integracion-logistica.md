# Spec: SPEC-04 — Broker de Integración con Logística

> **Estado:** APPROVED_BY_USER (2026-10-07)  
> **Nivel:** ARCHITECTURAL  
> **Fecha:** 2026-10-07  
> **Última modificación:** 2026-10-09: se completaron §2, §3, §5 y §7 según la plantilla de spec y se corrigió la deriva (D12/D33, compose de la demo). El estado no se promovió y §7 no pasó por design review. Después, el mismo día: D55 (Logística deduplica desde #886).  
> **Rama:** `E4_donaciones_broker`  
> **Módulos Impactados:** `donaciones-service`, `logistica-service`, `docker-compose.yml`, `docker-compose.demo.yml`, `postman/`, `docs/`  
> **ADR:** [`20261007-broker-de-integracion-con-logistica.md`](../../adr/20261007-broker-de-integracion-con-logistica.md) y [`20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md`](../../adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md) (ambos `proposed`)  
> **Plan detallado:** [`docs/entrega-4/donaciones/plan-broker-logistica.md`](../../entrega-4/donaciones/plan-broker-logistica.md)  
> **Bitácora:** [`docs/entrega-4/donaciones/bitacora-broker-logistica.md`](../../entrega-4/donaciones/bitacora-broker-logistica.md)

---

## 1. Objetivo Funcional (Goal)

Implementar el requerimiento «Broker de Integración con Logística» de la Entrega 4: `donaciones-service` tiene que poder **seleccionar entre más de un servicio de logística** (el propio y otro que cumpla el mismo objetivo) para cada donación asignada, sin perder pedidos en silencio y sin generar entregas duplicadas entre proveedores.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

* **donaciones-service:** broker in-process (`ILogisticaBroker`, `IProveedorLogistica`, `IEstrategiaSeleccionProveedor`), adapters AMQP y HTTP, outbox propio basado en datos (en memoria, detrás de un puerto), registro de solicitudes, procesador compartido de los eventos de vuelta (los proveedores informan por mensajería, no por HTTP: D50), verificación del origen de esos eventos (`VerificadorOrigenEventos`, token por proveedor en headers: D51), controller de administración del proveedor preferido (recortable), `ApiKeyFilter`, cableado en `PropuestaDeAsignacionService`.
* **Contrato:** comando `EntregaSolicitadaV1` (`entrega.solicitada.<proveedorId>.v1`) + JSON Schema.
* **logistica-service:** consumir el comando en lugar de `donacion.asignada.v1` (cola y binding por `LOGISTICA_INSTANCIA_ID`); firmar sus eventos de vuelta con `X-Proveedor-Id`/`X-Proveedor-Token` (D51) y publicarlos con el alias de la routing key en `__TypeId__` (D52). La deduplicación por donación en `POST /api/entregas` se evaluó y se revirtió desde el broker (D33); después la agregó el equipo de logística en #886 (409, `ERR-EST-816`, D55).
* **Demo:** segunda instancia de `logistica-service` (`logistica-externo`, `LOGISTICA_INSTANCIA_ID=externo`) en `docker-compose.demo.yml`, usada como proveedor HTTP, y la colección `postman/flujo-9-broker-logistica.json` con su guion.
* **Docs:** ADRs, catálogo de mensajes, matriz productor-consumidor, diagrama de componentes, `DEUDA_TECNICA.md`, índices.

### Out-of-Scope

* Persistencia JPA de `donaciones-service` y outbox transaccional real (`LogisticaOutboxJpa`).
* Failover por disponibilidad (health checks) hacia Logística.
* Seguridad de RabbitMQ por proveedor (usuarios, topic permissions, TLS): solo recomendación en el ADR.
* Autenticación real (Entrega 6).
* Integración con un proveedor de terceros real.
* Confirms/returns con reintento para los otros 9 eventos de `donaciones.exchange`.
* Cambios en la planificación de rutas de `logistica-service`.

## 3. Decisiones Acordadas y Trade-offs

### 3.1. Matriz de Trade-offs en 5 Dimensiones (5D)

Resume las alternativas principales de «Alternativas Consideradas» del ADR [20261007](../../adr/20261007-broker-de-integracion-con-logistica.md). La tabla completa está en el ADR, y la del camino de vuelta en el ADR [20261008](../../adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md).

`[INFERRED]` Las celdas se derivan de los drivers, los veredictos y las consecuencias del ADR 20261007. Lo que el ADR dice literalmente va con su sección entre paréntesis.

| Dimensión | Opción 1: Broker in-process en Donaciones (Elegida) | Opción 2: Microservicio broker dedicado (Rechazada) | Opción 3: Campo `proveedorLogistica` en `donacion.asignada.v1` + filtro en cada consumidor (Rechazada) |
|---|---|---|---|
| **1. Complejidad / YAGNI** | **Media:** Broker + Adapter + Strategy y un outbox interino detrás de un puerto. Una capa más de indirección (Consecuencias Negativas). | **Alta:** Suma un contenedor (Alternativas). | **Baja en Donaciones, alta en los proveedores:** un campo más en el hecho, pero cada proveedor tiene que filtrar (Alternativas). |
| **2. Cátedra / ADRs** | **Conforme:** Selección explícita entre ≥ 2 proveedores. La vuelta es por mensajería (requerimiento 3, ADR 20261008). | **Conforme:** Cumple la selección, «sin beneficio funcional» (Alternativas). | **Riesgo:** Mete datos de ruteo en un hecho de dominio, contra el driver «Higiene de contratos» del ADR 20261007. |
| **3. Acoplamiento** | **Bajo:** El hecho queda intacto. El comando va por routing key a un solo destinatario. | **Más alto:** Donaciones pasa a depender de un servicio más en la red. | **Alto:** Notificaciones recibe datos de ruteo (Alternativas). |
| **4. Performance / Flujo** | **Latencia acotada:** No hay saltos de red nuevos, pero el despacho pasa por el relay (cada 10 s, `donatrack.logistica.outbox.intervalo-ms`), que espera el acuse de cada entrada. No hay failover por disponibilidad en AMQP (Consecuencias Negativas). | **Peor:** Agrega un salto de red y un punto único de falla (Alternativas). | **Peor:** Todos los proveedores reciben todas las asignaciones y descartan las ajenas. |
| **5. Reversibilidad** | **Alta en código, con un cutover coordinado:** Proveedores, estrategia y outbox están detrás de puertos. Cambiar el canal de Logística exige coordinar dos servicios (Consecuencias Negativas). | **Media:** Revertirla implica retirar un servicio. | **Baja:** Cambia el contrato de un hecho que ya consumen otros servicios. |

* **Alternativa elegida:** opción 1.
* **Trade-off aceptado:** el outbox es en memoria hasta que exista persistencia JPA en Donaciones. Un reinicio pierde lo pendiente (DTI-14).

### 3.2. Decisiones

Resumidas; el detalle y la justificación están en los ADRs y en la bitácora (§«Decisiones»).

| # | Decisión |
|---|---|
| D1 | Broker in-process en Donaciones (Broker + Adapter + Strategy). |
| D2 | Logística consume el comando `entrega.solicitada.<id>.v1`, no el hecho `donacion.asignada.v1`. Revisión del 15/9 acordada por el grupo. |
| D3 | Outbox en memoria, detrás de un puerto, con entradas basadas en datos. |
| D4 | Dos transportes: AMQP y HTTP. |
| D5 | Estrategia base: preferencia configurable + reenvío. Las estrategias adicionales se acuerdan con el equipo. |
| D6 | API key en los endpoints expuestos. Desde D50 solo quedan los de administración. |
| D7 | Comando dirigido por routing key, no por un exchange por proveedor. Regla: eventos por hecho, comandos por destinatario. |
| D8 | `mandatory=true`: si RabbitMQ devuelve el mensaje, se reenvía a otro proveedor. |
| D9 | Segunda instancia de Logística para la demo. |
| D10 | Rechazado → siguiente proveedor; incierto → mismo proveedor. |
| D11 | El relay espera el acuse (`CorrelationData`); sin `ReturnsCallback` que dispare reenvíos. |
| D12 | ~~`POST /api/entregas` deduplica por donación (409).~~ Revertida por D33: nuestra Logística se alcanza solo por AMQP, donde el listener ya deduplica, y la idempotencia de un proveedor HTTP es requisito de su contrato. Desde #886 el 409 existe, agregado por el equipo de logística (D55). |
| D50 | Los proveedores informan solo por mensajería (`logistica.exchange`). HTTP queda solo de ida. Se quitó el callback HTTP. |
| D51 | Identidad del proveedor en los eventos de vuelta: `X-Proveedor-Id` y `X-Proveedor-Token`, verificados en Donaciones con falla cerrado. |
| D52 | Logística publica sus eventos de vuelta con el alias de la routing key en `__TypeId__`, no con el nombre de su clase. |

## 4. Invariantes

1. **`[INVARIANT]` Selección efectiva:** para cada donación asignada, la entrega se crea en **un solo** proveedor.
2. **`[INVARIANT]` Sin pérdidas silenciosas:** toda solicitud termina `ENVIADA` o `FALLIDA` con log `error`. Nunca desaparece.
3. **`[INVARIANT]` Reenvío solo ante rechazo seguro:** un envío incierto nunca se reenvía a otro proveedor.
4. **`[INVARIANT]` Hecho intacto:** `donacion.asignada.v1` no cambia su contrato ni deja de publicarse.
5. **`[INVARIANT]` Bindings exactos:** ningún consumidor del comando usa comodines.
6. **`[INVARIANT]` Sin secretos en el repo:** las API keys se leen de variables de entorno; falla cerrado si no están configuradas.

## 5. Criterios de Aceptación (Gherkin / Given-When-Then)

Salen de la sección «Validación» de los dos ADRs. Los tests que fijan cada escenario están en §7.2, que también lista lo que hoy no tiene test automatizado.

```gherkin
Escenario: Envío al proveedor preferido
  Dado que hay dos proveedores configurados, "donatrack" (AMQP) y "externo" (HTTP), con "donatrack" como preferido
  Cuando se aprueba la asignación de una donación
  Entonces se registra una solicitud de entrega y una entrada en el outbox
  Y el relay publica el comando "entrega.solicitada.donatrack.v1" con mandatory
  Y "donacion.asignada.v1" se sigue publicando sin cambios en su contrato.

Escenario: Rechazo seguro, se reenvía al siguiente proveedor
  Dado que el comando para el proveedor preferido no tiene binding y RabbitMQ lo devuelve
  Cuando el relay recibe el acuse con el mensaje devuelto
  Entonces el envío se clasifica como rechazado
  Y la solicitud se envía al siguiente proveedor según la estrategia.

Escenario: Envío incierto, se reintenta con el mismo proveedor
  Dado que el envío termina sin saber si el pedido llegó (nack o sin acuse a tiempo en AMQP; timeout de lectura o 500/502/504 en HTTP)
  Cuando el broker registra el resultado
  Entonces la entrada se reprograma para el mismo proveedor
  Y nunca se envía a otro proveedor
  Y si se agotan los intentos, la solicitud queda FALLIDA.

Escenario: Error de contrato, sin reenvío
  Dado que el proveedor rechaza el pedido por un error de contrato
  Cuando el broker registra el resultado
  Entonces la solicitud queda FALLIDA sin reintentar ni probar otro proveedor.

Escenario: Sin pérdidas silenciosas
  Dado que todos los proveedores rechazan la solicitud en todas las rondas permitidas
  Cuando se agotan las rondas
  Entonces la solicitud queda FALLIDA y se registra un log de nivel error.

Escenario: Vuelta por mensajería con identidad
  Dado un evento "ruta.asignada" publicado en "logistica.exchange" con "X-Proveedor-Id" y "X-Proveedor-Token" válidos, sobre una donación asignada a ese proveedor
  Cuando LogisticaEventListener lo recibe
  Entonces el evento se aplica una sola vez y la donación avanza de estado
  Pero si falta el token, el token no coincide o la donación es de otro proveedor, el evento se descarta con un warn y la donación no cambia.

Escenario: Administración protegida por API key
  Dado que "donatrack.logistica.admin-api-key" está configurada
  Cuando llega "PUT /api/logistica/proveedor-preferido" sin la clave, incluso con parámetros de ruta como "/api/logistica;x=1/proveedor-preferido"
  Entonces la respuesta es 401 "ERR-AUT-401" y el preferido no cambia
  Y con la clave correcta la respuesta es 200 con la lista actualizada.
```

## 6. Plan de Verificación

| Etapa | Gate |
|---|---|
| Baseline | `mvn clean test -pl donaciones-service,logistica-service -am` en verde (registrado en la bitácora). |
| Por fase | Tests de la fase según el plan (§3 «Fases de implementación»), con `mvn test -pl <modulo> -Dtest=...`. |
| Integración AMQP | Testcontainers: mensaje devuelto → rechazado; aislamiento de colas por instancia. |
| Contratos | `node scripts/validate-contracts.js`. |
| Cierre | Gate 3/4, `mvn spotless:check`, pre-flight SonarCloud, checklist AGENTS.md §12, `ENHANCED_REVIEW_REQUIRED`. |

## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1. Evaluación Two-Gate Rule para ADRs

1. **Gate A — Decisión nueva: SÍ.** Hay tres decisiones que no existían:
   * el broker in-process con selección de proveedor;
   * un comando dirigido por routing key, que revisa la decisión del 15/9;
   * la vuelta de los proveedores solo por mensajería, con identidad en headers.
2. **Gate B — Significancia arquitectónica: SÍ.** Agrega un contrato AMQP nuevo (`entrega.solicitada.<id>.v1`) y cambia el canal por el que Logística recibe los pedidos. Tiene impacto cross-service (`donaciones-service` y `logistica-service`) y una decisión de seguridad sobre los eventos de vuelta.
* **Resultado:** se requiere ADR. Están formalizados [20261007](../../adr/20261007-broker-de-integracion-con-logistica.md) (broker) y [20261008](../../adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md) (camino de vuelta), los dos en `proposed`. El diseño de componentes está en el [plan](../../entrega-4/donaciones/plan-broker-logistica.md).

### 7.2. Plan TDD y tests que fijan cada criterio

| Criterio (§5) | Tests |
|---|---|
| Envío al preferido y hecho intacto | `LogisticaBrokerTest`, `LogisticaBrokerFlujoTest`, `PropuestaDeAsignacionServiceTest`, `SeleccionPorPreferenciaConFallbackTest`, `EntradaOutboxLogisticaTest` |
| Rechazo seguro, incierto, error de contrato y rondas agotadas | `LogisticaBrokerTest`, `LogisticaOutboxRelayTest`, `ProveedorLogisticaAmqpTest`, `ProveedorLogisticaHttpTest`, `ProveedorLogisticaHttpRedRealTest`, `SolicitudEntregaTest` |
| Mensaje devuelto real y aislamiento de colas (Testcontainers) | `ProveedorLogisticaAmqpRabbitTest` (donaciones), `MensajeriaLogisticaRabbitTest` (logística) |
| Consumo del comando en Logística | `EntregaSolicitadaEventListenerTest`, `RabbitMQConfigTest` (binding exacto y cola por instancia) |
| Vuelta con identidad | `VerificadorOrigenEventosTest`, `ClaveSeguraTest`, `LogisticaEventListenerTest`, `ProcesadorEventosLogisticaTest`, `SolicitudesEntregaRepositoryTest` (solicitud más reciente por donación), `LogisticaEventPublisherTest`, `RabbitMQConfigTest` (alias de `__TypeId__`, D52) |
| Administración protegida | `ApiKeyFilterTest`, `LogisticaProveedorControllerTest`, `AdministracionProveedoresServiceTest`, `LogisticaProveedoresConfigTest` |
| Contrato del comando | `node scripts/validate-contracts.js` sobre `evento-entrega-solicitada-v1.schema.json` |

**Sin test automatizado (verificación manual o pendiente):**
* Ningún test captura logs. El `warn` del descarte de eventos se verifica en el log de la demo (guion, carpeta F). El log `error` de «Sin pérdidas silenciosas» queda pendiente: ningún escenario de la demo agota las rondas.
* La entrada por `logistica.exchange` de punta a punta, con eventos reales de nuestra Logística: solo la demo con Postman (carpetas F y H, ADR 20261008, «Validación»). `entrega.fallida` de punta a punta sigue `[A VERIFICAR]`.
