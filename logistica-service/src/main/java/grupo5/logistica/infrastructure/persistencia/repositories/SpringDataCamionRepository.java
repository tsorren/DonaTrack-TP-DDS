package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.CamionEntity;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCamionRepository extends JpaRepository<CamionEntity, UUID> {
  List<CamionEntity> findByEstadoCamion(EstadoCamion estadoCamion);

  List<CamionEntity> findByEstadoCamionNot(EstadoCamion estadoCamion);

  Optional<CamionEntity> findByPatenteIgnoreCase(String patente);
}
