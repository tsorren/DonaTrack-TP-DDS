package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.logistica.OutboxLogisticaEntity;
import grupo5.donaciones.services.logistica.EstadoEntradaOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataOutboxLogisticaRepository
    extends JpaRepository<OutboxLogisticaEntity, UUID> {

  List<OutboxLogisticaEntity> findByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntento(
      EstadoEntradaOutbox estado, LocalDateTime ahora);
}
