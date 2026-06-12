package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EntregaFallida implements EstadoDonacion {

  @Override
  public void retornar(DonacionIndependiente d) {
    d.cambiarEstado(new EnDeposito(), null);
  }

  @Override
  public void vencer(DonacionIndependiente d) {
    d.cambiarEstado(new Vencida(), null);
  }
}
