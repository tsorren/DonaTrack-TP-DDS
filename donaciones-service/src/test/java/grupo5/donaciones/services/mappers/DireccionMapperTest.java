package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DireccionMapperTest {

  private DireccionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new DireccionMapper();
  }

  @Test
  void toEntity_conDireccionInput_deberiaMapearCorrectamente() {
    DireccionInputDTO dto =
        new DireccionInputDTO(
            "Av. Corrientes", 1234, 5, "B", "1043", "CABA", "Buenos Aires", "Argentina");

    Direccion entity = mapper.toEntity(dto);

    assertNotNull(entity);
    assertEquals("Av. Corrientes", entity.calle());
    assertEquals(1234, entity.altura());
    assertEquals(5, entity.piso());
    assertEquals("B", entity.departamento());
    assertEquals("1043", entity.codigoPostal());

    assertNotNull(entity.localidad());
    assertEquals("CABA", entity.localidad().nombre());

    assertNotNull(entity.localidad().provincia());
    assertEquals("Buenos Aires", entity.localidad().provincia().nombre());

    assertNotNull(entity.localidad().provincia().pais());
    assertEquals("Argentina", entity.localidad().provincia().pais().nombre());
  }

  @Test
  void toEntity_conNull_deberiaRetornarNull() {
    assertNull(mapper.toEntity(null));
  }

  @Test
  void toOutputDTO_conDireccionCompleta_deberiaMapearCorrectamente() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("La Plata", provincia);

    Direccion direccion = new Direccion("Calle 50", 120, null, null, "1900", localidad);

    DireccionOutputDTO dto = mapper.toOutputDTO(direccion);

    assertNotNull(dto);
    assertEquals("Calle 50", dto.calle());
    assertEquals(120, dto.altura());
    assertNull(dto.piso());
    assertNull(dto.departamento());
    assertEquals("1900", dto.codigoPostal());
    assertEquals("La Plata", dto.localidad());
    assertEquals("Buenos Aires", dto.provincia());
    assertEquals("Argentina", dto.pais());
  }

  @Test
  void toOutputDTO_conCamposNulos_deberiaRetornarDtoConCamposNulos() {
    Localidad localidad = new Localidad("Alguna Localidad", null);

    Direccion direccion = new Direccion("Calle Falsa", 123, null, null, "0000", localidad);

    DireccionOutputDTO dto = mapper.toOutputDTO(direccion);

    assertNotNull(dto);
    assertEquals("Calle Falsa", dto.calle());
    assertEquals(123, dto.altura());
    assertEquals("Alguna Localidad", dto.localidad());
    assertNull(dto.provincia());
    assertNull(dto.pais());
  }

  @Test
  void toOutputDTO_conNull_deberiaRetornarNull() {
    assertNull(mapper.toOutputDTO(null));
  }
}
