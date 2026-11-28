package grupo5.donaciones.dto;

import grupo5.donaciones.models.entities.necesidades.ResultadoPeriodo;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un período de una necesidad recurrente, en {@code GET /api/necesidades/{id}/periodos}. Todas las
 * cifras van en las unidades de la necesidad. {@code asignado}: la cantidad de las donaciones con
 * destino en el período. {@code recibido}: la de las que llegaron ({@code ENTREGADA}). {@code
 * avance}: llegaron, asignadas sin llegar y sin asignar (HG-04); opcional.
 */
public record PeriodoNecesidadDTO(
    UUID id,
    LocalDate inicio,
    LocalDate fin,
    Integer objetivo,
    int asignado,
    int recibido,
    ResultadoPeriodo resultado,
    AvanceNecesidadDTO avance) {

  /** Los campos de antes de HG-04; el avance queda nulo. */
  public PeriodoNecesidadDTO(
      UUID id,
      LocalDate inicio,
      LocalDate fin,
      Integer objetivo,
      int asignado,
      int recibido,
      ResultadoPeriodo resultado) {
    this(id, inicio, fin, objetivo, asignado, recibido, resultado, null);
  }
}
