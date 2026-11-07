package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.logistica.EventoConsumidoEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataEventoConsumidoRepository
    extends JpaRepository<EventoConsumidoEntity, UUID> {

  // «is not distinct from» compara nulos como iguales, igual que la restricción única de V12
  @Query(
      value =
          "select exists (select 1 from donaciones.evento_consumido where event_type = :eventType"
              + " and business_id is not distinct from cast(:businessId as uuid)"
              + " and id_donacion_independiente is not distinct from cast(:donacionId as uuid))",
      nativeQuery = true)
  boolean existe(
      @Param("eventType") String eventType,
      @Param("businessId") UUID businessId,
      @Param("donacionId") UUID donacionId);
}
