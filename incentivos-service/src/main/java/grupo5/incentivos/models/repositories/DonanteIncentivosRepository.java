package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DonanteIncentivosRepository extends CrudRepositoryEnMemoria<DonanteIncentivos>
    implements IDonanteIncentivosRepository {
  @Override
  public Optional<DonanteIncentivos> findByIdPersona(UUID idPersona) {
    if (idPersona == null) {
      return Optional.empty();
    }
    return storage.values().stream()
        .filter(donante -> idPersona.equals(donante.getIdPersona()))
        .findFirst();
  }
}
