package grupo5.logistica.dto.camiones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CamionRequestDTO(
    @NotBlank String patente,
    @NotNull @Positive Float capacidadVolumen,
    @NotNull @Positive Float altura,
    @NotNull @Positive Float capacidadKG) {}
