package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import org.springframework.stereotype.Component;

@Component
public class SolicitudPlanificacionMapper {

  public SolicitudPlanificacionResponseDTO toResponseDTO(SolicitudPlanificacion solicitud) {
    if (solicitud == null) {
      return null;
    }

    return new SolicitudPlanificacionResponseDTO(
        solicitud.getId(),
        solicitud.getFecha(),
        solicitud.getEstado(),
        solicitud.getCantidadDonaciones(),
        solicitud.getCallbackUrl(),
        solicitud.getRutasGeneradas(),
        solicitud.getIntentosFallidos(),
        solicitud.getMotivoError());
  }
}
