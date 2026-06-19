package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRecurrentesRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class NecesidadesRecurrentesRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<NecesidadRecurrente>
    implements INecesidadesRecurrentesRepository {

  // Metodo para simular la búsqueda de necesidades recurrentes activas
  @Override
  public List<NecesidadRecurrente> findByActivaTrue() {
    return storage.values().stream().filter(NecesidadRecurrente::getActiva).toList();
  }
}
