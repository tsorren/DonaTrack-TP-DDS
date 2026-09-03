package grupo5.logistica.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.EjecucionPlanificacionResponseDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.GeneradorDeRutas;
import grupo5.logistica.models.entities.rutas.PlanificacionSolicitada;
import grupo5.logistica.models.entities.rutas.RespuestaPlanificacion;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.rutas.eventos.EventoRutaAsignada;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.IServicioExternoPlanificacion;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Prepara los casos de uso de planificación y delega las decisiones al dominio. */
@Service
public class PlanificacionService implements IPlanificacionService {

  private static final Logger log = LoggerFactory.getLogger(PlanificacionService.class);
  private static final String ESTADO_ERROR = "ERROR";
  private static final String ESTADO_OK = "OK";
  private static final String ESTADO_PARCIAL = "PARCIAL";

  private final ISolicitudPlanificacionRepository solicitudesRepository;
  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final ICamionRepository camionesRepository;
  private final IChoferesRepository choferesRepository;
  private final SolicitudPlanificacionMapper solicitudMapper;
  private final ComunicadorEventosLogistica comunicadorEventos;
  private final IServicioExternoPlanificacion planificadorExterno;
  private final GeneradorDeRutas generadorDeRutas;
  private final Clock clock;
  private final int maximoPorLote;
  private final String callbackUrl;
  private final boolean ejecucionManualHabilitada;

  public PlanificacionService(
      ISolicitudPlanificacionRepository solicitudesRepository,
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      ICamionRepository camionesRepository,
      IChoferesRepository choferesRepository,
      SolicitudPlanificacionMapper solicitudMapper,
      ComunicadorEventosLogistica comunicadorEventos,
      IServicioExternoPlanificacion planificadorExterno,
      GeneradorDeRutas generadorDeRutas,
      Clock clock,
      @Value("${logistica.planificacion.max-donaciones-por-lote:100}") int maximoPorLote,
      @Value("${logistica.self.base-url:http://localhost:8083}") String selfBaseUrl,
      @Value("${logistica.planificacion.manual-enabled:true}") boolean ejecucionManualHabilitada) {
    this.solicitudesRepository = solicitudesRepository;
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.camionesRepository = camionesRepository;
    this.choferesRepository = choferesRepository;
    this.solicitudMapper = solicitudMapper;
    this.comunicadorEventos = comunicadorEventos;
    this.planificadorExterno = planificadorExterno;
    this.generadorDeRutas = generadorDeRutas;
    this.clock = clock;
    this.maximoPorLote = Math.min(maximoPorLote, GeneradorDeRutas.MAX_ENTREGAS_POR_SOLICITUD);
    this.callbackUrl = selfBaseUrl + "/api/logistica/callback/rutas";
    this.ejecucionManualHabilitada = ejecucionManualHabilitada;
  }

  @Override
  public synchronized EjecucionPlanificacionResponseDTO iniciarPlanificacion() {
    return ejecutarPlanificacion();
  }

  @Override
  public synchronized EjecucionPlanificacionResponseDTO iniciarPlanificacionManual() {
    if (!ejecucionManualHabilitada) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_PLANIFICACION_TRANSICION_INVALIDA);
    }
    return ejecutarPlanificacion();
  }

  @Override
  public synchronized SolicitudPlanificacionResponseDTO procesarCallback(
      CallbackPlanificacionRequestDTO dto) {
    if (dto == null || dto.solicitudId() == null || dto.estado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    SolicitudPlanificacion solicitud = buscarSolicitud(dto.solicitudId());
    if (esResultadoDefinitivo(solicitud.getEstado())) {
      return solicitudMapper.toResponseDTO(solicitud);
    }

    String estado = dto.estado().toUpperCase(Locale.ROOT);
    if (ESTADO_ERROR.equals(estado)) {
      if (dto.rutas() != null && !dto.rutas().isEmpty()) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }
      solicitud.marcarError(dto.motivoError());
      return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
    }
    if ((!ESTADO_OK.equals(estado) && !ESTADO_PARCIAL.equals(estado))
        || dto.rutas() == null
        || dto.rutas().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    RespuestaPlanificacion respuesta = mapearRespuesta(dto, solicitud);
    Set<UUID> entregasAsignadas = obtenerEntregasAsignadas(respuesta);
    validarEstadoResultado(estado, solicitud, entregasAsignadas);
    List<Ruta> rutas = generadorDeRutas.calcularRutas(respuesta);
    persistirPlanificacion(rutas, respuesta);

    solicitud.procesarResultados(
        rutas.stream().map(Ruta::getId).toList(), List.copyOf(entregasAsignadas));
    return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
  }

  @Override
  public SolicitudPlanificacionResponseDTO obtenerPorId(UUID id) {
    return solicitudMapper.toResponseDTO(buscarSolicitud(id));
  }

  private EjecucionPlanificacionResponseDTO ejecutarPlanificacion() {
    LocalDate fechaObjetivo = LocalDate.now(clock).plusDays(1);
    List<SolicitudPlanificacion> pendientes =
        solicitudesRepository.findByEstado(EstadoSolicitud.PENDIENTE);
    Set<UUID> entregasReservadas =
        pendientes.stream()
            .flatMap(solicitud -> solicitud.getEntregaIds().stream())
            .collect(Collectors.toSet());
    List<Entrega> entregas =
        entregasRepository.findSinRuta().stream()
            .filter(entrega -> !entregasReservadas.contains(entrega.getId()))
            .toList();
    if (entregas.isEmpty()) {
      log.info("No hay entregas pendientes de planificación.");
      return resultadoVacio(fechaObjetivo, List.of());
    }

    Set<UUID> camionesReservados = obtenerCamionesReservados(pendientes, fechaObjetivo);
    Set<UUID> choferesReservados = obtenerChoferesReservados(pendientes, fechaObjetivo);
    List<Camion> camiones =
        camionesRepository.findDisponibles().stream()
            .filter(camion -> !camionesReservados.contains(camion.getId()))
            .toList();
    List<Chofer> choferes =
        choferesRepository.findDisponibles().stream()
            .filter(chofer -> !choferesReservados.contains(chofer.getId()))
            .toList();
    if (camiones.isEmpty() || choferes.isEmpty()) {
      log.warn(
          "Se pospone la planificación: camionesDisponibles={}, choferesDisponibles={}",
          camiones.size(),
          choferes.size());
      return resultadoVacio(fechaObjetivo, idsDeEntregas(entregas));
    }

    List<PlanificacionSolicitada> planificaciones =
        generadorDeRutas.planificar(entregas, camiones, choferes, fechaObjetivo, maximoPorLote);
    List<SolicitudPlanificacion> solicitudes =
        planificaciones.stream().map(this::enviarPlanificacion).toList();
    Set<UUID> entregasPlanificadas =
        solicitudes.stream()
            .flatMap(solicitud -> solicitud.getEntregaIds().stream())
            .collect(Collectors.toSet());
    List<UUID> entregasNoPlanificadas =
        entregas.stream()
            .map(Entrega::getId)
            .filter(id -> !entregasPlanificadas.contains(id))
            .toList();
    return new EjecucionPlanificacionResponseDTO(
        fechaObjetivo,
        solicitudes.stream().map(solicitudMapper::toResponseDTO).toList(),
        entregasNoPlanificadas);
  }

  private SolicitudPlanificacion enviarPlanificacion(PlanificacionSolicitada planificacion) {
    SolicitudPlanificacion seguimiento =
        new SolicitudPlanificacion(
            planificacion.id(),
            planificacion.fecha(),
            idsDeEntregas(planificacion.entregas()),
            planificacion.camionesDisponibles().stream().map(Camion::getId).toList(),
            planificacion.choferesDisponibles().stream().map(Chofer::getId).toList(),
            callbackUrl);
    solicitudesRepository.save(seguimiento);
    planificadorExterno.solicitarPlanificacion(seguimiento, planificacion);
    return seguimiento;
  }

  private RespuestaPlanificacion mapearRespuesta(
      CallbackPlanificacionRequestDTO dto, SolicitudPlanificacion solicitud) {
    Map<Camion, List<Entrega>> datos = new LinkedHashMap<>();
    Map<Camion, Chofer> choferesPorCamion = new LinkedHashMap<>();
    Set<UUID> camionesAsignados = new HashSet<>();
    Set<UUID> choferesAsignados = new HashSet<>();
    Set<UUID> entregasAsignadas = new HashSet<>();
    Set<UUID> camionesPermitidos = new HashSet<>(solicitud.getCamionIds());
    Set<UUID> choferesPermitidos = new HashSet<>(solicitud.getChoferIds());
    Set<UUID> entregasPermitidas = new HashSet<>(solicitud.getEntregaIds());
    validarConflictosExistentes(
        solicitud.getFecha(), camionesPermitidos, choferesPermitidos, entregasPermitidas);

    for (RutaPlanificadaDTO ruta : dto.rutas()) {
      if (ruta == null
          || ruta.camionId() == null
          || ruta.choferId() == null
          || ruta.fecha() == null
          || ruta.entregaIds() == null
          || ruta.entregaIds().isEmpty()
          || !solicitud.getFecha().equals(ruta.fecha())
          || !camionesPermitidos.contains(ruta.camionId())
          || !choferesPermitidos.contains(ruta.choferId())
          || !camionesAsignados.add(ruta.camionId())
          || !choferesAsignados.add(ruta.choferId())
          || ruta.entregaIds().stream()
              .anyMatch(
                  id ->
                      id == null
                          || !entregasPermitidas.contains(id)
                          || !entregasAsignadas.add(id))) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }

      Camion camion = buscarCamion(ruta.camionId());
      Chofer chofer = buscarChofer(ruta.choferId());
      datos.put(camion, ruta.entregaIds().stream().map(this::buscarEntrega).toList());
      choferesPorCamion.put(camion, chofer);
    }

    return new RespuestaPlanificacion(
        UUID.randomUUID(), dto.solicitudId(), solicitud.getFecha(), datos, choferesPorCamion);
  }

  private void validarConflictosExistentes(
      LocalDate fecha,
      Set<UUID> camionesPermitidos,
      Set<UUID> choferesPermitidos,
      Set<UUID> entregasPermitidas) {
    boolean existeConflicto =
        rutasRepository.findByFecha(fecha).stream()
            .anyMatch(
                ruta ->
                    camionesPermitidos.contains(ruta.getCamionId())
                        || choferesPermitidos.contains(ruta.getChoferId())
                        || ruta.getEntregaIds().stream().anyMatch(entregasPermitidas::contains));
    if (existeConflicto) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static void validarEstadoResultado(
      String estado, SolicitudPlanificacion solicitud, Set<UUID> entregasAsignadas) {
    boolean resultadoCompleto =
        entregasAsignadas.size() == solicitud.getEntregaIds().size()
            && entregasAsignadas.containsAll(solicitud.getEntregaIds());
    if ((ESTADO_OK.equals(estado) && !resultadoCompleto)
        || (ESTADO_PARCIAL.equals(estado) && resultadoCompleto)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  private static Set<UUID> obtenerEntregasAsignadas(RespuestaPlanificacion respuesta) {
    return respuesta.datos().values().stream()
        .flatMap(List::stream)
        .map(Entrega::getId)
        .collect(Collectors.toSet());
  }

  private Set<UUID> obtenerCamionesReservados(
      List<SolicitudPlanificacion> pendientes, LocalDate fechaObjetivo) {
    Set<UUID> reservados = recursosPendientes(pendientes, fechaObjetivo, true);
    rutasRepository.findByFecha(fechaObjetivo).stream()
        .map(Ruta::getCamionId)
        .forEach(reservados::add);
    return reservados;
  }

  private Set<UUID> obtenerChoferesReservados(
      List<SolicitudPlanificacion> pendientes, LocalDate fechaObjetivo) {
    Set<UUID> reservados = recursosPendientes(pendientes, fechaObjetivo, false);
    rutasRepository.findByFecha(fechaObjetivo).stream()
        .map(Ruta::getChoferId)
        .forEach(reservados::add);
    return reservados;
  }

  private static Set<UUID> recursosPendientes(
      List<SolicitudPlanificacion> pendientes, LocalDate fechaObjetivo, boolean camiones) {
    return pendientes.stream()
        .filter(solicitud -> fechaObjetivo.equals(solicitud.getFecha()))
        .flatMap(
            solicitud -> (camiones ? solicitud.getCamionIds() : solicitud.getChoferIds()).stream())
        .collect(Collectors.toSet());
  }

  private void persistirPlanificacion(List<Ruta> rutas, RespuestaPlanificacion respuesta) {
    Map<UUID, Entrega> entregasPorId = new LinkedHashMap<>();
    respuesta.datos().values().stream()
        .flatMap(List::stream)
        .forEach(entrega -> entregasPorId.put(entrega.getId(), entrega));

    rutas.forEach(rutasRepository::save);
    entregasPorId.values().forEach(entregasRepository::save);
    rutas.forEach(ruta -> publicarAsignaciones(ruta, entregasPorId));
  }

  private void publicarAsignaciones(Ruta ruta, Map<UUID, Entrega> entregasPorId) {
    ruta.getDomainEvents().stream()
        .filter(EventoRutaAsignada.class::isInstance)
        .map(EventoRutaAsignada.class::cast)
        .forEach(
            evento ->
                comunicadorEventos.comunicarRutaAsignada(
                    evento, obtenerEntregaAsignada(evento.getEntregaId(), entregasPorId)));
    ruta.clearDomainEvents();
  }

  private static Entrega obtenerEntregaAsignada(UUID entregaId, Map<UUID, Entrega> entregasPorId) {
    Entrega entrega = entregasPorId.get(entregaId);
    if (entrega == null) {
      throw new RecursoNoEncontradoException(entregaId);
    }
    return entrega;
  }

  private SolicitudPlanificacion buscarSolicitud(UUID id) {
    return solicitudesRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Camion buscarCamion(UUID id) {
    return camionesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Chofer buscarChofer(UUID id) {
    return choferesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private static boolean esResultadoDefinitivo(EstadoSolicitud estado) {
    return estado == EstadoSolicitud.PROCESADA
        || estado == EstadoSolicitud.PARCIAL
        || estado == EstadoSolicitud.ERROR;
  }

  private static List<UUID> idsDeEntregas(List<Entrega> entregas) {
    return entregas.stream().map(Entrega::getId).toList();
  }

  private static EjecucionPlanificacionResponseDTO resultadoVacio(
      LocalDate fechaObjetivo, List<UUID> entregasNoPlanificadas) {
    return new EjecucionPlanificacionResponseDTO(fechaObjetivo, List.of(), entregasNoPlanificadas);
  }
}
