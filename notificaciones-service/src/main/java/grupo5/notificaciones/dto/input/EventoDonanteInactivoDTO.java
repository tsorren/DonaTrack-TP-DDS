package grupo5.notificaciones.dto.input;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonanteInactivoDTO(
    UUID idPersonaDonante, LocalDateTime fecha, Integer diasInactivo)
    implements EventoNotificableDTO {}
