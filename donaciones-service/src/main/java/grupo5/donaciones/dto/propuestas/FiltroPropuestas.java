package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import java.util.List;
import java.util.UUID;

/**
 * Filtros opcionales de {@code GET /api/asignaciones/propuestas} (R11-24); un campo nulo o vacío no
 * filtra. {@code estado} y {@code situacion} se repiten; {@code situacion} solo deja pendientes.
 * {@code algoritmo}: {@code COMPATIBILIDAD_SEMANTICA}, {@code PRIORIDAD_SUBATENDIDOS} (solas o con
 * el otro) o {@code AMBOS} (las que sugirieron los dos). {@code donacion}: el id de una donación
 * independiente que piden. Un valor desconocido da 400 con {@code ERR-CSR-003}.
 */
public record FiltroPropuestas(
    List<EstadoPropuesta> estado,
    List<SituacionPropuesta> situacion,
    String algoritmo,
    UUID donacion) {

  public static FiltroPropuestas ninguno() {
    return new FiltroPropuestas(null, null, null, null);
  }
}
