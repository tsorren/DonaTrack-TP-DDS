package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;

public interface EstadoDonacion {

    default void registrar(DonacionIndependiente d) {
        lanzarTransicionInvalida("registrar");
    }

    default void asignar(DonacionIndependiente d) {
        lanzarTransicionInvalida("asignar");
    }

    default void planificarRuta(DonacionIndependiente d) {
        lanzarTransicionInvalida("planificarRuta");
    }

    default void iniciarRecorrido(DonacionIndependiente d) {
        lanzarTransicionInvalida("iniciarRecorrido");
    }

    default void confirmarEntrega(DonacionIndependiente d) {
        lanzarTransicionInvalida("confirmarEntrega");
    }

    default void registrarFalla(DonacionIndependiente d, String justificacion) {
        lanzarTransicionInvalida("registrarFalla");
    }

    default void retornar(DonacionIndependiente d) {
        lanzarTransicionInvalida("retornar");
    }

    default void vencer(DonacionIndependiente d) {
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