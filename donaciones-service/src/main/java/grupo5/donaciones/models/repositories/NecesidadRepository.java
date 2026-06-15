package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class NecesidadRepository {
  private final List<Necesidad> baseDeDatosFalsa = new ArrayList<>();

  // Método para simular que guardás una necesidad
  public void save(Necesidad necesidad) {
    if (!baseDeDatosFalsa.contains(necesidad)) {
      baseDeDatosFalsa.add(necesidad);
    }
  }

  // Método para simular la búsqueda de todas las necesidades
  public List<Necesidad> findAll() {
    return new ArrayList<>(baseDeDatosFalsa);
  }

  // Método para simular la búsqueda de necesidades insatisfechas
  public List<Necesidad> findInsatisfechas() {
    return baseDeDatosFalsa.stream().filter(n -> !n.estaSatisfecha()).toList();
  }
}
