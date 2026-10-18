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
}
