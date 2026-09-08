package grupo5.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class CambioEstadoDonacion {

  private final EstadoDonacion estadoAnterior;
  private final EstadoDonacion estadoNuevo;
  private final LocalDateTime timestamp;

  public CambioEstadoDonacion(EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timestamp = LocalDateTime.now(ZoneId.of("UTC"));
  }
}
