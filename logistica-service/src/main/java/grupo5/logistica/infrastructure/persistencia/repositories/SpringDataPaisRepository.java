package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.PaisEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaisRepository extends JpaRepository<PaisEntity, UUID> {
  Optional<PaisEntity> findByNombreIgnoreCase(String nombre);
}
