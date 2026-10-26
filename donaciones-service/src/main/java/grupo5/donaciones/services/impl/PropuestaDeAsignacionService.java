package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donantes.Donante;
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
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final DireccionMapper direccionMapper;
  private final IDonacionesRepository donacionesRepository;
  private final IDonantesRepository donantesRepository;
  private final IDonacionesEventPublisher donacionesEventPublisher;
  private final ILogisticaBroker logisticaBroker;
  private final AcumuladosDeAsignacion acumulados;

  @Override
  public List<PropuestaDTO> ejecutarAsignacion() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    // "Satisfecha" depende de lo asignado, que no guarda la necesidad (DTI-06)
    List<Necesidad> necesidades =
        necesidadRepository.findActivas().stream()
            .filter(n -> !acumulados.estaSatisfecha(n))
            .toList();

    List<Propuesta> propuestas =
        gestorPropuestas.generarPropuestas(
            necesidades, donaciones, acumulados.datosPara(necesidades));
    propuestas.forEach(propuestaRepository::save);

    EjecucionAsignacion ejecucion = new EjecucionAsignacion(propuestas.size());
    asignacionRepository.save(ejecucion);

    return propuestas.stream().map(propuestaMapper::toDTO).toList();
  }

  @Override
  public List<PropuestaDTO> listarPropuestas() {
    return propuestaRepository.findAll().stream().map(propuestaMapper::toDTO).toList();
  }

  /**
   * Aprobar una propuesta es una sola transacción: el listener síncrono de {@link
   * PropuestaAprobada} se une a ella, así que las donaciones asignadas, el pedido al broker y el
   * nuevo estado de la propuesta se confirman juntos o no se confirma ninguno. Los eventos AMQP
   * salen recién después del commit.
   */
  @Override
  @Transactional
  public void actualizarEstado(UUID id, EstadoPropuesta estado) {
    Propuesta propuesta =
        propuestaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    switch (estado) {
      case APROBADA -> {
        if (propuesta.getEstado() == EstadoPropuesta.PENDIENTE) {
          // Si ya no está pendiente, aceptar() informa la transición inválida.
          validarAprobable(propuesta);
        }
        propuesta.aceptar("SISTEMA");
        propuesta.getDomainEvents().forEach(eventPublisher::publishEvent);
        propuesta.clearDomainEvents();
      }
      case DESCARTADA -> propuesta.rechazar();
      default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    propuestaRepository.save(propuesta);
  }

  /**
   * Una propuesta solo se aprueba si su necesidad sigue activa y ninguna de sus donaciones
   * pertenece a la propia entidad beneficiaria (una jurídica no se auto-dona).
   */
  private void validarAprobable(Propuesta propuesta) {
    UUID necesidadId = propuesta.getNecesidadQueSatisfaceId();
    Necesidad necesidad =
        necesidadRepository
            .findById(necesidadId)
            .orElseThrow(() -> new RecursoNoEncontradoException(necesidadId));
    if (!necesidad.isActiva()) {
      throw new BusinessStateException(ErrorCatalog.NECESIDAD_INACTIVA);
    }

    List<PosibleFragmentacion> fragmentaciones = propuesta.getPosiblesFragmentaciones();
    if (fragmentaciones == null) {
      return;
    }
    UUID entidadId = necesidad.getEntidadId();
    boolean autoDonacion =
        entidadId != null
            && fragmentaciones.stream()
                .anyMatch(f -> entidadId.equals(donanteDe(f.getDonacionOriginalId())));
    if (autoDonacion) {
      throw new BusinessStateException(ErrorCatalog.DONACION_A_SI_MISMO);
    }
  }

  private UUID donanteDe(UUID donacionIndependienteId) {
    return donacionRepository
        .findById(donacionIndependienteId)
        .flatMap(
            independiente -> donacionesRepository.findById(independiente.getDonacionOriginalId()))
        .map(Donacion::getDonanteId)
        .orElse(null);
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

    // La entidad beneficiaria y su dirección dependen únicamente de `necesidad`, que es la misma
    // en todas las fragmentaciones de este evento — se resuelven una sola vez acá afuera del for,
    // en vez de una vez por cada fragmentación.
    DatosBeneficiario datosBeneficiario = resolverDatosBeneficiario(necesidad);

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

      publicarDonacionAsignada(donacionAsignar, datosBeneficiario);
      solicitarEntregaALogistica(donacionAsignar, datosBeneficiario);
    }
    // La necesidad no cambia: la asignación queda registrada en cada donación (DTI-06)
  }

  private record DatosBeneficiario(UUID personaBeneficiariaId, DestinoEventoDTO destino) {}

  private DatosBeneficiario resolverDatosBeneficiario(Necesidad necesidad) {
    try {
      EntidadBeneficiaria entidad =
          entidadesBeneficiariasRepository
              .findById(necesidad.getEntidadId())
              .orElseThrow(() -> new RecursoNoEncontradoException(necesidad.getEntidadId()));

      Persona personaBeneficiaria =
          personasRepository
              .findById(entidad.juridicaId())
              .orElseThrow(() -> new RecursoNoEncontradoException(entidad.juridicaId()));

      return new DatosBeneficiario(
          entidad.juridicaId(),
          direccionMapper.toDestinoEventoDTO(personaBeneficiaria.getDireccion()));
    } catch (Exception e) {
      log.error(
          "No se pudieron resolver los datos de la entidad beneficiaria para donacion.asignada.v1 (necesidad {}): {}",
          necesidad.getId(),
          e.getMessage(),
          e);
      return null;
    }
  }

  private void publicarDonacionAsignada(
      DonacionIndependiente donacionAsignar, DatosBeneficiario datosBeneficiario) {
    if (datosBeneficiario == null) {
      log.warn(
          "No se publica donacion.asignada.v1 para donación {}: no se pudieron resolver los datos"
              + " de la entidad beneficiaria",
          donacionAsignar.getId());
      return;
    }
    try {
      donacionesEventPublisher.publicarDonacionAsignada(
          construirEventoDonacionAsignada(donacionAsignar, datosBeneficiario));
    } catch (Exception e) {
      log.error(
          "No se pudo publicar donacion.asignada.v1 (donación {}): {}",
          donacionAsignar.getId(),
          e.getMessage(),
          e);
    }
  }

  /**
   * Le pide al broker que consiga un proveedor de logística para la donación. Logística ya no
   * escucha {@code donacion.asignada.v1}: recibe el pedido solo si el broker la eligió. El broker
   * solo registra el pedido en su outbox, no habla con la red. Corre dentro de la transacción de la
   * aprobación: si falla, la excepción se propaga y se revierte la aprobación completa (la donación
   * no puede quedar asignada sin su pedido de entrega).
   */
  private void solicitarEntregaALogistica(
      DonacionIndependiente donacionAsignar, DatosBeneficiario datosBeneficiario) {
    if (datosBeneficiario == null) {
      log.warn(
          "No se solicita la entrega de la donación {} a logística: no se pudieron resolver los"
              + " datos de la entidad beneficiaria",
          donacionAsignar.getId());
      return;
    }
    logisticaBroker.solicitarEntrega(
        new DatosEntregaLogistica(
            donacionAsignar.getId(),
            datosBeneficiario.personaBeneficiariaId(),
            datosBeneficiario.destino(),
            donacionAsignar.getPesoTotal(),
            donacionAsignar.getVolumenTotal(),
            LocalDateTime.now(ZoneId.systemDefault())));
  }

  private EventoDonacionAsignadaV1 construirEventoDonacionAsignada(
      DonacionIndependiente donacionAsignar, DatosBeneficiario datosBeneficiario) {
    Donacion donacionOriginal =
        donacionesRepository
            .findById(donacionAsignar.getDonacionOriginalId())
            .orElseThrow(
                () -> new RecursoNoEncontradoException(donacionAsignar.getDonacionOriginalId()));
    UUID donanteId = donacionOriginal.getDonanteId();
    Donante donante =
        donantesRepository
            .findById(donanteId)
            .orElseThrow(() -> new RecursoNoEncontradoException(donanteId));

    return new EventoDonacionAsignadaV1(
        donacionAsignar.getId(),
        donanteId,
        donante.personaId(),
        LocalDateTime.now(ZoneId.systemDefault()),
        datosBeneficiario.personaBeneficiariaId(),
        donacionAsignar.getDescripcion(),
        datosBeneficiario.destino(),
        donacionAsignar.getPesoTotal(),
        donacionAsignar.getVolumenTotal());
  }
}
