package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.security.SecureRandom;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de SMS telefónicos. Aplica {@link CriterioFalloSimulado} para
 * admitir escenarios de fallo controlado.
 */
@Component
public class TelefonoAdapterSimulado implements TelefonoAdapter {

  private static final Logger log = LoggerFactory.getLogger(TelefonoAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;
  private final SecureRandom random = new SecureRandom();

  public TelefonoAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarSms(String numero, String mensaje) {
    simularLatenciaDeRed();

    // 1. Falla permanente (HTTP 400) evaluada por la política original
    if (criterioFallo.debeFallar(numero, mensaje)) {
      log.warn("[TWILIO-MOCK] Rechazo de la API: Número inválido o no ruteable para SMS.");
      return false;
    }

    String sid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    String numeroEnmascarado = enmascararNumero(numero);

    log.info("[TWILIO-MOCK] HTTP 201 Created | SID: {} | Destinatario: {}", sid, numeroEnmascarado);
    log.debug("[TWILIO-MOCK] Payload original: {}", mensaje);

    return true;
  }

  private String enmascararNumero(String numero) {
    if (numero == null || numero.length() < 4) return "***";
    return numero.substring(0, numero.length() - 4).replaceAll(".", "*")
        + numero.substring(numero.length() - 4);
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
