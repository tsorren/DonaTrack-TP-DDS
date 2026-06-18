package grupo5.notificaciones.dto.input;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoMisionCumplidaDTO(
    UUID idPersonaDonante, LocalDateTime fecha, String nombreMision, String recompensa)
    implements EventoNotificableDTO {}
