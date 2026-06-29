package grupo5.logistica.models.entities.entregas;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioEstadoEntrega {
  private EstadoEntrega estadoAnterior;
  private EstadoEntrega estadoNuevo;
  private LocalDateTime timeStamp;
  private String actor;

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
