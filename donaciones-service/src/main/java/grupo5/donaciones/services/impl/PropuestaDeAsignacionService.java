package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.NuevaEntregaRequest;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.personas.Persona;
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
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
import grupo5.donaciones.services.mappers.LogisticaRequestMapper;
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
  private final IEntidadesBeneficiariasRepository entidadesBeneficiariasRepository;
  private final IPersonasRepository personasRepository;
  private final LogisticaAsyncService logisticaAsyncService;
  private final LogisticaRequestMapper logisticaRequestMapper;

  @Override
  public List<PropuestaDTO> ejecutarAsignacion() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    List<Necesidad> necesidades = necesidadRepository.findByEstaSatisfechaFalseActivaTrue();

    List<Propuesta> propuestas = gestorPropuestas.generarPropuestas(necesidades, donaciones);
    propuestas.forEach(propuestaRepository::save);

    EjecucionAsignacion ejecucion = new EjecucionAsignacion(propuestas.size());
    asignacionRepository.save(ejecucion);

    return propuestas.stream().map(propuestaMapper::toDTO).toList();
  }

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

    return logisticaRequestMapper.toRequest(donacionAsignar, entidad, persona);
  }
}
