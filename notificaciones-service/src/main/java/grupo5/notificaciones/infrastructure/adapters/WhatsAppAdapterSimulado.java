package grupo5.notificaciones.infrastructure.adapters;

import grupo5.notificaciones.infrastructure.WhatsAppAdapter;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de mensajes vía WhatsApp. Aplica {@link CriterioFalloSimulado}
 * para admitir escenarios de fallo controlado.
 */
@Component
public class WhatsAppAdapterSimulado extends BaseAdapterSimulado implements WhatsAppAdapter {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapterSimulado.class);

  public WhatsAppAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    super(criterioFallo);
  }

  @Override
  public boolean enviarWhatsApp(String numero, String mensaje) {
    simularLatenciaDeRed();

    // 1. Falla permanente (HTTP 400) evaluada por la política original
    if (criterioFallo.debeFallar(numero, mensaje)) {
      log.warn("[META-WHATSAPP-MOCK] Rechazo de la API: Número inválido o plantilla no aprobada.");
      return false;
    }

    String wamid = "wamid." + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    String numeroEnmascarado = enmascararNumero(numero);

    log.info(
        "[META-WHATSAPP-MOCK] HTTP 200 OK | MessageId: {} | Destinatario: {}",
        wamid,
        numeroEnmascarado);

    log.debug("[META-WHATSAPP-MOCK] Payload original: {}", mensaje);

    return true;
  }
}
