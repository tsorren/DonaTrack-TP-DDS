package grupo5.notificaciones.infrastructure.persistencia.repositories;

import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotificacionRepository extends JpaRepository<NotificacionEntity, UUID> {
  List<NotificacionEntity> findByEstadoNotificacion(EstadoNotificacion estadoNotificacion);

  List<NotificacionEntity> findByPersonaId(UUID personaId);
}
