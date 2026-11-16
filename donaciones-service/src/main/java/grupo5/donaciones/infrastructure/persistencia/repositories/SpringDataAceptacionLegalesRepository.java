package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.AceptacionLegalesEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAceptacionLegalesRepository
    extends JpaRepository<AceptacionLegalesEntity, UUID> {
  Optional<AceptacionLegalesEntity> findFirstByPersonaIdOrderByVersionDesc(UUID personaId);
}
