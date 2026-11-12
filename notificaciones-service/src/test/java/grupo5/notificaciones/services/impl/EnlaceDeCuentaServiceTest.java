package grupo5.notificaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import grupo5.notificaciones.infrastructure.CorreoAdapter;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.jdbc.core.JdbcTemplate;

class EnlaceDeCuentaServiceTest {

  private static final String PREFIJO = "http://localhost:8180/realms/donatrack/login-actions/";
  private static final String ENLACE = PREFIJO + "action-token?key=abc";

  private final List<String> destinatarios = new ArrayList<>();
  private final List<String> mensajes = new ArrayList<>();
  private final List<Object[]> consultasAlInbox = new ArrayList<>();
  private boolean correoAcepta = true;
  private int filasDelInbox = 1;

  private final CorreoAdapter correo =
      (destinatario, mensaje) -> {
        destinatarios.add(destinatario);
        mensajes.add(mensaje);
        return correoAcepta;
      };

  private final JdbcTemplate inbox =
      new JdbcTemplate() {
        @Override
        public int update(String sql, Object... args) {
          consultasAlInbox.add(args);
          return filasDelInbox;
        }
      };

  private final EnlaceDeCuentaService servicio = new EnlaceDeCuentaService(correo, inbox, PREFIJO);

  private static EventoCuentaEnlaceSolicitadoV1 evento(String enlace) {
    return new EventoCuentaEnlaceSolicitadoV1(
        EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE,
        UUID.randomUUID(),
        "ana@example.com",
        enlace,
        30,
        "es",
        Instant.now());
  }

  @Test
  void mandaElCorreoAlDestinatarioConElEnlace() {
    servicio.procesar(evento(ENLACE), UUID.randomUUID().toString());

    assertEquals(List.of("ana@example.com"), destinatarios);
    assertTrue(mensajes.get(0).contains(ENLACE), mensajes.get(0));
  }

  @Test
  void unEnlaceQueNoEsDeKeycloakSeRechazaSinReencolarYNoSeManda() {
    for (String ajeno :
        new String[] {
          "https://phishing.example.com/login-actions/x",
          "http://localhost:8180@evil.example.com/realms/donatrack/login-actions/x",
          "http://localhost:8180/realms/otro/login-actions/x",
          "http://localhost:81800/realms/donatrack/login-actions/x",
          "https://phishing.example.com/?volver=" + PREFIJO + "x",
          "https://phishing.example.com/" + PREFIJO + "x"
        }) {
      assertThrows(
          AmqpRejectAndDontRequeueException.class,
          () -> servicio.procesar(evento(ajeno), UUID.randomUUID().toString()),
          ajeno);
    }
    assertEquals(0, destinatarios.size());
    assertEquals(0, consultasAlInbox.size(), "ni siquiera toca el inbox");
  }

  @Test
  void siElCorreoSeRechazaVaALaColaDeMuertosSinReencolar() {
    correoAcepta = false;

    assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> servicio.procesar(evento(ENLACE), UUID.randomUUID().toString()));
  }

  @Test
  void unMensajeDuplicadoNoMandaOtroCorreo() {
    filasDelInbox = 0;

    servicio.procesar(evento(ENLACE), UUID.randomUUID().toString());

    assertEquals(0, destinatarios.size());
  }

  @Test
  void laClaveDelInboxEsElMessageId() {
    UUID messageId = UUID.randomUUID();

    servicio.procesar(evento(ENLACE), messageId.toString());

    assertEquals(messageId, consultasAlInbox.get(0)[0]);
  }

  @Test
  void unMessageIdQueNoEsUnUuidDaSiempreLaMismaClave() {
    servicio.procesar(evento(ENLACE), "id-raro");
    servicio.procesar(evento(ENLACE), "id-raro");

    assertEquals(consultasAlInbox.get(0)[0], consultasAlInbox.get(1)[0]);
  }

  @Test
  void sinMessageIdSeProcesaSinDeduplicar() {
    servicio.procesar(evento(ENLACE), null);
    servicio.procesar(evento(ENLACE), " ");

    assertEquals(2, destinatarios.size());
    assertEquals(0, consultasAlInbox.size());
  }

  @Test
  void sinBaseDeDatosNoHayInboxPeroSeManda() {
    EnlaceDeCuentaService enMemoria = new EnlaceDeCuentaService(correo, null, PREFIJO);

    enMemoria.procesar(evento(ENLACE), UUID.randomUUID().toString());

    assertEquals(1, destinatarios.size());
  }

  @Test
  void elServicioNoTieneNingunRepositorioParaGuardarElEnlace() {
    // El enlace da acceso a la cuenta y el historial de notificaciones lo puede leer un admin: este
    // servicio no puede guardar nada porque no tiene dónde.
    for (Field campo : EnlaceDeCuentaService.class.getDeclaredFields()) {
      String tipo = campo.getType().getSimpleName();
      assertFalse(tipo.contains("Repository"), "no debe tener repositorios: " + tipo);
      assertFalse(tipo.contains("EntityManager"), "no debe tener repositorios: " + tipo);
    }
  }
}
