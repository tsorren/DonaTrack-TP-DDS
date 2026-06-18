package grupo5.donaciones.infrastructure.clients;

import grupo5.donaciones.dto.replicas.PersonaReplicaDTO;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
