package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.infrastructure.clients.LogisticaFeignClient;
import grupo5.donaciones.services.ILogisticaAsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LogisticaAsyncService implements ILogisticaAsyncService {

  private static final Logger log = LoggerFactory.getLogger(LogisticaAsyncService.class);
  private final LogisticaFeignClient client;

  public LogisticaAsyncService(LogisticaFeignClient client) {
    this.client = client;
  }

  @Async
  public void registrarEntregaPendiente(NuevaEntregaRequest request) {
    try {
      client.registrarEntregaPendiente(request);
    } catch (Exception e) {
      log.error(
          "Fallo al registrar entrega pendiente en logistica-service para donación {}: {}",
          request.idDonacion(),
          e.getMessage(),
          e);
    }
  }
}
