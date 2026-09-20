package grupo5.donaciones.dto.propuestas;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionAsignacionDTO {
  private UUID id;
  private LocalDateTime fechaEjecucion;
  private Integer cantidadPropuestasGeneradas;
}
