package grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
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

/** Cada vez que una donación independiente entra a un estado (D-21). */
@Entity
@Table(name = "estado_donacion_independiente")
@Getter
@Setter
public class EstadoDonacionIndependienteEntity {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_donacion_independiente", nullable = false)
  private DonacionIndependienteEntity donacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 25)
  private TipoEstadoDonacion tipo;

  @Column(name = "patente_camion")
  private String patenteCamion;

  // Del DER, sin uso: el dominio no tiene plazo de replanificación
  @Column(name = "replanificable_hasta")
  private LocalDateTime replanificableHasta;
}
