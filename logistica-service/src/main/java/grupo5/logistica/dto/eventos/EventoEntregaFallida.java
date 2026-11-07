package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando una entrega no puede concretarse. */
public record EventoEntregaFallida(
    UUID entregaId,
    UUID donacionIndependienteId,
    String justificacion,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaFalla,
    boolean replanificable) {}
