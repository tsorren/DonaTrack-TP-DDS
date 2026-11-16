package grupo5.notificaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import java.util.UUID;

public interface INotificacionRepository extends CrudRepository<Notificacion> {

  List<Notificacion> findByEstado(EstadoNotificacion estado);

  List<Notificacion> findByPersonaId(UUID personaId);

  List<Notificacion> findByPersonaIdAndLeida(UUID personaId, boolean leida);

  long countByPersonaIdAndLeidaFalse(UUID personaId);

  /**
   * Una página de la bandeja de la persona con los filtros de {@code consulta}, en su orden, y el
   * total que los cumple (R11-14).
   */
  PaginaBandeja bandeja(UUID personaId, ConsultaBandeja consulta);

  void marcarTodasComoLeidas(UUID personaId);
}
