package grupo5.notificaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacionRepositoryEnMemoria extends CrudRepositoryEnMemoria<Notificacion>
    implements NotificacionRepository {

  @Override
  public List<Notificacion> findByEstado(EstadoNotificacion estado) {
    // Buscamos dentro de la colección en memoria que maneja la clase madre
    return this.findAll().stream().filter(n -> n.getEstadoNotificacion() == estado).toList();
  }

  @Override
  public List<Notificacion> findByPersonaId(UUID personaId) {
    return this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId))
        .toList();
  }
}
