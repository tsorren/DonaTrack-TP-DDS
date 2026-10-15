package grupo5.notificaciones.infrastructure.persistencia.repositories;

import grupo5.notificaciones.infrastructure.persistencia.entities.NotificacionEntity;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataNotificacionRepository extends JpaRepository<NotificacionEntity, UUID> {
  List<NotificacionEntity> findByEstadoNotificacion(EstadoNotificacion estadoNotificacion);

  List<NotificacionEntity> findByPersonaId(UUID personaId);

  List<NotificacionEntity> findByPersonaIdAndLeida(UUID personaId, Boolean leida);

  long countByPersonaIdAndLeidaFalse(UUID personaId);

  @Modifying
  @Query(
      "UPDATE NotificacionEntity n SET n.leida = true, n.fechaLectura = CURRENT_TIMESTAMP WHERE n.personaId = :personaId AND n.leida = false")
  void marcarTodasComoLeidas(@Param("personaId") UUID personaId);
}
