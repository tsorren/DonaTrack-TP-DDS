package grupo5.donaciones.schedulers;

import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeNecesidades {

  private final INecesidadesRepository necesidadRepository;
  private final PlanificacionNecesidadesService planificacionService;

  @Autowired
  public PlanificadorDeNecesidades(
      INecesidadesRepository necesidadRepository,
      PlanificacionNecesidadesService planificacionService) {
    this.necesidadRepository = necesidadRepository;
    this.planificacionService = planificacionService;
  }

  @Scheduled(cron = "0 0 0 * * ?")
  public void ejecutarPlanificacionDeNecesidades() {
    planificacionService.generarNuevosPeriodosParaNecesidadesRecurrentes();
  }

  public void generarNuevosPeriodos() {
    for (Necesidad necesidad :
        necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue()) {
      NecesidadRecurrente recurrente = (NecesidadRecurrente) necesidad;
      if (recurrente.hayQueGenerarNuevo(LocalDate.now(ZoneId.systemDefault()))) {
        crearPeriodoPara(recurrente);

        necesidadRepository.save(recurrente);
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
