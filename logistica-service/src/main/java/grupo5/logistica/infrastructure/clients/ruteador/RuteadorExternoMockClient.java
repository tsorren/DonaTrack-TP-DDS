package grupo5.logistica.infrastructure.clients.ruteador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RuteadorExternoMockClient implements RuteadorExternoClient {
  private static final Logger log = LoggerFactory.getLogger(RuteadorExternoMockClient.class);

  @Override
  public void solicitarPlanificacion(RuteadorRequestDTO request) {
    log.info(
        "[RUTEADOR_MOCK] Solicitud {} aceptada para fecha {} con {} entregas y {} camiones. Callback: {}",
        request.solicitudId(),
        request.fecha(),
        request.entregas().size(),
        request.camiones().size(),
        request.callbackUrl());
  }
}
