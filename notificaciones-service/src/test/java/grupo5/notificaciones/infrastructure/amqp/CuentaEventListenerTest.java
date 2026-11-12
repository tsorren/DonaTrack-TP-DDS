package grupo5.notificaciones.infrastructure.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import grupo5.notificaciones.infrastructure.CorreoAdapter;
import grupo5.notificaciones.services.impl.EnlaceDeCuentaService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class CuentaEventListenerTest {

  private static final String PREFIJO = "http://localhost:8180/realms/donatrack/login-actions/";

  private final List<String> destinatarios = new ArrayList<>();
  private final List<String> trazasVistas = new ArrayList<>();
  private boolean correoAcepta = true;

  private final CorreoAdapter correo =
      (destinatario, mensaje) -> {
        destinatarios.add(destinatario);
        trazasVistas.add(String.valueOf(MDC.get("traceId")));
        return correoAcepta;
      };

  private final CuentaEventListener listener =
      new CuentaEventListener(new EnlaceDeCuentaService(correo, null, PREFIJO));

  private static EventoCuentaEnlaceSolicitadoV1 evento() {
    return new EventoCuentaEnlaceSolicitadoV1(
        EventoCuentaEnlaceSolicitadoV1.RECUPERAR_CLAVE,
        UUID.randomUUID(),
        "ana@example.com",
        PREFIJO + "action-token?key=abc",
        30,
        "es",
        Instant.now());
  }

  @Test
  void delegaEnElServicioQueMandaElCorreo() {
    listener.onEnlaceSolicitado(evento(), UUID.randomUUID().toString(), "a1b2c3");

    assertEquals(List.of("ana@example.com"), destinatarios);
  }

  @Test
  void restableceElTraceIdMientrasProcesaYLoLimpiaDespues() {
    listener.onEnlaceSolicitado(evento(), UUID.randomUUID().toString(), "a1b2c3");

    assertEquals(List.of("a1b2c3"), trazasVistas);
    assertNull(MDC.get("traceId"));
  }

  @Test
  void sinTraceIdNoInventaUno() {
    listener.onEnlaceSolicitado(evento(), UUID.randomUUID().toString(), null);
    listener.onEnlaceSolicitado(evento(), UUID.randomUUID().toString(), "  ");

    assertEquals(List.of("null", "null"), trazasVistas);
  }

  @Test
  void limpiaElTraceIdAunqueElProcesamientoFalle() {
    correoAcepta = false;
    EventoCuentaEnlaceSolicitadoV1 evento = evento();
    String messageId = UUID.randomUUID().toString();

    assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> listener.onEnlaceSolicitado(evento, messageId, "a1b2c3"));

    assertNull(MDC.get("traceId"));
  }
}
