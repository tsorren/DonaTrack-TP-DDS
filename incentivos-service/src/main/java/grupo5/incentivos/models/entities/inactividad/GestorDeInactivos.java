package grupo5.incentivos.models.entities.inactividad;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GestorDeInactivos {

  public List<DonanteInactivo> procesarInactividad(
      List<CriterioInactividad> criterios, List<DonanteIncentivos> todos) {
    if (criterios == null || criterios.isEmpty() || todos == null || todos.isEmpty()) {
      return List.of();
    }

    Map<UUID, DonanteInactivo> inactivosPorPersona = new LinkedHashMap<>();

    for (CriterioInactividad criterio : criterios) {
      if (criterio == null) {
        continue;
      }
      List<DonanteInactivo> detectados = criterio.detectarInactivos(todos);
      if (detectados != null) {
        for (DonanteInactivo inactivo : detectados) {
          inactivosPorPersona.merge(
              inactivo.idPersona(),
              inactivo,
              (existente, nuevo) ->
                  nuevo.diasInactivo() > existente.diasInactivo() ? nuevo : existente);
        }
      }
    }
    return new ArrayList<>(inactivosPorPersona.values());
  }
}
