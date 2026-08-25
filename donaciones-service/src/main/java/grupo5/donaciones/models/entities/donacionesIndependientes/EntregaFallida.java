package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EntregaFallida implements EstadoDonacion {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.ENTREGA_FALLIDA;
  }

  @Override
  public void retornar(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new EnDeposito(), null, actor);
  }

  @Override
  public void retornar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    retornar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  @Override
  public void replanificar(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new AsignacionRealizada(), null, actor);
  }

  @Override
  public void replanificar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    replanificar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }
}
