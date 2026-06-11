package grupo5.donaciones.models.entities.donaciones;

public class AsignacionRealizada implements EstadoDonacion {

  @Override
  public void planificarRuta(DonacionIndependiente d) {
    d.cambiarEstado(new ListaParaEntregar(), null);
  }
}
