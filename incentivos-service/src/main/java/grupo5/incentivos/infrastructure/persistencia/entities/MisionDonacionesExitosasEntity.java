package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("EXITOSAS")
@Getter
@Setter
@NoArgsConstructor
public class MisionDonacionesExitosasEntity extends MisionEntity {

  @Column(name = "fecha_ultimo_donacion")
  private LocalDate fechaUltimoDonacion;
}
