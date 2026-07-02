# Modelado del Destinatario "Administración" como Persona Replicada de ID Fijo
- Status: accepted
- Date: 2026-07-02
- Deciders: Decisión Grupal

## Contexto y Problema
El evento EntregaFallida (Entrega 3) requiere notificar, además del donante y la entidad beneficiaria, a "personas administradoras del sistema" cuando una entrega falla. A diferencia de un donante o una entidad beneficiaria, la administración no es un actor con el que el Servicio de Donaciones interactúe de forma dinámica: en nuestro dominio se asume un único usuario administrador fijo, configurado de antemano en el Servicio de Donaciones. Debemos decidir cómo representar a este destinatario dentro de notificaciones-service: ¿como una Persona más (reutilizando el mecanismo de réplica ya existente), o como un concepto de dominio separado (por ejemplo, una lista de administradores gestionada internamente por notificaciones-service, o un medio de contacto fijo hardcodeado)?

## Alternativas Consideradas
* Administración como concepto de dominio separado (lista fija / configuración interna)
* Administración como una Persona más, replicada con id fijo

## Resultado de la Decisión

Alternativa elegida: "Administración como una Persona más, replicada con id fijo"

Justificación:
Reutilizar la entidad Persona y el mecanismo de sincronización ya construido (PUT /api/notificaciones/personas) evita duplicar lógica de resolución de destinatarios y de envío. El Servicio de Donaciones ya conoce el id fijo del usuario administrador (configurado como constante o variable de entorno de ese servicio) y lo envía como idPersonaAdmin en el EventoEntregaFallidaDTO, de la misma forma en que envía los ids de donante y beneficiario. Desde la perspectiva de notificaciones-service, la administración no es conceptualmente distinta de cualquier otra Persona: tiene sus propios medios de contacto, su propio esPredeterminado, y se le aplica exactamente la misma lógica de envío con fallback (Notificacion.notificar).

### Consecuencias Positivas
* Cero código nuevo de infraestructura: se reutiliza IPersonaRepository, PersonaReplicaDTO y NotificacionRouter tal cual existen.
* El admin puede tener sus propios medios de contacto configurados (correo, SMS, WhatsApp) igual que cualquier persona, sin lógica especial.
* Si en el futuro se necesita notificar a varios administradores, alcanza con que Donaciones envíe varios eventos (uno por administrador) o se extienda el DTO con una lista de ids, sin tocar el modelo de dominio de Persona.

### Consecuencias Negativas
* La responsabilidad de saber "quién es el administrador" queda completamente del lado de Donaciones; notificaciones-service no tiene forma de validar que el idPersonaAdmin recibido efectivamente corresponda a un usuario con rol administrador (confía ciegamente en el id recibido).
* Si el id del administrador fijo cambia o se desincroniza entre servicios, la notificación fallará silenciosamente con IllegalArgumentException ("Persona no encontrada") en EventoMapper, que se propaga como error 4xx/5xx al Servicio de Donaciones y no se le notifica a nadie.
