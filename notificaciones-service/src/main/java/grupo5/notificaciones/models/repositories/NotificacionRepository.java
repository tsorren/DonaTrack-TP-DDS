package grupo5.notificaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends CrudRepository<Notificacion> {

  List<Notificacion> findByEstado(EstadoNotificacion estado);

  List<Notificacion> findByPersonaId(Long personaId);
}
