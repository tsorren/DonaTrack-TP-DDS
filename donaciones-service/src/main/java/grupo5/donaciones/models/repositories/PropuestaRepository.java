package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class PropuestaRepository {
  private final List<Propuesta> baseDeDatosFalsa = new ArrayList<>();
  private Long nextId = 1L;

  public void save(Propuesta propuesta) {
    if (propuesta.getId() == null) {
      propuesta.setId(nextId++);
    }
    if (!baseDeDatosFalsa.contains(propuesta)) {
      baseDeDatosFalsa.add(propuesta);
    }
  }

  public List<Propuesta> findAll() {
    return new ArrayList<>(baseDeDatosFalsa);
  }

  public Optional<Propuesta> findById(Long id) {
    return baseDeDatosFalsa.stream()
        .filter(p -> p.getId() != null && p.getId().equals(id))
        .findFirst();
  }
}
