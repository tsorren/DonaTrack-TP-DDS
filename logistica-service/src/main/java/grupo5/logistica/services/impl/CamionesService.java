package grupo5.logistica.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.repositories.ICamionesRepository;
import grupo5.logistica.services.ICamionesService;
import grupo5.logistica.services.mappers.CamionMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CamionesService implements ICamionesService {
  private final ICamionesRepository camionesRepository;
  private final CamionMapper camionMapper;

  public CamionesService(ICamionesRepository camionesRepository, CamionMapper camionMapper) {
    this.camionesRepository = camionesRepository;
    this.camionMapper = camionMapper;
  }

  @Override
  public CamionResponseDTO crear(CamionRequestDTO dto) {
    if (dto == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (camionesRepository.findByPatente(dto.patente()).isPresent()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }

    Camion camion = camionMapper.toEntity(dto);
    return camionMapper.toResponseDTO(camionesRepository.save(camion));
  }

  @Override
  public List<CamionResponseDTO> listar() {
    return camionesRepository.findAll().stream().map(camionMapper::toResponseDTO).toList();
  }

  @Override
  public CamionResponseDTO obtenerPorId(UUID id) {
    return camionMapper.toResponseDTO(buscarCamion(id));
  }

  @Override
  public CamionResponseDTO cambiarEstado(UUID id, CambioEstadoCamionRequestDTO dto) {
    if (dto == null || dto.estado() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    Camion camion = buscarCamion(id);
    if (dto.estado() == EstadoCamion.DISPONIBLE) {
      camion.habilitar();
    } else if (dto.estado() == EstadoCamion.DESHABILITADO) {
      camion.deshabilitar();
    } else {
      throw new ValidationException(ErrorCatalog.ESTADO_CAMION_TRANSICION_INVALIDA);
    }

    return camionMapper.toResponseDTO(camionesRepository.save(camion));
  }

  @Override
  public void eliminar(UUID id) {
    Camion camion = buscarCamion(id);
    camion.deshabilitar();
    camionesRepository.save(camion);
  }

  private Camion buscarCamion(UUID id) {
    return camionesRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }
}
