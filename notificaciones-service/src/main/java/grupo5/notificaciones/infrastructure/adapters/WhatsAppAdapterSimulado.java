package grupo5.notificaciones.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.notificaciones.infrastructure.WhatsAppAdapter;
import grupo5.notificaciones.infrastructure.adapters.dtos.meta.MetaWhatsAppRequest;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para despacho de mensajes vía WhatsApp. Aplica {@link CriterioFalloSimulado}
 * para admitir escenarios de fallo controlado.
 */
@Component
public class WhatsAppAdapterSimulado implements WhatsAppAdapter {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapterSimulado.class);

  private final CriterioFalloSimulado criterioFallo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WhatsAppAdapterSimulado(CriterioFalloSimulado criterioFallo) {
    this.criterioFallo = criterioFallo;
  }

  @Override
  public boolean enviarWhatsApp(String numero, String mensaje) {
    simularLatenciaDeRed();

    // 1. Falla permanente (HTTP 400) evaluada por la política original
    if (criterioFallo.debeFallar(numero, mensaje)) {
      log.warn("[META-WHATSAPP-MOCK] Rechazo de la API: Número inválido o plantilla no aprobada.");
      return false;
    }

    // 2. Falla temporal (HTTP 500 / Timeout) simulada aleatoriamente (5% de las veces)
    if (simularFalloTemporalAleatorio()) {
      throw new RuntimeException("HTTP 503 Service Unavailable: Error de conexión con Meta API.");
    }

    String wamid =
        "wamid." + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);

    MetaWhatsAppRequest requestDTO =
        new MetaWhatsAppRequest(
            "whatsapp",
            "individual",
            numero,
            "text",
            new MetaWhatsAppRequest.TextContent(false, mensaje));

    String metaPayload;
    try {
      metaPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestDTO);
    } catch (JsonProcessingException e) {
      metaPayload = "Error serializing payload: " + e.getMessage();
    }

    log.info(
        "[META-WHATSAPP-MOCK] HTTP 200 OK | MessageId: {}\nPayload Enviado:\n{}",
        wamid,
        metaPayload);

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
