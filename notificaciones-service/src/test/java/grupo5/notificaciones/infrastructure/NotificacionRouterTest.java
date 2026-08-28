package grupo5.notificaciones.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.*;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionRouterTest {

  @Mock private CorreoAdapter correoApi;
  @Mock private TelefonoAdapter telefonoApi;
  @Mock private WhatsAppAdapter whatsappApi;

  @InjectMocks private NotificacionRouter router;

  private Persona persona;
  private Correo correo;
  private Telefono telefono;
  private Telefono whatsapp;

  @BeforeEach
  void setUp() {
    persona = new Persona(UUID.randomUUID(), new ArrayList<>(), "Donante Test", TipoPersona.HUMANA);

    correo = new Correo();
    correo.setDireccionCorreo("donante@test.com");
    correo.marcarComoPredeterminado();

    telefono = new Telefono();
    telefono.setCaracteristica("+54");
    telefono.setCodigoArea("11");
    telefono.setNumero("55556666");
    telefono.desmarcarComoPredeterminado();
    telefono.setTipo(TipoTelefono.ESTANDAR);

    whatsapp = new Telefono();
    whatsapp.setCaracteristica("+54");
    whatsapp.setCodigoArea("11");
    whatsapp.setNumero("99998888");
    whatsapp.desmarcarComoPredeterminado();
    whatsapp.setTipo(TipoTelefono.WHATSAPP);
  }

  @Test
  void notificar_cuandoCorreoEsPredeterminadoYApiRespondeTrue_estadoDeberiaSerEnviada() {
    persona.agregarMedioDeContacto(correo);
    when(correoApi.enviarMail(eq("donante@test.com"), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Tu donación fue asignada");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(correoApi, times(1)).enviarMail("donante@test.com", "Tu donación fue asignada");
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
    verify(whatsappApi, never()).enviarWhatsApp(anyString(), anyString());
  }

  @Test
  void notificar_cuandoTelefonoEsPredeterminadoYApiRespondeTrue_estadoDeberiaSerEnviada() {
    telefono.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(telefono);
    when(telefonoApi.enviarSms(eq("+541155556666"), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Tu donación fue asignada");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(telefonoApi, times(1)).enviarSms("+541155556666", "Tu donación fue asignada");
    verify(correoApi, never()).enviarMail(anyString(), anyString());
    verify(whatsappApi, never()).enviarWhatsApp(anyString(), anyString());
  }

  @Test
  void notificar_cuandoWhatsAppEsPredeterminadoYApiRespondeTrue_estadoDeberiaSerEnviada() {
    whatsapp.marcarComoPredeterminado();
    persona.agregarMedioDeContacto(whatsapp);
    when(whatsappApi.enviarWhatsApp(eq("+541199998888"), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Subiste de categoría");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(whatsappApi, times(1)).enviarWhatsApp("+541199998888", "Subiste de categoría");
    verify(correoApi, never()).enviarMail(anyString(), anyString());
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
  }

  @Test
  void notificar_cuandoCorreoPredeterminadoFalla_deberiaIntentarConElSiguienteMedio() {
    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(telefono);
    when(correoApi.enviarMail(anyString(), anyString())).thenReturn(false);
    when(telefonoApi.enviarSms(eq("+541155556666"), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Tu donación fue asignada");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(correoApi, times(1)).enviarMail(anyString(), anyString());
    verify(telefonoApi, times(1)).enviarSms(anyString(), anyString());
  }

  @Test
  void
      notificar_cuandoCorreoAgregadoUltimoYEsPredeterminado_deberiaUsarseAntesTelefonoAgregadoPrimero() {
    persona.agregarMedioDeContacto(telefono);
    persona.agregarMedioDeContacto(correo);
    when(correoApi.enviarMail(eq("donante@test.com"), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Mensaje de prueba");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
    verify(correoApi, times(1)).enviarMail(anyString(), anyString());
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
  }

  @Test
  void notificar_cuandoTodosLosMediosFallan_estadoDeberiaSerFallida() {
    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(telefono);
    when(correoApi.enviarMail(anyString(), anyString())).thenReturn(false);
    when(telefonoApi.enviarSms(anyString(), anyString())).thenReturn(false);

    Notificacion notificacion = new Notificacion(persona.getId(), "Mensaje que no llegará");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_cuandoApiLanzaExcepcion_deberiaContinuarConSiguienteMedio() {
    persona.agregarMedioDeContacto(correo);
    persona.agregarMedioDeContacto(telefono);
    when(correoApi.enviarMail(anyString(), anyString()))
        .thenThrow(new RuntimeException("SMTP timeout"));
    when(telefonoApi.enviarSms(anyString(), anyString())).thenReturn(true);

    Notificacion notificacion = new Notificacion(persona.getId(), "Mensaje con excepción");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstadoNotificacion());
  }

  @Test
  void notificar_cuandoPersonaSinMediosDeContacto_estadoDeberiaSerFallida() {
    Notificacion notificacion = new Notificacion(persona.getId(), "Sin destinatario");
    notificacion.notificar(persona, router);

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstadoNotificacion());
    verify(correoApi, never()).enviarMail(anyString(), anyString());
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
    verify(whatsappApi, never()).enviarWhatsApp(anyString(), anyString());
  }

  @Test
  void enviarA_correo_deberiaInvocarEnviarMailDelAdapter() {
    when(correoApi.enviarMail(anyString(), anyString())).thenReturn(true);

    router.enviarA(correo, "test double dispatch");

    verify(correoApi, times(1)).enviarMail("donante@test.com", "test double dispatch");
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
    verify(whatsappApi, never()).enviarWhatsApp(anyString(), anyString());
  }

  @Test
  void enviarA_telefono_deberiaInvocarEnviarSmsDelAdapter() {
    when(telefonoApi.enviarSms(anyString(), anyString())).thenReturn(true);

    router.enviarA(telefono, "test sms dispatch");

    verify(telefonoApi, times(1)).enviarSms("+541155556666", "test sms dispatch");
    verify(correoApi, never()).enviarMail(anyString(), anyString());
    verify(whatsappApi, never()).enviarWhatsApp(anyString(), anyString());
  }

  @Test
  void enviarA_whatsapp_deberiaInvocarEnviarWhatsAppDelAdapter() {
    when(whatsappApi.enviarWhatsApp(anyString(), anyString())).thenReturn(true);

    router.enviarA(whatsapp, "test whatsapp dispatch");

    verify(whatsappApi, times(1)).enviarWhatsApp("+541199998888", "test whatsapp dispatch");
    verify(correoApi, never()).enviarMail(anyString(), anyString());
    verify(telefonoApi, never()).enviarSms(anyString(), anyString());
  }
}
