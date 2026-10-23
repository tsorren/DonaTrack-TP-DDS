package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.algoritmos.DatosDeAsignacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Lo asignado a cada necesidad, calculado con consultas sobre las donaciones independientes. Existe
 * porque la necesidad ya no guarda sus donaciones (DTI-06): los servicios le piden el acumulado acá
 * y se lo pasan a la necesidad o a los algoritmos.
 */
@Component
public class AcumuladosDeAsignacion {

  /** Ventana del algoritmo de prioridad a sub-atendidos. */
  static final int MESES_SUB_ATENDIDOS = 3;

  private final IDonacionesIndependientesRepository donacionesIndependientes;

  public AcumuladosDeAsignacion(IDonacionesIndependientesRepository donacionesIndependientes) {
    this.donacionesIndependientes = donacionesIndependientes;
  }

  /** Lo asignado a la necesidad (al período actual, si es recurrente). */
  public int asignadoA(Necesidad necesidad) {
    return donacionesIndependientes.sumarCantidadAsignada(
        necesidad.getId(), necesidad.periodoActualId());
  }

  public boolean estaSatisfecha(Necesidad necesidad) {
    return necesidad.estaSatisfechaCon(asignadoA(necesidad));
  }

  public DatosDeAsignacion datosPara(List<Necesidad> necesidades) {
    Map<UUID, Integer> asignado = new HashMap<>();
    necesidades.forEach(n -> asignado.put(n.getId(), asignadoA(n)));
    LocalDateTime desde =
        LocalDateTime.now(ZoneId.systemDefault()).minusMonths(MESES_SUB_ATENDIDOS);
    return new DatosDeAsignacion(
        asignado, donacionesIndependientes.contarPorEntidadRegistradasDesde(desde));
  }
}
