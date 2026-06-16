package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;

public interface EstadoDonacion {

  TipoEstadoDonacion getTipo();

  default void registrar(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("registrar");
    lanzarTransicionInvalida();
  }

  default void asignar(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("asignar");
    lanzarTransicionInvalida();
  }

  default void planificarRuta(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("planificarRuta");
    lanzarTransicionInvalida();
  }

  default void iniciarRecorrido(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("iniciarRecorrido");
    lanzarTransicionInvalida();
  }

  default void confirmarEntrega(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("confirmarEntrega");
    lanzarTransicionInvalida();
  }

  default void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    // lanzarTransicionInvalida("registrarFalla");
    lanzarTransicionInvalida();
  }

  default void retornar(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("retornar");
    lanzarTransicionInvalida();
  }

  default void vencer(DonacionIndependiente d, String actor) {
    // lanzarTransicionInvalida("vencer");
    lanzarTransicionInvalida();
  }

  default void lanzarTransicionInvalida() {
    throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
  }
}
