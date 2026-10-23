package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.SolicitudPlanificacionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSolicitudPlanificacionRepository
    extends JpaRepository<SolicitudPlanificacionEntity, UUID> {}
