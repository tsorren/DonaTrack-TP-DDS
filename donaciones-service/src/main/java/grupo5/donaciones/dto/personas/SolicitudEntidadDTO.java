package grupo5.donaciones.dto.personas;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * La solicitud de una entidad que espera la aprobación (R11-41): lo que la cuenta envió al
 * registrarse y puede corregir mientras espera. {@code correo} es el del representante y es solo de
 * lectura: se ignora al corregir.
 */
public record SolicitudEntidadDTO(
    @NotBlank(message = "La razón social es obligatoria") String razonSocial,
    @NotBlank(message = "El rubro es obligatorio") String rubro,
    @NotNull(message = "El tipo jurídico es obligatorio") TipoJuridico tipoJuridico,
    @NotNull(message = "La dirección es obligatoria") @Valid Direccion direccion,
    @NotNull(message = "El representante es obligatorio") @Valid Representante representante,
    String correo) {

  /** La dirección de entrega; el país y el piso no se corrigen acá. */
  public record Direccion(
      @NotBlank(message = "La calle es obligatoria") String calle,
      @NotNull(message = "La altura es obligatoria")
          @Positive(message = "La altura debe ser positiva")
          Integer altura,
      @NotBlank(message = "El código postal es obligatorio") String codigoPostal,
      @NotBlank(message = "La localidad es obligatoria") String localidad,
      @NotBlank(message = "La provincia es obligatoria") String provincia) {}

  /** Quien la representa: el primero de sus representantes. */
  public record Representante(
      @NotBlank(message = "El nombre es obligatorio") String nombre,
      @NotBlank(message = "El apellido es obligatorio") String apellido) {}
}
