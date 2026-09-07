package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.NotificacionPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataNotificacionRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class NotificacionRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        Notificacion, NotificacionEntity, SpringDataNotificacionRepository>
    implements INotificacionRepository {

  private final NotificacionPersistenciaMapper mapper;

  public NotificacionRepositoryJpaAdapter(
      SpringDataNotificacionRepository springDataRepo, NotificacionPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public List<Notificacion> findByEstado(EstadoNotificacion estado) {
    return springDataRepo.findByEstadoNotificacion(estado).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Notificacion> findByPersonaId(UUID personaId) {
    return springDataRepo.findByPersonaId(personaId).stream().map(mapper::toDomain).toList();
  }
}
