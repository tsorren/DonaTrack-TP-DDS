package grupo5.logistica.services.impl;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RutasService {

  private final IServicioExternoPlanificacion servicioExterno;
  private final IPlanificacionService planificacionService;

  public RutasService(
      IServicioExternoPlanificacion servicioExterno, IPlanificacionService planificacionService) {
    this.servicioExterno = servicioExterno;
    this.planificacionService = planificacionService;
  }

  public List<Ruta> generarRutas(List<Entrega> entregas, List<Camion> camiones) {
    return servicioExterno.generarRutas(entregas, camiones);
  }

  @Deprecated(forRemoval = true)
  public void planificarEntregasPendientes() {
    planificacionService.solicitarPlanificacionParaSiguienteJornada();
  }
}
