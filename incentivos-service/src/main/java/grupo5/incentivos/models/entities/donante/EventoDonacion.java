package grupo5.incentivos.models.entities.donante;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventoDonacion {

  private final Long donacionId;
  private final List<String> categorias;
  private final Integer cantidadBienes;
  private final LocalDate fecha;
}
