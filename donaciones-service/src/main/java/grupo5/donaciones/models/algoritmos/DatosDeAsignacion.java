package grupo5.donaciones.models.algoritmos;

import java.util.Map;
import java.util.UUID;

/**
 * Lo que los algoritmos necesitan saber de asignaciones anteriores y que la necesidad ya no guarda
 * (DTI-06). Lo calcula la capa de aplicación con consultas sobre las donaciones independientes.
 *
 * @param asignadoPorNecesidad cantidad ya asignada a cada necesidad (al período actual, en las
 *     recurrentes)
 * @param recibidasPorEntidad donaciones recibidas por cada entidad en el último trimestre
 */
public record DatosDeAsignacion(
    Map<UUID, Integer> asignadoPorNecesidad, Map<UUID, Integer> recibidasPorEntidad) {

  public DatosDeAsignacion {
    asignadoPorNecesidad =
        asignadoPorNecesidad != null ? Map.copyOf(asignadoPorNecesidad) : Map.of();
    recibidasPorEntidad = recibidasPorEntidad != null ? Map.copyOf(recibidasPorEntidad) : Map.of();
  }

  public static DatosDeAsignacion sinAsignaciones() {
    return new DatosDeAsignacion(Map.of(), Map.of());
  }

  public int asignadoA(UUID necesidadId) {
    return necesidadId != null ? asignadoPorNecesidad.getOrDefault(necesidadId, 0) : 0;
  }

  public int recibidasPor(UUID entidadId) {
    return entidadId != null ? recibidasPorEntidad.getOrDefault(entidadId, 0) : 0;
  }
}
