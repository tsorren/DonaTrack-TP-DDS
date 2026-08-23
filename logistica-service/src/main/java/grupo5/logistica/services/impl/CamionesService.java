package grupo5.logistica.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.GestorDeCamiones;
import grupo5.logistica.models.entities.camiones.SolicitudNuevoCamion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.services.ICamionesService;
import grupo5.logistica.services.mappers.CamionMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CamionesService implements ICamionesService {

  private final ICamionRepository camionRepository;
  private final CamionMapper camionMapper;

  public CamionesService(ICamionRepository camionRepository, CamionMapper camionMapper) {
    this.camionRepository = camionRepository;
    this.camionMapper = camionMapper;
  }

  @Override
  public CamionResponseDTO crear(CamionRequestDTO request) {
    List<String> patentesExistentes =
        camionRepository.findAll().stream().map(Camion::getPatente).toList();
    SolicitudNuevoCamion solicitud = camionMapper.toSolicitud(request, patentesExistentes);
    Camion camion = GestorDeCamiones.procesarSolicitudNuevoCamion(solicitud).orElseThrow();
    camionRepository.save(camion);
    return camionMapper.toResponseDTO(camion);
  }

  @Override
  public List<CamionResponseDTO> consultarTodos() {
    return camionRepository.findActivos().stream().map(camionMapper::toResponseDTO).toList();
  }

  @Override
  public CamionResponseDTO consultarPorId(UUID id) {
    return camionMapper.toResponseDTO(buscarCamionActivo(id));
  }

  @Override
  public CamionResponseDTO cambiarEstado(UUID id, CambioEstadoCamionRequestDTO request) {
    Camion camion =
        camionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    GestorDeCamiones.cambiarEstado(camion, request.estado());

    camionRepository.save(camion);
    return camionMapper.toResponseDTO(camion);
  }

  @Override
  public void darDeBaja(UUID id) {
    Camion camion = buscarCamionActivo(id);
    GestorDeCamiones.cambiarEstado(camion, EstadoCamion.DESHABILITADO);
    camionRepository.save(camion);
  }

  private Camion buscarCamionActivo(UUID id) {
    Camion camion =
        camionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    if (camion.getEstado() == EstadoCamion.DESHABILITADO) {
      throw new RecursoNoEncontradoException(id);
    }

    return camion;
  }
}
