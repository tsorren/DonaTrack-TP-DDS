package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.personas.AceptacionLegales;
import java.util.Optional;
import java.util.UUID;

public interface IAceptacionesLegalesRepository extends CrudRepository<AceptacionLegales> {
  /** La de versión más alta que aceptó la persona, si aceptó alguna. */
  Optional<AceptacionLegales> ultimaDe(UUID personaId);
}
