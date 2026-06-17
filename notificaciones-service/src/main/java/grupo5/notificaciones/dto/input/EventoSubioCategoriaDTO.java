package grupo5.notificaciones.dto.input;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoSubioCategoriaDTO(
    UUID idPersonaDonante, LocalDateTime fecha, String categoriaNueva, String categoriaVieja)
    implements EventoNotificableDTO {}
