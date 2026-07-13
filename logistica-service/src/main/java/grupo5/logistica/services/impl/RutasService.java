package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.IniciarRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.infrastructure.GeneradorDeURLSeguimiento;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.IRutasService;
import grupo5.logistica.services.mappers.RutaMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RutasService implements IRutasService {

  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final ICamionRepository camionRepository;
  private final IChoferesRepository choferesRepository;
  private final RutaMapper rutaMapper;
  private final LogisticaEventPublisher eventPublisher;
  private final GeneradorDeURLSeguimiento generadorDeUrlSeguimiento;

  public RutasService(
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      ICamionRepository camionRepository,
      IChoferesRepository choferesRepository,
      RutaMapper rutaMapper,
      LogisticaEventPublisher eventPublisher,
      GeneradorDeURLSeguimiento generadorDeUrlSeguimiento) {
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.camionRepository = camionRepository;
    this.choferesRepository = choferesRepository;
    this.rutaMapper = rutaMapper;
    this.eventPublisher = eventPublisher;
    this.generadorDeUrlSeguimiento = generadorDeUrlSeguimiento;
  }

  @Override
  public List<RutaResponseDTO> listar() {
    return rutasRepository.findAll().stream().map(rutaMapper::toResponseDTO).toList();
  }

  @Override
  public RutaResponseDTO obtenerPorId(UUID id) {
    return rutaMapper.toResponseDTO(buscarRuta(id));
  }

  @Override
  public RutaConEntregasResponseDTO obtenerConEntregas(UUID id) {
    Ruta ruta = buscarRuta(id);
    return rutaMapper.toResponseDTOConEntregas(ruta, buscarEntregasDeRuta(ruta));
  }

  @Override
  public RutaResponseDTO agregarEntrega(UUID id, AgregarEntregaRutaRequestDTO dto) {
    if (dto == null || dto.entregaId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Ruta ruta = buscarRuta(id);
    Entrega entrega = buscarEntrega(dto.entregaId());

    ruta.agregarEntrega(entrega.getId());
    entrega.asignarRuta(ruta.getId());

    rutasRepository.save(ruta);
    entregasRepository.save(entrega);

    eventPublisher.publicarRutaAsignada(
        new EventoRutaAsignada(
            ruta.getId(), entrega.getIdDonacion(), LocalDateTime.now(ZoneId.of("UTC"))));

    return rutaMapper.toResponseDTO(ruta);
  }

  @Override
  public RutaResponseDTO iniciar(UUID id, IniciarRutaRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Ruta ruta = buscarRuta(id);

    if (!Objects.equals(ruta.getChoferId(), dto.choferId())) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    Camion camion = buscarCamion(ruta.getCamionId());
    Chofer chofer = buscarChofer(ruta.getChoferId());

    camion.asignarARuta(ruta.getId());
    chofer.asignarARuta(ruta.getId());
    ruta.iniciarRuta();

    List<Entrega> entregasDeRuta = buscarEntregasDeRuta(ruta);
    entregasDeRuta.forEach(
        entrega -> {
          entrega.iniciarRuta(dto.actor());
          entregasRepository.save(entrega);
        });

    camionRepository.save(camion);
    choferesRepository.save(chofer);
    rutasRepository.save(ruta);

    publicarRutaIniciada(ruta, camion, entregasDeRuta);

    return rutaMapper.toResponseDTO(ruta);
  }

  @Override
  public RutaResponseDTO completar(UUID id) {
    Ruta ruta = buscarRuta(id);
    Camion camion = buscarCamion(ruta.getCamionId());
    Chofer chofer = buscarChofer(ruta.getChoferId());

    ruta.completarRuta();
    camion.completarRuta();
    chofer.completarRuta();

    camionRepository.save(camion);
    choferesRepository.save(chofer);

    return rutaMapper.toResponseDTO(rutasRepository.save(ruta));
  }

  @Override
  public List<RutaResponseDTO> listarPorCamion(UUID camionId) {
    return rutasRepository.findByCamionId(camionId).stream()
        .map(rutaMapper::toResponseDTO)
        .toList();
  }

  /**
   * Arma y publica el evento de dominio de ruta iniciada, incluyendo la URL de seguimiento en
   * tiempo real. Logística no invoca directamente a Donaciones ni a Notificaciones (req. de
   * implementación de Entrega 3): es Donaciones quien escucha este evento y, por cada donación
   * transportada, arma las notificaciones correspondientes (ver ADR de granularidad de eventos).
   */
  private void publicarRutaIniciada(Ruta ruta, Camion camion, List<Entrega> entregasDeRuta) {
    String urlMapa = generadorDeUrlSeguimiento.generarUrl(ruta.getId());

    List<UUID> donacionesIndependientesIds =
        entregasDeRuta.stream().map(Entrega::getIdDonacion).toList();

    EventoRutaIniciada evento =
        new EventoRutaIniciada(
            ruta.getId(),
            camion.getId(),
            camion.getPatente(),
            donacionesIndependientesIds,
            ruta.getHoraInicioReal(),
            urlMapa);

    eventPublisher.publicarRutaIniciada(evento);
  }

  private Ruta buscarRuta(UUID id) {
    return rutasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Camion buscarCamion(UUID id) {
    return camionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Chofer buscarChofer(UUID id) {
    return choferesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private List<Entrega> buscarEntregasDeRuta(Ruta ruta) {
    return ruta.getEntregaIds().stream().map(this::buscarEntrega).toList();
  }
}
