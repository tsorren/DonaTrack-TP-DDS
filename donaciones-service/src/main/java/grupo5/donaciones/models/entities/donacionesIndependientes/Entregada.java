package grupo5.donaciones.models.entities.donacionesIndependientes;

public class Entregada implements EstadoDonacion {
  // Estado terminal: no permite ninguna transición.
  // Todos los métodos heredan el default que lanza IllegalStateException.

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.ENTREGADA;
  }
}
