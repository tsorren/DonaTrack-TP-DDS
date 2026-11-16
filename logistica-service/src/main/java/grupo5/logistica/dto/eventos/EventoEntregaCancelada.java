package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando cancela una entrega que no salió (HB-19). */
public record EventoEntregaCancelada(
    UUID entregaId,
    UUID donacionIndependienteId,
    String motivo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaCancelacion) {}
