package grupo5.notificaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.notificaciones.dto.MedioDeContactoReplicaDTO;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonaMapperTest {

  private PersonaMapper mapper;

  @BeforeEach
  void setUp() {
    MedioDeContactoMapper medioDeContactoMapper = new MedioDeContactoMapper();
    mapper = new PersonaMapper(medioDeContactoMapper);
  }

  @Test
  void toEntity_conReplicaDTO_deberiaMapearCorrectamente() {
    UUID id = UUID.randomUUID();
    MedioDeContactoReplicaDTO medioDto =
        new MedioDeContactoReplicaDTO("CORREO", true, "test@test.com", null, null, null);
    PersonaReplicaDTO replicaDTO =
        new PersonaReplicaDTO(id, "Juan Perez", TipoPersona.HUMANA, List.of(medioDto));

    Persona entity = mapper.toEntity(replicaDTO);

    assertNotNull(entity);
    assertEquals(id, entity.getId());
    assertEquals("Juan Perez", entity.getDenominacion());
    assertEquals(TipoPersona.HUMANA, entity.getTipoPersona());
    assertEquals(1, entity.getMediosDeContacto().size());
    assertTrue(entity.getMediosDeContacto().get(0) instanceof Correo);
    assertEquals(
        "test@test.com", ((Correo) entity.getMediosDeContacto().get(0)).getDireccionCorreo());
  }

  @Test
  void toReplicaDTO_conPersona_deberiaMapearCorrectamente() {
    UUID id = UUID.randomUUID();
    Persona persona = new Persona(id, new ArrayList<>(), "Empresa S.A.", TipoPersona.JURIDICA);

    Correo correo = new Correo();
    correo.setDireccionCorreo("contacto@empresa.com");
    correo.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(correo);

    PersonaReplicaDTO replica = mapper.toReplicaDTO(persona);

    assertNotNull(replica);
    assertEquals(id, replica.id());
    assertEquals("Empresa S.A.", replica.denominacion());
    assertEquals(TipoPersona.JURIDICA, replica.tipoPersona());
    assertEquals(1, replica.mediosDeContacto().size());
    assertEquals("CORREO", replica.mediosDeContacto().get(0).tipo());
  }

  @Test
  void toReplicaDTO_conEventoPersonaSincronizada_deberiaMapearCorrectamente() {
    UUID id = UUID.randomUUID();
    grupo5.notificaciones.dto.input.MedioDeContactoEventoDTO medio =
        new grupo5.notificaciones.dto.input.MedioDeContactoEventoDTO(
            "CORREO", true, "contacto@empresa.com", null, null, null);
    grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1 evento =
        new grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1(
            id, "Empresa S.A.", "JURIDICA", List.of(medio));

    PersonaReplicaDTO replica = mapper.toReplicaDTO(evento);

    assertNotNull(replica);
    assertEquals(id, replica.id());
    assertEquals("Empresa S.A.", replica.denominacion());
    assertEquals(TipoPersona.JURIDICA, replica.tipoPersona());
    assertEquals(1, replica.mediosDeContacto().size());
    assertEquals("CORREO", replica.mediosDeContacto().get(0).tipo());
  }

  @Test
  void
      toReplicaDTO_conEventoPersonaSincronizada_conTipoInvalido_deberiaLanzarValidationException() {
    UUID id = UUID.randomUUID();
    grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1 evento =
        new grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1(
            id, "Nombre", "TIPO_INVENTADO", List.of());

    assertThrows(
        grupo5.common.exceptions.ValidationException.class, () -> mapper.toReplicaDTO(evento));
  }

  @Test
  void toReplicaDTO_conEventoNulo_deberiaRetornarNull() {
    assertNull(
        mapper.toReplicaDTO((grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1) null));
  }
}
