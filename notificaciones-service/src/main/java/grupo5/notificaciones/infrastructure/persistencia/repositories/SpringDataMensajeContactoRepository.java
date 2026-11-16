package grupo5.notificaciones.infrastructure.persistencia.repositories;

import grupo5.notificaciones.infrastructure.persistencia.entities.MensajeContactoEntity;
import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMensajeContactoRepository
    extends JpaRepository<MensajeContactoEntity, Long> {

  Optional<MensajeContactoEntity> findByClaveEnvio(UUID claveEnvio);

  long countByCorreoAndRecibidoEnGreaterThanEqual(String correo, LocalDateTime desde);

  long countByIpOrigenAndRecibidoEnGreaterThanEqual(String ipOrigen, LocalDateTime desde);

  List<MensajeContactoEntity> findByEstado(EstadoMensajeContacto estado, Pageable pageable);

  long countByEstado(EstadoMensajeContacto estado);
}
