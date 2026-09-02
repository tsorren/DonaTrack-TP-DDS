package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonacionVencidaDTO(
    UUID idPersonaDonante,
    LocalDateTime fecha,
    UUID idPersonaAdmin,
    String detalleDonacion,
    String motivo)
    implements EventoNotificableDTO {}
