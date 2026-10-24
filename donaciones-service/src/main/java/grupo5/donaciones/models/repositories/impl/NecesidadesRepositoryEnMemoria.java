package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class NecesidadesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Necesidad>
    implements INecesidadesRepository {

  @Override
  public List<Necesidad> findActivas() {
    return storage.values().stream().filter(Necesidad::isActiva).toList();
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    return storage.values().stream().filter(n -> entidadId.equals(n.getEntidadId())).toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndRecurrenteTrue() {
    return storage.values().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.RECURRENTE)
        .filter(Necesidad::isActiva)
        .toList();
  }

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return storage.values().stream()
        .anyMatch(n -> Objects.equals(subcategoriaId, n.getSubcategoriaId()));
  }
}
