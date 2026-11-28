# Métricas de negocio

Contadores de Micrometer que cuentan hechos del negocio (donaciones registradas, entregas fallidas, notificaciones enviadas) para armar tableros y alertas en Grafana. Complementan las métricas automáticas de Spring Boot (JVM, HTTP) y siguen la decisión del ADR `docs/adr/20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md`: Micrometer exporta por OTLP a `otel-lgtm`.

Las cuentan los casos de uso o la infraestructura, nunca las entidades. Donaciones y Logística avisan a un puerto propio (`IMetricasDonaciones`, `IMetricasLogistica`) y un adaptador lo traduce a Micrometer. En Notificaciones, un decorator y un listener usan `MeterRegistry` directo, porque ya son infraestructura.

## Métricas

| Id | Servicio | Nombre | Etiquetas y valores | Pregunta que responde | Dónde se cuenta |
|---|---|---|---|---|---|
| M01 | Donaciones | `donatrack.donaciones.registradas` | `perecedera`: `si`, `no` | ¿Cuánto se dona y cómo evoluciona? | `DonacionesService.cargarDonacion`, al final, con la donación ya guardada |
| M04 | Donaciones | `donatrack.donaciones.entregas.fallidas` | `replanificable`: `si`, `no` | ¿Qué proporción de entregas falla? | `ProcesadorEventosLogistica.procesarEntregaFallida`, una vez por evento aplicado |
| M13 | Donaciones | `donatrack.donaciones.eventos.proveedor.descartados` | `motivo`: `sin_proveedor`, `sin_token_configurado`, `token_incorrecto`, `donacion_ajena` | ¿Llegan mensajes de proveedor no válidos? Es una señal de seguridad | `VerificadorOrigenEventos.esOrigenValido` |
| M16 | Notificaciones | `donatrack.notificaciones.envios` | `canal`: `correo`, `estandar`, `whatsapp` · `resultado`: `enviado`, `fallido` | ¿Anda cada canal de envío? | `NotificacionSenderConMetricas` (decorator del `NotificacionRouter`) |
| M16 | Notificaciones | `donatrack.notificaciones` | `estado`: `pendiente`, `enviada`, `fallida` | ¿Llegan los avisos? | `NotificacionMetricasListener`, sobre `NotificacionCreada`, `NotificacionEnviada` y `NotificacionFallida` |
| M10r | Logística | `donatrack.logistica.planificaciones` | `resultado`: `procesada`, `fallida` | ¿Está fallando la planificación de rutas? | `PlanificacionService.procesarCallback` |
| M24 | Gateway | `donatrack.gateway.rate_limit.rechazos` | `ruta`: `donaciones`, `notificaciones`, `incentivos`, `logistica` · `cubeta`: `usuario`, `ip` · `operacion`: `lectura`, `escritura` | ¿Se agotan los cupos del rate limiting, de quién y dónde? | `LimiteDeSolicitudes`, en cada `429` |
| M25 | Gateway | `donatrack.gateway.redis.fallas` | `tipo`: `timeout`, `error` | ¿El rate limiting está en *fail-open* porque Redis falla? | `LimiteDeSolicitudes`, en cada pedido que pasa sin límite porque falló el almacén de cubetas |

Prometheus las expone con otro formato: `donatrack.donaciones.registradas` pasa a `donatrack_donaciones_registradas_total`.

## Qué se cuenta y qué no

- M01 cuenta una donación guardada. Es `perecedera` si algún bien tiene fecha de vencimiento.
- M04 no cuenta eventos duplicados, donaciones que ya estaban en `ENTREGA_FALLIDA` ni intentos deshechos. Un reintento por conflicto cuenta una sola vez.
- M13 cuenta un descarte por mensaje, con el primer motivo que lo rechaza. La tabla de `requerimientos-e7.md` la ubica en Logística, pero el chequeo vive en Donaciones.
- M16 `envios` cuenta cada intento, no cada notificación: una notificación prueba los canales en orden hasta que uno anda, así que puede sumar varios `fallido` y un `enviado`. `estandar` es SMS. Las respuestas a mensajes de contacto (`ContactoService`) también pasan por el decorator y cuentan como envíos.
- M16 `estado` cuenta transiciones. Los pendientes del momento salen de restar a las creadas las enviadas y las fallidas.
- M10r no cuenta un callback repetido de una solicitud ya procesada ni un payload inválido. Un `ERROR` repetido sobre una solicitud que ya estaba en error vuelve a contar `fallida`, y como se cuenta dentro de la transacción del callback, un guardado que se deshace puede sumar uno de más.

- M24 cuenta un rechazo por pedido con `429`. `cubeta` y `operacion` juntas dicen cuál de los cuatro cupos se agotó (el "nivel"); `ruta` es la ruta del gateway (`RutasGateway`), no el path, así que las series son a lo sumo 16.
- M25 cuenta pedidos, no caídas: mientras Redis está caído crece con el tráfico. Es `timeout` si en la cadena de causas hay un timeout (Bucket4j, Lettuce o la JDK); los pedidos que llegan mientras el gateway espera para reintentar la conexión cuentan como `error`.

## Cómo leerlas en Grafana

- Los contadores arrancan en cero con cada réplica y cada reinicio. Para ver la evolución se usan `increase` o `rate` sobre una ventana, no el valor crudo.
- Una etiqueta sin ejemplos todavía no aparece: la serie se crea la primera vez que se cuenta ese valor.
- La exportación a `otel-lgtm` se activa con `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=true` y `MANAGEMENT_OTLP_METRICS_EXPORT_URL`; sin eso los contadores se registran pero no salen del servicio.
