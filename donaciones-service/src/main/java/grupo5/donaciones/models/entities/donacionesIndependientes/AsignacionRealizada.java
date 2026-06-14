package grupo5.donaciones.models.entities.donacionesIndependientes;

public class AsignacionRealizada implements EstadoDonacion {

  @Override
  public void planificarRuta(DonacionIndependiente d, String actor) {
    d.cambiarEstado(new ListaParaEntregar(), null, actor);
  }

}
