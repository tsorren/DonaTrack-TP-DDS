package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventosNotificablesTest {
  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JUNE, 18, 12, 0, 0);

  @Test
  void subioCategoria_deberiaGenerarMensajeCorrecto() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);

    SubioCategoria evento = new SubioCategoria(persona, "Bronce", "Platino", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals("Subiste a la categoría Platino", notificaciones.getFirst().getMensaje());
  }

  @Test
  void misionCumplida_deberiaGenerarMensajeCorrecto() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);

    MisionCumplida evento =
        new MisionCumplida(persona, "Donador Frecuente", "100 puntos", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals(
        "Completaste la misión Donador Frecuente. Recompensa: 100 puntos",
        notificaciones.getFirst().getMensaje());
  }

  @Test
  void generarNotificaciones_deberiaCrearMensajeDeBienvenida() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);
    DonanteRegistrado evento = new DonanteRegistrado(persona, "usuario: Usuario", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals("Bienvenido a DonaTrack usuario: Usuario", notificaciones.getFirst().getMensaje());

    assertEquals(persona.getId(), notificaciones.getFirst().getPersonaId());
  }

  @Test
  void generarNotificaciones_deberiaCrearNotificacionesParaDonanteYBeneficiario() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);

    DonacionAsignada evento =
        new DonacionAsignada(donante, beneficiario, "10 cajas de leche", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());

    assertEquals(
        "Tu donación de 10 cajas de leche fue asignada a Comedor Esperanza",
        notificaciones.get(0).getMensaje());

    assertEquals(
        "Se te ha asignado la donación del donante Juan. Contiene: 10 cajas de leche",
        notificaciones.get(1).getMensaje());
  }

  @Test
  void generarNotificaciones_deberiaCrearMensajesDeDonacionRecibida() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);

    DonacionRecibida evento =
        new DonacionRecibida(donante, beneficiario, "ropa", "AB123CD", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());

    assertTrue(
        notificaciones
            .get(0)
            .getMensaje()
            .startsWith("¡Gracias! Comedor Esperanza recibió tu donación de ropa"));
    assertTrue(notificaciones.get(0).getMensaje().contains("AB123CD"));

    assertTrue(
        notificaciones.get(1).getMensaje().startsWith("Ya llegó la donación ropa a tu sede"));
    assertTrue(notificaciones.get(1).getMensaje().contains("AB123CD"));
  }

  @Test
  void generarNotificaciones_deberiaCrearMensajeConEnlaceEnDonacionEnCamino() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);

    DonacionEnCamino evento =
        new DonacionEnCamino(
            donante, beneficiario, "ropa", "https://donatrack.app/mapa/123", TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());
    assertTrue(notificaciones.get(0).getMensaje().contains("https://donatrack.app/mapa/123"));
    assertTrue(notificaciones.get(1).getMensaje().contains("https://donatrack.app/mapa/123"));
  }

  @Test
  void generarNotificaciones_deberiaNotificarATresDestinatariosEnEntregaFallida() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);
    Persona admin =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Administración", TipoPersona.HUMANA);

    EntregaFallida evento =
        new EntregaFallida(
            donante, beneficiario, admin, "ropa", "Nadie respondió", true, TEST_DATE_TIME);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(3, notificaciones.size());
    assertEquals(donante.getId(), notificaciones.get(0).getPersonaId());
    assertEquals(beneficiario.getId(), notificaciones.get(1).getPersonaId());
    assertEquals(admin.getId(), notificaciones.get(2).getPersonaId());
  }
}
