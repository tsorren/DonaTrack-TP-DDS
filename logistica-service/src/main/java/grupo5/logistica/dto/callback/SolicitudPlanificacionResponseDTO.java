package grupo5.logistica.dto.callback;

import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SolicitudPlanificacionResponseDTO(
    UUID id,
    LocalDate fecha,
    EstadoSolicitud estado,
    Integer cantidadDonaciones,
    String callbackUrl,
    List<UUID> entregaIds,
    List<UUID> camionIds,
    List<UUID> choferIds,
    List<UUID> rutasGeneradas,
    List<UUID> entregasNoAsignadas,
    Integer intentosFallidos,
    String motivoError) {

  public SolicitudPlanificacionResponseDTO {
    entregaIds = List.copyOf(entregaIds);
    camionIds = List.copyOf(camionIds);
    choferIds = List.copyOf(choferIds);
    rutasGeneradas = List.copyOf(rutasGeneradas);
    entregasNoAsignadas = List.copyOf(entregasNoAsignadas);
  }
}
