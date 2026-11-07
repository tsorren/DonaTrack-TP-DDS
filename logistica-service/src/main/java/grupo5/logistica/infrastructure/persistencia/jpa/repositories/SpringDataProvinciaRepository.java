package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.ProvinciaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProvinciaRepository extends JpaRepository<ProvinciaEntity, UUID> {
  Optional<ProvinciaEntity> findByPais_IdPaisAndNombreIgnoreCase(UUID idPais, String nombre);
}
