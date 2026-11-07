package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.SolicitudCambioEstadoDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
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
  private final ISubcategoriasRepository subcategoriasRepository;
  private final DonacionIndependienteMapper donacionIndependienteMapper;
  private final ApplicationEventPublisher eventPublisher;

  public DonacionesIndependientesService(
      IDonacionesIndependientesRepository repositorio,
      IDonacionesRepository donacionesRepository,
      INecesidadesRepository necesidadRepository,
      ISubcategoriasRepository subcategoriasRepository,
      DonacionIndependienteMapper donacionIndependienteMapper,
      ApplicationEventPublisher eventPublisher) {
    this.repositorio = repositorio;
    this.donacionesRepository = donacionesRepository;
    this.necesidadRepository = necesidadRepository;
    this.subcategoriasRepository = subcategoriasRepository;
    this.donacionIndependienteMapper = donacionIndependienteMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public List<DonacionIndependienteResponseDTO> obtenerTodas() {
    return repositorio.findAll().stream().map(donacionIndependienteMapper::toDTO).toList();
  }

  @Override
  public List<DonacionIndependienteResponseDTO> obtenerConFiltros(
      FiltroDonacionesIndependientes filtro) {
    LocalDate desde = filtro.desde();
    LocalDate hasta = filtro.hasta();
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    TipoEstadoDonacion estado = filtro.estado();
    UUID subcategoriaId = filtro.subcategoriaId();
    UUID entidadId = filtro.entidadId();

    // ponytail: filtra en memoria sobre findAll(), como hacía este listado; pasar a una consulta
    // cuando el volumen lo pida.
    return repositorio.findAll().stream()
        .filter(delDonante(filtro.donanteId()))
        .filter(
            di ->
                estado == null
                    || (di.getEstadoActual() != null && di.getEstadoActual().getTipo() == estado))
        .filter(di -> subcategoriaId == null || tieneSubcategoria(di, Set.of(subcategoriaId)))
        .filter(deLaCategoria(filtro.categoriaId()))
        .filter(di -> entidadId == null || entidadId.equals(di.getEntidadBeneficiariaId()))
        .filter(di -> desde == null || !di.getFechaRegistro().toLocalDate().isBefore(desde))
        .filter(di -> hasta == null || !di.getFechaRegistro().toLocalDate().isAfter(hasta))
        .map(donacionIndependienteMapper::toDTO)
        .toList();
  }

  /** Las que salen de una donación del donante; sin donante, todas. */
  private Predicate<DonacionIndependiente> delDonante(UUID donanteId) {
    if (donanteId == null) {
      return di -> true;
    }
    Set<UUID> donaciones =
        donacionesRepository.findAll().stream()
            .filter(d -> donanteId.equals(d.getDonanteId()))
            .map(Donacion::getId)
            .collect(Collectors.toSet());
    return di -> donaciones.contains(di.getDonacionOriginalId());
  }

  /** Las que tienen algún ítem de una subcategoría de la categoría; sin categoría, todas. */
  private Predicate<DonacionIndependiente> deLaCategoria(UUID categoriaId) {
    if (categoriaId == null) {
      return di -> true;
    }
    Set<UUID> subcategorias =
        subcategoriasRepository.findAll().stream()
            .filter(s -> categoriaId.equals(s.getCategoriaId()))
            .map(Subcategoria::getId)
            .collect(Collectors.toSet());
    return di -> tieneSubcategoria(di, subcategorias);
  }

  // getItems() nunca es nula y cada ítem valida su bien al construirse.
  private static boolean tieneSubcategoria(DonacionIndependiente di, Set<UUID> subcategorias) {
    return di.getItems().stream()
        .anyMatch(item -> subcategorias.contains(item.bien().subcategoriaId()));
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
