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
public record EventoDonacionRecibidaV1(
    @NotNull(message = "El ID del donante es obligatorio") UUID donanteId,
    @NotNull(message = "El ID de la persona es obligatorio") UUID personaId,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha,
    @NotNull(message = "El ID de la persona beneficiaria es obligatorio")
        UUID personaBeneficiariaId,
    @NotBlank(message = "La descripción de la donación es obligatoria") String descripcion,
    @NotBlank(message = "La patente del camión es obligatoria") String patenteCamion) {}
