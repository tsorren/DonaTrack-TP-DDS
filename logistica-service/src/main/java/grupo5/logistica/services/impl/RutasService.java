package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.rutas.*;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.GestorDeRutas;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.ComunicadorEventosLogistica;
import grupo5.logistica.services.IRutasService;
import grupo5.logistica.services.mappers.RutaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RutasService implements IRutasService {

  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final ICamionRepository camionRepository;
  private final IChoferesRepository choferesRepository;
  private final RutaMapper rutaMapper;
  private final ComunicadorEventosLogistica comunicadorEventos;

  public RutasService(
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      ICamionRepository camionRepository,
      IChoferesRepository choferesRepository,
      RutaMapper rutaMapper,
      ComunicadorEventosLogistica comunicadorEventos) {
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.camionRepository = camionRepository;
    this.choferesRepository = choferesRepository;
    this.rutaMapper = rutaMapper;
    this.comunicadorEventos = comunicadorEventos;
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

    GestorDeRutas.agregarEntrega(ruta, entrega);

    rutasRepository.save(ruta);
    entregasRepository.save(entrega);

    comunicadorEventos.comunicarRutaAsignada(ruta, entrega);

    return rutaMapper.toResponseDTO(ruta);
  }

  @Override
  public List<RutaResponseDTO> listarPorCamion(UUID camionId) {
    return rutasRepository.findByCamionId(camionId).stream()
        .map(rutaMapper::toResponseDTO)
        .toList();
  }

  @Override
  public RutaResponseDTO cambiarEstado(UUID id, CambioEstadoRutaRequestDTO request) {
    if (request == null || request.estado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Ruta ruta = buscarRuta(id);

    UUID choferIdToUse = request.choferId() != null ? request.choferId() : ruta.getChoferId();
    Chofer chofer = buscarChofer(choferIdToUse);
    Camion camion = buscarCamion(ruta.getCamionId());

    List<Entrega> entregas = null;
    if (request.estado() == EstadoRuta.EN_TRASLADO) {
      entregas = buscarEntregasDeRuta(ruta);
    }

    GestorDeRutas.cambiarEstado(ruta, chofer, camion, entregas, request.estado(), request.actor());

    rutasRepository.save(ruta);
    choferesRepository.save(chofer);
    camionRepository.save(camion);
    if (entregas != null) {
      entregas.forEach(entregasRepository::save);
    }

    if (request.estado() == EstadoRuta.EN_TRASLADO) {
      comunicadorEventos.comunicarRutaIniciada(ruta, camion, entregas);
    }

    return rutaMapper.toResponseDTO(ruta);
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
