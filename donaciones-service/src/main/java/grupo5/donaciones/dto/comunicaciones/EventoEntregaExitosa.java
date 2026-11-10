package grupo5.donaciones.dto.comunicaciones;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code entrega.exitosa} de Logística. {@code reporte} es opcional: llega solo si la entidad contó
 * un problema al confirmar (HB-17, DTI-29).
 */
public record EventoEntregaExitosa(
    UUID entregaId,
    UUID donacionIndependienteId,
    UUID camionId,
    String patenteCamion,
    LocalDateTime fechaEntrega,
    Reporte reporte) {

  public EventoEntregaExitosa(
      UUID entregaId,
      UUID donacionIndependienteId,
      UUID camionId,
      String patenteCamion,
      LocalDateTime fechaEntrega) {
    this(entregaId, donacionIndependienteId, camionId, patenteCamion, fechaEntrega, null);
  }

  /** {@code motivo}: INCOMPLETA, DANADA u OTRO; {@code cantidadRecibida}, en la unidad. */
  public record Reporte(String motivo, String detalle, Double cantidadRecibida) {}
}
