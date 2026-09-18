package grupo5.logistica.dto.callback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CallbackPlanificacionRequestDTO(
    @NotNull UUID solicitudId,
    @Valid List<RutaPlanificadaDTO> rutas,
    @NotBlank String estado,
    String motivoError) {}
