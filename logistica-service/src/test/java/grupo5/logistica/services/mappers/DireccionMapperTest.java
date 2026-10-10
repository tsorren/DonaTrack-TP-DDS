package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.logistica.dto.rutas.DireccionDTO;
import org.junit.jupiter.api.Test;

class DireccionMapperTest {
  private final DireccionMapper mapper = new DireccionMapper();

  @Test
  void conservaTodaLaJerarquiaEnAmbasDirecciones() {
    DireccionDTO dto =
        new DireccionDTO("Corrientes", 1234, 5, "A", "C1043", "CABA", "Buenos Aires", "Argentina");
    var entidad = mapper.toEntity(dto);
    assertEquals(dto, mapper.toResponseDTO(entidad));
  }

  @Test
  void aceptaNulosEnAmbasDirecciones() {
    assertNull(mapper.toEntity(null));
    assertNull(mapper.toResponseDTO(null));
  }
}
