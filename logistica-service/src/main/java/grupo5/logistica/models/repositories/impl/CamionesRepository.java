package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.repositories.ICamionesRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CamionesRepository extends CrudRepositoryEnMemoria<Camion>
    implements ICamionesRepository {

  @Override
  public Optional<Camion> findByPatente(String patente) {
    if (patente == null) {
      return Optional.empty();
    }

    String patenteNormalizada = patente.trim();
    return storage.values().stream()
        .filter(camion -> camion.getPatente().equalsIgnoreCase(patenteNormalizada))
        .findFirst();
  }

  @Override
  public List<Camion> findDisponibles() {
    return storage.values().stream().filter(Camion::estaDisponibleParaAsignar).toList();
  }
}
