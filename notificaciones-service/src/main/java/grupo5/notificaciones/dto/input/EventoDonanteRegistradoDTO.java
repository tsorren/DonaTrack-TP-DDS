package grupo5.notificaciones.dto.input;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonanteRegistradoDTO(
    UUID idPersonaDonante, LocalDateTime fecha, String credencialesDeAcceso)
    implements EventoNotificableDTO {}
