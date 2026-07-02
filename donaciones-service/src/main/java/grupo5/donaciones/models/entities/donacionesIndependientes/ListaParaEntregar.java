package grupo5.donaciones.models.entities.donacionesIndependientes;

public class ListaParaEntregar implements EstadoDonacion {

  @Override
  public TipoEstadoDonacion getTipo() {
    return TipoEstadoDonacion.LISTA_PARA_ENTREGAR;
  }

  @Override
  public void iniciarRecorrido(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new EnTraslado(), null, actor);
  }
}
