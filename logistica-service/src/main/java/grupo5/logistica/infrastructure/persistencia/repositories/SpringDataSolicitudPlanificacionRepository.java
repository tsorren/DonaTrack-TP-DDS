package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.SolicitudPlanificacionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSolicitudPlanificacionRepository
    extends JpaRepository<SolicitudPlanificacionEntity, UUID> {}
