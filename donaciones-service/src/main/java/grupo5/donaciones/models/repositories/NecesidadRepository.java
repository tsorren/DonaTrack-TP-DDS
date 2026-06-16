package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class NecesidadRepository extends BaseRepositoryEnMemoria<Necesidad> {

  // Método para simular la búsqueda de necesidades insatisfechas
  public List<Necesidad> findInsatisfechas() {
    return findAll().stream().filter(n -> !n.estaSatisfecha()).toList();
  }
}
