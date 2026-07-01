package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IRutasRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RutasRepository extends CrudRepositoryEnMemoria<Ruta> implements IRutasRepository {

  @Override
  public List<Ruta> findByFecha(LocalDate fecha) {
    return storage.values().stream()
        .filter(ruta -> Objects.equals(ruta.getFecha(), fecha))
        .toList();
  }

  @Override
  public List<Ruta> findByCamionId(UUID camionId) {
    return storage.values().stream()
        .filter(ruta -> Objects.equals(ruta.getCamionId(), camionId))
        .toList();
  }

  @Override
  public List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha) {
    return storage.values().stream()
        .filter(ruta -> Objects.equals(ruta.getCamionId(), camionId))
        .filter(ruta -> Objects.equals(ruta.getFecha(), fecha))
        .toList();
  }
}
