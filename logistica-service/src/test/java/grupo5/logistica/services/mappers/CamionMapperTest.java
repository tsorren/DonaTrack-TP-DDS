package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import java.util.List;
import org.junit.jupiter.api.Test;

class CamionMapperTest {
  private final CamionMapper mapper = new CamionMapper();

  @Test
  void mapeaRequestADominioYSolicitud() {
    CamionRequestDTO dto = new CamionRequestDTO("AB123CD", 20f, 3f, 5000f);
    Camion camion = mapper.toDomain(dto);
    var solicitud = mapper.toSolicitud(dto, List.of("AA111AA"));
    assertEquals(dto.patente(), camion.getPatente());
    assertEquals(dto.capacidadVolumen(), camion.getCapacidadVolumen());
    assertEquals(dto.capacidadKG(), camion.getCapacidadKG());
    assertEquals(dto.altura(), camion.getAltura());
    assertEquals(dto.patente(), solicitud.patente());
  }

  @Test
  void mapeaDominioAResponse() {
    Camion camion = grupo5.logistica.testutils.CamionMother.disponible();
    var dto = mapper.toResponseDTO(camion);
    assertEquals(camion.getId(), dto.id());
    assertEquals(camion.getPatente(), dto.patente());
    assertEquals(camion.getEstado(), dto.estado());
  }
}
