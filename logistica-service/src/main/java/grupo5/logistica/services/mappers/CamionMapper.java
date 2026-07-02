package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import org.springframework.stereotype.Component;

@Component
public class CamionMapper {

  public Camion toEntity(CamionRequestDTO dto) {
    if (dto == null) {
      return null;
    }

    return new Camion(dto.patente(), dto.capacidadVolumen(), dto.capacidadKG(), dto.altura());
  }

  public CamionResponseDTO toResponseDTO(Camion camion) {
    if (camion == null) {
      return null;
    }

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
