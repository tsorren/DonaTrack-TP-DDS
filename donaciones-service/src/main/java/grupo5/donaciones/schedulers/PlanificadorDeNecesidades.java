package grupo5.donaciones.schedulers;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRecurrentesRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeNecesidades {

  private final INecesidadesRecurrentesRepository necesidadRepository;

  @Autowired
  public PlanificadorDeNecesidades(INecesidadesRecurrentesRepository necesidadRepository) {
    this.necesidadRepository = necesidadRepository;
  }

  @Scheduled(cron = "0 0 0 * * ?")
  public void generarNuevosPeriodos() {

    List<NecesidadRecurrente> recurrentesActivas = necesidadRepository.findByActivaTrue();

    for (NecesidadRecurrente recurrente : recurrentesActivas) {
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
