package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comando {@code entrega.cancelada.<instanciaId>.v1} del broker de donaciones-service: le pide a
 * esta instancia que cancele la entrega de una donación que todavía no salió (HB-19).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEntregaCanceladaV1(
    UUID donacionIndependienteId,
    String motivo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fecha) {}
