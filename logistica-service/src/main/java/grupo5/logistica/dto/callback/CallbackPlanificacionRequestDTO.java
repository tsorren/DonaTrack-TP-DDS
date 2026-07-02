package grupo5.logistica.dto.callback;

import java.util.List;
import java.util.UUID;

public record CallbackPlanificacionRequestDTO(
    UUID solicitudId, List<RutaPlanificadaDTO> rutas, String estado, String motivoError) {}
