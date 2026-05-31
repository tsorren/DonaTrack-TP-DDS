package grupo5.donaciones.models.entities.donaciones;

import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;

public class AsignacionRealizada implements EstadoDonacion {

    @Override
    public void planificarRuta(DonacionIndependiente d) {
        d.cambiarEstado(new ListaParaEntregar(), null);
    }

    @Override
    public void vencer(DonacionIndependiente d) {
        d.cambiarEstado(new Vencida(), null);
    }
}