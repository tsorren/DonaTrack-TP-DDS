package grupo5.logistica.dto.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando se le asigna una ruta a una donacion/entrega */
public record EventoRutaAsignada(
        UUID rutaId,
        UUID donacionIndependienteId,
        LocalDateTime fechaAsignacion) {}
