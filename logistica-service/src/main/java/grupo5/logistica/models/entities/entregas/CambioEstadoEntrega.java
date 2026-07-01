package grupo5.logistica.models.entities.entregas;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CambioEstadoEntrega {
  private final EstadoEntrega estadoAnterior;
  private final EstadoEntrega estadoNuevo;
  private final LocalDateTime timeStamp;
  private final String actor;

  public CambioEstadoEntrega(
      EstadoEntrega estadoAnterior,
      EstadoEntrega estadoNuevo,
      LocalDateTime timeStamp,
      String actor) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timeStamp = timeStamp;
    this.actor = actor;
  }
}
