package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import lombok.Data;

@Data
public class Donacion {
    private java.util.List<DonacionIndependiente> donaciones = new java.util.ArrayList<>();
    private Donante donante;

    public void agregarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
        this.donaciones.add(donacionIndependiente);
    }

    public void quitarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
        this.donaciones.remove(donacionIndependiente);
    }
}