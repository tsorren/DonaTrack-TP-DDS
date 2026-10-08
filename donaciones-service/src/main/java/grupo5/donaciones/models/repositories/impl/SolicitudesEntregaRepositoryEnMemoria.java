package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.logistica.SolicitudEntrega;
import grupo5.donaciones.models.repositories.ISolicitudesEntregaRepository;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitudesEntregaRepositoryEnMemoria extends CrudRepositoryEnMemoria<SolicitudEntrega>
    implements ISolicitudesEntregaRepository {

  @Override
  public Optional<SolicitudEntrega> findActivaPorDonacion(UUID donacionIndependienteId) {
    return storage.values().stream()
        .filter(s -> s.getDonacionIndependienteId().equals(donacionIndependienteId))
        .filter(SolicitudEntrega::estaActiva)
        .findFirst();
  }

  @Override
  public Optional<SolicitudEntrega> findMasRecientePorDonacion(UUID donacionIndependienteId) {
    return storage.values().stream()
        .filter(s -> s.getDonacionIndependienteId().equals(donacionIndependienteId))
        .max(Comparator.comparing(SolicitudEntrega::getFechaCreacion));
  }
}
