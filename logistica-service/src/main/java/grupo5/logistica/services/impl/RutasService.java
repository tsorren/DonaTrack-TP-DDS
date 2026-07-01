package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.IniciarRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import grupo5.logistica.models.entities.eventos.TipoEventoLogistico;
import grupo5.logistica.models.entities.rutas.Ruta;
import grupo5.logistica.models.repositories.ICamionesRepository;
import grupo5.logistica.models.repositories.IEntregasRepository;
import grupo5.logistica.models.repositories.IEventosLogisticosRepository;
import grupo5.logistica.models.repositories.IRutasRepository;
import grupo5.logistica.services.IRutasService;
import grupo5.logistica.services.mappers.RutaMapper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RutasService implements IRutasService {
  private final IRutasRepository rutasRepository;
  private final IEntregasRepository entregasRepository;
  private final ICamionesRepository camionesRepository;
  private final IEventosLogisticosRepository eventosRepository;
  private final RutaMapper rutaMapper;

  public RutasService(
      IRutasRepository rutasRepository,
      IEntregasRepository entregasRepository,
      ICamionesRepository camionesRepository,
      IEventosLogisticosRepository eventosRepository,
      RutaMapper rutaMapper) {
    this.rutasRepository = rutasRepository;
    this.entregasRepository = entregasRepository;
    this.camionesRepository = camionesRepository;
    this.eventosRepository = eventosRepository;
    this.rutaMapper = rutaMapper;
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
    camion.asignarARuta(ruta.getId());
    ruta.iniciarRuta();

    buscarEntregasDeRuta(ruta)
        .forEach(
            entrega -> {
              entrega.iniciarRuta(dto.actor());
              entregasRepository.save(entrega);
            });

    camionesRepository.save(camion);
    rutasRepository.save(ruta);
    eventosRepository.save(
        new EventoLogistico(TipoEventoLogistico.RUTA_INICIADA, ruta.getId(), null, null, null));
    return rutaMapper.toResponseDTO(ruta);
  }

  @Override
  public RutaResponseDTO completar(UUID id) {
    Ruta ruta = buscarRuta(id);
    Camion camion = buscarCamion(ruta.getCamionId());
    ruta.completarRuta();
    camion.completarRuta();
    camionesRepository.save(camion);
    return rutaMapper.toResponseDTO(rutasRepository.save(ruta));
  }

  @Override
  public List<RutaResponseDTO> listarPorCamion(UUID camionId) {
    return rutasRepository.findByCamionId(camionId).stream()
        .map(rutaMapper::toResponseDTO)
        .toList();
  }

  private Ruta buscarRuta(UUID id) {
    return rutasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Entrega buscarEntrega(UUID id) {
    return entregasRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private Camion buscarCamion(UUID id) {
    return camionesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }

  private List<Entrega> buscarEntregasDeRuta(Ruta ruta) {
    return ruta.getEntregaIds().stream().map(this::buscarEntrega).toList();
  }
}
