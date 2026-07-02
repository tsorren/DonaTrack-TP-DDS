package grupo5.logistica.services;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;
import java.util.Map;

/**
 * Define el contrato para distribuir un conjunto de entregas entre los camiones operativos,
 * respetando las restricciones físicas de cada camión (peso, volumen y altura). Forma parte del
 * patrón Strategy que permite intercambiar el criterio de asignación sin acoplar dicha lógica al
 * {@code GeneradorDeRutas}.
 */
public interface AlgoritmoAsignadorDeEntregas {

  /**
   * Distribuye las entregas recibidas entre los camiones disponibles.
   *
   * @param entregas entregas a asignar (se recomienda que ya vengan ordenadas).
   * @param camiones camiones operativos disponibles para la jornada.
   * @return un mapa donde cada camión posee la lista ordenada de entregas que le fueron asignadas.
   *     Las entregas que no pudieron encajar en ningún camión no se incluyen en el resultado.
   */
  Map<Camion, List<Entrega>> asignar(List<Entrega> entregas, List<Camion> camiones);
}
