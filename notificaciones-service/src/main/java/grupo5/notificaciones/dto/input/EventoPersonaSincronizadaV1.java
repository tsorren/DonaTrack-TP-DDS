package grupo5.notificaciones.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoPersonaSincronizadaV1(
    @NotNull(message = "El ID de la persona es obligatorio") UUID personaId,
    @NotBlank(message = "La denominación es obligatoria") String denominacion,
    @NotBlank(message = "El tipo de persona es obligatorio") String tipoPersona,
    List<@Valid MedioDeContactoEventoDTO> mediosDeContacto) {}
