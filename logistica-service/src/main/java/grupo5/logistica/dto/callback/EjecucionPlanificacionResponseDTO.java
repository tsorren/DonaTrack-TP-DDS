package grupo5.logistica.dto.callback;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Resultado inmediato de disparar una corrida de planificación. */
public record EjecucionPlanificacionResponseDTO(
    LocalDate fechaObjetivo,
    List<SolicitudPlanificacionResponseDTO> solicitudes,
    List<UUID> entregasNoPlanificadas) {

  public EjecucionPlanificacionResponseDTO {
    solicitudes = List.copyOf(solicitudes);
    entregasNoPlanificadas = List.copyOf(entregasNoPlanificadas);
  }
}
