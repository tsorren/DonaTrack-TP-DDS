package grupo5.logistica.models.entities.planificacion;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;
import java.util.Map;

public interface AlgoritmoAsignadorDeEntregas {
  Map<Camion, List<Entrega>> asignar(List<Entrega> entregas, List<Camion> camiones);
}
