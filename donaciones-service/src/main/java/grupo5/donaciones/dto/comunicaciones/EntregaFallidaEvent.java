package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EntregaFallidaEvent(
    UUID entregaId,
    UUID donacionIndependienteId,
    String justificacion,
    LocalDateTime fechaFalla,
    boolean replanificable) {}
