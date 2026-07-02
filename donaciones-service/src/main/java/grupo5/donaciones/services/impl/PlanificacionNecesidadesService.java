package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IPlanificacionNecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionNecesidadesService implements IPlanificacionNecesidadesService {

  private final INecesidadesRepository necesidadRepository;

  public PlanificacionNecesidadesService(INecesidadesRepository necesidadRepository) {
    this.necesidadRepository = necesidadRepository;
  }

  @Override
  public void generarNuevosPeriodosParaNecesidadesRecurrentes() {
    for (Necesidad necesidad :
        necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue()) {
      NecesidadRecurrente recurrente = (NecesidadRecurrente) necesidad;
      if (recurrente.hayQueGenerarNuevo(LocalDate.now(ZoneId.systemDefault()))) {
        crearPeriodoPara(recurrente);
        necesidadRepository.save(recurrente);
      }
    }
  }

  private static void crearPeriodoPara(NecesidadRecurrente necesidadRecurrente) {
    if (necesidadRecurrente.obtenerPeriodoActual() != null) {
      necesidadRecurrente.obtenerPeriodoActual().finalizo();
    }
    necesidadRecurrente.generarNuevoPeriodo();
  }
}
