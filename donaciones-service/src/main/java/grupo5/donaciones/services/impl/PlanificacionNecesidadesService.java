package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IPlanificacionNecesidadesService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionNecesidadesService implements IPlanificacionNecesidadesService {

  private static final Logger log = LoggerFactory.getLogger(PlanificacionNecesidadesService.class);

  private final INecesidadesRepository necesidadRepository;
  private final AcumuladosDeAsignacion acumulados;

  public PlanificacionNecesidadesService(
      INecesidadesRepository necesidadRepository, AcumuladosDeAsignacion acumulados) {
    this.necesidadRepository = necesidadRepository;
    this.acumulados = acumulados;
  }

  @Override
  public void generarNuevosPeriodosParaNecesidadesRecurrentes() {
    LocalDate hoy = LocalDate.now(ZoneId.systemDefault());
    List<NecesidadRecurrente> recurrentes =
        necesidadRepository.findByActivaTrueAndRecurrenteTrue().stream()
            .map(n -> (NecesidadRecurrente) n)
            .toList();
    // Lo asignado a todas en una sola consulta, en vez de una por necesidad
    Map<UUID, Integer> asignado = acumulados.asignadoA(List.copyOf(recurrentes));

    recurrentes.stream()
        .filter(n -> n.renovarPeriodoSiCorresponde(hoy, asignado.getOrDefault(n.getId(), 0)))
        .forEach(this::guardarRenovada);
  }

  // Si otra instancia (o la API) ya modificó la necesidad, la renovación de esa se saltea: la
  // próxima corrida la reevalúa. No debe frenar las renovaciones de las demás.
  private void guardarRenovada(NecesidadRecurrente necesidad) {
    try {
      necesidadRepository.save(necesidad);
    } catch (BusinessStateException e) {
      if (e.getError() != ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE) {
        throw e;
      }
      log.warn(
          "No se renovó el período de la necesidad {}: otro proceso la modificó a la vez",
          necesidad.getId());
    }
  }
}
