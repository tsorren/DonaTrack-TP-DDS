package grupo5.incentivos.jobs;

import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.services.IncentivosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InactividadJob {

  private static final Logger log = LoggerFactory.getLogger(InactividadJob.class);

  private final IncentivosService service;
  private final NotificacionesClient notificacionesClient;
  private final List<CriterioInactividad> criterios;

  public InactividadJob(
      IncentivosService service,
      NotificacionesClient notificacionesClient,
      List<CriterioInactividad> criterios) {
    this.service = service;
    this.notificacionesClient = notificacionesClient;
    this.criterios = criterios;
  }

  // Se ejecuta todos los días a las 8:00 AM
  @Scheduled(cron = "0 0 8 * * *")
  public void ejecutar() {
    log.info("Iniciando chequeo diario de inactividad con {} criterio(s)", criterios.size());
    List<DonanteIncentivos> todos = service.listarTodos();

    for (CriterioInactividad criterio : criterios) {
      List<DonanteIncentivos> inactivos = criterio.detectarInactivos(todos);
      log.info(
          "Criterio [{}] detectó {} donante(s) inactivo(s)",
          criterio.getClass().getSimpleName(),
          inactivos.size());

      inactivos.forEach(
          donante -> {
            try {
              notificacionesClient.notificarInactividad(donante.getId(), 20);
              log.info("Notificación de inactividad enviada al donante {}", donante.getId());
            } catch (Exception e) {
              log.warn("No se pudo notificar al donante {}: {}", donante.getId(), e.getMessage());
            }
          });
    }
  }
}
