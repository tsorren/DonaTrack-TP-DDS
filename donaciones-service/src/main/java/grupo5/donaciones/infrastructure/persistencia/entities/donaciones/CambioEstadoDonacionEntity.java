package grupo5.donaciones.infrastructure.persistencia.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Historial append-only de los estados de una donación. */
@Entity
@Table(name = "cambio_estado_donacion")
@Getter
@Setter
public class CambioEstadoDonacionEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_donacion", nullable = false)
  private DonacionEntity donacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_anterior", length = 20)
  private EstadoDonacion estadoAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo", nullable = false, length = 20)
  private EstadoDonacion estadoNuevo;

  @Column(name = "\"timestamp\"", nullable = false)
  private LocalDateTime timestamp;
}
