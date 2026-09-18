package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import org.springframework.stereotype.Component;

@Component
public class EjecucionAsignacionMapper {

  public EjecucionAsignacionDTO toDTO(EjecucionAsignacion entity) {
    if (entity == null) {
      return null;
    }
    return new EjecucionAsignacionDTO(
        entity.getId(), entity.getFechaEjecucion(), entity.getCantidadPropuestasGeneradas());
  }

  public EjecucionAsignacion toEntity(EjecucionAsignacionDTO dto) {
    if (dto == null) {
      return null;
    }
    return new EjecucionAsignacion(
        dto.getId(), dto.getFechaEjecucion(), dto.getCantidadPropuestasGeneradas());
  }
}
