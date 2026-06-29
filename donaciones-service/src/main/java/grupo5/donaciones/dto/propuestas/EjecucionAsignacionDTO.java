package grupo5.donaciones.dto.propuestas;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class EjecucionAsignacionDTO {
  private UUID id;
  private LocalDateTime fechaEjecucion;
  private Integer cantidadPropuestasGeneradas;

  public EjecucionAsignacionDTO() {
    this.id = UUID.randomUUID();
  }
}
