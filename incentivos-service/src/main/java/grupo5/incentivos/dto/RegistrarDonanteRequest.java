package grupo5.incentivos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegistrarDonanteRequest(
    @NotNull(message = "El ID de donante es obligatorio") UUID idDonante,
    @NotNull(message = "El ID de persona es obligatorio") UUID idPersona,
    @NotBlank(message = "El nombre del donante es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre) {}
