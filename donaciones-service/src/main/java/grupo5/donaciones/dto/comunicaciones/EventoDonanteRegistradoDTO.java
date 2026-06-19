package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonanteRegistradoDTO(
    UUID idPersonaDonante, LocalDateTime fecha, String credencialesDeAcceso)
    implements EventoNotificableDTO {}
