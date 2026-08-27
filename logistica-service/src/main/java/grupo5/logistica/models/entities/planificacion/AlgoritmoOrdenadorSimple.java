package grupo5.logistica.models.entities.planificacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoOrdenadorSimple implements AlgoritmoOrdenadorDeEntregas {

  @Override
  public List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    return entregas.stream().sorted(Comparator.comparing(Entrega::getId)).toList();
  }
}
