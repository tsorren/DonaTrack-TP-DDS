package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NecesidadesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Necesidad>
    implements INecesidadesRepository {

  @Override
  public List<Necesidad> findByEstaSatisfechaFalse() {
    return storage.values().stream().filter(n -> !n.estaSatisfecha()).toList();
  }

  @Override
  public List<Necesidad> findByEstaSatisfechaFalseActivaTrue() {
    return storage.values().stream()
        .filter(n -> !n.estaSatisfecha())
        .filter(n -> n instanceof NecesidadRecurrente r && r.getActiva())
        .toList();
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    return storage.values().stream().filter(n -> entidadId.equals(n.getEntidad().getId())).toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalse() {
    return storage.values().stream()
        .filter(n -> !(n instanceof NecesidadRecurrente r) || r.getActiva())
        .filter(n -> !n.estaSatisfecha())
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue() {
    return storage.values().stream()
        .filter(n -> n instanceof NecesidadRecurrente r && r.getActiva())
        .filter(n -> !n.estaSatisfecha())
        .toList();
  }

  @Override
  public List<Necesidad> findByActivaTrueAndSatisfechaFalseAndRecurrenteFalse() {
    return storage.values().stream()
        .filter(n -> n.getTipoNecesidad() == TipoNecesidad.EXTRAORDINARIA)
        .filter(n -> !n.estaSatisfecha())
        .toList();
  }
}
