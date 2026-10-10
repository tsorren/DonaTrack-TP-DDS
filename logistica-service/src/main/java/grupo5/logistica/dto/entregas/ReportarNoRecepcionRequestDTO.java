package grupo5.logistica.dto.entregas;

import jakarta.validation.constraints.NotBlank;

public record ReportarNoRecepcionRequestDTO(
    @NotBlank String actor, @NotBlank String justificacion, Boolean replanificable) {}
