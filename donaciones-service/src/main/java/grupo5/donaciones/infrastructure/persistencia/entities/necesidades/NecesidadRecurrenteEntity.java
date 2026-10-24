package grupo5.donaciones.infrastructure.persistencia.entities.necesidades;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("RECURRENTE")
@Getter
@Setter
public class NecesidadRecurrenteEntity extends NecesidadEntity {
  /** {@link java.time.Period} en ISO-8601 ({@code P1M}, {@code P1W}, {@code P1Y2M}). */
  @Column(name = "periodo", length = 20)
  private String periodo;

  @OneToMany(
      mappedBy = "necesidad",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<PeriodoNecesidadEntity> periodos = new ArrayList<>();
}
