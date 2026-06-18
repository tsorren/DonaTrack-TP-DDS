package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EnTraslado implements EstadoDonacion {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.EN_TRASLADO;
  }

  @Override
  public void confirmarEntrega(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new Entregada(), null, actor);
  }

  @Override
  public void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    if (justificacion == null || justificacion.isBlank()) {
      throw new IllegalArgumentException(
          "La justificación es obligatoria para registrar una entrega fallida.");
    }
    d.cambiarEstado(new EntregaFallida(), justificacion, actor);
  }
}
