package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.CorreoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.security.SecureRandom;
import java.util.UUID;
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
  private final SecureRandom random = new SecureRandom();

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

    String messageId = UUID.randomUUID().toString();
    String destinatarioEnmascarado = enmascararCorreo(destinatario);

    log.info(
        "[SENDGRID-MOCK] HTTP 202 Accepted | MessageId: {} | Destinatario: {}",
        messageId,
        destinatarioEnmascarado);

    log.debug("[SENDGRID-MOCK] Payload original: {}", mensaje);

    return true;
  }

  private static String enmascararCorreo(String correo) {
    if (correo == null || !correo.contains("@")) return "***";
    String[] partes = correo.split("@");
    String local = partes[0];
    if (local.length() <= 2) return local + "***@" + partes[1];
    return local.substring(0, 2) + "***@" + partes[1];
  }

  protected void simularLatenciaDeRed() {
    try {
      // Simula un tiempo de respuesta de API entre 100 y 500 ms
      long latencia = 100L + this.random.nextInt(400);
      Thread.sleep(latencia);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
