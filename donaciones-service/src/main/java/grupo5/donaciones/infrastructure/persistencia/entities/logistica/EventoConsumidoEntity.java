package grupo5.donaciones.infrastructure.persistencia.entities.logistica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evento_consumido")
@Getter
@Setter
public class EventoConsumidoEntity {
  @Id private UUID id;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "queue_name")
  private String queueName;

  @Column(name = "business_id")
  private UUID businessId;

  @Column(name = "id_donacion_independiente")
  private UUID donacionId;

  @Column(name = "fecha_procesamiento", nullable = false)
  private LocalDateTime fechaProcesamiento;
}
