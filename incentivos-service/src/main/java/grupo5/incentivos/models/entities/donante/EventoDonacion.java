package grupo5.incentivos.models.entities.donante;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventoDonacion {

  private final Long donacionId;
  private final Long organizacionId;
  private final String subcategoria;
  private final Integer cantidadBienes;
  private final LocalDate fecha;
  private final boolean exitosa;
}
