package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionService implements IPlanificacionService {

  private static final Logger log = LoggerFactory.getLogger(PlanificacionService.class);

  private final ISolicitudPlanificacionRepository solicitudesRepository;
  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final ICamionRepository camionesRepository;
  private final IServicioExternoPlanificacion generadorDeRutas;
  private final SolicitudPlanificacionMapper solicitudMapper;
  private final LogisticaEventPublisher eventPublisher;
  private final int maxDonacionesPorLote;
  private final String callbackUrl;

  public PlanificacionService(
      ISolicitudPlanificacionRepository solicitudesRepository,
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      ICamionRepository camionesRepository,
      IServicioExternoPlanificacion generadorDeRutas,
      SolicitudPlanificacionMapper solicitudMapper,
      LogisticaEventPublisher eventPublisher,
      @Value("${logistica.planificacion.max-donaciones-por-lote:100}") int maxDonacionesPorLote,
      @Value("${logistica.self.base-url:http://localhost:8083}") String selfBaseUrl) {
    this.solicitudesRepository = solicitudesRepository;
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.camionesRepository = camionesRepository;
    this.generadorDeRutas = generadorDeRutas;
    this.solicitudMapper = solicitudMapper;
    this.eventPublisher = eventPublisher;
    this.maxDonacionesPorLote =
        Math.min(maxDonacionesPorLote, SolicitudPlanificacion.MAX_DONACIONES_POR_LOTE);
    this.callbackUrl = selfBaseUrl + "/api/logistica/callback/rutas";
  }

  @Override
  public List<SolicitudPlanificacionResponseDTO> iniciarPlanificacion() {
    List<Entrega> entregasPendientes = obtenerEntregasPendientesDeRuta();
    if (entregasPendientes.isEmpty()) {
      log.info("No hay entregas pendientes de planificación. No se generarán rutas.");
      return List.of();
    }

    List<Camion> camionesDisponibles = obtenerCamionesDisponibles();
    if (camionesDisponibles.isEmpty()) {
      log.warn(
          "Hay {} entrega(s) pendiente(s) pero no hay camiones disponibles. Se pospone al"
              + " próximo ciclo.",
          entregasPendientes.size());
      return List.of();
    }

    LocalDate fechaLote = LocalDate.now(ZoneId.of("UTC"));
    List<SolicitudPlanificacionResponseDTO> solicitudesCreadas = new ArrayList<>();
    for (List<Entrega> lote : particionarEnLotes(entregasPendientes, maxDonacionesPorLote)) {
      solicitudesCreadas.add(solicitarPlanificacionDeLote(lote, camionesDisponibles, fechaLote));
    }
    return solicitudesCreadas;
  }

  private SolicitudPlanificacionResponseDTO solicitarPlanificacionDeLote(
      List<Entrega> lote, List<Camion> camionesDisponibles, LocalDate fecha) {
    SolicitudPlanificacion solicitud = new SolicitudPlanificacion(fecha, lote.size(), callbackUrl);
    solicitud = solicitudesRepository.save(solicitud);

    log.info(
        "[SOLICITUD-PLANIFICACION-ENVIADA] id={} cantidadDonaciones={} callbackUrl={}",
        solicitud.getId(),
        lote.size(),
        callbackUrl);

    // El disparador (scheduler o endpoint manual) corta acá.
    generadorDeRutas.generarRutas(solicitud, lote, camionesDisponibles);

    return solicitudMapper.toResponseDTO(solicitud);
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

  @Override
  public SolicitudPlanificacionResponseDTO procesarCallback(CallbackPlanificacionRequestDTO dto) {
    if (dto == null || dto.solicitudId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    SolicitudPlanificacion solicitud = buscarSolicitud(dto.solicitudId());

    if (solicitud.getEstado() == EstadoSolicitud.PROCESADA) {
      return solicitudMapper.toResponseDTO(solicitud);
    }

    if ("ERROR".equalsIgnoreCase(dto.estado())) {
      solicitud.marcarError(dto.motivoError());
      return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
    }

    if (dto.rutas() == null || dto.rutas().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    List<UUID> rutasGeneradas = dto.rutas().stream().map(this::guardarRutaPlanificada).toList();

    solicitud.procesarResultados(rutasGeneradas);
    return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
  }

  @Override
  public SolicitudPlanificacionResponseDTO obtenerPorId(UUID id) {
    return solicitudMapper.toResponseDTO(buscarSolicitud(id));
  }

  private UUID guardarRutaPlanificada(RutaPlanificadaDTO dto) {
    if (dto == null || dto.entregaIds() == null || dto.entregaIds().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    Ruta ruta = new Ruta(dto.fecha(), dto.choferId(), dto.camionId());
    List<Entrega> entregasAsignadas =
        dto.entregaIds().stream()
            .map(
                entregaId -> {
                  Entrega entrega = buscarEntrega(entregaId);
                  ruta.agregarEntrega(entrega.getId());
                  entrega.asignarRuta(ruta.getId());
                  return entregasRepository.save(entrega);
                })
            .toList();

    UUID rutaId = rutasRepository.save(ruta).getId();

    entregasAsignadas.forEach(
        entrega ->
            eventPublisher.publicarRutaAsignada(
                new EventoRutaAsignada(
                    rutaId, entrega.getIdDonacion(), LocalDateTime.now(ZoneId.of("UTC")))));

    return rutaId;
  }

  private SolicitudPlanificacion buscarSolicitud(UUID id) {
    return solicitudesRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }
}
