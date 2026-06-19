package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoDonacionRecibidaDTO(
    UUID idPersonaDonante, LocalDateTime fecha, UUID idPersonaBeneficiaria, String detalleDonacion)
    implements EventoNotificableDTO {}
