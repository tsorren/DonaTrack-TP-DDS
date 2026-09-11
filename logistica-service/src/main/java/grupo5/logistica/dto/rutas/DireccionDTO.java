package grupo5.logistica.dto.rutas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DireccionDTO(
    @NotBlank String calle,
    @NotNull @Positive Integer altura,
    Integer piso,
    String departamento,
    @NotBlank String codigoPostal,
    @NotBlank String localidad,
    @NotBlank String provincia,
    @NotBlank String pais) {}
