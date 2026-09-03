package grupo5.logistica.dto.callback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public record CallbackPlanificacionRequestDTO(
    @NotNull UUID solicitudId,
    @Valid List<RutaPlanificadaDTO> rutas,
    @NotBlank @Pattern(regexp = "(?i)OK|PARCIAL|ERROR") String estado,
    String motivoError) {}
