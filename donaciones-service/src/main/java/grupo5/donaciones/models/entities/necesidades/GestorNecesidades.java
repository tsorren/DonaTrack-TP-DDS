package grupo5.donaciones.models.entities.necesidades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GestorNecesidades {

  public List<NecesidadRecurrente> generarNuevosPeriodos(
      List<NecesidadRecurrente> necesidades, LocalDate fecha) {
    List<NecesidadRecurrente> modificadas = new ArrayList<>();
    for (NecesidadRecurrente necesidad : necesidades) {
      if (necesidad.hayQueGenerarNuevo(fecha)) {
        crearPeriodoPara(necesidad);
        modificadas.add(necesidad);
      }
    }
    return modificadas;
  }

  private void crearPeriodoPara(NecesidadRecurrente necesidad) {
    if (necesidad.obtenerPeriodoActual() != null) {
      necesidad.obtenerPeriodoActual().finalizo();
    }
    necesidad.generarNuevoPeriodo();
  }
}
