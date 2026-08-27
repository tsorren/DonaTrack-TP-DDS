package grupo5.donaciones.dto.direcciones;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DireccionInputDTO(
    @NotBlank(message = "La calle es obligatoria") String calle,
    // Opcional: hay direcciones reales sin numeración (S/N).
    @Positive(message = "La altura debe ser positiva") Integer altura,
    Integer piso,
    String departamento,
    // Opcional: hay direcciones reales sin código postal asignado (zonas rurales).
    String codigoPostal,
    @NotBlank(message = "La localidad es obligatoria") String localidad,
    @NotBlank(message = "La provincia es obligatoria") String provincia,
    @NotBlank(message = "El país es obligatorio") String pais) {}
