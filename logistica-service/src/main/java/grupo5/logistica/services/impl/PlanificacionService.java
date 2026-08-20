package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.entities.solicitudes.EstadoSolicitud;
import grupo5.logistica.models.entities.solicitudes.SolicitudPlanificacion;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Único punto de entrada de escritura de una {@link SolicitudPlanificacion} es el scheduler ({@code
 * PlanificadorDeEntregas}), que la crea y dispara {@code IServicioExternoPlanificacion}
 * directamente. Este service no crea solicitudes: sólo recibe el resultado de esa ejecución
 * (callback) y permite consultarlas.
 */
@Service
public class PlanificacionService implements IPlanificacionService {

  private final ISolicitudPlanificacionRepository solicitudesRepository;
  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final SolicitudPlanificacionMapper solicitudMapper;
  private final LogisticaEventPublisher eventPublisher;

  public PlanificacionService(
      ISolicitudPlanificacionRepository solicitudesRepository,
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      SolicitudPlanificacionMapper solicitudMapper,
      LogisticaEventPublisher eventPublisher) {
    this.solicitudesRepository = solicitudesRepository;
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.solicitudMapper = solicitudMapper;
    this.eventPublisher = eventPublisher;
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

    // INICIO LOGICA DE NEGOCIO

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

    // FIN LOGICA DE NEGOCIO

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
