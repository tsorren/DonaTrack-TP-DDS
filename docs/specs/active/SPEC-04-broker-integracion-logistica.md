# Spec: SPEC-04 — Broker de Integración con Logística

> **Estado:** APPROVED_BY_USER (2026-10-07)  
> **Nivel:** ARCHITECTURAL  
> **Fecha:** 2026-10-07  
> **Rama:** `E4_donaciones_broker`  
> **Módulos Impactados:** `donaciones-service`, `logistica-service`, `docker-compose.yml`, `docs/`  
> **ADR:** [`20261007-broker-de-integracion-con-logistica.md`](../../adr/20261007-broker-de-integracion-con-logistica.md) (`proposed`)  
> **Plan detallado:** [`docs/entrega-4/donaciones/plan-broker-logistica.md`](../../entrega-4/donaciones/plan-broker-logistica.md)  
> **Bitácora:** [`docs/entrega-4/donaciones/bitacora-broker-logistica.md`](../../entrega-4/donaciones/bitacora-broker-logistica.md)

---

## 1. Objetivo Funcional (Goal)

Implementar el requerimiento «Broker de Integración con Logística» de la Entrega 4: `donaciones-service` tiene que poder **seleccionar entre más de un servicio de logística** (el propio y otro que cumpla el mismo objetivo) para cada donación asignada, sin perder pedidos en silencio y sin generar entregas duplicadas entre proveedores.

## 2. Alcance (Scope)

### In-Scope

* **donaciones-service:** broker in-process (`ILogisticaBroker`, `IProveedorLogistica`, `IEstrategiaSeleccionProveedor`), adapters AMQP y HTTP, outbox propio basado en datos (en memoria, detrás de un puerto), registro de solicitudes, procesador compartido de los eventos de vuelta (los proveedores informan por mensajería, no por HTTP), controller de administración del proveedor preferido (recortable), `ApiKeyFilter`, cableado en `PropuestaDeAsignacionService`.
* **Contrato:** comando `EntregaSolicitadaV1` (`entrega.solicitada.<proveedorId>.v1`) + JSON Schema.
* **logistica-service:** consumir el comando en lugar de `donacion.asignada.v1` (cola y binding por `LOGISTICA_INSTANCIA_ID`), deduplicación por donación en `POST /api/entregas` (409).
* **Demo:** segunda instancia de `logistica-service` en `docker-compose.yml`, usada como proveedor HTTP.
* **Docs:** ADR, catálogo de mensajes, matriz productor-consumidor, diagrama de componentes, `DEUDA_TECNICA.md`, índices.

### Out-of-Scope

* Persistencia JPA de `donaciones-service` y outbox transaccional real (`LogisticaOutboxJpa`).
* Failover por disponibilidad (health checks) hacia Logística.
* Seguridad de RabbitMQ por proveedor (usuarios, topic permissions, TLS): solo recomendación en el ADR.
* Autenticación real (Entrega 6).
* Integración con un proveedor de terceros real.
* Confirms/returns con reintento para los otros 9 eventos de `donaciones.exchange`.
* Cambios en la planificación de rutas de `logistica-service`.

## 3. Decisiones Acordadas

Resumidas; el detalle y la justificación están en el ADR y en el plan (§«Decisiones»).

| # | Decisión |
|---|---|
| D1 | Broker in-process en Donaciones (Broker + Adapter + Strategy). |
| D2 | Logística consume el comando `entrega.solicitada.<id>.v1`, no el hecho `donacion.asignada.v1`. Revisión del 15/9 acordada por el grupo. |
| D3 | Outbox en memoria, detrás de un puerto, con entradas basadas en datos. |
| D4 | Dos transportes: AMQP y HTTP. |
| D5 | Estrategia base: preferencia configurable + reenvío. Las estrategias adicionales se acuerdan con el equipo. |
| D6 | API key en los endpoints expuestos. |
| D7 | Comando dirigido por routing key, no por un exchange por proveedor. Regla: eventos por hecho, comandos por destinatario. |
| D8 | `mandatory=true`: si RabbitMQ devuelve el mensaje, se reenvía a otro proveedor. |
| D9 | Segunda instancia de Logística para la demo. |
| D10 | Rechazado → siguiente proveedor; incierto → mismo proveedor. |
| D11 | El relay espera el acuse (`CorrelationData`); sin `ReturnsCallback` que dispare reenvíos. |
| D12 | `POST /api/entregas` deduplica por donación (409). |

## 4. Invariantes

1. **`[INVARIANT]` Selección efectiva:** para cada donación asignada, la entrega se crea en **un solo** proveedor.
2. **`[INVARIANT]` Sin pérdidas silenciosas:** toda solicitud termina `ENVIADA` o `FALLIDA` con log `error`. Nunca desaparece.
3. **`[INVARIANT]` Reenvío solo ante rechazo seguro:** un envío incierto nunca se reenvía a otro proveedor.
4. **`[INVARIANT]` Hecho intacto:** `donacion.asignada.v1` no cambia su contrato ni deja de publicarse.
5. **`[INVARIANT]` Bindings exactos:** ningún consumidor del comando usa comodines.
6. **`[INVARIANT]` Sin secretos en el repo:** las API keys se leen de variables de entorno; falla cerrado si no están configuradas.

## 5. Plan de Verificación

| Etapa | Gate |
|---|---|
| Baseline | `mvn clean test -pl donaciones-service,logistica-service -am` en verde (registrado en la bitácora). |
| Por fase | Tests de la fase según el plan (§3 «Fases de implementación»), con `mvn test -pl <modulo> -Dtest=...`. |
| Integración AMQP | Testcontainers: mensaje devuelto → rechazado; aislamiento de colas por instancia. |
| Contratos | `node scripts/validate-contracts.js`. |
| Cierre | Gate 3/4, `mvn spotless:check`, pre-flight SonarCloud, checklist AGENTS.md §12, `ENHANCED_REVIEW_REQUIRED`. |
