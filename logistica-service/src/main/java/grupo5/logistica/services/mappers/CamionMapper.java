package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.SolicitudNuevoCamion;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CamionMapper {

  public Camion toDomain(CamionRequestDTO dto) {
    return new Camion(dto.patente(), dto.capacidadVolumen(), dto.capacidadKG(), dto.altura());
  }

  public SolicitudNuevoCamion toSolicitud(CamionRequestDTO dto, List<String> patentesExistentes) {
    return new SolicitudNuevoCamion(
        dto.patente(), dto.capacidadVolumen(), dto.altura(), dto.capacidadKG(), patentesExistentes);
  }

  public CamionResponseDTO toResponseDTO(Camion camion) {
    return new CamionResponseDTO(
        camion.getId(),
        camion.getPatente(),
        camion.getCapacidadVolumen(),
        camion.getAltura(),
        camion.getCapacidadKG(),
        camion.getEstado(),
        camion.getRutaId());
  }
}
