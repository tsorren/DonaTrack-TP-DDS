package grupo5.logistica.dto.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado por Logística cuando la entidad beneficiaria confirma la recepción de una
 * donación.
 */
public record EventoEntregaExitosa(
    UUID entregaId,
    UUID donacionIndependienteId,
    UUID camionId,
    String patenteCamion,
    LocalDateTime fechaEntrega) {}
