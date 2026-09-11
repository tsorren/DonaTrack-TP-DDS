package grupo5.logistica.dto.choferes;

import jakarta.validation.constraints.NotBlank;

public record ChoferRequestDTO(
    @NotBlank String nombre,
    @NotBlank String apellido,
    @NotBlank String licencia,
    @NotBlank String telefonoContacto) {}
