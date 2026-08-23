package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.UUID;

public interface IEntregasRepository extends CrudRepository<Entrega> {

  List<Entrega> findByEstado(EstadoEntrega estado);

  List<Entrega> findByRutaId(UUID rutaId);

  List<Entrega> findSinRuta();
}
