package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.logistica.SolicitudEntregaEntity;
import grupo5.donaciones.models.entities.logistica.EstadoSolicitudEntrega;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSolicitudEntregaRepository
    extends JpaRepository<SolicitudEntregaEntity, UUID> {

  Optional<SolicitudEntregaEntity> findFirstByDonacionIndependienteIdAndEstadoNot(
      UUID donacionIndependienteId, EstadoSolicitudEntrega estado);

  Optional<SolicitudEntregaEntity> findFirstByDonacionIndependienteIdOrderByFechaCreacionDesc(
      UUID donacionIndependienteId);
}
