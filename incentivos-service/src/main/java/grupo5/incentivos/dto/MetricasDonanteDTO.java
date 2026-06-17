package grupo5.incentivos.dto;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;
import java.util.Map;

public record MetricasDonanteDTO(
    Long donanteId,
    CategoriaDonante categoria,
    Integer totalDonacionesHistoricas,
    Integer totalOrganizacionesAyudadas,
    Integer totalDonacionesExitosas,
    LocalDate ultimaDonacion,
    int misionesCompletadasTotal,
    MisionActivaDTO misionActiva,
    Map<String, Long> donacionesPorPeriodo,
    long donacionesMesActual,
    long donacionesMesAnterior,
    Integer posicionEnRanking) {

  public static MetricasDonanteDTO desde(
      DonanteIncentivos donante,
      Integer posicionEnRanking,
      int misionesCompletadas,
      Map<String, Long> evolucion) {
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

    return new MetricasDonanteDTO(
        donante.getDonanteId(),
        donante.getCategoria(),
        donante.getMetricas().getTotalDonacionesHistoricas(),
        donante.getMetricas().getTotalOrganizacionesAyudadas(),
        donante.getMetricas().getTotalDonacionesExitosas(),
        donante.getMetricas().getUltimaDonacion(),
        misionesCompletadas,
        misionActiva,
        evolucion,
        donante.getMetricas().donacionesMesActual(),
        donante.getMetricas().donacionesMesAnterior(),
        posicionEnRanking);
  }

  public record MisionActivaDTO(
      String nombre,
      String descripcion,
      int progresoActual,
      int objetivo,
      int porcentaje,
      int distanciaAlObjetivo) {}
}
