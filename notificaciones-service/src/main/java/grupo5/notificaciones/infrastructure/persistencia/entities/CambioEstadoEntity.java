package grupo5.notificaciones.infrastructure.persistencia.entities;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacion_historial_estado")
@Getter
@Setter
@NoArgsConstructor
public class CambioEstadoEntity {
  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_anterior", length = 20)
  private EstadoNotificacion estadoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo", nullable = false, length = 20)
  private EstadoNotificacion estadoNuevo;

  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;
}
