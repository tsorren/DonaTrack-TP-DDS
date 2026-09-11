package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;

public interface EstadoDonacionIndependiente {

  TipoEstadoDonacion getTipo();

  default void registrar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void asignar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void asignar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    asignar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void planificarRuta(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void planificarRuta(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    planificarRuta(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void iniciarRecorrido(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void iniciarRecorrido(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    iniciarRecorrido(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void confirmarEntrega(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void confirmarEntrega(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    confirmarEntrega(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void registrarFalla(DonacionIndependiente d, String justificacion, String actor) {
    lanzarTransicionInvalida();
  }

  default void registrarFalla(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    registrarFalla(
        d,
        solicitud != null ? solicitud.getJustificacion() : null,
        solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void retornar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void retornar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    retornar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void replanificar(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void replanificar(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    replanificar(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void vencer(DonacionIndependiente d, String actor) {
    lanzarTransicionInvalida();
  }

  default void vencer(
      DonacionIndependiente d, SolicitudCambioEstadoDonacionIndependiente solicitud) {
    vencer(d, solicitud != null ? solicitud.getActor() : "SISTEMA");
  }

  default void lanzarTransicionInvalida() {
    throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
  }
}
