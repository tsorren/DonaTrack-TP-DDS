package grupo5.donaciones.models.entities.donacionesIndependientes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class CambioEstado {

  private final EstadoDonacionIndependiente estadoAnterior;
  private final EstadoDonacionIndependiente estadoNuevo;
  private final LocalDateTime timestamp;
  private final String justificacion;
  private final String actor; // quién realizó la transición (ej: "admin@donatrack.org", "SISTEMA")

  public CambioEstado(
      EstadoDonacionIndependiente estadoAnterior,
      EstadoDonacionIndependiente estadoNuevo,
      String justificacion,
      String actor) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timestamp = LocalDateTime.now(ZoneId.of("UTC"));
    this.justificacion = justificacion;
    this.actor = actor;
  }
}
