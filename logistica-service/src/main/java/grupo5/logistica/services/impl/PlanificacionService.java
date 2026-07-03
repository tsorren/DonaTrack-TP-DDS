package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.RutaPlanificadaDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.planificacion.EstadoSolicitud;
import grupo5.logistica.models.entities.planificacion.SolicitudPlanificacion;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudPlanificacionRepository;
import grupo5.logistica.services.IPlanificacionService;
import grupo5.logistica.services.mappers.SolicitudPlanificacionMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PlanificacionService implements IPlanificacionService {
  private static final int MAX_ENTREGAS_POR_SOLICITUD = 100;

  private final ISolicitudPlanificacionRepository solicitudesRepository;
  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final SolicitudPlanificacionMapper solicitudMapper;

  public PlanificacionService(
      ISolicitudPlanificacionRepository solicitudesRepository,
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      SolicitudPlanificacionMapper solicitudMapper) {
    this.solicitudesRepository = solicitudesRepository;
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.solicitudMapper = solicitudMapper;
  }

  @Override
  public SolicitudPlanificacionResponseDTO crearSolicitud(SolicitudPlanificacionRequestDTO dto) {
    validarSolicitud(dto);
    dto.entregaIds().forEach(this::buscarEntrega);

    SolicitudPlanificacion solicitud = solicitudMapper.toEntity(dto);
    return solicitudMapper.toResponseDTO(solicitudesRepository.save(solicitud));
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

  @Override
  public void solicitarPlanificacionParaSiguienteJornada() {
    // La planificación automática queda como punto de entrada del scheduler.
    // En esta etapa, las solicitudes concretas se crean desde crearSolicitud(...)
    // con los ids de entregas recibidos por API.
  }

  private UUID guardarRutaPlanificada(RutaPlanificadaDTO dto) {
    if (dto == null || dto.entregaIds() == null || dto.entregaIds().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    Ruta ruta = new Ruta(dto.fecha(), dto.choferId(), dto.camionId());
    dto.entregaIds()
        .forEach(
            entregaId -> {
              Entrega entrega = buscarEntrega(entregaId);
              ruta.agregarEntrega(entrega.getId());
              entrega.asignarRuta(ruta.getId());
              entregasRepository.save(entrega);
            });

    return rutasRepository.save(ruta).getId();
  }

  private void validarSolicitud(SolicitudPlanificacionRequestDTO dto) {
    if (dto == null || dto.entregaIds() == null || dto.entregaIds().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    if (dto.entregaIds().size() > MAX_ENTREGAS_POR_SOLICITUD) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
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
