package grupo5.logistica.services.impl;

import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RutasService {
  private static final Logger log = LoggerFactory.getLogger(RutasService.class);

  private final IRutasRepository rutasRepository;
  private final IServicioExternoPlanificacion servicioExterno;
  private final ISolicitudPlanificacionRepository solicitudRepo;
  private final IEntregasRepository entregasRepository;
  private final ICamionesRepository camionesRepository;
  private final int maxDonacionesPorLote;
  private final String callbackUrl;

  public RutasService(
      IRutasRepository rutasRepository,
      IServicioExternoPlanificacion servicioExterno,
      ISolicitudPlanificacionRepository solicitudRepo,
      IEntregasRepository entregasRepository,
      ICamionesRepository camionesRepository,
      @Value("${logistica.planificacion.max-donaciones-por-lote:100}") int maxDonacionesPorLote,
      @Value("${logistica.planificacion.callback-url:/api/logistica/rutas/callback}")
          String callbackUrl) {
    this.rutasRepository = rutasRepository;
    this.servicioExterno = servicioExterno;
    this.solicitudRepo = solicitudRepo;
    this.entregasRepository = entregasRepository;
    this.camionesRepository = camionesRepository;
    this.maxDonacionesPorLote =
        Math.min(maxDonacionesPorLote, SolicitudPlanificacion.MAX_DONACIONES_POR_LOTE);
    this.callbackUrl = callbackUrl;
  }

  public void planificarEntregasPendientes() {
    List<Entrega> entregasPendientes = obtenerEntregasPendientesDeRuta();
    if (entregasPendientes.isEmpty()) {
      log.info("No hay entregas pendientes de planificación. No se generarán rutas.");
      return;
    }

    List<Camion> camionesDisponibles = obtenerCamionesDisponibles();
    if (camionesDisponibles.isEmpty()) {
      log.warn(
          "Hay {} entrega(s) pendiente(s) pero no hay camiones disponibles. Se pospone la"
              + " planificación al próximo ciclo.",
          entregasPendientes.size());
      return;
    }

    LocalDate fechaLote = LocalDate.now(ZoneId.of("UTC"));
    for (List<Entrega> lote : particionarEnLotes(entregasPendientes, maxDonacionesPorLote)) {
      procesarLote(lote, camionesDisponibles, fechaLote);
    }
  }

  private void procesarLote(List<Entrega> lote, List<Camion> camionesDisponibles, LocalDate fecha) {
    SolicitudPlanificacion solicitud = new SolicitudPlanificacion(fecha, lote.size(), callbackUrl);
    solicitudRepo.save(solicitud);
    log.info(
        "[SOLICITUD-PLANIFICACION-INICIO] id={} cantidadDonaciones={}",
        solicitud.getId(),
        lote.size());

    try {
      List<Ruta> rutasGeneradas = servicioExterno.generarRutas(lote, camionesDisponibles);

      rutasRepository.saveAll(rutasGeneradas);
      entregasRepository.saveAll(lote);
      marcarCamionesEnRuta(rutasGeneradas);

      solicitud.procesarResultados(rutasGeneradas.stream().map(Ruta::getId).toList());
      solicitudRepo.save(solicitud);
      log.info(
          "[SOLICITUD-PLANIFICACION-OK] id={} rutasGeneradas={}",
          solicitud.getId(),
          rutasGeneradas.size());
    } catch (Exception e) {
      solicitud.marcarError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
      solicitudRepo.save(solicitud);
      log.error(
          "[SOLICITUD-PLANIFICACION-ERROR] id={} intentosFallidos={} motivo={}",
          solicitud.getId(),
          solicitud.getIntentosFallidos(),
          solicitud.getMotivoError(),
          e);
    }
  }

  private void marcarCamionesEnRuta(List<Ruta> rutasGeneradas) {
    for (Ruta ruta : rutasGeneradas) {
      Optional<Camion> camion = camionesRepository.findById(ruta.getCamionId());
      camion.ifPresent(
          c -> {
            c.asignarARuta(ruta.getId());
            camionesRepository.save(c);
          });
    }
  }

  private List<Entrega> obtenerEntregasPendientesDeRuta() {
    return entregasRepository.findAll().stream().filter(e -> e.getIdRuta() == null).toList();
  }

  private List<Camion> obtenerCamionesDisponibles() {
    return camionesRepository.findAll().stream().filter(Camion::estaDisponibleParaAsignar).toList();
  }

  private List<List<Entrega>> particionarEnLotes(List<Entrega> entregas, int tamanioLote) {
    List<List<Entrega>> lotes = new ArrayList<>();
    for (int i = 0; i < entregas.size(); i += tamanioLote) {
      lotes.add(entregas.subList(i, Math.min(i + tamanioLote, entregas.size())));
    }
    return lotes;
  }
}
