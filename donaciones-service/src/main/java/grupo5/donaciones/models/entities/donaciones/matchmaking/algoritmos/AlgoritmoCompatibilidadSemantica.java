package grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos;

import grupo5.donaciones.infraestructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.ArrayList;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica extends AlgoritmoAsignacion {

    private final ComparadorTexto comparadorTexto;

    public AlgoritmoCompatibilidadSemantica(ComparadorTexto comparadorTexto) {
        this.comparadorTexto = comparadorTexto;
    }

    @Override
    public List<DonacionIndependiente> filtrarDonaciones(
            Necesidad necesidad, List<DonacionIndependiente> donaciones) {
        validarParametrosFiltrado(necesidad, donaciones);
        List<DonacionIndependiente> filtradas = new ArrayList<>();
        for (DonacionIndependiente donacion : donaciones) {
            if (mismaSubcategoria(donacion, necesidad)) {
                filtradas.add(donacion);
            }
        }
        return ordenarPorScoreDescendente(filtradas, necesidad);
    }

    private List<DonacionIndependiente> ordenarPorScoreDescendente(
            List<DonacionIndependiente> donaciones, Necesidad necesidad) {
        List<DonacionIndependiente> ordenadas = new ArrayList<>(donaciones);
        int n = ordenadas.size();
        for (int i = 0; i < n - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (calcularScore(necesidad, ordenadas.get(j)) > calcularScore(necesidad, ordenadas.get(maxIdx))) {
                    maxIdx = j;
                }
            }
            DonacionIndependiente temp = ordenadas.get(i);
            ordenadas.set(i, ordenadas.get(maxIdx));
            ordenadas.set(maxIdx, temp);
        }
        return ordenadas;
    }

    private int calcularScore(Necesidad necesidad, DonacionIndependiente donacion) {
        String descripcion = donacion.getDescripcion();
        if (descripcion == null) return 0;
        return comparadorTexto.contarPalabrasEnComun(necesidad.getDescripcion(), descripcion);
    }
}
