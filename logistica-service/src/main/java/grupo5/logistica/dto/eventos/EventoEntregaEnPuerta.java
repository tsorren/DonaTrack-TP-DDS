package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando el camión llega al destino de la entrega (HB-22). */
public record EventoEntregaEnPuerta(
    UUID entregaId,
    UUID donacionIndependienteId,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaLlegada) {}
