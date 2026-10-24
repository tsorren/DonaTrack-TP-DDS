package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.PaisEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaisRepository extends JpaRepository<PaisEntity, UUID> {
  Optional<PaisEntity> findByNombreIgnoreCase(String nombre);
}
