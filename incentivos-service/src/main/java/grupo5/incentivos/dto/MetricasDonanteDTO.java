package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;

public record MetricasDonanteDTO(
    Long donanteId,
    CategoriaDonante categoria,
    Integer totalDonacionesHistoricas,
    Integer totalOrganizacionesAyudadas,
    Integer totalDonacionesExitosas,
    LocalDate ultimaDonacion,
    int misionesCompletadasTotal,
    MisionActivaDTO misionActiva) {

  public static MetricasDonanteDTO desde(DonanteIncentivos donante) {
    var misionActiva =
        donante.getMisionActiva() != null
            ? new MisionActivaDTO(
                donante.getMisionActiva().getNombre(),
                donante.getMisionActiva().getDescripcion(),
                donante.getMisionActiva().getProgresoActual(),
                donante.getMisionActiva().getObjetivo(),
                donante.getMisionActiva().getPorcentajeProgreso(),
                donante.getMisionActiva().getDistanciaAlObjetivo())
            : null;

    int completadas = (int) donante.getMisiones().stream().filter(m -> m.isCompletada()).count();

    return new MetricasDonanteDTO(
        donante.getDonanteId(),
        donante.getCategoria(),
        donante.getTotalDonacionesHistoricas(),
        donante.getTotalOrganizacionesAyudadas(),
        donante.getTotalDonacionesExitosas(),
        donante.getUltimaDonacion(),
        completadas,
        misionActiva);
  }

  public record MisionActivaDTO(
      String nombre,
      String descripcion,
      int progresoActual,
      int objetivo,
      int porcentaje,
      int distanciaAlObjetivo) {}
}
