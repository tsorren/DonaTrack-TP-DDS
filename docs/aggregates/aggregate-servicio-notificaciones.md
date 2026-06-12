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
*   **Responsabilidad**: Almacenar la denominación y el ruteo de medios de contacto del usuario destinatario. Este agregado se actualiza asíncronamente cuando el servicio consume los eventos `PersonaHumanaCreadaEvent`, `PersonaJuridicaCreadaEvent` y `DonanteActualizadoEvent`.
*   **Paquete**: `grupo5.notificaciones.models.entities.personas` (y subpaquete `medioDeContacto`).

### 2.2. Agregado: Notificación
*   **Aggregate Root**: `Notificacion` (Representa el intento y estado final de una notificación despachada a un usuario).
*   **Componentes Internos**: `EstadoNotificacion` (Enum con valores *PENDIENTE*, *ENVIADA*, *FALLIDA*).
*   **Referencias Externas (por ID)**: `personaId` (UUID que apunta al agregado `Persona`).
*   **Responsabilidad**: Registrar el mensaje, el destinatario, la fecha y el estado de la comunicación. Provee la lógica para ordenar los canales por prioridad (respetando la predeterminación) y ejecutar reintentos de envío (*fallback*) ante fallos de proveedores de mensajería.
*   **Paquete**: `grupo5.notificaciones.models.entities.notificaciones`

---

## 3. Políticas y Procesamiento de Eventos (`EventoNotificable`)

La jerarquía de clases bajo el paquete `grupo5.notificaciones.models.entities.notificaciones.eventos` (tales como `EventoDeDonacion`, `DonanteRegistrado`, `MisionCumplida`, `SubioCategoria`) representa **Políticas de Dominio (Domain Policies)** y no agregados persistentes:

*   **Rol en el Diseño**: Actúan como factorías polimórficas de alertas (`generarNotificaciones()`).
*   **Funcionamiento**:
    1.  Al recibir un mensaje de RabbitMQ (ej: `DonanteCreadoEvent`), el consumidor recupera la `Persona` destinataria de la réplica local.
    2.  Instancia el `EventoNotificable` correspondiente.
    3.  El evento genera las instancias de `Notificacion` necesarias adaptando el texto según el contexto.
    4.  Se ejecuta la acción de envío y se persiste la `Notificacion` resultante.
*   **Conclusión**: No tienen identificador único persistente en el servicio de notificaciones y por ende **no son Aggregate Roots**. Son clases de lógica transitoria que ayudan a orquestar el comportamiento reactivo del sistema.
