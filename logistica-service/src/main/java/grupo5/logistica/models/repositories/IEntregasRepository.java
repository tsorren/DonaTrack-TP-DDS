package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEntregasRepository extends CrudRepository<Entrega> {

  List<Entrega> findByEstado(EstadoEntrega estado);

  List<Entrega> findByRutaId(UUID rutaId);

  List<Entrega> findSinRuta();

  /** Si la donación tiene una entrega activa (cualquiera que no esté {@code CANCELADA}). */
  boolean existsByIdDonacion(UUID idDonacion);

  /** La entrega activa de la donación: hay a lo sumo una (las canceladas quedan como historia). */
  Optional<Entrega> findByIdDonacion(UUID idDonacion);

  /** Las entregas canceladas de la donación (HB-19). */
  List<Entrega> findCanceladasByIdDonacion(UUID idDonacion);

  List<Entrega> findByIdBeneficiaria(UUID idBeneficiaria);
}
