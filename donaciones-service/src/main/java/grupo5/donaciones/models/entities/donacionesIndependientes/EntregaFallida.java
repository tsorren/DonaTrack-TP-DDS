package grupo5.donaciones.models.entities.donacionesIndependientes;

public class EntregaFallida implements EstadoDonacion {

  @Override
  public void retornar(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new EnDeposito(), null, actor);
  }
}
