package grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Historial append-only de los estados de una donación independiente. */
@Entity
@Table(name = "cambio_estado_donacion_independiente")
@Getter
@Setter
public class CambioEstadoDonacionIndependienteEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_donacion_independiente", nullable = false)
  private DonacionIndependienteEntity donacion;

  // Ids y no asociaciones: las FK a estado_donacion_independiente se verifican al confirmar la
  // transacción (V10), así no importa en qué orden Hibernate inserta o borra las filas
  @Column(name = "id_estado_anterior")
  private UUID estadoAnteriorId;

  @Column(name = "id_estado_nuevo", nullable = false)
  private UUID estadoNuevoId;

  @Column(name = "\"timestamp\"", nullable = false)
  private LocalDateTime timestamp;

  @Column(name = "actor")
  private String actor;

  // D-22
  @Column(name = "justificacion")
  private String justificacion;
}
