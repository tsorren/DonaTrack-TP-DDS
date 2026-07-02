package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Telefono;
import grupo5.donaciones.models.entities.personas.TipoTelefono;
import grupo5.donaciones.models.privacidad.Anonimizable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedioDeContactoTest {

  private Correo correo;
  private Telefono telefono;
  private Telefono whatsApp;

  @BeforeEach
  void setUp() {
    correo = new Correo();
    correo.setDireccionCorreo("test@example.com");

    telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");
    telefono.setTipo(TipoTelefono.ESTANDAR);

    whatsApp = new Telefono();
    whatsApp.setCaracteristica("+54");
    whatsApp.setCodigoArea("11");
    whatsApp.setNumero("87654321");
    whatsApp.setTipo(TipoTelefono.WHATSAPP);
  }

  @Test
  void testTelefonoNumeroCompleto() {
    assertEquals("+541112345678", telefono.obtenerNumeroCompleto());
  }

  @Test
  void testTelefonoAnonimizar() {
    telefono.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, telefono.getCaracteristica());
    assertEquals(Anonimizable.VALOR_STRING, telefono.getCodigoArea());
    assertEquals(Anonimizable.VALOR_STRING, telefono.getNumero());
  }

  @Test
  void testCorreoAnonimizar() {
    correo.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, correo.getDireccionCorreo());
  }

  @Test
  void testWhatsAppNumeroCompleto() {
    assertEquals("+541187654321", whatsApp.obtenerNumeroCompleto());
  }

  @Test
  void testWhatsAppAnonimizar() {
    whatsApp.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, whatsApp.getCaracteristica());
    assertEquals(Anonimizable.VALOR_STRING, whatsApp.getCodigoArea());
    assertEquals(Anonimizable.VALOR_STRING, whatsApp.getNumero());
  }

  @Test
  void testEsPredeterminadoDefault() {
    assertFalse(correo.getEsPredeterminado());
    assertFalse(telefono.getEsPredeterminado());
    assertFalse(whatsApp.getEsPredeterminado());
  }

  @Test
  void testSetEsPredeterminado() {
    correo.setEsPredeterminado(true);
    assertTrue(correo.getEsPredeterminado());
  }
}
