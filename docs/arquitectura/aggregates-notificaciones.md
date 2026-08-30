# Modelo de Agregados — Servicio de Notificaciones (DDD)

Este documento detalla el diseño táctico de **Domain-Driven Design (DDD)** para el **Servicio de Notificaciones** en DonaTrack, especificando sus límites de agregados, raíces, y las políticas de procesamiento de eventos.

---

## 1. Principios de Diseño y Objetivos del Servicio

El *Servicio de Notificaciones* es un servicio mayoritariamente de soporte, encargado de reaccionar a eventos de integración del sistema y despachar mensajes (por email, SMS o WhatsApp). Sus principios clave de diseño son:

1.  **Aislamiento y Consistencia**: La base de datos del servicio es independiente. No realiza llamadas REST síncronas a otros servicios para obtener datos de las personas; en su lugar, se suscribe a los eventos y mantiene una proyección o réplica local ligera del perfil de los usuarios.
2.  **Transaccionalidad en Envío de Alertas**: Cada alerta despachada y su resultado final se registran de forma inmutable para auditoría.
3.  **Encapsulación**: Las clases internas del agregado (como los medios de contacto concretos) se definen con visibilidad restringida de paquete para proteger sus límites.

---

## 2. Catálogo Detallado de Agregados

### 2.1. Agregado: Persona (Réplica Local de Lectura)
*   **Aggregate Root**: `Persona`.
*   **Componentes Internos**: 
    *   `MedioDeContacto` (Clase base abstracta de tipo Objeto de Valor/Entidad interna).
    *   `Correo`, `Telefono`, `WhatsApp` (Especializaciones del medio de contacto).
*   **Responsabilidad**: Almacenar la denominación y el ruteo de medios de contacto del usuario destinatario. Este agregado se sincroniza síncronamente vía endpoint HTTP REST (`PUT /api/notificaciones/personas`) invocado mediante Feign cuando se crea o actualiza una persona en `donaciones-service`.
*   **Paquete**: `grupo5.notificaciones.models.entities.personas` (y subpaquete `medioDeContacto`).

### 2.2. Agregado: Notificación
*   **Aggregate Root**: `Notificacion` (Representa el intento y estado final de una notificación despachada a un usuario).
*   **Componentes Internos**: `EstadoNotificacion` (Enum con valores *PENDIENTE*, *ENVIADA*, *FALLIDA*).
*   **Referencias Externas (por ID)**: `personaId` (UUID que apunta al agregado `Persona`).
*   **Responsabilidad**: Registrar el mensaje, el destinatario, la fecha y el estado de la comunicación. Provee la lógica para ordenar los canales por prioridad (respetando la predeterminación) y ejecutar reintentos de envío (*fallback*) ante fallos de proveedores de mensajería (Double Dispatch con `NotificacionRouter` y adaptadores multicanal).
*   **Paquete**: `grupo5.notificaciones.models.entities.notificaciones`

---

## 3. Políticas y Procesamiento de Eventos (`EventoNotificable`)

La jerarquía de clases bajo el paquete `grupo5.notificaciones.models.entities.notificaciones.eventos` (tales como `EventoDeDonacion`, `DonanteRegistrado`, `MisionCumplida`, `SubioCategoria`, `DonacionEnCamino`, `EntregaFallida`) representa **Políticas de Dominio (Domain Policies)** y no agregados persistentes:

*   **Rol en el Diseño**: Actúan como factorías polimórficas de alertas (`generarNotificaciones()`).
*   **Funcionamiento**:
    1.  Al recibir una petición HTTP en `POST /notificaciones` con un `EventoNotificableDTO`, el controlador responde `202 Accepted` y delega en `NotificacionService`.
    2.  Se recupera la `Persona` destinataria (o el Administrador por ID fijo si aplica) desde el repositorio local.
    3.  Se instancia el `EventoNotificable` correspondiente.
    4.  El evento genera las instancias de `Notificacion` necesarias (pudiendo generar 1 a N notificaciones, ej: al donante y al administrador).
    5.  Se ejecuta el envío multicanal a través del `NotificacionRouter` y se persiste cada `Notificacion` generada para auditoría.
*   **Conclusión**: No tienen identificador único persistente en el servicio de notificaciones y por ende **no son Aggregate Roots**. Son clases de lógica transitoria que orquestan el comportamiento reactivo del sistema.
