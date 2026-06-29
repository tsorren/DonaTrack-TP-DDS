package grupo5.incentivos.jobs;

import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.services.IIncentivosService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactividadJob {

  private static final Logger log = LoggerFactory.getLogger(InactividadJob.class);

  private final IIncentivosService service;
  private final NotificacionesClient notificacionesClient;
  private final List<CriterioInactividad> criterios;
  private final int diasLimite;

  public InactividadJob(
      IIncentivosService service,
      NotificacionesClient notificacionesClient,
      List<CriterioInactividad> criterios,
      @Value("${donante.inactividad.limite:20}") int diasLimite) {
    this.service = service;
    this.notificacionesClient = notificacionesClient;
    this.criterios = criterios;
    this.diasLimite = diasLimite;
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
              notificacionesClient.notificarInactividad(donante.getIdPersona(), diasLimite);
              log.info("Notificación de inactividad enviada al donante {}", donante.getId());
            } catch (Exception e) {
              log.warn("No se pudo notificar al donante {}: {}", donante.getId(), e.getMessage());
            }
          });
    }
  }
}
