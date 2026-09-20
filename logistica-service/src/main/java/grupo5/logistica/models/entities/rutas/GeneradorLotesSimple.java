package grupo5.logistica.models.entities.rutas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.ArrayList;
import java.util.List;

public class GeneradorLotesSimple implements GeneradorLotes {

  @Override
  public List<List<Entrega>> particionarEnLotes(List<Entrega> entregas, int maximoPorLote) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    if (maximoPorLote <= 0) {
      throw new ValidationException(ErrorCatalog.SOLICITUD_PLANIFICACION_CANTIDAD_INVALIDA);
    }

    List<List<Entrega>> lotes = new ArrayList<>();
    for (int inicio = 0; inicio < entregas.size(); inicio += maximoPorLote) {
      int fin = Math.min(inicio + maximoPorLote, entregas.size());
      lotes.add(List.copyOf(entregas.subList(inicio, fin)));
    }
    return List.copyOf(lotes);
  }
}
