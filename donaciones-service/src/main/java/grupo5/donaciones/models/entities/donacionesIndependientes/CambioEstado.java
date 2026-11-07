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
    this.timestamp = LocalDateTime.now(ZoneId.systemDefault());
    this.justificacion = justificacion;
    this.actor = actor;
  }

  /** Constructor de reconstitución para el adaptador JPA: conserva el momento del cambio. */
  public CambioEstado(
      EstadoDonacionIndependiente estadoAnterior,
      EstadoDonacionIndependiente estadoNuevo,
      LocalDateTime timestamp,
      String justificacion,
      String actor) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timestamp = timestamp;
    this.justificacion = justificacion;
    this.actor = actor;
  }
}
