package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ValidationException;
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
  void conservaCoordenadasOpcionalesCuandoEstanPresentes() {
    DireccionDTO dto =
        new DireccionDTO(
            "Medrano",
            951,
            null,
            null,
            "C1179",
            "CABA",
            "Buenos Aires",
            "Argentina",
            -34.5986,
            -58.4201);
    var entidad = mapper.toEntity(dto);
    assertEquals(-34.5986, entidad.coordenadas().latitud());
    assertEquals(-58.4201, entidad.coordenadas().longitud());
    assertEquals(dto, mapper.toResponseDTO(entidad));
  }

  @Test
  void rechazaCoordenadasIncompletas() {
    DireccionDTO soloLatitud =
        new DireccionDTO(
            "Medrano",
            951,
            null,
            null,
            "C1179",
            "CABA",
            "Buenos Aires",
            "Argentina",
            -34.5986,
            null);
    assertThrows(ValidationException.class, () -> mapper.toEntity(soloLatitud));
  }

  @Test
  void aceptaNulosEnAmbasDirecciones() {
    assertNull(mapper.toEntity(null));
    assertNull(mapper.toResponseDTO(null));
  }
}
