package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Persona;
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
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final IPersonasRepository personasRepository;
  private final DireccionMapper direccionMapper;
  private final LogisticaAsyncService logisticaAsyncService;

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

    asignacionRepository.save(ejecucion);

    return propuestas.stream().map(propuestaMapper::toDTO).toList();
  }

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
