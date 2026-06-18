package grupo5.incentivos.models.entities.donante;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EventoDonacion {

  private final UUID donacionId;
  private final List<String> categorias;
  private final Integer cantidadBienes;
  private final LocalDate fecha;
}
