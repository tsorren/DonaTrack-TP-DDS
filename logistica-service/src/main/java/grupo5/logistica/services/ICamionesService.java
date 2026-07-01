package grupo5.logistica.services;

import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import java.util.List;
import java.util.UUID;

public interface ICamionesService {
  CamionResponseDTO crear(CamionRequestDTO dto);

  List<CamionResponseDTO> listar();

  CamionResponseDTO obtenerPorId(UUID id);

  CamionResponseDTO cambiarEstado(UUID id, CambioEstadoCamionRequestDTO dto);

  void eliminar(UUID id);
}
