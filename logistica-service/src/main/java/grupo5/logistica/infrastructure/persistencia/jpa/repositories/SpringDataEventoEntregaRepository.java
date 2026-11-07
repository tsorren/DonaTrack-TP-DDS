package grupo5.logistica.infrastructure.persistencia.jpa.repositories;

import grupo5.logistica.infrastructure.persistencia.jpa.entities.EventoEntregaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEventoEntregaRepository
    extends JpaRepository<EventoEntregaEntity, UUID> {}
