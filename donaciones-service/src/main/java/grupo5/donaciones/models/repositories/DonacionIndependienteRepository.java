package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.List;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class DonacionIndependienteRepository
    extends BaseRepositoryEnMemoria<DonacionIndependiente> {

  // Metodo para simular la búsqueda de donaciones en depósito
  public List<DonacionIndependiente> findEnDeposito() {
    return findAll().stream()
        .filter(d -> d.getEstadoActual().getTipo() == TipoEstadoDonacion.EN_DEPOSITO)
        .toList();
  }
}
