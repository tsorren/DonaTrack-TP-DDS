package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comando {@code entrega.solicitada.<instanciaId>.v1} publicado por el broker de logística de
 * donaciones-service: le pide a esta instancia de logística que cree la entrega de una donación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEntregaSolicitadaV1(
    @NotNull(message = "El ID de la donación independiente es obligatorio")
        UUID donacionIndependienteId,
    @NotNull(message = "El ID de la persona beneficiaria es obligatorio")
        UUID personaBeneficiariaId,
    @NotNull(message = "El destino es obligatorio") @Valid DestinoEventoDTO destino,
    @NotNull(message = "El peso total es obligatorio")
        @Positive(message = "El peso total debe ser positivo")
        Double pesoTotalKG,
    @NotNull(message = "El volumen total es obligatorio")
        @Positive(message = "El volumen total debe ser positivo")
        Double volumenTotalM3,
    @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime fecha) {}
