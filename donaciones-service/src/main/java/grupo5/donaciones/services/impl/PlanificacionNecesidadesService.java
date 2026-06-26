package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.impl.NecesidadRecurrenteRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionNecesidadesService {

  private final NecesidadRecurrenteRepository necesidadRepository;

  public PlanificacionNecesidadesService(NecesidadRecurrenteRepository necesidadRepository) {
    this.necesidadRepository = necesidadRepository;
  }

  public void generarNuevosPeriodosParaNecesidadesRecurrentes() {
    List<NecesidadRecurrente> recurrentesActivas = necesidadRepository.findByActivaTrue();

    for (NecesidadRecurrente recurrente : recurrentesActivas) {
      if (recurrente.hayQueGenerarNuevo(LocalDate.now(ZoneId.systemDefault()))) {
        crearPeriodoPara(recurrente);
        necesidadRepository.save(recurrente);
      }
    }
  }

  private void crearPeriodoPara(NecesidadRecurrente necesidadRecurrente) {
    if (necesidadRecurrente.obtenerPeriodoActual() != null) {
      necesidadRecurrente.obtenerPeriodoActual().finalizo();
    }
    necesidadRecurrente.generarNuevoPeriodo();
  }
}
