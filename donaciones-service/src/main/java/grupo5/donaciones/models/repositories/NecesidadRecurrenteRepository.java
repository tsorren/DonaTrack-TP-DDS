package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.beneficiarios.NecesidadRecurrente;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class NecesidadRecurrenteRepository {
  private final List<NecesidadRecurrente> baseDeDatosFalsa = new ArrayList<>();

  // Método para simular que guardás un comedor
  public void save(NecesidadRecurrente necesidad) {
    if (!baseDeDatosFalsa.contains(necesidad)) {
      baseDeDatosFalsa.add(necesidad);
    }
  }

  // Método para simular la búsqueda de comedores activos
  public List<NecesidadRecurrente> findByActivaTrue() {
    return baseDeDatosFalsa.stream()
        .filter(NecesidadRecurrente::getActiva)
        .toList();
  }
}
