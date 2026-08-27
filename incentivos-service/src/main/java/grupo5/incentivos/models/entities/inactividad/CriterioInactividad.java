package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.LocalDate;
import java.util.List;

public abstract class CriterioInactividad {

  public abstract List<DonanteInactivo> detectarInactivos(List<DonanteIncentivos> donantes);

  protected DonanteInactivo crear(DonanteIncentivos donante, int diasInactivo, LocalDate fecha) {
    return new DonanteInactivo(donante.getId(), donante.getIdPersona(), diasInactivo, fecha);
  }
}
