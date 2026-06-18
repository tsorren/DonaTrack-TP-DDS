package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EnDeposito implements EstadoDonacion {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.EN_DEPOSITO;
  }

  @Override
  public void asignar(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new AsignacionRealizada(), null, actor);
  }

  @Override
  public void vencer(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new Vencida(), null, actor);
  }
}
