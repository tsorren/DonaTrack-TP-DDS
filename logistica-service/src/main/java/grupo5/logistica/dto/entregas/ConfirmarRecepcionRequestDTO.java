package grupo5.logistica.dto.entregas;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarRecepcionRequestDTO(@NotBlank String actor) {}
