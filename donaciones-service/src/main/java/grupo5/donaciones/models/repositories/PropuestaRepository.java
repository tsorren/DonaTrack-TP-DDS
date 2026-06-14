package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class PropuestaRepository {
  private final List<Propuesta> baseDeDatosFalsa = new ArrayList<>();
  private final AtomicLong idCounter = new AtomicLong(1);

  // Método para simular que guardás una propuesta
  public void save(Propuesta propuesta) {
    if (!baseDeDatosFalsa.contains(propuesta)) {
      if (propuesta.getId() == null) {
        propuesta.setId(idCounter.getAndIncrement());
      }
      baseDeDatosFalsa.add(propuesta);
    }
  }

  // Método para simular la búsqueda de todas las propuestas
  public List<Propuesta> findAll() {
    return new ArrayList<>(baseDeDatosFalsa);
  }

  // Método para simular la búsqueda de propuestas activas
  public List<Propuesta> findByActivaTrue() {
    return baseDeDatosFalsa.stream().filter(Propuesta::estaActiva).toList();
  }

  // Método para simular la búsqueda de una propuesta por id
  public Optional<Propuesta> findById(Long id) {
    return baseDeDatosFalsa.stream().filter(p -> id.equals(p.getId())).findFirst();
  }
}
