package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private static final Logger log = LoggerFactory.getLogger(DonacionesIndependientesService.class);

  private final IDonacionesIndependientesRepository repositorio;
  private final IDonacionesRepository donacionesRepository;
  private final INecesidadesRepository necesidadRepository;
  private final DonacionIndependienteMapper donacionIndependienteMapper;
  private final ApplicationEventPublisher eventPublisher;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      IDonacionesRepository donacionesRepository,
      INecesidadesRepository necesidadRepository,
      DonacionIndependienteMapper donacionIndependienteMapper,
      ApplicationEventPublisher eventPublisher) {
    this.repositorio = repositorio;
    this.donacionesRepository = donacionesRepository;
    this.necesidadRepository = necesidadRepository;
    this.donacionIndependienteMapper = donacionIndependienteMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public List<DonacionIndependienteResponseDTO> obtenerTodas() {
    return repositorio.findAll().stream().map(donacionIndependienteMapper::toDTO).toList();
  }

  @Override
  public List<DonacionIndependienteResponseDTO> obtenerConFiltros(
      TipoEstadoDonacion estado, UUID subcategoriaId, UUID donanteId) {
    Set<UUID> donacionesOriginalesPermitidas = null;
    if (donanteId != null) {
      donacionesOriginalesPermitidas =
          donacionesRepository.findAll().stream()
              .filter(d -> donanteId.equals(d.getDonanteId()))
              .map(Donacion::getId)
              .collect(Collectors.toSet());
    }

    final Set<UUID> filtroDonaciones = donacionesOriginalesPermitidas;

    return repositorio.findAll().stream()
        .filter(
            di -> filtroDonaciones == null || filtroDonaciones.contains(di.getDonacionOriginalId()))
        .filter(
            di ->
                estado == null
                    || (di.getEstadoActual() != null && di.getEstadoActual().getTipo() == estado))
        .filter(
            di ->
                subcategoriaId == null
                    || (di.getItems() != null
                        && di.getItems().stream()
                            .anyMatch(
                                item ->
                                    item.bien() != null
                                        && subcategoriaId.equals(item.bien().subcategoriaId()))))
        .map(donacionIndependienteMapper::toDTO)
        .toList();
  }

  @Override
  public DonacionIndependienteResponseDTO obtener(UUID id) {
    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return donacionIndependienteMapper.toDTO(donacion);
  }

  @Override
  public DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor) {

    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    Necesidad necesidad = null;
    if (request.necesidadId() != null) {
      necesidad =
          necesidadRepository
              .findById(request.necesidadId())
              .orElseThrow(() -> new RecursoNoEncontradoException(request.necesidadId()));
    }

    SolicitudCambioEstadoDonacionIndependiente solicitud =
        new SolicitudCambioEstadoDonacionIndependiente(
            request.estado(),
            necesidad,
            request.justificacion(),
            request.urlMapa(),
            request.patenteCamion(),
            request.replanificable(),
            actor);

    donacion.cambiarEstado(solicitud);

    repositorio.save(donacion);
    donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    donacion.clearDomainEvents();

    return donacionIndependienteMapper.toDTO(donacion);
  }

  @Override
  public void vencer() {
    repositorio.findEnDeposito().stream()
        .filter(DonacionIndependiente::estaVencida)
        .forEach(this::vencerUna);
  }

  // Si otra operación (por ejemplo una asignación) guardó la donación a la vez, se saltea solo
  // esa: la próxima corrida la reevalúa. No debe frenar el vencimiento de las demás.
  private void vencerUna(DonacionIndependiente donacion) {
    try {
      donacion.vencer("SISTEMA");
      repositorio.save(donacion);
    } catch (BusinessStateException e) {
      if (e.getError() != ErrorCatalog.DONACION_INDEPENDIENTE_MODIFICADA_CONCURRENTEMENTE) {
        throw e;
      }
      log.warn("No se venció la donación {}: otro proceso la modificó a la vez", donacion.getId());
      return;
    }
    donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    donacion.clearDomainEvents();
  }
}
