package grupo5.notificaciones.models.repositories;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificacionRepository {

    Optional<Notificacion> findById(UUID id);

    List<Notificacion> findByEstado(EstadoNotificacion estado);

    List<Notificacion> findByPersonaId(Long personaId);

    Notificacion save (Notificacion notificacion);
}

//Debería extender de JPARepository?