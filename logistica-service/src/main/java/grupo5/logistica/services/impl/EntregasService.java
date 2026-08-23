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
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.ConfirmacionRecepcion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.entregas.GestorDeEntregas;
import grupo5.logistica.models.entities.entregas.NoRecepcion;
import grupo5.logistica.models.entities.entregas.RegresoDeposito;
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
  public EntregaResponseDTO confirmarRecepcion(UUID id, ConfirmarRecepcionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    ConfirmacionRecepcion solicitud = entregaMapper.toSolicitud(entrega, dto);
    GestorDeEntregas.cambiarEstado(solicitud);
    entregasRepository.save(entrega);

    Camion camion = buscarCamionDeEntrega(entrega);
    comunicadorEventos.comunicarEntregaExitosa(entrega, camion);

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
    NoRecepcion solicitud = entregaMapper.toSolicitud(entrega, dto);
    GestorDeEntregas.cambiarEstado(solicitud);
    entregasRepository.save(entrega);

    comunicadorEventos.comunicarEntregaFallida(solicitud);

    return entregaMapper.toResponseDTO(entrega);
  }

  @Override
  public EntregaResponseDTO regresarAlDeposito(UUID id, RegresarAlDepositoRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Entrega entrega = buscarEntrega(id);
    RegresoDeposito solicitud = entregaMapper.toSolicitud(entrega, dto);
    GestorDeEntregas.cambiarEstado(solicitud);
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
    Ruta ruta =
        rutasRepository
            .findById(entrega.getIdRuta())
            .orElseThrow(() -> new RecursoNoEncontradoException(entrega.getIdRuta()));
    return camionRepository
        .findById(ruta.getCamionId())
        .orElseThrow(() -> new RecursoNoEncontradoException(ruta.getCamionId()));
  }
}
