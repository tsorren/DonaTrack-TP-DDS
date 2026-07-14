package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.Clock;
import java.time.LocalDate;
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
  public List<DonanteIncentivos> detectarInactivos(List<DonanteIncentivos> donantes) {
    LocalDate umbral = LocalDate.now(clock).minusDays(diasSinDonar);
    return donantes.stream().filter(d -> esInactivo(d, umbral)).toList();
  }

  private static boolean esInactivo(DonanteIncentivos donante, LocalDate umbral) {
    LocalDate ultimaDonacion = donante.getMetricas().getUltimaDonacion();
    return ultimaDonacion == null || ultimaDonacion.isBefore(umbral);
  }
}
