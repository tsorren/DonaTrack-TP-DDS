package grupo5.donaciones.dto;

import grupo5.common.repositories.RecursoDTO;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class EjecucionAsignacionDTO implements RecursoDTO {

  private UUID id;
  private LocalDateTime fechaEjecucion;
  private Integer cantidadPropuestasGeneradas;
}
