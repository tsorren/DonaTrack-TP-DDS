package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.Optional;

public interface IDonacionesIndependientesRepository {

  /** Persiste o actualiza una DonacionIndependiente. Retorna la entidad guardada. */
  DonacionIndependiente save(DonacionIndependiente donacion);

  /** Busca por identificador. Retorna vacío si no existe. */
  Optional<DonacionIndependiente> findById(Long id);
}
