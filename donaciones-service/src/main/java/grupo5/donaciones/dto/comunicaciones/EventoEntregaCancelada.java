package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de vuelta {@code entrega.cancelada}: Logística canceló una entrega que no salió (HB-19).
 */
public record EventoEntregaCancelada(
    UUID entregaId, UUID donacionIndependienteId, String motivo, LocalDateTime fechaCancelacion) {}
