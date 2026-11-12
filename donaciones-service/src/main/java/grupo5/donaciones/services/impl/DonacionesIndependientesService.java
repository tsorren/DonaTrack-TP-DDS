package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioDonacionesIndependientes;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

  private static final Logger log = LoggerFactory.getLogger(DonacionesIndependientesService.class);

  private final IDonacionesIndependientesRepository repositorio;
  private final INecesidadesRepository necesidadRepository;
  private final DonacionIndependienteMapper donacionIndependienteMapper;
  private final ApplicationEventPublisher eventPublisher;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      INecesidadesRepository necesidadRepository,
      DonacionIndependienteMapper donacionIndependienteMapper,
      ApplicationEventPublisher eventPublisher) {
    this.repositorio = repositorio;
    this.necesidadRepository = necesidadRepository;
    this.donacionIndependienteMapper = donacionIndependienteMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Pagina<DonacionIndependienteResponseDTO> obtenerConFiltros(
      FiltroDonacionesIndependientes filtro, ParametrosListado listado) {
    LocalDate desde = filtro.desde();
    LocalDate hasta = filtro.hasta();
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    // R11-01: la búsqueda, los filtros, el orden y el tramo se resuelven en la base
    Tramo<DonacionIndependiente> tramo =
        repositorio.buscar(
            new CriterioDonacionesIndependientes(
                filtro.estado() == null ? Set.of() : Set.copyOf(filtro.estado()),
                filtro.subcategoriaId(),
                filtro.categoriaId(),
                filtro.donanteId(),
                filtro.entidadId(),
                desde,
                hasta,
                filtro.necesidadId(),
                listado.texto(),
                listado.orden(OrdenPorFecha.class, OrdenPorFecha.RECIENTES),
                listado.paginado()));
    return new Pagina<>(donacionIndependienteMapper.toDTOs(tramo.elementos()), (int) tramo.total());
  }

  @Override
  public DonacionIndependienteResponseDTO obtener(UUID id) {
    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    return donacionIndependienteMapper.toDTO(donacion);
  }

  @Override
  public void registrarCantidadRecibida(UUID id, double cantidadRecibida) {
    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    donacion.registrarCantidadRecibida(cantidadRecibida);
    repositorio.save(donacion);
  }

  @Override
  public void registrarFotosDeRecepcion(UUID id, LocalDateTime fechaEntrega, List<String> fotos) {
    DonacionIndependiente donacion =
        repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    donacion.registrarFotosDeRecepcion(fechaEntrega, fotos);
    repositorio.save(donacion);
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
      // HB-19 (punto 9 del ADR de la baja): como al aprobar una propuesta
      if (request.estado() == TipoEstadoDonacion.ASIGNACION_REALIZADA
          && !necesidad.admiteAsignaciones(LocalDate.now(ZoneId.systemDefault()))) {
        throw new BusinessStateException(ErrorCatalog.NECESIDAD_INACTIVA);
      }
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
