package grupo5.notificaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.MedioDeContactoReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.MedioDeContacto;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.ports.NotificacionSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Cubre {@code MedioDeContactoMapper} de punta a punta (extendido en Oleada 8): la traducción de
 * esPredeterminado (Boolean, nullable en el DTO) a los métodos semánticos de MedioDeContacto
 * (Oleada 1, RF-01), las excepciones de tipo no soportado (Oleada 3, RF-05), y — cerrado en esta
 * oleada, hueco real detectado al revisar el archivo — los caminos felices de TELEFONO/WHATSAPP en
 * toEntity() y de los 3 casos de toReplicaDTO(), que no tenían ningún test dedicado.
 */
class MedioDeContactoMapperTest {

  private MedioDeContactoMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new MedioDeContactoMapper();
  }

  @Test
  void toEntity_conEsPredeterminadoTrue_deberiaQuedarMarcado() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("CORREO", true, "test@test.com", null, null, null);

    MedioDeContacto medio = mapper.toEntity(dto);

    assertTrue(medio.getEsPredeterminado());
  }

  @Test
  void toEntity_conEsPredeterminadoFalse_deberiaQuedarDesmarcado() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("CORREO", false, "test@test.com", null, null, null);

    MedioDeContacto medio = mapper.toEntity(dto);

    assertFalse(medio.getEsPredeterminado());
  }

  @Test
  void toEntity_conEsPredeterminadoNull_deberiaQuedarDesmarcado() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("CORREO", null, "test@test.com", null, null, null);

    MedioDeContacto medio = mapper.toEntity(dto);

    assertFalse(medio.getEsPredeterminado());
  }

  @Test
  void toEntity_conTipoTelefono_deberiaMapearCamposYQuedarEstandar() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("TELEFONO", true, null, "+54", "11", "12345678");

    MedioDeContacto medio = mapper.toEntity(dto);

    Telefono telefono = assertInstanceOf(Telefono.class, medio);
    assertEquals("+54", telefono.getCaracteristica());
    assertEquals("11", telefono.getCodigoArea());
    assertEquals("12345678", telefono.getNumero());
    assertEquals(TipoTelefono.ESTANDAR, telefono.getTipo());
    assertTrue(telefono.getEsPredeterminado());
  }

  @Test
  void toEntity_conTipoWhatsapp_deberiaMapearCamposYQuedarWhatsapp() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("WHATSAPP", false, null, "+54", "11", "87654321");

    MedioDeContacto medio = mapper.toEntity(dto);

    Telefono telefono = assertInstanceOf(Telefono.class, medio);
    assertEquals(TipoTelefono.WHATSAPP, telefono.getTipo());
    assertFalse(telefono.getEsPredeterminado());
  }

  @Test
  void toEntity_esInsensibleAMayusculasEnElTipo() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("correo", true, "test@test.com", null, null, null);

    assertInstanceOf(Correo.class, mapper.toEntity(dto));
  }

  @Test
  void toReplicaDTO_conCorreo_deberiaMapearCorrectamente() {
    Correo correo = new Correo();
    correo.setDireccionCorreo("test@test.com");
    correo.marcarComoPredeterminado();

    MedioDeContactoReplicaDTO dto = mapper.toReplicaDTO(correo);

    assertEquals("CORREO", dto.tipo());
    assertEquals("test@test.com", dto.direccionCorreo());
    assertTrue(dto.esPredeterminado());
  }

  @Test
  void toReplicaDTO_conTelefonoEstandar_deberiaMapearCorrectamente() {
    Telefono telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");
    telefono.setTipo(TipoTelefono.ESTANDAR);

    MedioDeContactoReplicaDTO dto = mapper.toReplicaDTO(telefono);

    assertEquals("TELEFONO", dto.tipo());
    assertEquals("+54", dto.caracteristica());
    assertEquals("11", dto.codigoArea());
    assertEquals("12345678", dto.numero());
  }

  @Test
  void toReplicaDTO_conTelefonoWhatsapp_deberiaMapearComoWhatsapp() {
    Telefono whatsapp = new Telefono();
    whatsapp.setCaracteristica("+54");
    whatsapp.setCodigoArea("11");
    whatsapp.setNumero("87654321");
    whatsapp.setTipo(TipoTelefono.WHATSAPP);

    MedioDeContactoReplicaDTO dto = mapper.toReplicaDTO(whatsapp);

    assertEquals("WHATSAPP", dto.tipo());
  }

  @Test
  void toEntity_conDtoNulo_deberiaDevolverNull() {
    assertNull(mapper.toEntity(null));
  }

  @Test
  void toReplicaDTO_conEntidadNula_deberiaDevolverNull() {
    assertNull(mapper.toReplicaDTO(null));
  }

  @Test
  void toEntity_conTipoNoSoportado_deberiaLanzarValidationException() {
    MedioDeContactoReplicaDTO dto =
        new MedioDeContactoReplicaDTO("FAX", true, null, null, null, null);

    assertThrows(ValidationException.class, () -> mapper.toEntity(dto));
  }

  @Test
  void toReplicaDTO_conTipoNoSoportado_deberiaLanzarValidationException() {
    MedioDeContacto medioNoSoportado =
        new MedioDeContacto() {
          @Override
          public boolean enviarMensaje(String mensaje, NotificacionSender sender) {
            return false;
          }

          @Override
          public void anonimizar() {}
        };

    assertThrows(ValidationException.class, () -> mapper.toReplicaDTO(medioNoSoportado));
  }
}
