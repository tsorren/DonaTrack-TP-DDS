package grupo5.incentivos.services;

import grupo5.incentivos.infrastructure.INotificacionesClient;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.entities.inactividad.DonanteInactivo;
import grupo5.incentivos.models.entities.inactividad.GestorDeInactivos;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InactividadService implements IInactividadService {

  private static final Logger log = LoggerFactory.getLogger(InactividadService.class);

  private final IDonanteIncentivosRepository repository;
  private final GestorDeInactivos gestorDeInactivos;
  private final List<CriterioInactividad> criterios;
  private final INotificacionesClient notificacionesClient;

  public InactividadService(
      IDonanteIncentivosRepository repository,
      GestorDeInactivos gestorDeInactivos,
      List<CriterioInactividad> criterios,
      INotificacionesClient notificacionesClient) {
    this.repository = repository;
    this.gestorDeInactivos = gestorDeInactivos;
    this.criterios = criterios;
    this.notificacionesClient = notificacionesClient;
  }

  @Override
  public void procesarInactividad() {
    log.info("Iniciando chequeo diario de inactividad con {} criterio(s)", criterios.size());
    List<DonanteIncentivos> todos = repository.findAll();

    List<DonanteInactivo> inactivos = gestorDeInactivos.procesarInactividad(criterios, todos);

    inactivos.forEach(
        inactivo -> {
          try {
            notificacionesClient.notificarInactividad(
                inactivo.idPersona(), inactivo.diasInactivo());
            log.info("Notificación de inactividad enviada al donante {}", inactivo.idDonante());
          } catch (Exception e) {
            log.warn(
                "No se pudo notificar al donante {}: {}", inactivo.idDonante(), e.getMessage());
          }
        });
  }
}
