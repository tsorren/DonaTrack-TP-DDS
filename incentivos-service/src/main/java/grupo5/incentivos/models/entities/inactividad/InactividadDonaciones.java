package grupo5.incentivos.models.entities.inactividad;

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
      throw new IllegalArgumentException("Los días de inactividad deben ser mayores a cero");
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
    LocalDate ultimaDonacion = donante.getMetricas().getUltimaDonacion();
    return ultimaDonacion == null || ultimaDonacion.isBefore(umbral);
  }

  private DonanteInactivo aDonanteInactivo(DonanteIncentivos donante, LocalDate hoy) {
    LocalDate ultimaDonacion = donante.getMetricas().getUltimaDonacion();
    // Si nunca donó no hay fecha de referencia real: reportamos el umbral configurado
    // como piso de días inactivo en lugar de inventar un número.
    int diasInactivo =
        ultimaDonacion == null ? diasSinDonar : (int) ChronoUnit.DAYS.between(ultimaDonacion, hoy);
    return crear(donante, diasInactivo, hoy);
  }
}
