package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.mediosDeContacto.CorreoInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.CorreoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.MedioDeContactoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.TelefonoInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.TelefonoOutputDTO;
import grupo5.donaciones.dto.mediosDeContacto.WhatsAppInputDTO;
import grupo5.donaciones.dto.mediosDeContacto.WhatsAppOutputDTO;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.MedioDeContacto;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedioDeContactoMapperTest {

  private MedioDeContactoMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new MedioDeContactoMapper();
  }

  @Test
  void toEntity_conCorreoInput_deberiaMapearCorrectamente() {
    CorreoInputDTO dto = new CorreoInputDTO(true, "test@example.com");

    MedioDeContacto entity = mapper.toEntity(dto);

    assertNotNull(entity);
    assertTrue(entity instanceof Correo);
    Correo correo = (Correo) entity;
    assertTrue(correo.getEsPredeterminado());
    assertEquals("test@example.com", correo.getDireccionCorreo());
  }

  @Test
  void toEntity_conTelefonoInput_deberiaMapearCorrectamente() {
    TelefonoInputDTO dto = new TelefonoInputDTO(false, "+54", "11", "12345678");

    MedioDeContacto entity = mapper.toEntity(dto);

    assertNotNull(entity);
    assertTrue(entity instanceof Telefono);
    Telefono telefono = (Telefono) entity;
    assertFalse(telefono.getEsPredeterminado());
    assertEquals("+54", telefono.getCaracteristica());
    assertEquals("11", telefono.getCodigoArea());
    assertEquals("12345678", telefono.getNumero());
    assertEquals(TipoTelefono.ESTANDAR, telefono.getTipo());
  }

  @Test
  void toEntity_conWhatsAppInput_deberiaMapearCorrectamente() {
    WhatsAppInputDTO dto = new WhatsAppInputDTO(true, "+54", "9", "1187654321");

    MedioDeContacto entity = mapper.toEntity(dto);

    assertNotNull(entity);
    assertTrue(entity instanceof Telefono);
    Telefono telefono = (Telefono) entity;
    assertTrue(telefono.getEsPredeterminado());
    assertEquals("+54", telefono.getCaracteristica());
    assertEquals("9", telefono.getCodigoArea());
    assertEquals("1187654321", telefono.getNumero());
    assertEquals(TipoTelefono.WHATSAPP, telefono.getTipo());
  }

  @Test
  void toEntity_conNull_deberiaRetornarNull() {
    assertNull(mapper.toEntity(null));
  }

  @Test
  void toOutputDTO_conCorreo_deberiaMapearCorrectamente() {
    Correo correo = new Correo();
    correo.setDireccionCorreo("out@example.com");
    correo.setEsPredeterminado(true);

    MedioDeContactoOutputDTO dto = mapper.toOutputDTO(correo);

    assertNotNull(dto);
    assertTrue(dto instanceof CorreoOutputDTO);
    CorreoOutputDTO correoDTO = (CorreoOutputDTO) dto;
    assertTrue(correoDTO.esPredeterminado());
    assertEquals("out@example.com", correoDTO.direccionCorreo());
  }

  @Test
  void toOutputDTO_conTelefono_deberiaMapearCorrectamente() {
    Telefono telefono = new Telefono();
    telefono.setCaracteristica("+1");
    telefono.setCodigoArea("305");
    telefono.setNumero("5550199");
    telefono.setEsPredeterminado(false);
    telefono.setTipo(TipoTelefono.ESTANDAR);

    MedioDeContactoOutputDTO dto = mapper.toOutputDTO(telefono);

    assertNotNull(dto);
    assertTrue(dto instanceof TelefonoOutputDTO);
    TelefonoOutputDTO telDTO = (TelefonoOutputDTO) dto;
    assertFalse(telDTO.esPredeterminado());
    assertEquals("+1", telDTO.caracteristica());
    assertEquals("305", telDTO.codigoArea());
    assertEquals("5550199", telDTO.numero());
  }

  @Test
  void toOutputDTO_conWhatsApp_deberiaMapearCorrectamente() {
    Telefono whatsapp = new Telefono();
    whatsapp.setCaracteristica("+55");
    whatsapp.setCodigoArea("11");
    whatsapp.setNumero("999998888");
    whatsapp.setEsPredeterminado(true);
    whatsapp.setTipo(TipoTelefono.WHATSAPP);

    MedioDeContactoOutputDTO dto = mapper.toOutputDTO(whatsapp);

    assertNotNull(dto);
    assertTrue(dto instanceof WhatsAppOutputDTO);
    WhatsAppOutputDTO waDTO = (WhatsAppOutputDTO) dto;
    assertTrue(waDTO.esPredeterminado());
    assertEquals("+55", waDTO.caracteristica());
    assertEquals("11", waDTO.codigoArea());
    assertEquals("999998888", waDTO.numero());
  }

  @Test
  void toOutputDTO_conNull_deberiaRetornarNull() {
    assertNull(mapper.toOutputDTO(null));
  }

  @Test
  void toOutputDTO_conTipoNoSoportado_deberiaLanzarExcepcion() {
    MedioDeContacto anonimo =
        new MedioDeContacto() {
          @Override
          public void anonimizar() {
            // No-op: clase anónima de prueba para simular tipo de medio de contacto no soportado
          }
        };

    assertThrows(IllegalArgumentException.class, () -> mapper.toOutputDTO(anonimo));
  }
}
