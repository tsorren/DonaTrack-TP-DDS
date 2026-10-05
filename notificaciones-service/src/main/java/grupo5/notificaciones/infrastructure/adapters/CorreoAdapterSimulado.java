package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.CorreoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
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
    if (new java.util.Random().nextInt(100) < 5) {
      throw new RuntimeException(
          "HTTP 503 Service Unavailable: Timeout conectando con el proveedor.");
    }

    String messageId = java.util.UUID.randomUUID().toString();
    String sendGridPayload =
        String.format(
            """
            {
              "personalizations": [{"to": [{"email": "%s"}]}],
              "from": {"email": "no-reply@donatrack.org", "name": "DonaTrack"},
              "subject": "Notificación de DonaTrack",
              "content": [{"type": "text/plain", "value": "%s"}]
            }""",
            destinatario, mensaje.replace("\"", "\\\""));

    log.info(
        "[SENDGRID-MOCK] HTTP 202 Accepted | MessageId: {}\nPayload Enviado:\n{}",
        messageId,
        sendGridPayload);

    return true;
  }

  private void simularLatenciaDeRed() {
    try {
      // Simula un tiempo de respuesta de API entre 100 y 500 ms
      long latencia = 100 + new java.util.Random().nextInt(400);
      Thread.sleep(latencia);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
