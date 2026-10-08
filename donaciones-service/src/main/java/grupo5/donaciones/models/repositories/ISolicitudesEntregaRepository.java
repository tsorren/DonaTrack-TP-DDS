package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import java.util.Optional;
import java.util.UUID;

public interface ISolicitudesEntregaRepository extends CrudRepository<SolicitudEntrega> {

  /** Solicitud {@code PENDIENTE} o {@code ENVIADA} de la donación, si existe. */
  Optional<SolicitudEntrega> findActivaPorDonacion(UUID donacionIndependienteId);

  /** La solicitud más reciente de la donación, en cualquier estado (incluida {@code FALLIDA}). */
  Optional<SolicitudEntrega> findMasRecientePorDonacion(UUID donacionIndependienteId);
}
