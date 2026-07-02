package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.models.entities.personas.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonaMapperTest {

  private PersonaMapper mapper;

  @BeforeEach
  void setUp() {
    DireccionMapper direccionMapper = new DireccionMapper();
    MedioDeContactoMapper medioDeContactoMapper = new MedioDeContactoMapper();
    mapper = new PersonaMapper(direccionMapper, medioDeContactoMapper);
  }

  @Test
  void toEntity_conHumanaInput_deberiaMapearCorrectamente() {
    HumanaInputDTO input =
        new HumanaInputDTO(
            TipoPersona.HUMANA,
            TipoDocumento.DNI,
            "12345678",
            null,
            Collections.emptyList(),
            "Juan",
            "Perez",
            Genero.HOMBRE,
            LocalDate.of(1990, Month.JANUARY, 1));

    Persona entity = mapper.toEntity(input);

    assertTrue(entity instanceof Humana);
    Humana humana = (Humana) entity;
    assertEquals("Juan", humana.getNombre());
    assertEquals("Perez", humana.getApellido());
    assertEquals(TipoPersona.HUMANA, humana.getTipoPersona());
    assertEquals(TipoDocumento.DNI, humana.getTipoDocumento());
    assertEquals("12345678", humana.getDocumento());
  }

  @Test
  void toEntity_conJuridicaInput_deberiaMapearCorrectamente() {
    HumanaInputDTO representanteInput =
        new HumanaInputDTO(
            TipoPersona.HUMANA,
            TipoDocumento.DNI,
            "11111111",
            null,
            Collections.emptyList(),
            "Representante",
            "Uno",
            Genero.PREFIERO_NO_DECIR,
            LocalDate.of(1980, Month.JANUARY, 1));

    JuridicaInputDTO input =
        new JuridicaInputDTO(
            TipoPersona.JURIDICA,
            TipoDocumento.CUIT,
            "30-12345678-9",
            null,
            Collections.emptyList(),
            "Empresa S.A.",
            TipoJuridico.EMPRESA,
            "Tecnologia",
            List.of(representanteInput));

    Persona entity = mapper.toEntity(input);

    assertTrue(entity instanceof Juridica);
    Juridica juridica = (Juridica) entity;
    assertEquals("Empresa S.A.", juridica.getRazonSocial());
    assertEquals(TipoJuridico.EMPRESA, juridica.getTipo());
    assertEquals("Tecnologia", juridica.getRubro());
    assertEquals(1, juridica.getRepresentantes().size());
    assertEquals("Representante", juridica.getRepresentantes().get(0).getNombre());
  }

  @Test
  void updateEntity_conHumana_deberiaModificarCampos() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    humana.actualizarDocumento(null, "12345678");

    HumanaInputDTO updatedInput =
        new HumanaInputDTO(
            TipoPersona.HUMANA,
            TipoDocumento.PASAPORTE,
            "87654321",
            null,
            Collections.emptyList(),
            "Carlos",
            "Gomez",
            Genero.HOMBRE,
            LocalDate.of(1995, Month.MAY, 5));

    mapper.updateEntity(humana, updatedInput);

    assertEquals("Carlos", humana.getNombre());
    assertEquals("Gomez", humana.getApellido());
    assertEquals(TipoDocumento.PASAPORTE, humana.getTipoDocumento());
    assertEquals("87654321", humana.getDocumento());
    assertEquals(LocalDate.of(1995, Month.MAY, 5), humana.getFechaNacimiento());
  }

  @Test
  void toReplicaDTO_deberiaMapearCorrectamente() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    humana.actualizarDocumento(null, "12345678");

    PersonaReplicaDTO replica = mapper.toReplicaDTO(humana);

    assertNotNull(replica);
    assertEquals(humana.getId(), replica.id());
    assertEquals("Juan Perez", replica.denominacion());
    assertEquals(TipoPersona.HUMANA, replica.tipoPersona());
  }
}
