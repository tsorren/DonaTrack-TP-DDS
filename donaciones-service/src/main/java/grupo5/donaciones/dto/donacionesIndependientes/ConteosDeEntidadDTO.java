package grupo5.donaciones.dto.donacionesIndependientes;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta de {@code GET /donaciones-independientes/conteos} (R11-43): las cifras de las pantallas
 * de la organización, contadas por el servicio y no por el largo de una página.
 *
 * @param porEstado los 8 estados, siempre, en el orden del ciclo de vida, con 0 si no hay
 * @param entregadasEnElMes las que pasaron a {@code ENTREGADA} en el mes en curso
 * @param porNecesidad solo las necesidades con alguna asignada, y en cada una los estados con
 *     alguna
 */
public record ConteosDeEntidadDTO(
    List<PorEstado> porEstado,
    EntregadasEnElMes entregadasEnElMes,
    List<PorNecesidad> porNecesidad) {

  public ConteosDeEntidadDTO {
    porEstado = List.copyOf(porEstado);
    porNecesidad = List.copyOf(porNecesidad);
  }

  public record PorEstado(String estado, long cantidad) {}

  /** {@code mes} en formato {@code AAAA-MM}, como el resumen del admin. */
  public record EntregadasEnElMes(String mes, long cantidad) {}

  public record PorNecesidad(UUID necesidadId, List<PorEstado> porEstado) {

    public PorNecesidad {
      porEstado = List.copyOf(porEstado);
    }
  }
}
