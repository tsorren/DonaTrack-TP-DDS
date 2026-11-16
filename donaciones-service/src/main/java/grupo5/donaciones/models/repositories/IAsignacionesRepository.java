package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Comparator;
import java.util.List;

public interface IAsignacionesRepository {
  EjecucionAsignacion save(EjecucionAsignacion aggregate);

  List<EjecucionAsignacion> obtenerHistorial();

  /**
   * Una página del historial, de la más reciente a la más vieja (R11-24). Las ejecuciones viven en
   * memoria y son pocas por día: se ordenan y se cortan acá.
   */
  default Tramo<EjecucionAsignacion> historial(Paginado paginado) {
    return Tramo.de(
        obtenerHistorial().stream()
            .sorted(
                Comparator.comparing(EjecucionAsignacion::getFechaEjecucion)
                    .reversed()
                    .thenComparing(EjecucionAsignacion::getId))
            .toList(),
        paginado);
  }

  void deleteAll();
}
