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
  @Column(name = "periodo")
  private Integer periodo;

  @OneToMany(
      mappedBy = "necesidad",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<PeriodoNecesidadEntity> periodos = new ArrayList<>();
}
