package grupo5.logistica.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.logistica.infrastructure.persistencia.entities.SolicitudPlanificacionEntity;
import grupo5.logistica.infrastructure.persistencia.mappers.SolicitudPlanificacionPersistenciaMapper;
import grupo5.logistica.infrastructure.persistencia.repositories.SpringDataSolicitudPlanificacionRepository;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional
public class SolicitudPlanificacionRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        SolicitudPlanificacion,
        SolicitudPlanificacionEntity,
        SpringDataSolicitudPlanificacionRepository>
    implements ISolicitudPlanificacionRepository {

  private final SolicitudPlanificacionPersistenciaMapper mapper;

  public SolicitudPlanificacionRepositoryJpaAdapter(
      SpringDataSolicitudPlanificacionRepository springDataRepo,
      SolicitudPlanificacionPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public SolicitudPlanificacion save(SolicitudPlanificacion aggregate) {
    SolicitudPlanificacionEntity existing = springDataRepo.findById(aggregate.getId()).orElse(null);
    SolicitudPlanificacionEntity saved = springDataRepo.save(mapper.toEntity(aggregate, existing));
    return mapper.toDomain(saved);
  }

  @Override
  public List<SolicitudPlanificacion> saveAll(List<SolicitudPlanificacion> aggregates) {
    if (aggregates == null) {
      return List.of();
    }
    return aggregates.stream().map(this::save).toList();
  }
}
