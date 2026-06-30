package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.comunicaciones.EventoNotificableDTO;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "notificaciones-service",
    url = "${donatrack.notificaciones.url}",
    configuration = FeignRetryConfig.class)
public interface NotificacionesFeignClient {

  @PutMapping("${donatrack.routes.notificaciones.personas-base}")
  void sincronizarPersona(@RequestBody PersonaReplicaDTO dto);

  @DeleteMapping(
      "${donatrack.routes.notificaciones.personas-base}${donatrack.routes.notificaciones.personas-id}")
  void anonimizarPersona(@PathVariable("id") UUID id);

  @PostMapping("${donatrack.routes.notificaciones.notificaciones-base}")
  void enviarEvento(@RequestBody EventoNotificableDTO dto);
}
