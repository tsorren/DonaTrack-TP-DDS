package grupo5.donaciones.services.impl;

<<<<<<< HEAD
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
=======
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Persona;
<<<<<<< HEAD
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.mappers.DireccionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PropuestaDeAsignacionService {

  private static final Logger log = LoggerFactory.getLogger(PropuestaDeAsignacionService.class);

  private final AsignacionService asignacionService;
  private final IAsignacionesRepository asignacionRepository;
  private final grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository;
  private final grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository
      donacionRepository;
  private final grupo5.donaciones.services.mappers.PropuestaMapper propuestaMapper;
=======
import grupo5.donaciones.models.entities.propuestas.EjecucionAsignacion;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.GestorPropuestasDeAsignacion;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.entities.propuestas.PropuestaAprobada;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropuestaDeAsignacionService implements IPropuestaDeAsignacionService {

  private static final Logger log = LoggerFactory.getLogger(PropuestaDeAsignacionService.class);

  private final GestorPropuestasDeAsignacion gestorPropuestas;
  private final IDonacionesIndependientesRepository donacionRepository;
  private final INecesidadesRepository necesidadRepository;
  private final IPropuestasRepository propuestaRepository;
  private final IAsignacionesRepository asignacionRepository;
  private final PropuestaMapper propuestaMapper;
  private final EjecucionAsignacionMapper ejecucionMapper;
  private final ApplicationEventPublisher eventPublisher;
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final IPersonasRepository personasRepository;
  private final DireccionMapper direccionMapper;
  private final LogisticaAsyncService logisticaAsyncService;

<<<<<<< HEAD
  public PropuestaDeAsignacionService(
      AsignacionService asignacionService,
      IAsignacionesRepository asignacionRepository,
      grupo5.donaciones.models.repositories.INecesidadesRepository necesidadRepository,
      grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository donacionRepository,
      grupo5.donaciones.services.mappers.PropuestaMapper propuestaMapper,
      IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository,
      IPersonasRepository personasRepository,
      DireccionMapper direccionMapper,
      LogisticaAsyncService logisticaAsyncService) {
    this.asignacionService = asignacionService;
    this.asignacionRepository = asignacionRepository;
    this.necesidadRepository = necesidadRepository;
    this.donacionRepository = donacionRepository;
    this.propuestaMapper = propuestaMapper;
    this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    this.personasRepository = personasRepository;
    this.direccionMapper = direccionMapper;
    this.logisticaAsyncService = logisticaAsyncService;
  }

  public List<grupo5.donaciones.dto.propuestas.PropuestaDTO> ejecutarAsignacion() {
    List<Propuesta> propuestas = asignacionService.generarPropuestas();

    EjecucionAsignacionDTO ejecucion = new EjecucionAsignacionDTO();
    ejecucion.setFechaEjecucion(LocalDateTime.now(java.time.ZoneId.systemDefault()));
    ejecucion.setCantidadPropuestasGeneradas(propuestas.size());

=======
  @Override
  public List<PropuestaDTO> ejecutarAsignacion() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    List<Necesidad> necesidades = necesidadRepository.findByEstaSatisfechaFalseActivaTrue();

    List<Propuesta> propuestas = gestorPropuestas.generarPropuestas(necesidades, donaciones);
    propuestas.forEach(propuestaRepository::save);

    EjecucionAsignacion ejecucion = new EjecucionAsignacion(propuestas.size());
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb
    asignacionRepository.save(ejecucion);

    return propuestas.stream().map(propuestaMapper::toDTO).toList();
  }

<<<<<<< HEAD
  public List<grupo5.donaciones.dto.propuestas.PropuestaDTO> listarPropuestas() {
    return asignacionService.listarPropuestas().stream().map(propuestaMapper::toDTO).toList();
  }

  public void actualizarEstado(UUID id, EstadoPropuesta estado) {
    asignacionService.actualizarEstadoPropuesta(id, estado);
  }

  public List<EjecucionAsignacionDTO> historialEjecuciones() {
    return asignacionRepository.obtenerHistorial();
  }

  @org.springframework.context.event.EventListener
  public void onPropuestaAprobada(
      // TODO: Modificar este metodo
      grupo5.donaciones.models.entities.propuestas.PropuestaAprobada event) {
    grupo5.donaciones.models.entities.necesidades.Necesidad necesidad =
        necesidadRepository
            .findById(event.necesidadId())
            .orElseThrow(
                () ->
                    new grupo5.common.exceptions.ValidationException(
                        grupo5.common.exceptions.ErrorCatalog.RECURSO_NO_ENCONTRADO));
    String actor = event.actor();

    for (grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion f :
        event.fragmentaciones()) {
      grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente
          donacionOriginal =
              donacionRepository
                  .findById(f.getDonacionOriginalId())
                  .orElseThrow(
                      () ->
                          new grupo5.common.exceptions.ValidationException(
                              grupo5.common.exceptions.ErrorCatalog.RECURSO_NO_ENCONTRADO));
      Integer cantidadNecesaria = f.getCantidadNecesaria();
      grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente
          donacionAsignar;

      if (donacionOriginal.getCantidad() > cantidadNecesaria) {
        donacionAsignar = donacionOriginal.fragmentarse(cantidadNecesaria);
      } else {
        donacionAsignar = donacionOriginal;
      }

      donacionAsignar.asignar(actor, necesidad);
      necesidad.asignarDonacion(donacionAsignar);

      donacionRepository.save(donacionOriginal);
      donacionRepository.save(donacionAsignar);
=======
  @Override
  public List<PropuestaDTO> listarPropuestas() {
    return propuestaRepository.findAll().stream().map(propuestaMapper::toDTO).toList();
  }

  @Override
  public void actualizarEstado(UUID id, EstadoPropuesta estado) {
    Propuesta propuesta =
        propuestaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    switch (estado) {
      case APROBADA -> {
        propuesta.aceptar("SISTEMA");
        propuesta.getDomainEvents().forEach(eventPublisher::publishEvent);
        propuesta.clearDomainEvents();
      }
      case DESCARTADA -> propuesta.rechazar();
      default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    propuestaRepository.save(propuesta);
  }

  @Override
  public List<EjecucionAsignacionDTO> historialEjecuciones() {
    return asignacionRepository.obtenerHistorial().stream().map(ejecucionMapper::toDTO).toList();
  }

  @EventListener
  public void onPropuestaAprobada(PropuestaAprobada event) {
    log.info("Procesando PropuestaAprobada para necesidad {}", event.necesidadId());
    Necesidad necesidad =
        necesidadRepository
            .findById(event.necesidadId())
            .orElseThrow(() -> new RecursoNoEncontradoException(event.necesidadId()));
    String actor = event.actor();

    for (PosibleFragmentacion f : event.fragmentaciones()) {
      DonacionIndependiente donacionOriginal =
          donacionRepository
              .findById(f.getDonacionOriginalId())
              .orElseThrow(() -> new RecursoNoEncontradoException(f.getDonacionOriginalId()));

      f.setDonacionOriginal(donacionOriginal);
      DonacionIndependiente donacionAsignar = f.confirmar(necesidad, actor);

      donacionRepository.save(donacionOriginal);
      if (donacionAsignar != donacionOriginal) {
        donacionRepository.save(donacionAsignar);
      }
>>>>>>> c157e6e3625f7aab65222bbcdb0be485471ebbfb

      notificarLogistica(donacionAsignar, necesidad);
    }

    necesidadRepository.save(necesidad);
  }

  private void notificarLogistica(DonacionIndependiente donacionAsignar, Necesidad necesidad) {
    try {
      logisticaAsyncService.registrarEntregaPendiente(
          construirSolicitudEntrega(donacionAsignar, necesidad));
    } catch (Exception e) {
      log.error(
          "No se pudo armar la solicitud de entrega para logística (donación {}): {}",
          donacionAsignar.getId(),
          e.getMessage(),
          e);
    }
  }

  private NuevaEntregaRequest construirSolicitudEntrega(
      DonacionIndependiente donacionAsignar, Necesidad necesidad) {
    EntidadBeneficiaria entidad =
        entidadesBeneficiariasRepository
            .findById(necesidad.getEntidadId())
            .orElseThrow(() -> new RecursoNoEncontradoException(necesidad.getEntidadId()));

    Persona persona =
        personasRepository
            .findById(entidad.juridicaId())
            .orElseThrow(() -> new RecursoNoEncontradoException(entidad.juridicaId()));

    return new NuevaEntregaRequest(
        donacionAsignar.getId(),
        entidad.getId(),
        direccionMapper.toOutputDTO(persona.getDireccion()),
        donacionAsignar.getPesoTotal(),
        donacionAsignar.getVolumenTotal());
  }
}
