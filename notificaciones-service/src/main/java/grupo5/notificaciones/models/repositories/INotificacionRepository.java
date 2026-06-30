package grupo5.notificaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import java.util.UUID;

public interface INotificacionRepository extends CrudRepository<Notificacion> {

  List<Notificacion> findByEstado(EstadoNotificacion estado);

  List<Notificacion> findByPersonaId(UUID personaId);
}
