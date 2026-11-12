package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.actividad.ActividadEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataActividadRepository extends JpaRepository<ActividadEntity, UUID> {}
