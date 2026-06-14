package grupo5.donaciones.models.entities.donacionesIndependientes;

public interface EstadoDonacion {

  default void registrar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("registrar");
  }

  default void asignar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("asignar");
  }

  default void planificarRuta(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("planificarRuta");
  }

  default void iniciarRecorrido(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("iniciarRecorrido");
  }

  default void confirmarEntrega(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("confirmarEntrega");
  }

  default void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    lanzarTransicionInvalida("registrarFalla");
  }

  default void retornar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("retornar");
  }

  default void vencer(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida("vencer");
  }

  default void lanzarTransicionInvalida(String accion) {
    throw new IllegalStateException(
        "Transición inválida: no se puede ejecutar '"
            + accion
            + "' desde el estado "
            + this.getClass().getSimpleName());
  }
}
