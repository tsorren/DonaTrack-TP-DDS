package grupo5.donaciones.infrastructure.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacion {

  @Override
  public List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades) {
    if (necesidades == null)
      throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);
    LocalDateTime hace3meses = LocalDateTime.now(ZoneId.systemDefault()).minusMonths(3);

    Map<EntidadBeneficiaria, Integer> donacionesPorEntidad = new HashMap<>();
    for (Necesidad necesidad : necesidades) {
      if (necesidad.getEntidad() == null) continue;
      int cantidad = contarDonacionesRecientes(necesidad, hace3meses);
      int totalActual = donacionesPorEntidad.getOrDefault(necesidad.getEntidad(), 0);
      donacionesPorEntidad.put(necesidad.getEntidad(), totalActual + cantidad);
    }

    List<Necesidad> ordenadas = new ArrayList<>(necesidades);
    ordenadas.sort(
        (a, b) -> {
          int donA = donacionesPorEntidad.getOrDefault(a.getEntidad(), 0);
          int donB = donacionesPorEntidad.getOrDefault(b.getEntidad(), 0);
          return Integer.compare(donA, donB);
        });
    return ordenadas;
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
    return filtradas;
  }

  private static int contarDonacionesRecientes(Necesidad necesidad, LocalDateTime desde) {
    if (!(necesidad instanceof NecesidadExtraordinaria)) return 0;
    List<DonacionIndependiente> asignadas =
        ((NecesidadExtraordinaria) necesidad).getDonacionesAsignadas();
    if (asignadas == null) return 0;
    int contador = 0;
    for (DonacionIndependiente donacion : asignadas) {
      if (donacion.getFechaRegistro().isAfter(desde)) {
        contador++;
      }
    }
    return contador;
  }
}
