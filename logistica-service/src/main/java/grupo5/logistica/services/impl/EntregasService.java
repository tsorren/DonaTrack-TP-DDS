package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.ConfirmarRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.dto.entregas.RegresarAlDepositoRequestDTO;
import grupo5.logistica.dto.entregas.ReportarNoRecepcionRequestDTO;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.infrastructure.LogisticaEventPublisher;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IRutasService;
import grupo5.logistica.services.mappers.EntregaMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EntregasService implements IEntregasService {
  private final IEntregasRepository entregasRepository;
  private final IRutasService rutasService;
  private final ICamionRepository camionRepository;
  private final EntregaMapper entregaMapper;
  private final LogisticaEventPublisher eventPublisher;

  public EntregasService(
      IEntregasRepository entregasRepository,
      IRutasService rutasService,
      ICamionRepository camionRepository,
      EntregaMapper entregaMapper,
      LogisticaEventPublisher eventPublisher) {
    this.entregasRepository = entregasRepository;
    this.rutasService = rutasService;
    this.camionRepository = camionRepository;
    this.entregaMapper = entregaMapper;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public EntregaResponseDTO crear(CrearEntregaRequestDTO dto) {
    Entrega entrega = entregaMapper.toEntity(dto);
    if (entrega == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  public List<EntregaResponseDTO> listar() {
    return entregasRepository.findAll().stream().map(entregaMapper::toResponseDTO).toList();
  }

  @Override
  public EntregaResponseDTO obtenerPorId(UUID id) {
    return entregaMapper.toResponseDTO(buscarEntrega(id));
  }

  @Override
  public EntregaResponseDTO confirmarRecepcion(UUID id, ConfirmarRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.confirmarEntrega(dto.actor());
    entregasRepository.save(entrega);

    Camion camion = buscarCamionDeEntrega(entrega);
    eventPublisher.publicarEntregaExitosa(
        new EventoEntregaExitosa(
            entrega.getId(),
            entrega.getIdDonacion(),
            camion.getId(),
            camion.getPatente(),
            LocalDateTime.now(ZoneId.of("UTC"))));

    return entregaMapper.toResponseDTO(entrega);
  }

  @Override
  public EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.adjuntarFotoRecepcion(dto.fotoRecepcionUrl());
    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  public EntregaResponseDTO reportarNoRecepcion(UUID id, ReportarNoRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.negarEntrega(dto.actor());
    entregasRepository.save(entrega);

    boolean replanificable = dto.replanificable() == null || dto.replanificable();
    eventPublisher.publicarEntregaFallida(
        new EventoEntregaFallida(
            entrega.getId(),
            entrega.getIdDonacion(),
            dto.justificacion(),
            LocalDateTime.now(ZoneId.of("UTC")),
            replanificable));

    return entregaMapper.toResponseDTO(entrega);
  }

  @Override
  public EntregaResponseDTO regresarAlDeposito(UUID id, RegresarAlDepositoRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.regresarAlDeposito(dto.actor());
    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  public List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id) {
    return buscarEntrega(id).getHistorialEstado().stream()
        .map(entregaMapper::toCambioEstadoResponseDTO)
        .toList();
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Camion buscarCamionDeEntrega(Entrega entrega) {
    RutaResponseDTO ruta = rutasService.obtenerPorId(entrega.getIdRuta());
    return camionRepository
        .findById(ruta.camionId())
        .orElseThrow(() -> new RecursoNoEncontradoException(ruta.camionId()));
  }
}
