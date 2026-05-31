package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;

public class ListaParaEntregar implements EstadoDonacion {

    @Override
    public void iniciarRecorrido(DonacionIndependiente d) {
        d.cambiarEstado(new EnTraslado(), null);
    }

    @Override
    public void vencer(DonacionIndependiente d) {
        d.cambiarEstado(new Vencida(), null);
    }
}