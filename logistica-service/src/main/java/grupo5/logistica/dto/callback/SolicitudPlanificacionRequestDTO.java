package grupo5.logistica.dto.callback;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SolicitudPlanificacionRequestDTO(
    LocalDate fecha, List<UUID> entregaIds, String callbackUrl) {}
