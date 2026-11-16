package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Última posición conocida del camión, con coordenadas exactas (ADR-E6-07, D24 revisada). {@code
 * registradaEn} es la hora de origen del dato: la "última actualización" del marcador.
 */
public record PosicionCamionDTO(
    double latitud,
    double longitud,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant registradaEn) {}
