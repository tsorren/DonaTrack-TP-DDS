package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.ArrayList;
import java.util.List;

public class GestorDeInactivos {

  public List<DonanteInactivo> procesarInactividad(
      List<CriterioInactividad> criterios, List<DonanteIncentivos> todos) {
    List<DonanteInactivo> inactivos = new ArrayList<>();
    for (CriterioInactividad criterio : criterios) {
      inactivos.addAll(criterio.detectarInactivos(todos));
    }
    return inactivos;
  }
}
