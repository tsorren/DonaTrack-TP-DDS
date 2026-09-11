package grupo5.logistica.dto.entregas;

import jakarta.validation.constraints.NotBlank;

public record RegresarAlDepositoRequestDTO(@NotBlank String actor) {}
