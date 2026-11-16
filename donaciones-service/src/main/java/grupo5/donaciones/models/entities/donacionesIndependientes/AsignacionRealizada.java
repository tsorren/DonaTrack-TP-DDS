package grupo5.donaciones.models.entities.donacionesIndependientes;

public class AsignacionRealizada implements EstadoDonacionIndependiente {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.ASIGNACION_REALIZADA;
  }

  @Override
  public void planificarRuta(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new ListaParaEntregar(), null, actor);
  }

  @Override
  public void planificarRuta(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    planificarRuta(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  @Override
  public void liberarPorCancelacion(DonacionIndependiente d, String actor) {
    // HB-19: Logística confirmó que la entrega no salió; vuelve al stock sin destino
    d.asignarDestino(null);
    d.cambiarEstado(new EnDeposito(), "Su necesidad se dio de baja", actor);
  }
}
