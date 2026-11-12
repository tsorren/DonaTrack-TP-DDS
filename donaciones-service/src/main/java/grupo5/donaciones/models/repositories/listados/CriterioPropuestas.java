package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.entities.propuestas.TipoAlgoritmo;
import java.util.Set;
import java.util.UUID;

/**
 * Búsqueda de propuestas de asignación (R11-24). Un campo nulo no filtra; un conjunto vacío
 * tampoco. {@code situaciones} solo deja pendientes. {@code algoritmo}: las que sugirió ese
 * algoritmo, solas o con el otro; {@code ambos}: las que sugirieron los dos. {@code donacionId}:
 * las que piden esa donación independiente.
 */
public record CriterioPropuestas(
    Set<EstadoPropuesta> estados,
    Set<SituacionPropuesta> situaciones,
    TipoAlgoritmo algoritmo,
    boolean ambos,
    UUID donacionId,
    OrdenPropuestas orden,
    Paginado paginado) {

  public CriterioPropuestas {
    estados = estados == null ? Set.of() : Set.copyOf(estados);
    situaciones = situaciones == null ? Set.of() : Set.copyOf(situaciones);
    orden = orden == null ? OrdenPropuestas.PUESTO : orden;
  }

  /** Con {@code situacion=EN_CONFLICTO} se agrupan las que compiten por la misma donación. */
  public boolean agrupaPorDonacion() {
    return orden == OrdenPropuestas.PUESTO && situaciones.contains(SituacionPropuesta.EN_CONFLICTO);
  }
}
