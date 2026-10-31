package grupo5.donaciones.infrastructure.persistencia.entities.necesidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "periodo_necesidad")
@Getter
@Setter
public class PeriodoNecesidadEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "necesidad_id", nullable = false)
  private NecesidadRecurrenteEntity necesidad;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDateTime fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDateTime fechaFin;

  @Column(name = "cantidad_objetivo", nullable = false)
  private Long cantidadObjetivo;

  public PeriodoNecesidadEntity() {
    this.id = UUID.randomUUID();
  }
}
