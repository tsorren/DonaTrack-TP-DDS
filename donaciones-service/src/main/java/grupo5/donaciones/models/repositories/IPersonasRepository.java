package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.personas.Persona;
import java.util.Optional;

public interface IPersonasRepository extends CrudRepository<Persona> {
  Optional<Persona> findByDocumento(String documento);
}
