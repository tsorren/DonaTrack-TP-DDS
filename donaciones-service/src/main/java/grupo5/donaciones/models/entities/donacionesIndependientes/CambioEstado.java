package grupo5.donaciones.models.entities.donacionesIndependientes;

import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.Getter;

@Getter
public class CambioEstado {

  private final EstadoDonacion estadoAnterior;
  private final EstadoDonacion estadoNuevo;
  private final LocalDateTime timestamp;
  private final String justificacion;

  public CambioEstado(
      EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo, String justificacion) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timestamp = LocalDateTime.now(ZoneId.systemDefault());
    this.justificacion = justificacion;
  }
}
