package grupo5.notificaciones.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.notificaciones.infrastructure.TelefonoAdapter;
import grupo5.notificaciones.infrastructure.adapters.dtos.twilio.TwilioSmsRequest;
import grupo5.notificaciones.infrastructure.adapters.politicas.CriterioFalloSimulado;
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
  private final ObjectMapper objectMapper = new ObjectMapper();

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

    // 2. Falla temporal (HTTP 500 / Timeout) simulada aleatoriamente (5% de las veces)
    if (simularFalloTemporalAleatorio()) {
      throw new RuntimeException("HTTP 503 Service Unavailable: No se pudo contactar a Twilio.");
    }

    String sid = "SM" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);

    TwilioSmsRequest requestDTO = new TwilioSmsRequest(numero, "+1234567890", mensaje);
    String twilioPayload;
    try {
      twilioPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestDTO);
    } catch (JsonProcessingException e) {
      twilioPayload = "Error serializing payload: " + e.getMessage();
    }

    log.info("[TWILIO-MOCK] HTTP 201 Created | SID: {}\nPayload Enviado:\n{}", sid, twilioPayload);

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
