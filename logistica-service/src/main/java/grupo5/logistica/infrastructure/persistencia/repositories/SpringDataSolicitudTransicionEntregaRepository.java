package grupo5.logistica.infrastructure.persistencia.repositories;

import grupo5.logistica.infrastructure.persistencia.entities.SolicitudTransicionEntregaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSolicitudTransicionEntregaRepository
    extends JpaRepository<SolicitudTransicionEntregaEntity, UUID> {}
