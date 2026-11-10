package grupo5.donaciones.dto;

import grupo5.donaciones.models.entities.necesidades.ResultadoPeriodo;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un período de una necesidad recurrente, en {@code GET /api/necesidades/{id}/periodos}. {@code
 * asignado}: las donaciones con destino en el período. {@code recibido}: las que llegaron ({@code
 * ENTREGADA}).
 */
public record PeriodoNecesidadDTO(
    UUID id,
    LocalDate inicio,
    LocalDate fin,
    Integer objetivo,
    int asignado,
    int recibido,
    ResultadoPeriodo resultado) {}
