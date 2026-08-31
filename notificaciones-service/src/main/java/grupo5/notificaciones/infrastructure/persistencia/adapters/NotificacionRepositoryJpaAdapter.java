package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.NotificacionPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataNotificacionRepository;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class NotificacionRepositoryJpaAdapter implements INotificacionRepository {
  private final SpringDataNotificacionRepository springDataRepo;
  private final NotificacionPersistenciaMapper mapper;

  public NotificacionRepositoryJpaAdapter(
      SpringDataNotificacionRepository springDataRepo, NotificacionPersistenciaMapper mapper) {
    this.springDataRepo = springDataRepo;
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

  @Override
  public Notificacion save(Notificacion aggregate) {
    NotificacionEntity entity = mapper.toEntity(aggregate);
    NotificacionEntity saved = springDataRepo.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Notificacion> saveAll(List<Notificacion> aggregates) {
    List<NotificacionEntity> entities = aggregates.stream().map(mapper::toEntity).toList();
    return springDataRepo.saveAll(entities).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Notificacion> findAll() {
    return springDataRepo.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<Notificacion> findById(UUID id) {
    return springDataRepo.findById(id).map(mapper::toDomain);
  }

  @Override
  public void delete(Notificacion aggregate) {
    springDataRepo.deleteById(aggregate.getId());
  }

  @Override
  public boolean existsById(UUID id) {
    return springDataRepo.existsById(id);
  }

  @Override
  public long count() {
    return springDataRepo.count();
  }

  @Override
  public void deleteAll() {
    springDataRepo.deleteAll();
  }
}
