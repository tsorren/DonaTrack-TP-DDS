package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.EntregaEntity;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEntregaRepository extends JpaRepository<EntregaEntity, UUID> {
  List<EntregaEntity> findByEstado(EstadoEntrega estado);

  List<EntregaEntity> findByIdRutaOrderByIdEntregaAsc(UUID idRuta);

  List<EntregaEntity> findByIdRutaIsNull();

  boolean existsByIdDonacion(UUID idDonacion);
}
