package grupo5.logistica.dto.rutas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IniciarRutaRequestDTO(@NotNull UUID choferId, @NotBlank String actor) {}
