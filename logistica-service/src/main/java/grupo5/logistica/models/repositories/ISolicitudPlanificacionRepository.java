package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.planificacion.SolicitudPlanificacion;
import java.util.Optional;
import java.util.UUID;

public interface ISolicitudPlanificacionRepository extends CrudRepository<SolicitudPlanificacion> {
  Optional<SolicitudPlanificacion> findByCorrelationId(UUID correlationId);
}
