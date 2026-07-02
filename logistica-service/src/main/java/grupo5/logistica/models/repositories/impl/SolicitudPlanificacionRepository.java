package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.planificacion.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitudPlanificacionRepository
    extends CrudRepositoryEnMemoria<SolicitudPlanificacion>
    implements ISolicitudPlanificacionRepository {

  @Override
  public Optional<SolicitudPlanificacion> findByCorrelationId(UUID correlationId) {
    return storage.values().stream()
        .filter(solicitud -> solicitud.getCorrelationId().equals(correlationId))
        .findFirst();
  }
}
