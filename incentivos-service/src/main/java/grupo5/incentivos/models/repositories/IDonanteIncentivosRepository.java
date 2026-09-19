package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.UUID;
import java.util.Optional;

public interface IDonanteIncentivosRepository extends CrudRepository<DonanteIncentivos> {
    Optional<DonanteIncentivos> findByIdPersona(UUID idPersona);
}
