package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donantes.Donante;
import org.springframework.stereotype.Repository;

@Repository
public interface IDonantesRepository extends CrudRepository<Donante> {}
