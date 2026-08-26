package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.*;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.*;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.mappers.EntregaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EntregasService implements IEntregasService {
  private final IEntregasRepository entregasRepository;
  private final IRutasRepository rutasRepository;
  private final ICamionRepository camionRepository;
  private final EntregaMapper entregaMapper;
  private final ComunicadorEventosLogistica comunicadorEventos;

  public EntregasService(
      IEntregasRepository entregasRepository,
      IRutasRepository rutasRepository,
      ICamionRepository camionRepository,
      EntregaMapper entregaMapper,
      ComunicadorEventosLogistica comunicadorEventos) {
    this.entregasRepository = entregasRepository;
    this.rutasRepository = rutasRepository;
    this.camionRepository = camionRepository;
    this.entregaMapper = entregaMapper;
    this.comunicadorEventos = comunicadorEventos;
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
  public EntregaResponseDTO adjuntarFotoRecepcion(UUID id, AdjuntarFotoRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    entrega.adjuntarFotoRecepcion(dto.fotoRecepcionUrl());
    return entregaMapper.toResponseDTO(entregasRepository.save(entrega));
  }

  @Override
  public EntregaResponseDTO cambiarEstado(UUID id, CambioEstadoEntregaRequestDTO request) {
    if (request == null || request.estado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);

    switch (request.estado()) {
      case ENTREGADA -> procesarEntregaEntregada(request.actor(), entrega);
      case NO_RECIBIDA -> procesarEntregaNoRecibida(
          request.actor(), entrega, request.justificacion(), request.replanificable());
      case PENDIENTE -> procesarEntregaPendiente(request.actor(), entrega);
      case EN_TRASLADO, REVISION -> throw new ValidationException(
          ErrorCatalog.ESTADO_ENTREGA_TRANSICION_INVALIDA);
      default -> throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    entregasRepository.save(entrega);
    return entregaMapper.toResponseDTO(entrega);
  }

  @Override
  public List<CambioEstadoEntregaResponseDTO> obtenerHistorial(UUID id) {
    return buscarEntrega(id).getHistorialEstado().stream()
        .map(entregaMapper::toCambioEstadoResponseDTO)
        .toList();
  }

  private void procesarEntregaEntregada(String actor, Entrega entrega) {
    ConfirmacionRecepcion solicitud = new ConfirmacionRecepcion(entrega, actor, null);

    GestorDeEntregas.cambiarEstado(solicitud);

    Camion camion = buscarCamionDeEntrega(entrega);
    comunicadorEventos.comunicarEntregaExitosa(entrega, camion);
  }

  private void procesarEntregaNoRecibida(
      String actor, Entrega entrega, String justificacion, Boolean replanificable) {

    boolean esReplanificable = replanificable == null || replanificable;
    NoRecepcion solicitud = new NoRecepcion(entrega, actor, justificacion, esReplanificable);

    GestorDeEntregas.cambiarEstado(solicitud);

    comunicadorEventos.comunicarEntregaFallida(solicitud);
  }

  private void procesarEntregaPendiente(String actor, Entrega entrega) {
    RegresoDeposito solicitud = new RegresoDeposito(entrega, actor);
    GestorDeEntregas.cambiarEstado(solicitud);
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
