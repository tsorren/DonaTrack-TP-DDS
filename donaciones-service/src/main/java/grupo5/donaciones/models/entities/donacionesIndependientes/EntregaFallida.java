package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EntregaFallida implements EstadoDonacionIndependiente {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.ENTREGA_FALLIDA;
  }

  @Override
  public void retornar(DonacionIndependiente d, String actor) {
    // Vuelve al stock: deja de estar asignada y el algoritmo la puede asignar a otra necesidad.
    // El reintento hacia el mismo destino es replanificar, que no pasa por el depósito.
    d.asignarDestino(null);
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
