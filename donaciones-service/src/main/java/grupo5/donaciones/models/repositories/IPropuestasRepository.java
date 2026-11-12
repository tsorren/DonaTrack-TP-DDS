package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;

public interface IPropuestasRepository extends CrudRepository<Propuesta> {

  /** Cuántas propuestas están en ese estado (resumen del admin, R11-01). */
  long contarEnEstado(EstadoPropuesta estado);
}
