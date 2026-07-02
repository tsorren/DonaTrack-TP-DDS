package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoEntregaFallidaDTO(
    UUID idPersonaDonante,
    LocalDateTime fecha,
    UUID idPersonaBeneficiaria,
    String detalleDonacion,
    UUID idPersonaAdmin,
    String motivo,
    boolean replanificable)
    implements EventoNotificableDTO {}
