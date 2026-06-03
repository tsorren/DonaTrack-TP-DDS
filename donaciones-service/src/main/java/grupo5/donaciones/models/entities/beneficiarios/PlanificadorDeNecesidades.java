package grupo5.donaciones.models.entities.beneficiarios;

import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeNecesidades {

  private final List<NecesidadRecurrente> necesidadesPlanificadas = new ArrayList<>();

  public void registrarNecesidad(NecesidadRecurrente necesidad) {
    this.necesidadesPlanificadas.add(necesidad);
  }

  // se ejecuta todos los días a la medianoche
  @Scheduled(cron = "0 0 0 * * ?")
  public void generarNuevosPeriodos() {
    for (NecesidadRecurrente recurrente : necesidadesPlanificadas) {
      if (recurrente.getActiva() && recurrente.hayQueGenerarNuevo()) {
        crearPeriodoPara(recurrente);
      }
    }
  }

  public void crearPeriodoPara(NecesidadRecurrente necesidadRecurrente) {
    if (necesidadRecurrente.obtenerPeriodoActual() != null) {
      necesidadRecurrente.obtenerPeriodoActual().finalizo();
    }
    necesidadRecurrente.generarNuevoPeriodo();
  }
}
