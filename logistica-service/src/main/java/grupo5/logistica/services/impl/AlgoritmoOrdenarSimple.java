package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.services.AlgoritmoOrdenadorDeEntregas;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Implementación inicial y más simple de {@link AlgoritmoOrdenadorDeEntregas}: ordena las entregas
 * por su identificador (UUID), de forma determinística y estable, como paso previo a la
 * distribución entre camiones. Sirve como punto de partida sobre el cual, en el futuro, la cátedra
 * podrá exigir criterios de ordenamiento más complejos (por ejemplo, por cercanía geográfica) sin
 * modificar al {@code GeneradorDeRutas}.
 */
@Component
public class AlgoritmoOrdenarSimple implements AlgoritmoOrdenadorDeEntregas {

  @Override
  public List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    return entregas.stream().sorted(Comparator.comparing(Entrega::getId)).toList();
  }
}
