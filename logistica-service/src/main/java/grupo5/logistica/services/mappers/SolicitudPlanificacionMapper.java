package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.callback.SolicitudPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.planificacion.SolicitudPlanificacion;
import org.springframework.stereotype.Component;

@Component
public class SolicitudPlanificacionMapper {

  public SolicitudPlanificacion toEntity(SolicitudPlanificacionRequestDTO dto) {
    if (dto == null) {
      return null;
    }

    int cantidadDonaciones = dto.entregaIds() == null ? 0 : dto.entregaIds().size();
    return new SolicitudPlanificacion(dto.fecha(), cantidadDonaciones, dto.callbackUrl());
  }

  public SolicitudPlanificacionResponseDTO toResponseDTO(SolicitudPlanificacion solicitud) {
    if (solicitud == null) {
      return null;
    }

    return new SolicitudPlanificacionResponseDTO(
        solicitud.getId(),
        solicitud.getCorrelationId(),
        solicitud.getFecha(),
        solicitud.getEstado(),
        solicitud.getCantidadDonaciones(),
        solicitud.getCallbackUrl(),
        solicitud.getRutasGeneradas(),
        solicitud.getIntentosFallidos(),
        solicitud.getMotivoError());
  }
}
