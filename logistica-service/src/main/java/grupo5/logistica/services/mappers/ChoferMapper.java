package grupo5.logistica.services.mappers;

import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.models.entities.choferes.Chofer;
import org.springframework.stereotype.Component;

@Component
public class ChoferMapper {

  public Chofer toDomain(ChoferRequestDTO dto) {
    return new Chofer(dto.nombre(), dto.apellido(), dto.licencia(), dto.telefonoContacto());
  }

  public ChoferResponseDTO toResponseDTO(Chofer chofer) {
    return new ChoferResponseDTO(
        chofer.getId(),
        chofer.getNombre(),
        chofer.getApellido(),
        chofer.getLicencia(),
        chofer.getTelefonoContacto(),
        chofer.getEstado(),
        chofer.getRutaId());
  }
}
