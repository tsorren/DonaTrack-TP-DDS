package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDonanteRegistradoV1(
    @NotNull(message = "El ID del donante es obligatorio") UUID donanteId,
    @NotNull(message = "El ID de la persona es obligatorio") UUID personaId,
    @NotBlank(message = "El nombre del donante es obligatorio") String nombre,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha,
    @NotBlank(message = "Las credenciales de acceso son obligatorias") String credencialesDeAcceso) {}
