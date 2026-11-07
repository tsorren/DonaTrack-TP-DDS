package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.SolicitudTransicionEntregaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSolicitudTransicionEntregaRepository
    extends JpaRepository<SolicitudTransicionEntregaEntity, UUID> {}
