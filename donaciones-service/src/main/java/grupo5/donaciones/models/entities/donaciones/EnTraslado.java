package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;

public class EnTraslado implements EstadoDonacion {

    @Override
    public void confirmarEntrega(DonacionIndependiente d) {
        d.cambiarEstado(new Entregada(), null);
    }

    @Override
    public void registrarFalla(DonacionIndependiente d, String justificacion) {
        if (justificacion == null || justificacion.isBlank()) {
            throw new IllegalArgumentException(
                    "La justificación es obligatoria para registrar una entrega fallida.");
        }
        d.cambiarEstado(new EntregaFallida(), justificacion);
    }
}