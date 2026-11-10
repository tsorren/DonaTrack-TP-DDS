package grupo5.logistica.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.PosicionCamion;
import grupo5.logistica.models.entities.entregas.*;
import grupo5.logistica.models.entities.entregas.eventos.EntregaCancelada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaConfirmada;
import grupo5.logistica.models.entities.entregas.eventos.EntregaFallida;
import grupo5.logistica.models.entities.entregas.eventos.EventoEntrega;
import grupo5.logistica.models.entities.rutas.CambioEstadoRuta;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.models.repositories.ISolicitudesTransicionEntregaRepository;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IGeocodificadorDestino;
import grupo5.logistica.services.mappers.EntregaMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregasService implements IEntregasService {
  private static final Logger log = LoggerFactory.getLogger(EntregasService.class);
  private static final String ACTOR_BROKER = "broker-donaciones";
  private final IEntregasRepository entregasRepository;
  private final IRutasRepository rutasRepository;
  private final ICamionRepository camionRepository;
  private final EntregaMapper entregaMapper;
  private final ComunicadorEventosLogistica comunicadorEventos;
  private final ISolicitudesTransicionEntregaRepository solicitudesTransicionRepository;
  private final IGeocodificadorDestino geocodificadorDestino;

  public EntregasService(
      IEntregasRepository entregasRepository,
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      EntregaMapper entregaMapper,
      ComunicadorEventosLogistica comunicadorEventos) {
    this(
        entregasRepository,
        rutasRepository,
        camionRepository,
        entregaMapper,
        comunicadorEventos,
        null,
        null);
  }

  public EntregasService(
      IEntregasRepository entregasRepository,
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      EntregaMapper entregaMapper,
      ComunicadorEventosLogistica comunicadorEventos,
      @Nullable ISolicitudesTransicionEntregaRepository solicitudesTransicionRepository) {
    this(
        entregasRepository,
        rutasRepository,
        camionRepository,
        entregaMapper,
        comunicadorEventos,
        solicitudesTransicionRepository,
        null);
  }

  @Autowired
  public EntregasService(
      IEntregasRepository entregasRepository,
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      EntregaMapper entregaMapper,
      ComunicadorEventosLogistica comunicadorEventos,
      @Nullable ISolicitudesTransicionEntregaRepository solicitudesTransicionRepository,
      @Nullable IGeocodificadorDestino geocodificadorDestino) {
    this.entregasRepository = entregasRepository;
    this.rutasRepository = rutasRepository;
    this.camionRepository = camionRepository;
    this.entregaMapper = entregaMapper;
    this.comunicadorEventos = comunicadorEventos;
    this.solicitudesTransicionRepository = solicitudesTransicionRepository;
    this.geocodificadorDestino = geocodificadorDestino;
  }

  @Override
  @Transactional
  public EntregaResponseDTO crear(CrearEntregaRequestDTO dto) {
    Entrega entrega = entregaMapper.toEntity(dto);
    if (entrega == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (entregasRepository.existsByIdDonacion(entrega.getIdDonacion())) {
      throw new BusinessStateException(ErrorCatalog.ENTREGA_DONACION_DUPLICADA);
    }
    if (!entrega.getDestino().tieneCoordenadas() && geocodificadorDestino != null) {
      geocodificadorDestino
          .geocodificar(entrega.getDestino())
          .ifPresent(entrega.getDestino()::actualizarCoordenadas);
    }

    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EntregaResponseDTO> listar() {
    return entregasRepository.findAll().stream().map(entregaMapper::toResponseDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<EntregaMapaPublicoResponseDTO> listarMapaPublico() {
    return entregasRepository.findByEstado(EstadoEntrega.ENTREGADA).stream()
        .filter(e -> e.getDestino() != null && e.getDestino().tieneCoordenadas())
        .map(entregaMapper::toMapaPublicoResponseDTO)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public EntregaResponseDTO obtenerPorId(UUID id) {
    return entregaMapper.toResponseDTO(buscarEntrega(id));
  }

  @Override
  @Transactional(readOnly = true)
  public SeguimientoEntregaResponseDTO obtenerSeguimientoPorDonacion(UUID idDonacion) {
    if (idDonacion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Entrega entrega =
        entregasRepository
            .findByIdDonacion(idDonacion)
            .orElseThrow(() -> new RecursoNoEncontradoException(idDonacion));
    return construirSeguimiento(entrega);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SeguimientoEntregaResponseDTO> listarSeguimientoPorBeneficiaria(UUID idBeneficiaria) {
    if (idBeneficiaria == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    return entregasRepository.findByIdBeneficiaria(idBeneficiaria).stream()
        .map(this::construirSeguimiento)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SeguimientoEntregaResponseDTO> listarSeguimientoPorDonante(UUID donanteId) {
    if (donanteId == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    return entregasRepository.findByDonanteId(donanteId).stream()
        .filter(EntregasService::estaActiva)
        .map(this::construirSeguimiento)
        .toList();
  }

  private static boolean estaActiva(Entrega entrega) {
    return entrega.getEstadoActual() == EstadoEntrega.PENDIENTE
        || entrega.getEstadoActual() == EstadoEntrega.EN_TRASLADO;
  }

  @Override
  @Transactional
  public EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.adjuntarFotosRecepcion(dto.obtenerFotos());
    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  @Transactional
  public EntregaResponseDTO cambiarEstado(UUID id, CambioEstadoEntregaRequestDTO request) {
    if (request == null || request.estado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);

    SolicitudTransicionEntrega solicitud =
        switch (request.estado()) {
          case ENTREGADA -> procesarEntregaEntregada(entrega, request);
          case NO_RECIBIDA ->
              procesarEntregaNoRecibida(
                  request.actor(), entrega, request.justificacion(), request.replanificable());
          case REVISION -> procesarEntregaEnRevision(request.actor(), entrega);
          case PENDIENTE -> procesarEntregaPendiente(request.actor(), entrega);
            // CANCELADA entra solo por el comando del broker (HB-19), nunca por PATCH
          case EN_TRASLADO, CANCELADA ->
              throw new ValidationException(ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
          default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
        };

    entregasRepository.save(entrega);
    if (solicitudesTransicionRepository != null) {
      solicitudesTransicionRepository.registrar(solicitud);
    }
    publicarEventos(entrega);
    entrega.clearDomainEvents();
    return entregaMapper.toResponseDTO(entrega);
  }

  /**
   * Cancela la entrega de una donación que todavía no salió (HB-19, comando {@code
   * entrega.cancelada.<instancia>.v1}). Idempotente: si ya estaba cancelada vuelve a publicar la
   * confirmación, por si la primera se perdió; si ya salió, no hace nada (lo dice {@code
   * ruta.iniciada}).
   */
  @Override
  @Transactional
  public void cancelarPorDonacion(UUID idDonacion, String motivo) {
    if (idDonacion == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    Entrega activa = entregasRepository.findByIdDonacion(idDonacion).orElse(null);
    if (activa == null) {
      reconfirmarCancelacion(idDonacion, motivo);
      return;
    }
    if (activa.getEstadoActual() != EstadoEntrega.PENDIENTE) {
      log.info(
          "Cancelación ignorada: la entrega {} de la donación {} ya salió (estado {})",
          activa.getId(),
          idDonacion,
          activa.getEstadoActual());
      return;
    }

    UUID idRuta = activa.getIdRuta();
    activa.cancelar(ACTOR_BROKER, motivo);
    if (idRuta != null) {
      Ruta ruta =
          rutasRepository
              .findById(idRuta)
              .orElseThrow(() -> new RecursoNoEncontradoException(idRuta));
      ruta.quitarEntrega(activa.getId());
      rutasRepository.save(ruta);
    }
    entregasRepository.save(activa);
    publicarEventos(activa);
    activa.clearDomainEvents();
    log.info("Entrega {} de la donación {} cancelada ({})", activa.getId(), idDonacion, motivo);
  }

  private void reconfirmarCancelacion(UUID idDonacion, String motivo) {
    entregasRepository.findCanceladasByIdDonacion(idDonacion).stream()
        .max(Comparator.comparing(EntregasService::fechaUltimoCambio))
        .ifPresentOrElse(
            cancelada -> {
              log.info(
                  "Cancelación repetida: se vuelve a confirmar la entrega {}", cancelada.getId());
              comunicadorEventos.comunicarEntregaCancelada(
                  new EntregaCancelada(cancelada.getId(), idDonacion, motivo));
            },
            () ->
                log.warn(
                    "Cancelación ignorada: la donación {} no tiene entrega en esta instancia",
                    idDonacion));
  }

  private static LocalDateTime fechaUltimoCambio(Entrega entrega) {
    List<CambioEstadoEntrega> historial = entrega.getHistorialEstado();
    return historial.isEmpty() ? LocalDateTime.MIN : historial.getLast().timeStamp();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id) {
    return buscarEntrega(id).getHistorialEstado().stream()
        .map(entregaMapper::toCambioEstadoResponseDTO)
        .toList();
  }

  private SeguimientoEntregaResponseDTO construirSeguimiento(Entrega entrega) {
    UUID idRuta = entrega.getIdRuta();
    Ruta ruta = idRuta != null ? rutasRepository.findById(idRuta).orElse(null) : null;
    if (ruta == null) {
      return new SeguimientoEntregaResponseDTO(
          entrega.getId(),
          entrega.getIdDonacion(),
          entrega.getEstadoActual(),
          false,
          null,
          null,
          null,
          null,
          null,
          0,
          0,
          0,
          aInstantUtc(entrega.getHoraSalida()),
          aInstantUtc(entrega.getHoraArribo()),
          calcularUltimaActualizacion(entrega, null, List.of()),
          List.of());
    }

    Camion camion =
        ruta.getCamionId() != null
            ? camionRepository.findById(ruta.getCamionId()).orElse(null)
            : null;
    String patenteCamion = camion != null ? camion.getPatente() : null;

    List<UUID> idsParadas = ruta.getEntregaIds();
    List<ParadaSeguimientoDTO> paradas = new ArrayList<>(idsParadas.size());
    List<Entrega> entregasDeRuta = new ArrayList<>(idsParadas.size());
    Integer ordenVisitaConsultada = null;
    int paradasEntregadas = 0;
    int paradasProcesadas = 0;

    for (int i = 0; i < idsParadas.size(); i++) {
      UUID idParada = idsParadas.get(i);
      int ordenVisita = i + 1;
      boolean esConsultada = entrega.getId().equals(idParada);
      Entrega paradaEntrega =
          esConsultada ? entrega : entregasRepository.findById(idParada).orElse(null);

      if (esConsultada) {
        ordenVisitaConsultada = ordenVisita;
      }
      if (paradaEntrega != null) {
        entregasDeRuta.add(paradaEntrega);
        EstadoEntrega estadoParada = paradaEntrega.getEstadoActual();
        if (estadoParada == EstadoEntrega.ENTREGADA) {
          paradasEntregadas++;
          paradasProcesadas++;
        } else if (estadoParada == EstadoEntrega.NO_RECIBIDA) {
          paradasProcesadas++;
        }
        paradas.add(
            new ParadaSeguimientoDTO(
                ordenVisita,
                estadoParada,
                esConsultada,
                aInstantUtc(paradaEntrega.getHoraArribo())));
      } else {
        paradas.add(new ParadaSeguimientoDTO(ordenVisita, null, esConsultada, null));
      }
    }

    Instant ultimaActualizacion = calcularUltimaActualizacion(entrega, ruta, entregasDeRuta);

    return new SeguimientoEntregaResponseDTO(
        entrega.getId(),
        entrega.getIdDonacion(),
        entrega.getEstadoActual(),
        true,
        ruta.getId(),
        ruta.getFecha(),
        ruta.getEstado(),
        patenteCamion,
        ordenVisitaConsultada,
        idsParadas.size(),
        paradasEntregadas,
        paradasProcesadas,
        aInstantUtc(entrega.getHoraSalida()),
        aInstantUtc(entrega.getHoraArribo()),
        ultimaActualizacion,
        List.copyOf(paradas),
        posicionCamion(ruta, camion));
  }

  private static PosicionCamionDTO posicionCamion(Ruta ruta, @Nullable Camion camion) {
    if (ruta.getEstado() != EstadoRuta.EN_TRASLADO || camion == null) {
      return null;
    }
    PosicionCamion posicion = camion.getPosicion();
    // Una posición anterior al inicio de esta ruta es de la ruta previa del camión.
    boolean esDeEstaRuta =
        posicion != null
            && ruta.getHoraInicioReal() != null
            && !posicion.registradaEn().isBefore(aInstantUtc(ruta.getHoraInicioReal()));
    return esDeEstaRuta
        ? new PosicionCamionDTO(posicion.latitud(), posicion.longitud(), posicion.registradaEn())
        : null;
  }

  private static Instant calcularUltimaActualizacion(
      Entrega entrega, @Nullable Ruta ruta, List<Entrega> entregasDeRuta) {
    List<LocalDateTime> marcas = new ArrayList<>();
    agregarMarcasEntrega(entrega, marcas);
    if (ruta != null) {
      if (ruta.getHoraInicioReal() != null) {
        marcas.add(ruta.getHoraInicioReal());
      }
      if (ruta.getHoraFinReal() != null) {
        marcas.add(ruta.getHoraFinReal());
      }
      for (CambioEstadoRuta cambio : ruta.getHistorialEstado()) {
        if (cambio != null && cambio.timestamp() != null) {
          marcas.add(cambio.timestamp());
        }
      }
    }
    for (Entrega parada : entregasDeRuta) {
      agregarMarcasEntrega(parada, marcas);
    }
    return marcas.stream()
        .max(Comparator.naturalOrder())
        .map(EntregasService::aInstantUtc)
        .orElse(null);
  }

  private static Instant aInstantUtc(LocalDateTime ldt) {
    return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
  }

  private static void agregarMarcasEntrega(Entrega entrega, List<LocalDateTime> marcas) {
    if (entrega == null) {
      return;
    }
    if (entrega.getHoraSalida() != null) {
      marcas.add(entrega.getHoraSalida());
    }
    if (entrega.getHoraArribo() != null) {
      marcas.add(entrega.getHoraArribo());
    }
    for (CambioEstadoEntrega cambio : entrega.getHistorialEstado()) {
      if (cambio != null && cambio.timeStamp() != null) {
        marcas.add(cambio.timeStamp());
      }
    }
  }

  private static SolicitudTransicionEntrega procesarEntregaEntregada(
      Entrega entrega, CambioEstadoEntregaRequestDTO request) {
    SolicitudTransicionEntrega solicitud =
        new ConfirmacionRecepcion(
            entrega,
            request.actor(),
            request.obtenerFotosRecepcion(),
            request.numeroRemito(),
            request.reporte() != null ? request.reporte().aDominio() : null);

    GestorDeEntregas.cambiarEstado(solicitud);
    return solicitud;
  }

  private static SolicitudTransicionEntrega procesarEntregaNoRecibida(
      String actor, Entrega entrega, String justificacion, Boolean replanificable) {

    boolean esReplanificable = replanificable == null || replanificable;
    NoRecepcion solicitud = new NoRecepcion(entrega, actor, justificacion, esReplanificable);

    GestorDeEntregas.cambiarEstado(solicitud);
    return solicitud;
  }

  private static SolicitudTransicionEntrega procesarEntregaPendiente(
      String actor, Entrega entrega) {
    RegresoDeposito solicitud = new RegresoDeposito(entrega, actor);
    GestorDeEntregas.cambiarEstado(solicitud);
    return solicitud;
  }

  private static SolicitudTransicionEntrega procesarEntregaEnRevision(
      String actor, Entrega entrega) {
    RevisionEntrega solicitud = new RevisionEntrega(entrega, actor);
    GestorDeEntregas.cambiarEstado(solicitud);
    return solicitud;
  }

  private void publicarEventos(Entrega entrega) {
    for (EventoEntrega evento : entrega.getDomainEvents()) {
      switch (evento) {
        case EntregaConfirmada confirmada ->
            comunicadorEventos.comunicarEntregaExitosa(confirmada, buscarCamionDeEntrega(entrega));
        case EntregaFallida fallida -> comunicadorEventos.comunicarEntregaFallida(fallida);
        case EntregaCancelada cancelada -> comunicadorEventos.comunicarEntregaCancelada(cancelada);
      }
    }
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Camion buscarCamionDeEntrega(Entrega entrega) {
    Ruta ruta =
        rutasRepository
            .findById(entrega.getIdRuta())
            .orElseThrow(() -> new RecursoNoEncontradoException(entrega.getIdRuta()));
    return camionRepository
        .findById(ruta.getCamionId())
        .orElseThrow(() -> new RecursoNoEncontradoException(ruta.getCamionId()));
  }
}
