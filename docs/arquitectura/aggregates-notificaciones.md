# Modelo de Agregados — Servicio de Notificaciones (DDD)

Este documento detalla el diseño táctico de **Domain-Driven Design (DDD)** para el **Servicio de Notificaciones** en DonaTrack, especificando sus límites de agregados, raíces, y las políticas de procesamiento de eventos.

---

## 1. Principios de Diseño y Objetivos del Servicio

El *Servicio de Notificaciones* es un servicio mayoritariamente de soporte, encargado de reaccionar a eventos de integración del sistema y despachar mensajes (por email, SMS o WhatsApp). Sus principios clave de diseño son:

1.  **Aislamiento, Consistencia y Desacoplamiento**: La base de datos del servicio es independiente (PostgreSQL con esquema `notificaciones`). Ingesta eventos asíncronamente vía AMQP (`notificaciones.exchange`) como canal primario y mantiene endpoints REST para retrocompatibilidad y consultas. No realiza llamadas REST síncronas a otros servicios para obtener datos de las personas; en su lugar, mantiene una réplica local persistida del perfil de los usuarios.
2.  **Idempotencia con Transactional Inbox**: Cada evento recibido cuenta con un `eventId` unívoco persistido de forma atómica en `notificaciones.evento_procesado`. Si un evento ya fue procesado, se descarta silenciosamente para prevenir despachos duplicados en reintentos o reconexiones.
3.  **Transaccionalidad en Envío de Alertas**: Cada alerta despachada y su resultado final se registran en la base de datos de forma inmutable para auditoría (`notificaciones.notificacion` y `notificaciones.auditoria_notificacion`).
4.  **Encapsulación**: Las clases internas del agregado (como los medios de contacto concretos) se definen con visibilidad restringida de paquete para proteger sus límites.

---

## 2. Catálogo Detallado de Agregados

### 2.1. Agregado: Persona (Réplica Local de Lectura)
*   **Aggregate Root**: `Persona`.
*   **Componentes Internos**: 
    *   `MedioDeContacto` (Clase base abstracta de tipo Objeto de Valor/Entidad interna que implementa `Anonimizable`).
    *   `Correo`, `Telefono` (Especializaciones del medio de contacto; los canales físicos de salida como Correo, SMS/Teléfono y WhatsApp se despachan mediante adaptadores de infraestructura).
    *   `TipoPersona`, `TipoTelefono` (Enums).
*   **Responsabilidad**: Almacenar la denominación y el ruteo de medios de contacto del usuario destinatario. Este agregado se sincroniza vía endpoint HTTP REST canónico (`PUT /api/notificaciones/personas`) y alias legacy (`PUT /notificaciones/personas`) invocado mediante Feign cuando se crea o actualiza una persona en `donaciones-service`.
*   **Reconciliación Diferencial (JPA / Flyway V1)**: Para evitar violaciones de clave foránea y *Key Churn* en `notificaciones.medio_contacto`, la actualización de medios de contacto se realiza de forma diferencial: actualiza los existentes en sitio, añade los nuevos y remueve exclusivamente los ausentes, preservando la identidad de los registros existentes.
*   **Paquete**: `grupo5.notificaciones.models.entities.personas`

### 2.2. Agregado: Notificación
*   **Aggregate Root**: `Notificacion` (Representa el intento y estado final de una notificación despachada a un usuario).
*   **Componentes Internos**: `EstadoNotificacion` (Enum con valores *PENDIENTE*, *ENVIADA*, *FALLIDA*) y `CambioEstadoNotificacion` (auditoría inmutable).
*   **Eventos de Dominio**: `NotificacionCreada`, `NotificacionEnviada`, `NotificacionFallida` (en `grupo5.notificaciones.models.entities.notificaciones.events`).
*   **Referencias Externas (por ID)**: `personaId` (UUID que apunta al agregado `Persona`).
*   **Responsabilidad**: Registrar el mensaje, el destinatario, la fecha y el estado de la comunicación. Provee la lógica para ordenar los canales por prioridad (respetando la predeterminación) y ejecutar reintentos de envío (*fallback*) ante fallos de proveedores de mensajería (Double Dispatch con `NotificacionRouter` y adaptadores multicanal).
*   **Paquete**: `grupo5.notificaciones.models.entities.notificaciones`

---

## 3. Políticas y Procesamiento de Eventos (`EventoNotificable`)

La jerarquía de clases bajo el paquete `grupo5.notificaciones.models.entities.notificaciones.eventos` (tales como `EventoDeDonacion`, `DonanteRegistrado`, `DonanteInactivo`, `DonacionAsignada`, `DonacionRecibida`, `MisionCumplida`, `SubioCategoria`, `DonacionEnCamino`, `EntregaFallida`, `DonacionVencida`) representa **Políticas de Dominio (Domain Policies)** y no agregados persistentes:

*   **Rol en el Diseño**: Actúan como factorías polimórficas de alertas (`generarNotificaciones()`).
*   **Canales de Ingesta**:
    1.  **AMQP (Primario)**: Cola `cola.eventos.notificaciones` vinculada a `notificaciones.exchange` (`notificaciones.#`). Consumida por `NotificacionRabbitListener` de forma asíncrona y resiliente.
    2.  **HTTP REST (Dual / Retrocompatibilidad)**: Endpoints `POST /api/notificaciones/eventos` (canónico) y `POST /notificaciones` (deprecated). Responde `202 Accepted` de forma inmediata.
*   **Funcionamiento y Flujo**:
    1.  El listener AMQP o el controlador REST recibe el payload y delega en `NotificacionService.procesarEvento(dto)`.
    2.  **Verificación de Idempotencia (Transactional Inbox)**: Se consulta si `eventId` existe en `notificaciones.evento_procesado`. Si ya existe, se aborta la ejecución de forma idempotente sin duplicar despachos. Si no existe, se registra dentro de la transacción.
    3.  Se recupera la `Persona` destinataria (o el Administrador por ID fijo si aplica) desde el repositorio local (`PersonaRepository`).
    4.  Se instancia el `EventoNotificable` correspondiente.
    5.  El evento genera las instancias de `Notificacion` necesarias (pudiendo generar 1 a N notificaciones, ej: al donante y al administrador).
    6.  Se ejecuta el envío multicanal a través del `NotificacionRouter` y se persiste cada `Notificacion` generada para auditoría en `notificaciones.notificacion`.
*   **Conclusión**: No tienen identificador único persistente en el servicio de notificaciones y por ende **no son Aggregate Roots**. Son clases de lógica transitoria que orquestan el comportamiento reactivo del sistema. La migración pendiente de los clientes que aún usan REST hacia AMQP se encuentra catalogada en la deuda técnica [DTI-13](../adr/DEUDA_TECNICA.md#dti-13-migracion-de-clientes-consumidores-de-la-api-rest-deprecada-de-notificaciones-a-ruta-canonica-y-amqp).

