package grupo5.donaciones.models.entities.donacionesIndependientes;

import lombok.Getter;

@Getter
public class Entregada implements EstadoDonacionIndependiente {
  // Estado terminal: no permite ninguna transición.
  // Todos los métodos heredan el default que lanza BusinessStateException.

  /** Patente del camión que confirmó la entrega, si Logística la informó. */
  private final String patenteCamion;

  public Entregada(String patenteCamion) {
    this.patenteCamion = patenteCamion;
  }

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.ENTREGADA;
  }
}
