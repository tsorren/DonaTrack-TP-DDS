package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.dto.propuestas.DonacionEnPropuestasDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.FiltroPropuestas;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.ResumenPropuestasDTO;
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
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.entities.propuestas.TipoAlgoritmo;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioPropuestas;
import grupo5.donaciones.models.repositories.listados.OrdenPropuestas;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.EjecucionRealizada;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import grupo5.donaciones.services.ParametrosListado;
import grupo5.donaciones.services.logistica.ILogisticaBroker;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.DonacionIndependienteMapper;
import grupo5.donaciones.services.mappers.EjecucionAsignacionMapper;
import grupo5.donaciones.services.mappers.PropuestaMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
  private static final String AMBOS = "AMBOS";

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
  private final DonacionIndependienteMapper donacionIndependienteMapper;

  @Override
  public EjecucionRealizada ejecutarAsignacion() {
    List<DonacionIndependiente> donaciones = donacionRepository.findEnDeposito();
    // "Satisfecha" depende de lo asignado, que no guarda la necesidad (DTI-06)
    List<Necesidad> necesidades =
        necesidadRepository.findActivas().stream()
            .filter(n -> n.admiteAsignaciones(LocalDate.now(ZoneId.systemDefault())))
            .filter(n -> !acumulados.estaSatisfecha(n))
            .toList();

    List<Propuesta> propuestas =
        gestorPropuestas.generarPropuestas(
            necesidades, donaciones, acumulados.datosPara(necesidades));
    propuestas.forEach(propuestaRepository::save);

    EjecucionAsignacion ejecucion = new EjecucionAsignacion(propuestas.size());
    asignacionRepository.save(ejecucion);

    return new EjecucionRealizada(ejecucion.getId(), propuestaMapper.toDTOsSinCalculo(propuestas));
  }

  @Override
  public Pagina<PropuestaDTO> listarPropuestas(FiltroPropuestas filtro, ParametrosListado listado) {
    TipoAlgoritmo algoritmo = null;
    boolean ambos = false;
    if (filtro.algoritmo() != null && !filtro.algoritmo().isBlank()) {
      String pedido = filtro.algoritmo().trim().toUpperCase(Locale.ROOT);
      if (AMBOS.equals(pedido)) {
        ambos = true;
      } else {
        algoritmo = enumDe(TipoAlgoritmo.class, pedido);
      }
    }
    Tramo<PropuestaEnListado> tramo =
        propuestaRepository.buscar(
            new CriterioPropuestas(
                filtro.estado() == null ? Set.of() : Set.copyOf(filtro.estado()),
                filtro.situacion() == null ? Set.of() : Set.copyOf(filtro.situacion()),
                algoritmo,
                ambos,
                filtro.donacion(),
                listado.orden(OrdenPropuestas.class, OrdenPropuestas.PUESTO),
                listado.paginado()));
    return new Pagina<>(propuestaMapper.toDTOs(tramo.elementos()), (int) tramo.total());
  }

  @Override
  public ResumenPropuestasDTO resumen() {
    Map<SituacionPropuesta, Long> conteo = propuestaRepository.contarPendientesPorSituacion();
    long libres = conteo.getOrDefault(SituacionPropuesta.LIBRE, 0L);
    long enConflicto = conteo.getOrDefault(SituacionPropuesta.EN_CONFLICTO, 0L);
    long sinStock = conteo.getOrDefault(SituacionPropuesta.SIN_STOCK, 0L);
    return new ResumenPropuestasDTO(libres + enConflicto + sinStock, libres, enConflicto, sinStock);
  }

  @Override
  public Pagina<DonacionEnPropuestasDTO> donacionesEnPendientes(
      UUID categoriaId, boolean soloEnConflicto, ParametrosListado listado) {
    Tramo<StockEnPropuestas> tramo =
        propuestaRepository.buscarDonacionesEnPendientes(
            categoriaId, soloEnConflicto, listado.paginado());
    Map<UUID, DonacionIndependienteResponseDTO> dtos = new HashMap<>();
    donacionIndependienteMapper
        .toDTOs(
            donacionRepository.findAllById(
                tramo.elementos().stream().map(StockEnPropuestas::donacionId).toList()))
        .forEach(dto -> dtos.put(dto.id(), dto));
    return new Pagina<>(
        tramo.elementos().stream()
            .map(
                s ->
                    new DonacionEnPropuestasDTO(
                        dtos.get(s.donacionId()),
                        s.disponible(),
                        s.pedidaEnPendientes(),
                        s.propuestasPendientes(),
                        s.alcanza()))
            .toList(),
        (int) tramo.total());
  }

  private static <E extends Enum<E>> E enumDe(Class<E> tipo, String valor) {
    try {
      return Enum.valueOf(tipo, valor);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }

  /**
   * Aprobar una propuesta es una sola transacción: el listener síncrono de {@link
   * PropuestaAprobada} se une a ella, así que las donaciones asignadas, el pedido al broker y el
   * nuevo estado de la propuesta se confirman juntos o no se confirma ninguno. Los eventos AMQP
   * salen recién después del commit.
   */
  @Override
  @Transactional
  public void actualizarEstado(UUID id, EstadoPropuesta estado, String actor) {
    Propuesta propuesta =
        propuestaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    switch (estado) {
      case APROBADA -> {
        if (propuesta.getEstado() == EstadoPropuesta.PENDIENTE) {
          // Si ya no está pendiente, aceptar() informa la transición inválida.
          validarAprobable(propuesta);
        }
        propuesta.aceptar(actor);
        propuesta.getDomainEvents().forEach(eventPublisher::publishEvent);
        propuesta.clearDomainEvents();
      }
      case DESCARTADA -> propuesta.rechazar(actor);
      default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    propuestaRepository.save(propuesta);
  }

  /**
   * Una propuesta solo se aprueba si su necesidad sigue admitiendo asignaciones (activa y sin
   * vencer) y ninguna de sus donaciones pertenece a la propia entidad beneficiaria (una jurídica no
   * se auto-dona).
   */
  private void validarAprobable(Propuesta propuesta) {
    UUID necesidadId = propuesta.getNecesidadQueSatisfaceId();
    Necesidad necesidad =
        necesidadRepository
            .findById(necesidadId)
            .orElseThrow(() -> new RecursoNoEncontradoException(necesidadId));
    if (!necesidad.admiteAsignaciones(LocalDate.now(ZoneId.systemDefault()))) {
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
  public Pagina<EjecucionAsignacionDTO> historialEjecuciones(ParametrosListado listado) {
    Tramo<EjecucionAsignacion> tramo = asignacionRepository.historial(listado.paginado());
    return new Pagina<>(
        tramo.elementos().stream().map(ejecucionMapper::toDTO).toList(), (int) tramo.total());
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
            LocalDateTime.now(ZoneId.systemDefault()),
            donacionesRepository
                .findById(donacionAsignar.getDonacionOriginalId())
                .map(Donacion::getDonanteId)
                .orElse(null)));
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
