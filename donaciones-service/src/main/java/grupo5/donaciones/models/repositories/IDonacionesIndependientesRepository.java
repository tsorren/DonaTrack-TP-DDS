package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDonacionesIndependientesRepository extends CrudRepository<DonacionIndependiente> {

  List<DonacionIndependiente> findEnDeposito();

  /**
   * Cantidad asignada a una necesidad: la suma de las donaciones con ese destino. {@code periodoId}
   * es el período en las recurrentes y {@code null} en las extraordinarias (DTI-06).
   */
  int sumarCantidadAsignada(UUID necesidadId, UUID periodoId);

  /** Cuántas donaciones asignadas a cada entidad se registraron después de {@code desde}. */
  Map<UUID, Integer> contarPorEntidadRegistradasDesde(LocalDateTime desde);
}
