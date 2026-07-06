package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;

public interface EstadoDonacion {

  TipoEstadoDonacion getTipo();

  default void registrar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void asignar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void planificarRuta(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void iniciarRecorrido(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void confirmarEntrega(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    lanzarTransicionInvalida();
  }

  default void retornar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void replanificar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void vencer(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void lanzarTransicionInvalida() {
    throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
  }
}
