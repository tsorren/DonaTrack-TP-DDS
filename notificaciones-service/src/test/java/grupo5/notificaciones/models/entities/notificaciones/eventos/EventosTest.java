package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.persona.Persona;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventosNotificablesTest {

  @Test
  void subioCategoria_deberiaGenerarMensajeCorrecto() {
    Persona persona = new Persona();
    persona.setDenominacion("Persona");

    SubioCategoria evento = new SubioCategoria();
    evento.setPersona(persona);
    evento.setCategoria("Platino");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals("Subiste a la categoría Platino", notificaciones.getFirst().getMensaje());
  }

  @Test
  void misionCumplida_deberiaGenerarMensajeCorrecto() {
    Persona persona = new Persona();

    MisionCumplida evento = new MisionCumplida();

    evento.setPersona(persona);
    evento.setNombreMision("Donador Frecuente");
    evento.setRecompensa("100 puntos");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals(
        "Completaste la misión Donador Frecuente. Recompensa: 100 puntos",
        notificaciones.getFirst().getMensaje());
  }

  @Test
  void generarNotificaciones_deberiaCrearMensajeDeBienvenida() {
    Persona persona = new Persona();
    DonanteRegistrado evento = new DonanteRegistrado();

    evento.setPersona(persona);
    evento.setBienvenida("Bienvenido a DonaTrack");
    evento.setCredencialesDeAcceso("usuario: Usuario");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(1, notificaciones.size());

    assertEquals(
        "Bienvenido a DonaTrack\nusuario: Usuario", notificaciones.getFirst().getMensaje());

    assertEquals(persona, notificaciones.getFirst().getPersona());
  }

  @Test
  void generarNotificaciones_deberiaCrearNotificacionesParaDonanteYBeneficiario() {
    Persona donante = new Persona();
    donante.setDenominacion("Juan");

    Persona beneficiario = new Persona();
    beneficiario.setDenominacion("Comedor Esperanza");

    DonacionAsignada evento = new DonacionAsignada();

    evento.setPersona(donante);
    evento.setEntidadBeneficiaria(beneficiario);
    evento.setDetalleDonacion("10 cajas de leche");

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
    Persona donante = new Persona();
    donante.setDenominacion("Juan");

    Persona beneficiario = new Persona();
    beneficiario.setDenominacion("Comedor Esperanza");

    DonacionRecibida evento = new DonacionRecibida();

    evento.setPersona(donante);
    evento.setEntidadBeneficiaria(beneficiario);
    evento.setDetalleDonacion("ropa");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());

    assertEquals(
        "¡Gracias! Comedor Esperanza recibió tu donación de ropa",
        notificaciones.get(0).getMensaje());

    assertEquals("Ya llegó la donación ropa a tu sede", notificaciones.get(1).getMensaje());
  }
}
