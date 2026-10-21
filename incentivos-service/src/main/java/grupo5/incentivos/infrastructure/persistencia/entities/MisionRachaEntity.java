package grupo5.incentivos.infrastructure.persistencia.entities;

import grupo5.incentivos.infrastructure.persistencia.YearMonthAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.YearMonth;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("RACHA")
@Getter
@Setter
@NoArgsConstructor
public class MisionRachaEntity extends MisionEntity {

  @Convert(converter = YearMonthAttributeConverter.class)
  @Column(name = "ultimo_mes_donado", length = 7)
  private YearMonth ultimoMesDonado;
}
