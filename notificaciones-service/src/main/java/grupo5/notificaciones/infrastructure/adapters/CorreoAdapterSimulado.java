package grupo5.notificaciones.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.notificaciones.infrastructure.CorreoAdapter;
import grupo5.notificaciones.infrastructure.adapters.dtos.sendgrid.SendGridEmailRequest;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de correos electrónicos. Aplica {@link CriterioFalloSimulado}
 * para admitir escenarios de fallo controlado.
 */
@Component
public class CorreoAdapterSimulado implements CorreoAdapter {
  private static final Logger log = LoggerFactory.getLogger(CorreoAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public CorreoAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarMail(String destinatario, String mensaje) {
    simularLatenciaDeRed();

    // 1. Falla permanente (HTTP 400) evaluada por la política original
    if (criterioFallo.debeFallar(destinatario, mensaje)) {
      log.warn("[SENDGRID-MOCK] Rechazo de la API: Destinatario inválido o bloqueado.");
      return false;
    }

    // 2. Falla temporal (HTTP 500 / Timeout) simulada aleatoriamente (5% de las veces)
    if (simularFalloTemporalAleatorio()) {
      throw new RuntimeException(
          "HTTP 503 Service Unavailable: Timeout conectando con el proveedor.");
    }

    String messageId = java.util.UUID.randomUUID().toString();

    SendGridEmailRequest requestDTO =
        new SendGridEmailRequest(
            List.of(
                new SendGridEmailRequest.Personalization(
                    List.of(new SendGridEmailRequest.EmailAddress(destinatario, null)))),
            new SendGridEmailRequest.EmailAddress("no-reply@donatrack.org", "DonaTrack"),
            "Notificación de DonaTrack",
            List.of(new SendGridEmailRequest.Content("text/plain", mensaje)));

    String sendGridPayload;
    try {
      sendGridPayload =
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestDTO);
    } catch (JsonProcessingException e) {
      sendGridPayload = "Error serializing payload: " + e.getMessage();
    }

    log.info(
        "[SENDGRID-MOCK] HTTP 202 Accepted | MessageId: {}\nPayload Enviado:\n{}",
        messageId,
        sendGridPayload);

    return true;
  }

  protected boolean simularFalloTemporalAleatorio() {
    return new java.util.Random().nextInt(100) < 5;
  }

  protected void simularLatenciaDeRed() {
    try {
      // Simula un tiempo de respuesta de API entre 100 y 500 ms
      long latencia = 100 + new java.util.Random().nextInt(400);
      Thread.sleep(latencia);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
