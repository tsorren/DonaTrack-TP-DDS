package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.comunicaciones.EventoNotificableDTO;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "notificaciones-service",
    url = "${donatrack.notificaciones.url}",
    configuration = FeignRetryConfig.class)
public interface NotificacionesFeignClient {

  @PutMapping("/api/notificaciones/personas")
  void sincronizarPersona(@RequestBody PersonaReplicaDTO dto);

  @PostMapping("/notificaciones")
  void enviarEvento(@RequestBody EventoNotificableDTO dto);
}
