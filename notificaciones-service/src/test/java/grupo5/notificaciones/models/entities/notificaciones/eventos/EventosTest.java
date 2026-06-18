package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventosNotificablesTest {

  @Test
  void subioCategoria_deberiaGenerarMensajeCorrecto() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);

    SubioCategoria evento = new SubioCategoria(persona, "Bronce", "Platino", LocalDateTime.now());

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals("Subiste a la categoría Platino", notificaciones.getFirst().getMensaje());
  }

  @Test
  void misionCumplida_deberiaGenerarMensajeCorrecto() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);

    MisionCumplida evento =
        new MisionCumplida(persona, "Donador Frecuente", "100 puntos", LocalDateTime.now());

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
    DonanteRegistrado evento =
        new DonanteRegistrado(persona, "usuario: Usuario", LocalDateTime.now());

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals(
        "Bienvenido a DonaTrack\nusuario: Usuario", notificaciones.getFirst().getMensaje());

    assertEquals(persona, notificaciones.getFirst().getPersona());
  }

  @Test
  void generarNotificaciones_deberiaCrearNotificacionesParaDonanteYBeneficiario() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);

    DonacionAsignada evento =
        new DonacionAsignada(donante, beneficiario, "10 cajas de leche", LocalDateTime.now());

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
        new DonacionRecibida(donante, beneficiario, "ropa", LocalDateTime.now());

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());

    assertEquals(
        "¡Gracias! Comedor Esperanza recibió tu donación de ropa",
        notificaciones.get(0).getMensaje());

    assertEquals("Ya llegó la donación ropa a tu sede", notificaciones.get(1).getMensaje());
  }
}
