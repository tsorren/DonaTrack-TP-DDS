package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.repositories.IEntregasRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EntregasRepository extends CrudRepositoryEnMemoria<Entrega>
    implements IEntregasRepository {

  @Override
  public List<Entrega> findByEstado(EstadoEntrega estado) {
    return storage.values().stream()
        .filter(entrega -> entrega.getEstadoActual() == estado)
        .toList();
  }

  @Override
  public List<Entrega> findByRutaId(UUID rutaId) {
    return storage.values().stream()
        .filter(entrega -> Objects.equals(entrega.getIdRuta(), rutaId))
        .toList();
  }

  @Override
  public List<Entrega> findSinRuta() {
    return storage.values().stream().filter(entrega -> entrega.getIdRuta() == null).toList();
  }
}
