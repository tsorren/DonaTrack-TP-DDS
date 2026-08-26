package grupo5.donaciones.models.repositories.impl;

import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AsignacionesRepositoryEnMemoria implements IAsignacionesRepository {
  private final Map<UUID, EjecucionAsignacion> storage = new ConcurrentHashMap<>();

  @Override
  public EjecucionAsignacion save(EjecucionAsignacion aggregate) {
    if (aggregate == null || aggregate.getId() == null) {
      throw new IllegalArgumentException("Aggregate or its ID cannot be null");
    }
    storage.put(aggregate.getId(), aggregate);
    return aggregate;
  }

  @Override
  public List<EjecucionAsignacion> obtenerHistorial() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public void deleteAll() {
    storage.clear();
  }
}
