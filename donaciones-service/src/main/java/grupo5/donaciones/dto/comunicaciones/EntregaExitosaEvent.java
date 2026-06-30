package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

public record EntregaExitosaEvent(
    UUID entregaId,
    UUID donacionIndependienteId,
    UUID camionId,
    String patenteCamion,
    LocalDateTime fechaEntrega) {}
