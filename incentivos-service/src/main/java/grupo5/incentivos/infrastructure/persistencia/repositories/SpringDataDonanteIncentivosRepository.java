package grupo5.incentivos.infrastructure.persistencia.repositories;

import grupo5.incentivos.infrastructure.persistencia.entities.DonanteIncentivosEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDonanteIncentivosRepository
    extends JpaRepository<DonanteIncentivosEntity, UUID> {

  Optional<DonanteIncentivosEntity> findByPersonaId(UUID personaId);
}
