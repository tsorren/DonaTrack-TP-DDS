package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.Optional;
import java.util.UUID;

public interface IDonanteIncentivosRepository extends CrudRepository<DonanteIncentivos> {
  Optional<DonanteIncentivos> findByIdPersona(UUID idPersona);

  /** Cambia solo el nombre del donante. Devuelve false si el donante no existe. */
  boolean actualizarNombre(UUID donanteId, String nombre);

  /**
   * Cambia la visibilidad de una insignia. Devuelve false si el donante o la insignia no existen.
   */
  boolean actualizarVisibilidadInsignia(UUID donanteId, String nombreInsignia, boolean visible);

  /** Elimina el donante y tod lo suyo. Devuelve false si no existía. */
  boolean eliminarPorId(UUID donanteId);
}
