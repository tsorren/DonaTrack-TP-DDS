# Anexo — Detalle por servicio (Entrega 4)

Propósito: lo propio de cada servicio, como justificación (entregable 3) del documento de arquitectura.

Vuelta al documento: [`arquitectura-sistema.md`](arquitectura-sistema.md). Los IDs T1–T19 remiten a su §9.

---

## Anexo A — Donaciones

| Campo | Contenido |
|---|---|
| Responsabilidad | Personas, donantes, entidades beneficiarias, donaciones, necesidades, asignación. Broker de integración con logística |
| Agregados | Persona · Donante y Entidad beneficiaria (comparten la identidad de la persona, #892) · Donación · Donación independiente · Necesidad · Propuesta de asignación |
| Patrones propios | State (7 estados) · Template Method + Strategy en la asignación · Strategy en normalización, segmentación y selección de proveedor · Factory de personas · broker de integración |
| Entradas | REST: `/api/donaciones`, `/api/donantes`, `/api/entidades`, `/api/necesidades`, `/api/personas`, `/api/asignaciones`, `/donaciones-independientes`, administración `/api/logistica` · AMQP: 4 colas de vuelta |
| Salidas | AMQP: 9 hechos en `donaciones.exchange` y el comando `entrega.solicitada.<proveedorId>.v1` · HTTP `POST /api/entregas` al proveedor `externo` |
| Procesos programados | Algoritmos de asignación y necesidades recurrentes (medianoche) · vencimiento de donaciones (04:00) · relay del outbox (cada 10 s) |
| Datos | Todo en memoria: repositorios, outbox, solicitudes, dedup y preferido |
| Deuda propia | T1, T2, T11 · dominio que usa un DTO y aplicación que importa infraestructura (DTI-02, DTI-16) · 3 propiedades de URL de otros servicios sin uso |

## Anexo B — Logística

| Campo | Contenido |
|---|---|
| Responsabilidad | Camiones, choferes, entregas, rutas y planificación. Proveedor `donatrack` del broker de integración |
| Agregados | Camión · Chofer · Entrega · Ruta · Solicitud de planificación |
| Patrones propios | State (enum con transiciones) en entrega, ruta y camión · Strategy en orden de paradas, asignación por dimensión y lotes · gestores de dominio puros · Data Mapper · consumidor idempotente por donación |
| Entradas | REST: `/api/camiones`, `/api/choferes`, `/api/entregas`, `/api/rutas`, `/api/logistica` (callback y consulta) · AMQP: `logistica.<instancia>.entregas.solicitadas`, ligada a `donaciones.exchange` |
| Salidas | AMQP: `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa`, `entrega.fallida` en `logistica.exchange`, firmados con `logistica.instancia-id` y su token · HTTP solo hacia su propio callback, desde su planificador simulado interno |
| Datos | PostgreSQL (perfil `postgres`): 15 entidades JPA, Flyway V1–V2, `validate`, bloqueo optimista en 5 tablas, 4 tablas de historial, UUID sin clave foránea hacia Donaciones |
| Despliegue | Único servicio con despliegue en la nube (§7 del documento) |
| Deuda propia | T3, T19 · consumo del comando sin DLQ · `evento_entrega` sin relay |

## Anexo C — Incentivos

| Campo | Contenido |
|---|---|
| Responsabilidad | Misiones, categorías, insignias, ranking mensual e inactividad |
| Agregados | Perfil de gamificación del donante · Ranking mensual |
| Patrones propios | Template Method en misiones · Factory de misiones estándar · Strategy de inactividad · gestores puros de ranking e inactividad · insignia como plantilla y como logro |
| Entradas | AMQP: 5 colas desde `donaciones.exchange` · REST `/api/incentivos/**` · 3 procesos programados: inactividad (diario), rachas (mensual), ranking (fin de mes) |
| Salidas | AMQP: `incentivo.mision-cumplida.v1`, `incentivo.subio-categoria.v1`, `incentivo.donante-inactivo.v1` en `incentivos.exchange` · n8n: 2 webhooks (insignia ganada, ranking), sin esperar respuesta; un fallo solo deja un aviso · MinIO |
| Datos | PostgreSQL (perfil `postgres`, #889): 7 entidades, Flyway V1, SINGLE_TABLE en misiones · MinIO, bucket `insignias`; si MinIO no responde, el servicio arranca igual |
| Interino | Feign hacia Notificaciones solo con `incentivos.rabbitmq.enabled=false`; por defecto `true` (DTI-13) |
| Deuda propia | T4, T5, T6, T7, T9 · n8n difunde contra un servicio de eco HTTP externo, sin reintento |

## Anexo D — Notificaciones

| Campo | Contenido |
|---|---|
| Responsabilidad | Avisos a personas por su medio preferido, con fallback entre medios |
| Agregados | Notificación, con historial de estados · Persona (réplica de contacto) |
| Patrones propios | Adapter + Router por medio de notificación · Strategy de fallo simulado · Template Method en los simuladores · Transactional Inbox · Dead Letter Channel |
| Entradas | AMQP: `notificaciones.donaciones` (`donacion.#`, `donante.#`, `persona.#`) y `notificaciones.incentivos` (`incentivo.#`) · REST `/api/notificaciones` (más alias legado) |
| Salidas | Medios de notificación (correo, SMS y WhatsApp) simulados en el proceso, sin red |
| Datos | PostgreSQL (perfil `postgres`): 5 entidades, Flyway V1–V2, SINGLE_TABLE en medios de contacto, inbox `evento_procesado` |
| Garantía | Efecto único de la persistencia de la notificación, en perfil `postgres`. El envío ocurre después del commit, de forma asíncrona. Una notificación fallida no se reintenta |
| Deuda propia | T9, T17, T18 · fallback de dedup por id de negocio cuando falta `messageId` (T4) |
