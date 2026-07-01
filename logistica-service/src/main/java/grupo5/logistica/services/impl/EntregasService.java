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
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import grupo5.logistica.models.entities.eventos.TipoEventoLogistico;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IEventosLogisticosRepository;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.mappers.EntregaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EntregasService implements IEntregasService {
  private final IEntregasRepository entregasRepository;
  private final IEventosLogisticosRepository eventosRepository;
  private final EntregaMapper entregaMapper;

  public EntregasService(
      IEntregasRepository entregasRepository,
      IEventosLogisticosRepository eventosRepository,
      EntregaMapper entregaMapper) {
    this.entregasRepository = entregasRepository;
    this.eventosRepository = eventosRepository;
    this.entregaMapper = entregaMapper;
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
    registrarEvento(entrega, TipoEventoLogistico.ENTREGA_EXITOSA);
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
    registrarEvento(entrega, TipoEventoLogistico.ENTREGA_FALLIDA);
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

  private void registrarEvento(Entrega entrega, TipoEventoLogistico tipo) {
    eventosRepository.save(
        new EventoLogistico(
            tipo,
            entrega.getIdRuta(),
            entrega.getId(),
            entrega.getIdDonacion(),
            entrega.getIdBeneficiaria()));
  }
}
