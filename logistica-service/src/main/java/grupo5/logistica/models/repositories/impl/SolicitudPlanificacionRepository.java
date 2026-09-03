package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitudPlanificacionRepository
    extends CrudRepositoryEnMemoria<SolicitudPlanificacion>
    implements ISolicitudPlanificacionRepository {

  @Override
  public List<SolicitudPlanificacion> findByEstado(EstadoSolicitud estado) {
    return storage.values().stream().filter(solicitud -> solicitud.getEstado() == estado).toList();
  }
}
