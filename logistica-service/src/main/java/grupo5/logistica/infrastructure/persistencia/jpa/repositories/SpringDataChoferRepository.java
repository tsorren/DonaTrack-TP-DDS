package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.ChoferEntity;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChoferRepository extends JpaRepository<ChoferEntity, UUID> {
  List<ChoferEntity> findByEstadoChofer(EstadoChofer estadoChofer);

  List<ChoferEntity> findByEstadoChoferNot(EstadoChofer estadoChofer);
}
