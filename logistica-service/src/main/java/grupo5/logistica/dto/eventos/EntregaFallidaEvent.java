package grupo5.logistica.dto.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado por Logística cuando una entrega no puede concretarse. */
public record EntregaFallidaEvent(
    UUID entregaId, UUID donacionIndependienteId, String justificacion, LocalDateTime fechaFalla) {}
