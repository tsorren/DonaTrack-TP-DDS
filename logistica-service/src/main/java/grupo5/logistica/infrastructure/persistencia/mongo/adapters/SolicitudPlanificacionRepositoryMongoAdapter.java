package grupo5.logistica.infrastructure.persistencia.mongo.adapters; // NOSONAR

import grupo5.logistica.infrastructure.persistencia.mongo.documents.SolicitudPlanificacionDocument;
import grupo5.logistica.infrastructure.persistencia.mongo.mappers.SolicitudPlanificacionMongoMapper;
import grupo5.logistica.infrastructure.persistencia.mongo.repositories.SpringDataSolicitudPlanificacionMongoRepository;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
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

  public SolicitudPlanificacionRepositoryMongoAdapter(
      SpringDataSolicitudPlanificacionMongoRepository springDataRepo,
      SolicitudPlanificacionMongoMapper mapper) {
    super(springDataRepo, mapper::toDocument, mapper::toDomain);
  }
}
