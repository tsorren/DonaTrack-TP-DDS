package grupo5.logistica.dto.eventos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import grupo5.logistica.dto.entregas.ReporteRecepcionDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evento publicado por Logística cuando la entidad beneficiaria confirma la recepción de una
 * donación.
 *
 * <p>{@code reporte} es opcional: viaja solo si la entidad contó un problema (DTI-29). El remito no
 * viaja. {@code fotosRecepcion} lleva las URLs de las fotos para que Donaciones las replique en las
 * destacadas (HB-01, ADR de réplicas por evento); no viaja si la entrega no tiene fotos.
 */
public record EventoEntregaExitosa(
    UUID entregaId,
    UUID donacionIndependienteId,
    UUID camionId,
    String patenteCamion,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime fechaEntrega,
    @JsonInclude(JsonInclude.Include.NON_NULL) ReporteRecepcionDTO reporte,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> fotosRecepcion) {

  public EventoEntregaExitosa(
      UUID entregaId,
      UUID donacionIndependienteId,
      UUID camionId,
      String patenteCamion,
      LocalDateTime fechaEntrega) {
    this(
        entregaId, donacionIndependienteId, camionId, patenteCamion, fechaEntrega, null, List.of());
  }
}
