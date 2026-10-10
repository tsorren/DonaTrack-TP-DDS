package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de SMS telefónicos. Aplica {@link CriterioFalloSimulado} para
 * admitir escenarios de fallo controlado.
 */
@Component
public class TelefonoAdapterSimulado extends BaseAdapterSimulado implements TelefonoAdapter {

  private static final Logger log = LoggerFactory.getLogger(TelefonoAdapterSimulado.class);

  public TelefonoAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    super(criterioFallo);
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
}
