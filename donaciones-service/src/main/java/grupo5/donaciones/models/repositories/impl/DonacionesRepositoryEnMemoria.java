package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class DonacionesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Donacion>
    implements IDonacionesRepository {

  @Override
  public Set<UUID> idsDeDonantes() {
    return storage.values().stream().map(Donacion::getDonanteId).collect(Collectors.toSet());
  }
}
