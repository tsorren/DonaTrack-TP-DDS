package grupo5.donaciones.infrastructure.persistencia.entities.necesidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("EXTRAORDINARIA")
@Getter
@Setter
public class NecesidadExtraordinariaEntity extends NecesidadEntity {
  /** Nula en las creadas antes de exigirla (V17). */
  @Column(name = "fecha_limite")
  private LocalDate fechaLimite;
}
