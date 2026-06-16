package grupo5.notificaciones.models.repositories;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository {

  Optional<Notificacion> findById(UUID id);

  List<Notificacion> findByEstado(EstadoNotificacion estado);

  List<Notificacion> findByPersonaId(Long personaId);

  Notificacion save(Notificacion notificacion);
}

// Debería extender de JPARepository?
