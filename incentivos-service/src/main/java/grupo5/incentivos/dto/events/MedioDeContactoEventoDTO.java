package grupo5.incentivos.dto.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedioDeContactoEventoDTO(
    @NotBlank(message = "El tipo de medio de contacto es obligatorio") String tipo,
    Boolean esPredeterminado,
    String direccionCorreo,
    String caracteristica,
    String codigoArea,
    String numero) {}
