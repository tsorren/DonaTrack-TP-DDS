package grupo5.notificaciones.infrastructure.persistencia.entities;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionEntity {
  @Id private UUID id;

  @Column(name = "persona_id", nullable = false)
  private UUID personaId;

  @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
  private String mensaje;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_notificacion", nullable = false, length = 20)
  private EstadoNotificacion estadoNotificacion;

  @SuppressWarnings(
      "squid:S1319") // FetchType.EAGER justificado: Agregado Notificacion requiere consistencia
  // total de su historial
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "notificacion_historial_estado",
      joinColumns = @JoinColumn(name = "notificacion_id", nullable = false))
  private List<CambioEstadoEmbeddable> historialEstado = new ArrayList<>();
}
