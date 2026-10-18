package grupo5.logistica.models.entities.rutas;

import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;

public interface GeneradorLotes {
  List<List<Entrega>> particionarEnLotes(List<Entrega> entregas, int maximoPorLote);
}
