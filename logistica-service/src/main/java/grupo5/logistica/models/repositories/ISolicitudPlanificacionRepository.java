package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import java.util.List;

public interface ISolicitudPlanificacionRepository extends CrudRepository<SolicitudPlanificacion> {
  List<SolicitudPlanificacion> findByEstado(EstadoSolicitud estado);
}
