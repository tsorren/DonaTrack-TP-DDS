package grupo5.donaciones.models.entities.propuestas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

/**
 * Qué algoritmo sugirió una propuesta y en qué puesto de su lista de esa corrida (R11-24). {@code
 * puesto}: 1 es la primera; {@code deCuantas}: cuántas propuso ese algoritmo en esa corrida. {@code
 * eligioLasDonaciones}: las donaciones de la propuesta son las que eligió este algoritmo (cuando
 * los dos sugieren la misma necesidad, queda la del segundo).
 */
public record SugerenciaDeAlgoritmo(
    TipoAlgoritmo algoritmo, int puesto, int deCuantas, boolean eligioLasDonaciones) {

  public SugerenciaDeAlgoritmo {
    if (algoritmo == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (puesto < 1 || deCuantas < puesto) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  /**
   * La misma sugerencia, sin haber elegido las donaciones (la propuesta quedó con las del otro).
   */
  public SugerenciaDeAlgoritmo sinElegirLasDonaciones() {
    return new SugerenciaDeAlgoritmo(algoritmo, puesto, deCuantas, false);
  }
}
