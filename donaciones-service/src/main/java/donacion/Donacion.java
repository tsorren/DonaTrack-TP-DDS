package donacion;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import donante.Donante;

@Data
public class Donacion {
    private List<DonacionIndependiente> donaciones = new ArrayList<>();
    private Donante donante;

    public void agregarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
        donaciones.add(donacionIndependiente);
    }

    public void quitarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
        donaciones.remove(donacionIndependiente);
    }
}