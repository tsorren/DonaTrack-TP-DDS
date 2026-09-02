package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.mothers.EventoNotificableMother;
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

    assertEquals(
        "¡Felicitaciones! Has ascendido de la categoría Bronce a Platino.",
        notificaciones.getFirst().getMensaje());
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

  // Oleada 8: EntregaFallida.generarNotificaciones() con y sin replanificable, confirmando el
  // mensaje al admin — antes solo alcanzable indirectamente vía armarMensajeAdmin() (privado).

  @Test
  void entregaFallida_conReplanificableTrue_elMensajeAlAdminDeberiaDecirQueSiSeReplanifico() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);
    Persona admin =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Administración", TipoPersona.HUMANA);

    EntregaFallida evento =
        EventoNotificableMother.entregaFallida(donante, beneficiario, admin, true);

    List<Notificacion> notificaciones = evento.generarNotificaciones();
    String mensajeAdmin = notificaciones.get(2).getMensaje();

    assertTrue(mensajeAdmin.contains("donante: Juan"));
    assertTrue(mensajeAdmin.contains("entidad: Comedor Esperanza"));
    assertTrue(mensajeAdmin.contains("Motivo: Nadie respondió"));
    assertTrue(mensajeAdmin.contains("Replanificada: sí"));
  }

  @Test
  void entregaFallida_conReplanificableFalse_elMensajeAlAdminDeberiaDecirQueNoSeReplanifico() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);
    Persona admin =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Administración", TipoPersona.HUMANA);

    EntregaFallida evento =
        EventoNotificableMother.entregaFallida(donante, beneficiario, admin, false);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertTrue(notificaciones.get(2).getMensaje().contains("Replanificada: no"));
    // Los mensajes a donante y beneficiario también dependen de replanificable — se confirman
    // acá para no dejar ese costado sin cubrir.
    assertTrue(notificaciones.get(0).getMensaje().endsWith("Nadie respondió."));
    assertTrue(notificaciones.get(1).getMensaje().endsWith("Nadie respondió."));
  }

  // Oleada 3 (RF-06): guardas de obligatoriedad de los nuevos constructores protegidos de
  // EventoNotificable/EventoDeDonacion, ejercitadas a través de una subclase concreta de cada uno.

  @Test
  void constructor_conPersonaNula_deberiaLanzarValidationException() {
    assertThrows(
        ValidationException.class,
        () -> new SubioCategoria(null, "Bronce", "Platino", TEST_DATE_TIME));
  }

  @Test
  void constructor_conFechaNula_deberiaLanzarValidationException() {
    Persona persona =
        new Persona(UUID.randomUUID(), new ArrayList<>(), "Persona", TipoPersona.HUMANA);

    assertThrows(
        ValidationException.class, () -> new SubioCategoria(persona, "Bronce", "Platino", null));
  }

  @Test
  void constructorEventoDeDonacion_conEntidadBeneficiariaNula_deberiaLanzarValidationException() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);

    assertThrows(
        ValidationException.class,
        () -> new DonacionAsignada(donante, null, "10 cajas de leche", TEST_DATE_TIME));
  }

  @Test
  void constructorEventoDeDonacion_conDetalleDonacionNulo_deberiaLanzarValidationException() {
    Persona donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    Persona beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);

    assertThrows(
        ValidationException.class,
        () -> new DonacionAsignada(donante, beneficiario, null, TEST_DATE_TIME));
  }
}
