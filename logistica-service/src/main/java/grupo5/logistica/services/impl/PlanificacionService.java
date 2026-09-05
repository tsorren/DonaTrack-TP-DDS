package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Prepara los casos de uso de planificación y delega las decisiones al dominio. */
@Service
public class PlanificacionService implements IPlanificacionService {

  private static final Logger log = LoggerFactory.getLogger(PlanificacionService.class);

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
      @Value("${logistica.self.base-url:http://localhost:8083}") String selfBaseUrl) {
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
  }

  @Override
  public void iniciarPlanificacion() {
    List<Entrega> entregas = entregasRepository.findSinRuta();
    if (entregas.isEmpty()) {
      log.info("No hay entregas pendientes de planificación.");
      return;
    }

    List<Camion> camiones = camionesRepository.findDisponibles();
    List<Chofer> choferes = choferesRepository.findDisponibles();
    if (camiones.isEmpty() || choferes.isEmpty()) {
      log.warn(
          "Se pospone la planificación: camionesDisponibles={}, choferesDisponibles={}",
          camiones.size(),
          choferes.size());
      return;
    }

    List<PlanificacionSolicitada> solicitudes =
        generadorDeRutas.planificar(
            entregas, camiones, choferes, LocalDate.now(clock).plusDays(1), maximoPorLote);
    solicitudes.forEach(this::enviarPlanificacion);
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

    RespuestaPlanificacion respuesta = mapearRespuesta(dto);
    List<Ruta> rutas = generadorDeRutas.calcularRutas(respuesta);
    persistirPlanificacion(rutas, respuesta);

    solicitud.procesarResultados(rutas.stream().map(Ruta::getId).toList());
    return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
  }

  @Override
  public SolicitudPlanificacionResponseDTO obtenerPorId(UUID id) {
    return solicitudMapper.toResponseDTO(buscarSolicitud(id));
  }

  private void enviarPlanificacion(PlanificacionSolicitada planificacion) {
    SolicitudPlanificacion seguimiento =
        new SolicitudPlanificacion(
            planificacion.id(),
            planificacion.fecha(),
            planificacion.entregas().size(),
            callbackUrl);
    solicitudesRepository.save(seguimiento);
    planificadorExterno.solicitarPlanificacion(seguimiento, planificacion);
  }

  private RespuestaPlanificacion mapearRespuesta(CallbackPlanificacionRequestDTO dto) {
    LocalDate fecha = null;
    Map<Camion, List<Entrega>> datos = new LinkedHashMap<>();
    Map<Camion, Chofer> choferesPorCamion = new LinkedHashMap<>();
    Set<UUID> camionesAsignados = new HashSet<>();
    Set<UUID> choferesAsignados = new HashSet<>();
    Set<UUID> entregasAsignadas = new HashSet<>();

    for (RutaPlanificadaDTO ruta : dto.rutas()) {
      if (ruta == null
          || (fecha != null && !fecha.equals(ruta.fecha()))
          || !camionesAsignados.add(ruta.camionId())
          || !choferesAsignados.add(ruta.choferId())
          || ruta.entregaIds().stream().anyMatch(id -> id == null || !entregasAsignadas.add(id))) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }
      fecha = ruta.fecha();

      Camion camion = buscarCamion(ruta.camionId());
      Chofer chofer = buscarChofer(ruta.choferId());
      datos.put(camion, ruta.entregaIds().stream().map(this::buscarEntrega).toList());
      choferesPorCamion.put(camion, chofer);
    }

    return new RespuestaPlanificacion(
        UUID.randomUUID(), dto.solicitudId(), fecha, datos, choferesPorCamion);
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
}
