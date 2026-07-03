package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "logistica-service",
    url = "${donatrack.logistica.url}/api/logistica",
    configuration = FeignRetryConfig.class)
public interface LogisticaFeignClient {

  @PostMapping("/entregas")
  void registrarEntregaPendiente(@RequestBody NuevaEntregaRequest request);
}
