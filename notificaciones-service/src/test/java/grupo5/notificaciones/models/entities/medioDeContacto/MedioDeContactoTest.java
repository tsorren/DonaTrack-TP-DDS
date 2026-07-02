package grupo5.notificaciones.models.entities.medioDeContacto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import grupo5.notificaciones.models.entities.personas.Correo;
import grupo5.notificaciones.models.entities.personas.Telefono;
import grupo5.notificaciones.models.entities.personas.TipoTelefono;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MedioDeContactoTest {

  @Mock private NotificacionSender sender;

  private Correo correo;
  private Telefono telefono;
  private Telefono whatsapp;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    correo = new Correo();
    correo.setDireccionCorreo("test@mail.com");

    telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("12345678");
    telefono.setTipo(TipoTelefono.ESTANDAR);

    whatsapp = new Telefono();
    whatsapp.setCaracteristica("+54");
    whatsapp.setCodigoArea("11");
    whatsapp.setNumero("87654321");
    whatsapp.setTipo(TipoTelefono.WHATSAPP);
  }

  @Test
  void correo_enviarMensaje_deberiaUsarSenderCorreo() {
    when(sender.enviarA(correo, "hola")).thenReturn(true);

    boolean resultado = correo.enviarMensaje("hola", sender);

    assertTrue(resultado);

    verify(sender, times(1)).enviarA(correo, "hola");
  }

  @Test
  void telefono_enviarMensaje_deberiaUsarSenderTelefono() {
    when(sender.enviarA(telefono, "mensaje sms")).thenReturn(true);

    boolean resultado = telefono.enviarMensaje("mensaje sms", sender);

    assertTrue(resultado);

    verify(sender, times(1)).enviarA(telefono, "mensaje sms");
  }

  @Test
  void whatsapp_enviarMensaje_deberiaUsarSenderWhatsapp() {
    when(sender.enviarA(whatsapp, "mensaje whatsapp")).thenReturn(true);

    boolean resultado = whatsapp.enviarMensaje("mensaje whatsapp", sender);

    assertTrue(resultado);

    verify(sender, times(1)).enviarA(whatsapp, "mensaje whatsapp");
  }

  @Test
  void telefono_obtenerNumeroCompleto_deberiaConcatenarCorrectamente() {
    String numeroCompleto = telefono.obtenerNumeroCompleto();

    assertEquals("+541112345678", numeroCompleto);
  }

  @Test
  void whatsapp_obtenerNumeroCompleto_deberiaConcatenarCorrectamente() {
    String numeroCompleto = whatsapp.obtenerNumeroCompleto();

    assertEquals("+541187654321", numeroCompleto);
  }

  @Test
  void correo_anonimizar_deberiaFuncionarCorrectamente() {
    correo.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, correo.getDireccionCorreo());
  }

  @Test
  void telefono_anonimizar_deberiaFuncionarCorrectamente() {
    telefono.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, telefono.getCaracteristica());
    assertEquals(Anonimizable.VALOR_STRING, telefono.getCodigoArea());
    assertEquals(Anonimizable.VALOR_STRING, telefono.getNumero());
  }

  @Test
  void whatsapp_anonimizar_deberiaFuncionarCorrectamente() {
    whatsapp.anonimizar();
    assertEquals(Anonimizable.VALOR_STRING, whatsapp.getCaracteristica());
    assertEquals(Anonimizable.VALOR_STRING, whatsapp.getCodigoArea());
    assertEquals(Anonimizable.VALOR_STRING, whatsapp.getNumero());
  }
}
