package grupo5.notificaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class NotificacionRepositoryEnMemoria extends CrudRepositoryEnMemoria<Notificacion>
    implements INotificacionRepository {

  @Override
  public List<Notificacion> findByEstado(EstadoNotificacion estado) {
    return this.findAll().stream().filter(n -> n.getEstadoNotificacion() == estado).toList();
  }

  @Override
  public List<Notificacion> findByPersonaId(UUID personaId) {
    return this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId))
        .toList();
  }

  @Override
  public List<Notificacion> findByPersonaIdAndLeida(UUID personaId, boolean leida) {
    return this.findAll().stream()
        .filter(
            n ->
                n.getPersonaId() != null
                    && n.getPersonaId().equals(personaId)
                    && n.isLeida() == leida)
        .toList();
  }

  @Override
  public long countByPersonaIdAndLeidaFalse(UUID personaId) {
    return this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId) && !n.isLeida())
        .count();
  }

  @Override
  public void marcarTodasComoLeidas(UUID personaId) {
    this.findAll().stream()
        .filter(n -> n.getPersonaId() != null && n.getPersonaId().equals(personaId) && !n.isLeida())
        .forEach(Notificacion::marcarComoLeida);
  }
}
