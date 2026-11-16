package grupo5.donaciones.dto.comunicaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code entrega.exitosa} de Logística. {@code reporte} es opcional: llega solo si la entidad contó
 * un problema al confirmar (HB-17, DTI-29). {@code fotosRecepcion} es opcional: las URLs estables
 * de las fotos, que Donaciones replica para las destacadas (HB-01, ADR de réplicas por evento).
 *
 * <p>Ignora los campos desconocidos sin depender de la configuración del conversor (Tolerant
 * Reader, ADR de réplicas por evento).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEntregaExitosa(
    UUID entregaId,
    UUID donacionIndependienteId,
    UUID camionId,
    String patenteCamion,
    LocalDateTime fechaEntrega,
    Reporte reporte,
    List<String> fotosRecepcion) {

  public EventoEntregaExitosa(
      UUID entregaId,
      UUID donacionIndependienteId,
      UUID camionId,
      String patenteCamion,
      LocalDateTime fechaEntrega) {
    this(entregaId, donacionIndependienteId, camionId, patenteCamion, fechaEntrega, null, null);
  }

  public EventoEntregaExitosa(
      UUID entregaId,
      UUID donacionIndependienteId,
      UUID camionId,
      String patenteCamion,
      LocalDateTime fechaEntrega,
      Reporte reporte) {
    this(entregaId, donacionIndependienteId, camionId, patenteCamion, fechaEntrega, reporte, null);
  }

  /** {@code motivo}: INCOMPLETA, DANADA u OTRO; {@code cantidadRecibida}, en la unidad. */
  public record Reporte(String motivo, String detalle, Double cantidadRecibida) {}
}
