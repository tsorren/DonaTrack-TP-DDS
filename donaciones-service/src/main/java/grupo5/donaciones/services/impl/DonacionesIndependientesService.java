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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DonacionesIndependientesService implements IDonacionesIndependientesService {

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
    return switch (request.estado()) {
      case ASIGNACION_REALIZADA -> asignar(id, request.necesidadId(), actor);
      case LISTA_PARA_ENTREGAR ->
          planificarRuta(id, request.urlMapa(), request.patenteCamion(), actor);
      case EN_TRASLADO -> iniciarRecorrido(id, actor);
      case ENTREGADA -> confirmarEntrega(id, actor);
      case ENTREGA_FALLIDA ->
          registrarFalla(id, request.justificacion(), request.replanificable(), actor);
      case EN_DEPOSITO -> retornar(id, actor);
      case VENCIDA -> vencer(id, actor);
      default -> throw new BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA);
    };
  }

  private DonacionIndependienteResponseDTO asignar(UUID id, UUID necesidadId, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    Necesidad necesidad =
        necesidadRepository
            .findById(necesidadId)
            .orElseThrow(() -> new RecursoNoEncontradoException(necesidadId));
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ASIGNACION_REALIZADA, necesidad, actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO planificarRuta(
      UUID id, String urlMapa, String patenteCamion, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.LISTA_PARA_ENTREGAR,
            null,
            null,
            urlMapa,
            patenteCamion,
            null,
            actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO iniciarRecorrido(UUID id, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(TipoEstadoDonacion.EN_TRASLADO, actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO confirmarEntrega(UUID id, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(TipoEstadoDonacion.ENTREGADA, actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO registrarFalla(
      UUID id, String justificacion, Boolean replanificable, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(
            TipoEstadoDonacion.ENTREGA_FALLIDA,
            null,
            justificacion,
            null,
            null,
            replanificable,
            actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO retornar(UUID id, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(TipoEstadoDonacion.EN_DEPOSITO, actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependienteResponseDTO vencer(UUID id, String actor) {
    DonacionIndependiente donacion = buscarDonacion(id);
    donacion.cambiarEstado(
        new SolicitudCambioEstadoDonacionIndependiente(TipoEstadoDonacion.VENCIDA, actor));
    return guardarYRetornar(donacion);
  }

  private DonacionIndependiente buscarDonacion(UUID id) {
    return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private DonacionIndependienteResponseDTO guardarYRetornar(DonacionIndependiente donacion) {
    repositorio.save(donacion);
    donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    donacion.clearDomainEvents();
    return donacionIndependienteMapper.toDTO(donacion);
  }

  @Override
  public void vencer() {
    repositorio.findEnDeposito().stream()
        .filter(DonacionIndependiente::estaVencida)
        .forEach(
            donacion -> {
              donacion.vencer("SISTEMA");
              repositorio.save(donacion);
              donacion.getDomainEvents().forEach(eventPublisher::publishEvent);
              donacion.clearDomainEvents();
            });
  }
}
