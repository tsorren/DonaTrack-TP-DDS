package grupo5.donaciones.dto.donantes;

import jakarta.validation.constraints.NotBlank;

public record ArchivoInputDTO(
    @NotBlank(message = "La ruta del archivo es obligatoria") String path) {}
