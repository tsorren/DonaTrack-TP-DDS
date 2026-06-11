package grupo5.donaciones.models.entities.donaciones;

public class EntregaFallida implements EstadoDonacion {

  @Override
  public void retornar(DonacionIndependiente d) {
    d.cambiarEstado(new EnDeposito(), null);
  }
}
