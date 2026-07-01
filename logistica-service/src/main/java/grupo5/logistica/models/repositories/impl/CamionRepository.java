package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.repositories.ICamionRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CamionRepository extends CrudRepositoryEnMemoria<Camion> implements ICamionRepository {

  @Override
  public List<Camion> findByEstado(EstadoCamion estado) {
    return findAll().stream().filter(camion -> camion.getEstado() == estado).toList();
  }
}
