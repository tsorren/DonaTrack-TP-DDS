package grupo5.incentivos.models.entities.inactividad;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class InactividadDonaciones extends CriterioInactividad {

  private final int diasSinDonar;
  private final Clock clock;

  public InactividadDonaciones(int diasSinDonar) {
    this(diasSinDonar, Clock.systemDefaultZone());
  }

  public InactividadDonaciones(int diasSinDonar, Clock clock) {
    if (diasSinDonar <= 0) {
      throw new ValidationException(ErrorCatalog.INACTIVIDAD_DIAS_INVALIDOS);
    }
    this.diasSinDonar = diasSinDonar;
    this.clock = clock;
  }

  @Override
  public List<DonanteInactivo> detectarInactivos(List<DonanteIncentivos> donantes) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate umbral = hoy.minusDays(diasSinDonar);
    return donantes.stream()
        .filter(d -> esInactivo(d, umbral))
        .map(d -> aDonanteInactivo(d, hoy))
        .toList();
  }

  private static boolean esInactivo(DonanteIncentivos donante, LocalDate umbral) {
    LocalDate fechaReferencia =
        donante.getMetricas().getUltimaDonacion() != null
            ? donante.getMetricas().getUltimaDonacion()
            : donante.getFechaRegistro();
    return fechaReferencia != null && fechaReferencia.isBefore(umbral);
  }

  private DonanteInactivo aDonanteInactivo(DonanteIncentivos donante, LocalDate hoy) {
    LocalDate fechaReferencia =
        donante.getMetricas().getUltimaDonacion() != null
            ? donante.getMetricas().getUltimaDonacion()
            : donante.getFechaRegistro();
    int diasInactivo =
        fechaReferencia != null
            ? (int) ChronoUnit.DAYS.between(fechaReferencia, hoy)
            : diasSinDonar;
    return crear(donante, diasInactivo, hoy);
  }
}
