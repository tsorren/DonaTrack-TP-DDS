package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres & !mongo")
public class CamionRepository extends CrudRepositoryEnMemoria<Camion> implements ICamionRepository {

  @Override
  public List<Camion> findByEstado(EstadoCamion estado) {
    return findAll().stream().filter(camion -> camion.getEstado() == estado).toList();
  }

  @Override
  public Optional<Camion> findByPatente(String patente) {
    if (patente == null || patente.isBlank()) {
      return Optional.empty();
    }

    String patenteNormalizada = ValidadorPatentes.normalizar(patente);
    return storage.values().stream()
        .filter(camion -> camion.getPatente().equalsIgnoreCase(patenteNormalizada))
        .findFirst();
  }

  @Override
  public List<Camion> findActivos() {
    return storage.values().stream()
        .filter(camion -> camion.getEstado() != EstadoCamion.DESHABILITADO)
        .toList();
  }

  @Override
  public List<Camion> findDisponibles() {
    return storage.values().stream().filter(Camion::estaDisponibleParaAsignar).toList();
  }

  @Override
  public List<Camion> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ids.stream().distinct().map(storage::get).filter(Objects::nonNull).toList();
  }
}
