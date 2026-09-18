package grupo5.donaciones.models.entities.donacionesIndependientes;

public class Vencida implements EstadoDonacionIndependiente {
  // Estado terminal: no permite ninguna transición.
  // Todos los métodos heredan el default que lanza BusinessStateException.

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.VENCIDA;
  }
}
