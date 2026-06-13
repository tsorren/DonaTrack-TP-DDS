package grupo5.common.notificaciones.alertas.commands;

import java.util.UUID;

public record EnviarNotificacionSincronicaDTO(
    UUID usuarioId, String mensaje, String canal // "CORREO", "SMS", "WHATSAPP"
    ) {}
