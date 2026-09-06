package grupo5.donaciones.models.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
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
    LocalDateTime hace3meses = LocalDateTime.now(ZoneId.of("UTC")).minusMonths(3);

    Map<java.util.UUID, Integer> donacionesPorEntidad = new HashMap<>();
    for (Necesidad necesidad : necesidades) {
      if (necesidad.getEntidadId() == null) continue;
      int cantidad = contarDonacionesRecientes(necesidad, hace3meses);
      int totalActual = donacionesPorEntidad.getOrDefault(necesidad.getEntidadId(), 0);
      donacionesPorEntidad.put(necesidad.getEntidadId(), totalActual + cantidad);
    }

    List<Necesidad> ordenadas = new ArrayList<>(necesidades);
    ordenadas.sort(
        (a, b) -> {
          int donA = donacionesPorEntidad.getOrDefault(a.getEntidadId(), 0);
          int donB = donacionesPorEntidad.getOrDefault(b.getEntidadId(), 0);
          return Integer.compare(donA, donB);
        });
    return ordenadas;
  }

  @Override
  public List<DonacionIndependiente> filtrarDonaciones(
      Necesidad necesidad, List<DonacionIndependiente> donaciones) {
    return filtrarPorSubcategoria(necesidad, donaciones);
  }

  private static int contarDonacionesRecientes(Necesidad necesidad, LocalDateTime desde) {
    if (necesidad == null || desde == null) return 0;
    return necesidad.contarDonacionesAsignadasDesde(desde);
  }
}
