package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.repositories.IChoferesRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ChoferesRepository extends CrudRepositoryEnMemoria<Chofer>
    implements IChoferesRepository {

  @Override
  public List<Chofer> findActivos() {
    return storage.values().stream()
        .filter(chofer -> chofer.getEstado() != EstadoChofer.DESHABILITADO)
        .toList();
  }

  @Override
  public List<Chofer> findDisponibles() {
    return storage.values().stream().filter(Chofer::estaDisponibleParaAsignar).toList();
  }
}
