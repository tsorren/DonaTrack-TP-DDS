package grupo5.logistica.models.entities.planificacion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.entregas.Entrega;
import java.util.Comparator;
import java.util.List;

/**
 * Implementación simple que ordena las entregas por {@code id} (UUID). El criterio es determinista
 * y reproducible entre ejecuciones, pero arbitrario desde el punto de vista del negocio.
 *
 * <p>📝 Deuda conocida: este orden debería reemplazarse por un criterio de negocio real (p. ej.
 * agrupación geográfica por localidad o cercanía al depósito de origen) para reducir la distancia
 * total recorrida. El cambio requiere que {@code Entrega} exponga coordenadas o una referencia
 * geográfica procesable — hoy no lo hace.
 */
public class AlgoritmoOrdenadorSimple implements AlgoritmoOrdenadorDeEntregas {

  @Override
  public List<Entrega> obtenerEntregasOrdenadas(List<Entrega> entregas) {
    if (entregas == null) {
      throw new ValidationException(ErrorCatalog.GENERADOR_RUTAS_ENTREGAS_NULAS);
    }
    return entregas.stream().sorted(Comparator.comparing(Entrega::getId)).toList();
  }
}
