package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.EntregaEntity;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEntregaRepository extends JpaRepository<EntregaEntity, UUID> {
  List<EntregaEntity> findByEstado(EstadoEntrega estado);

  List<EntregaEntity> findByIdRutaOrderByIdEntregaAsc(UUID idRuta);

  List<EntregaEntity> findByIdRutaIsNull();

  boolean existsByIdDonacion(UUID idDonacion);

  Optional<EntregaEntity> findByIdDonacion(UUID idDonacion);

  List<EntregaEntity> findByIdBeneficiarioOrderByIdEntregaAsc(UUID idBeneficiario);
}
