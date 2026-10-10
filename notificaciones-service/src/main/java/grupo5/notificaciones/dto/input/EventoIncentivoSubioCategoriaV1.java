package grupo5.notificaciones.dto.input;

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
public record EventoIncentivoSubioCategoriaV1(
    @NotNull(message = "El ID de la persona donante es obligatorio") UUID personaDonanteId,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha,
    @NotBlank(message = "El nombre de la nueva categoría es obligatorio")
        String nombreNuevaCategoria,
    @NotBlank(message = "El nombre de la vieja categoría es obligatorio")
        String nombreViejaCategoria) {}
