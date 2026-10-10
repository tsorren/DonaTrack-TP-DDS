package grupo5.donaciones.dto.donantes;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DonanteInputDTO(
    @NotNull(message = "El ID de persona es obligatorio") UUID idPersona) {}
