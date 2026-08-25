package grupo5.donaciones.models.algoritmos;

import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.normalizacion.ComparadorTexto;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica extends AlgoritmoAsignacion {

  private final ComparadorTexto comparadorTexto;

  public AlgoritmoCompatibilidadSemantica(ComparadorTexto comparadorTexto) {
    this.comparadorTexto = comparadorTexto;
  }

  @Override
  public List<DonacionIndependiente> filtrarDonaciones(
      Necesidad necesidad, List<DonacionIndependiente> donaciones) {
    List<DonacionIndependiente> filtradas = filtrarPorSubcategoria(necesidad, donaciones);
    return ordenarPorScoreDescendente(filtradas, necesidad);
  }

  private List<DonacionIndependiente> ordenarPorScoreDescendente(
      List<DonacionIndependiente> donaciones, Necesidad necesidad) {
    return donaciones.stream()
        .sorted(
            Comparator.comparing(
                donacion -> calcularScore(necesidad, donacion), Comparator.reverseOrder()))
        .toList();
  }

  private int calcularScore(Necesidad necesidad, DonacionIndependiente donacion) {
    String descripcion = donacion.getDescripcion();
    if (descripcion == null) return 0;
    return comparadorTexto.contarPalabrasEnComun(necesidad.getDescripcion(), descripcion);
  }
}
