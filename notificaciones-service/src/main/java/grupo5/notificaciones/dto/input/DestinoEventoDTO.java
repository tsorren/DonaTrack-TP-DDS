package grupo5.notificaciones.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DestinoEventoDTO(
    @NotBlank(message = "La calle es obligatoria") String calle,
    @NotNull(message = "La altura es obligatoria")
        @Positive(message = "La altura debe ser positiva")
        Integer altura,
    Integer piso,
    String departamento,
    @NotBlank(message = "El código postal es obligatorio") String codigoPostal,
    @NotBlank(message = "La localidad es obligatoria") String localidad,
    @NotBlank(message = "La provincia es obligatoria") String provincia,
    @NotBlank(message = "El país es obligatorio") String pais) {}
