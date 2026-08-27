package grupo5.incentivos.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DonacionExitosaRequest(
    @NotNull(message = "El ID de donante es obligatorio") UUID donanteId,
    @NotNull(message = "El ID de organización es obligatorio") UUID organizacionId) {}
