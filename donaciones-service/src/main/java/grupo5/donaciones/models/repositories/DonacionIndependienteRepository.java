package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class DonacionIndependienteRepository {
  private final List<DonacionIndependiente> baseDeDatosFalsa = new ArrayList<>();

  public void save(DonacionIndependiente donacion) {
    if (!baseDeDatosFalsa.contains(donacion)) {
      baseDeDatosFalsa.add(donacion);
    }
  }

  public List<DonacionIndependiente> findAll() {
    return new ArrayList<>(baseDeDatosFalsa);
  }

  public List<DonacionIndependiente> findEnDeposito() {
    return baseDeDatosFalsa.stream()
        .filter(d -> d.getEstadoActual() instanceof EnDeposito)
        .toList();
  }
}
