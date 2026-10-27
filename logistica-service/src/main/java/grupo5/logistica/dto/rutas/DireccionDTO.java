package grupo5.logistica.dto.rutas;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @NotBlank String pais,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud) {

  @SuppressWarnings("java:S107")
  public DireccionDTO(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      String localidad,
      String provincia,
      String pais) {
    this(calle, altura, piso, departamento, codigoPostal, localidad, provincia, pais, null, null);
  }
}
