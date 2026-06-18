package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.List;

public abstract class CriterioInactividad {

  public abstract List<DonanteIncentivos> detectarInactivos(List<DonanteIncentivos> donantes);
}
