package grupo5.logistica.schedulers;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeEntregas {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeEntregas.class);

  private final IEntregasRepository entregasRepository;
  private final ICamionRepository camionesRepository;
  private final ISolicitudPlanificacionRepository solicitudRepo;
  private final IServicioExternoPlanificacion generadorDeRutas;
  private final int maxDonacionesPorLote;
  private final String callbackUrl;

  public PlanificadorDeEntregas(
      IEntregasRepository entregasRepository,
      ICamionRepository camionesRepository,
      ISolicitudPlanificacionRepository solicitudRepo,
      IServicioExternoPlanificacion generadorDeRutas,
      @Value("${logistica.planificacion.max-donaciones-por-lote:100}") int maxDonacionesPorLote,
      @Value("${logistica.self.base-url:http://localhost:8083}") String selfBaseUrl) {
    this.entregasRepository = entregasRepository;
    this.camionesRepository = camionesRepository;
    this.solicitudRepo = solicitudRepo;
    this.generadorDeRutas = generadorDeRutas;
    this.maxDonacionesPorLote =
        Math.min(maxDonacionesPorLote, SolicitudPlanificacion.MAX_DONACIONES_POR_LOTE);
    this.callbackUrl = selfBaseUrl + "/api/logistica/callback/rutas";
  }

  @Scheduled(cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}")
  public void ejecutar() {
    List<Entrega> entregasPendientes = obtenerEntregasPendientesDeRuta();
    if (entregasPendientes.isEmpty()) {
      log.info("No hay entregas pendientes de planificación. No se generarán rutas.");
      return;
    }

    List<Camion> camionesDisponibles = obtenerCamionesDisponibles();
    if (camionesDisponibles.isEmpty()) {
      log.warn(
          "Hay {} entrega(s) pendiente(s) pero no hay camiones disponibles. Se pospone al"
              + " próximo ciclo.",
          entregasPendientes.size());
      return;
    }

    LocalDate fechaLote = LocalDate.now(ZoneId.of("UTC"));
    for (List<Entrega> lote : particionarEnLotes(entregasPendientes, maxDonacionesPorLote)) {
      solicitarPlanificacionDeLote(lote, camionesDisponibles, fechaLote);
    }
  }

  private void solicitarPlanificacionDeLote(
      List<Entrega> lote, List<Camion> camionesDisponibles, LocalDate fecha) {
    SolicitudPlanificacion solicitud = new SolicitudPlanificacion(fecha, lote.size(), callbackUrl);
    solicitudRepo.save(solicitud);

    log.info(
        "[SOLICITUD-PLANIFICACION-ENVIADA] id={} cantidadDonaciones={} callbackUrl={}",
        solicitud.getId(),
        lote.size(),
        callbackUrl);

    // El scheduler corta acá. GeneradorDeRutas sigue procesando en otro hilo y va a avisar
    // el resultado por HTTP contra callbackUrl cuando termine.
    generadorDeRutas.generarRutas(solicitud, lote, camionesDisponibles);
  }

  private List<Entrega> obtenerEntregasPendientesDeRuta() {
    return entregasRepository.findAll().stream().filter(e -> e.getIdRuta() == null).toList();
  }

  private List<Camion> obtenerCamionesDisponibles() {
    return camionesRepository.findAll().stream().filter(Camion::estaDisponibleParaAsignar).toList();
  }

  private static List<List<Entrega>> particionarEnLotes(List<Entrega> entregas, int tamanioLote) {
    List<List<Entrega>> lotes = new ArrayList<>();
    for (int i = 0; i < entregas.size(); i += tamanioLote) {
      lotes.add(entregas.subList(i, Math.min(i + tamanioLote, entregas.size())));
    }
    return lotes;
  }
}
