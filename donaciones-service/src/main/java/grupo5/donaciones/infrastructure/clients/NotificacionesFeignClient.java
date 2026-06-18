package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.replicas.PersonaReplicaDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "notificaciones-service",
    url = "${donatrack.notificaciones.url}/api/notificaciones",
    configuration = FeignRetryConfig.class)
public interface NotificacionesFeignClient {

  @PutMapping("/personas")
  void sincronizarPersona(@RequestBody PersonaReplicaDTO dto);

  @DeleteMapping("/personas/{id}")
  void anonimizarPersona(@PathVariable("id") UUID id);
}
