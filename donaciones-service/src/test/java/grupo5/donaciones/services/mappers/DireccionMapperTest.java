package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.models.entities.personas.Direccion;
import grupo5.donaciones.models.entities.personas.Localidad;
import grupo5.donaciones.models.entities.personas.Pais;
import grupo5.donaciones.models.entities.personas.Provincia;
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
    assertEquals("Av. Corrientes", entity.getCalle());
    assertEquals(1234, entity.getAltura());
    assertEquals(5, entity.getPiso());
    assertEquals("B", entity.getDepartamento());
    assertEquals("1043", entity.getCodigoPostal());

    assertNotNull(entity.getLocalidad());
    assertEquals("CABA", entity.getLocalidad().getNombre());

    assertNotNull(entity.getLocalidad().getProvincia());
    assertEquals("Buenos Aires", entity.getLocalidad().getProvincia().getNombre());

    assertNotNull(entity.getLocalidad().getProvincia().getPais());
    assertEquals("Argentina", entity.getLocalidad().getProvincia().getPais().getNombre());
  }

  @Test
  void toEntity_conNull_deberiaRetornarNull() {
    assertNull(mapper.toEntity(null));
  }

  @Test
  void toOutputDTO_conDireccionCompleta_deberiaMapearCorrectamente() {
    Pais pais = new Pais();
    pais.setNombre("Argentina");

    Provincia provincia = new Provincia();
    provincia.setNombre("Buenos Aires");
    provincia.setPais(pais);

    Localidad localidad = new Localidad();
    localidad.setNombre("La Plata");
    localidad.setProvincia(provincia);

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
    Localidad localidad = new Localidad();
    localidad.setNombre("Alguna Localidad");
    localidad.setProvincia(null);

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
