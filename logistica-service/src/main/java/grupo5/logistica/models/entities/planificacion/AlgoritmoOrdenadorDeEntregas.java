package grupo5.logistica.models.entities.planificacion;

import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;

public interface AlgoritmoOrdenadorDeEntregas {
  List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas);
}
