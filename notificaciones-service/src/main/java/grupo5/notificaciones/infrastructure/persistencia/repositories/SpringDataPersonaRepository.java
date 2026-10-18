package grupo5.notificaciones.infrastructure.persistencia.repositories;

import grupo5.notificaciones.infrastructure.persistencia.entities.PersonaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPersonaRepository extends JpaRepository<PersonaEntity, UUID> {}
