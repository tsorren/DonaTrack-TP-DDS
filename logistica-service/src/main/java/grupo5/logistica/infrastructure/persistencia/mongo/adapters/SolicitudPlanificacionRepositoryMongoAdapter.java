package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.SolicitudPlanificacionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataSolicitudPlanificacionMongoRepository;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@SuppressWarnings("java:S120")
@Repository
@Profile("mongo")
public class SolicitudPlanificacionRepositoryMongoAdapter
    extends CrudRepositoryMongoAdapter<
        SolicitudPlanificacion,
        SolicitudPlanificacionDocument,
        SpringDataSolicitudPlanificacionMongoRepository>
    implements ISolicitudPlanificacionRepository {

  private final SolicitudPlanificacionMongoMapper mapper;

  public SolicitudPlanificacionRepositoryMongoAdapter(
      SpringDataSolicitudPlanificacionMongoRepository springDataRepo,
      SolicitudPlanificacionMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public SolicitudPlanificacion save(SolicitudPlanificacion aggregate) {
    if (aggregate == null) {
      return null;
    }
    SolicitudPlanificacionDocument existing =
        aggregate.getId() != null ? springDataRepo.findById(aggregate.getId()).orElse(null) : null;
    SolicitudPlanificacionDocument saved =
        springDataRepo.save(mapper.toDocument(aggregate, existing));
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
