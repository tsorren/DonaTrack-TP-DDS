package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.personas.AceptacionLegales;
import grupo5.donaciones.models.repositories.IAceptacionesLegalesRepository;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class AceptacionesLegalesRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<AceptacionLegales> implements IAceptacionesLegalesRepository {

  @Override
  public Optional<AceptacionLegales> ultimaDe(UUID personaId) {
    return findAll().stream()
        .filter(a -> personaId.equals(a.personaId()))
        .max(Comparator.comparingInt(AceptacionLegales::version));
  }
}
