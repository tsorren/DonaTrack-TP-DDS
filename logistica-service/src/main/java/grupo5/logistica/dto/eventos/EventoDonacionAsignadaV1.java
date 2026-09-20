package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDonacionAsignadaV1(
    @NotNull(message = "El ID de la donación independiente es obligatorio")
        UUID donacionIndependienteId,
    @NotNull(message = "El ID del donante es obligatorio") UUID donanteId,
    @NotNull(message = "El ID de la persona es obligatorio") UUID personaId,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha,
    @NotNull(message = "El ID de la persona beneficiaria es obligatorio")
        UUID personaBeneficiariaId,
    @NotBlank(message = "La descripción de la donación es obligatoria") String descripcion,
    @NotNull(message = "El destino es obligatorio") @Valid DestinoEventoDTO destino,
    @NotNull(message = "El peso total es obligatorio")
        @Positive(message = "El peso total debe ser positivo")
        Double pesoTotalKG,
    @NotNull(message = "El volumen total es obligatorio")
        @Positive(message = "El volumen total debe ser positivo")
        Double volumenTotalM3) {}
