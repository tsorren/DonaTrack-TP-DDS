package grupo5.notificaciones.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionDTO(
    UUID id, String mensaje, String estado, LocalDateTime fechaCreacion) {}
