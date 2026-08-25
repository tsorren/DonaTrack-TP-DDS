package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IPlanificacionNecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionNecesidadesService implements IPlanificacionNecesidadesService {

  private final INecesidadesRepository necesidadRepository;

  public PlanificacionNecesidadesService(INecesidadesRepository necesidadRepository) {
    this.necesidadRepository = necesidadRepository;
  }

  @Override
  public void generarNuevosPeriodosParaNecesidadesRecurrentes() {
    LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
    List<NecesidadRecurrente> modificadas =
        necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue().stream()
            .map(n -> (NecesidadRecurrente) n)
            .filter(n -> n.renovarPeriodoSiCorresponde(hoy))
            .toList();

    modificadas.forEach(necesidadRepository::save);
  }
}
