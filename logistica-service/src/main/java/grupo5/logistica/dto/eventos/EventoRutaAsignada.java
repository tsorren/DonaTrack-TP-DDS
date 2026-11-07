package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando se le asigna una ruta a una donacion/entrega */
public record EventoRutaAsignada(
    UUID rutaId,
    UUID donacionIndependienteId,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaAsignacion) {}
