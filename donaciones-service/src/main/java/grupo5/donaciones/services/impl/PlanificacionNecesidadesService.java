package grupo5.donaciones.services.impl;

import grupo5.donaciones.models.entities.necesidades.GestorNecesidades;
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
  private final GestorNecesidades gestorNecesidades;

  public PlanificacionNecesidadesService(
      INecesidadesRepository necesidadRepository, GestorNecesidades gestorNecesidades) {
    this.necesidadRepository = necesidadRepository;
    this.gestorNecesidades = gestorNecesidades;
  }

  @Override
  public void generarNuevosPeriodosParaNecesidadesRecurrentes() {
    List<NecesidadRecurrente> necesidades =
        necesidadRepository.findByActivaTrueAndSatisfechaFalseAndRecurrenteTrue().stream()
            .map(n -> (NecesidadRecurrente) n)
            .toList();

    List<NecesidadRecurrente> modificadas =
        gestorNecesidades.generarNuevosPeriodos(necesidades, LocalDate.now(ZoneId.systemDefault()));

    modificadas.forEach(necesidadRepository::save);
  }
}
