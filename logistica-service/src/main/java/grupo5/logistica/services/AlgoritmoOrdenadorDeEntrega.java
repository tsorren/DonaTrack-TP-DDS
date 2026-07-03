package grupo5.logistica.services;

import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.List;

/**
 * Define el contrato para ordenar las entregas antes de que sean distribuidas entre los camiones
 * disponibles. Forma parte del patrón Strategy que permite intercambiar el criterio de ordenamiento
 * sin acoplar dicha lógica al {@code GeneradorDeRutas}.
 */
public interface AlgoritmoOrdenadorDeEntrega {

  /**
   * Devuelve las entregas recibidas ordenadas según el criterio de la implementación concreta. Una
   * implementación puede, incluso, devolver la misma lista recibida sin modificaciones.
   *
   * @param entregas entregas a ordenar.
   * @return una nueva lista con las entregas ordenadas.
   */
  List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas);
}
