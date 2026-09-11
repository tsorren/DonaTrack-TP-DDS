package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.models.entities.choferes.Chofer;
import org.junit.jupiter.api.Test;

class ChoferMapperTest {
  private final ChoferMapper mapper = new ChoferMapper();

  @Test
  void mapeaRequestADominio() {
    ChoferRequestDTO dto = new ChoferRequestDTO("Ada", "Lovelace", "LIC-1", "1111");
    Chofer chofer = mapper.toDomain(dto);
    assertEquals(dto.nombre(), chofer.getNombre());
    assertEquals(dto.apellido(), chofer.getApellido());
    assertEquals(dto.licencia(), chofer.getLicencia());
    assertEquals(dto.telefonoContacto(), chofer.getTelefonoContacto());
  }

  @Test
  void mapeaDominioAResponse() {
    Chofer chofer = grupo5.logistica.testutils.ChoferMother.disponible();
    var dto = mapper.toResponseDTO(chofer);
    assertEquals(chofer.getId(), dto.id());
    assertEquals(chofer.getNombre(), dto.nombre());
    assertEquals(chofer.getEstado(), dto.estado());
  }
}
