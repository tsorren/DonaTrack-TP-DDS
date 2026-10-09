package grupo5.donaciones.dto.logistica;

import jakarta.validation.constraints.NotBlank;

public record PreferenciaProveedorRequestDTO(
    @NotBlank(message = "El proveedor es obligatorio") String proveedorId) {}
