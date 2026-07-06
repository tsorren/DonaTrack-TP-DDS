package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PersonasRepositoryEnMemoria extends CrudRepositoryEnMemoria<Persona>
    implements IPersonasRepository {

  @Override
  public Optional<Persona> findByDocumento(String documento) {
    if (documento == null) {
      return Optional.empty();
    }
    return storage.values().stream().filter(p -> documento.equals(p.getDocumento())).findFirst();
  }
}
