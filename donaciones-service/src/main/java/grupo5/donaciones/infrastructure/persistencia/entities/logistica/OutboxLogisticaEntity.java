package grupo5.donaciones.infrastructure.persistencia.entities.logistica;

import grupo5.donaciones.services.logistica.EstadoEntradaOutbox;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_logistica")
@Getter
@Setter
public class OutboxLogisticaEntity {
  @Id private UUID id;

  @Column(name = "id_solicitud_entrega", nullable = false)
  private UUID solicitudId;

  @Column(name = "proveedor_id", nullable = false)
  private String proveedorId;

  // D6: el pedido completo, como JSON
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false)
  private String payload;

  @Column(name = "trace_id")
  private String traceId;

  @Column(name = "proximo_intento", nullable = false)
  private LocalDateTime proximoIntento;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoEntradaOutbox estado;

  @Column(name = "intentos", nullable = false)
  private Integer intentos;

  @Column(name = "max_intentos", nullable = false)
  private Integer maxIntentos;
}
