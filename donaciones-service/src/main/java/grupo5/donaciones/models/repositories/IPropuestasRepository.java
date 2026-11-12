package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.repositories.listados.CriterioPropuestas;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Map;
import java.util.UUID;

public interface IPropuestasRepository extends CrudRepository<Propuesta> {

  /** Cuántas propuestas están en ese estado (resumen del admin, R11-01). */
  long contarEnEstado(EstadoPropuesta estado);

  /**
   * Una página de propuestas con el filtro y el orden de {@code criterio}, cada una con su
   * situación y lo que piden las otras pendientes (R11-24).
   */
  Tramo<PropuestaEnListado> buscar(CriterioPropuestas criterio);

  /** Cuántas pendientes hay en cada situación; las tres claves siempre están (R11-24). */
  Map<SituacionPropuesta, Long> contarPendientesPorSituacion();

  /**
   * Las donaciones independientes que están en al menos una pendiente, de a una página: primero las
   * que no alcanzan, después las que más días llevan en el depósito (R11-24). {@code categoriaId}
   * nulo no filtra; {@code soloEnConflicto}: solo las que no alcanzan.
   */
  Tramo<StockEnPropuestas> buscarDonacionesEnPendientes(
      UUID categoriaId, boolean soloEnConflicto, Paginado paginado);
}
