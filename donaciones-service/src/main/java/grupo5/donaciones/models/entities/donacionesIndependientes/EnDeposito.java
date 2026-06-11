package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EnDeposito implements EstadoDonacion {

  @Override
  public void registrar(DonacionIndependiente d) {
    d.cambiarEstado(new AsignacionRealizada(), null);
  }

  @Override
  public void asignar(DonacionIndependiente d) {
    d.cambiarEstado(new AsignacionRealizada(), null);
  }

  @Override
  public void vencer(DonacionIndependiente d) {
    d.cambiarEstado(new Vencida(), null);
  }
}
