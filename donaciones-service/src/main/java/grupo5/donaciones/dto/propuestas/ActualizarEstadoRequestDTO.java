package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarEstadoRequestDTO {
  private EstadoPropuesta estado;
}
