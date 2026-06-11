package grupo5.donaciones.models.entities.donaciones;

public class ListaParaEntregar implements EstadoDonacion {

  @Override
  public void iniciarRecorrido(DonacionIndependiente d) {
    d.cambiarEstado(new EnTraslado(), null);
  }
}
