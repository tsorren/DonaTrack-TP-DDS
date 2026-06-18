package grupo5.donaciones.infrastructure.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.infrastructure.analizadores.ComparadorTexto;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.ArrayList;
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
    if (necesidad == null) throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDAD_NULA);
    if (donaciones == null) throw new ValidationException(ErrorCatalog.ALGORITMO_DONACIONES_NULAS);
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
