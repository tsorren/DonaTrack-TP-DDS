package grupo5.incentivos.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificaciones-service", url = "${notificaciones.service.url}")
public interface NotificacionesFeignClient {

  @PostMapping("/notificaciones")
  void procesarEvento(@RequestBody Object evento);
}
