package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;
import java.util.List;

public class InactividadDonaciones extends CriterioInactividad {

  private final int diasSinDonar;

  public InactividadDonaciones(int diasSinDonar) {
    if (diasSinDonar <= 0) {
      throw new IllegalArgumentException("Los días de inactividad deben ser mayores a cero");
    }
    this.diasSinDonar = diasSinDonar;
  }

  @Override
  public List<DonanteIncentivos> detectarInactivos(List<DonanteIncentivos> donantes) {
    LocalDate umbral = LocalDate.now().minusDays(diasSinDonar);
    return donantes.stream().filter(d -> esInactivo(d, umbral)).toList();
  }

  private boolean esInactivo(DonanteIncentivos donante, LocalDate umbral) {
    LocalDate ultimaDonacion = donante.getMetricas().getUltimaDonacion();
    return ultimaDonacion == null || ultimaDonacion.isBefore(umbral);
  }
}
