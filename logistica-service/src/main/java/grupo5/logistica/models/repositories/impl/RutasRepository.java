package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IRutasRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres & !mongo")
public class RutasRepository extends CrudRepositoryEnMemoria<Ruta> implements IRutasRepository {

  @Override
  public List<Ruta> findByFecha(LocalDate fecha) {
    return storage.values().stream()
        .filter(ruta -> Objects.equals(ruta.getFecha(), fecha))
        .toList();
  }

  @Override
  public List<Ruta> findByEstado(EstadoRuta estado) {
    return storage.values().stream().filter(ruta -> ruta.getEstado() == estado).toList();
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

  @Override
  public List<Ruta> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ids.stream().distinct().map(storage::get).filter(Objects::nonNull).toList();
  }
}
