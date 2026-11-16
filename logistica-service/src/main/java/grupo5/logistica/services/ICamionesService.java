package grupo5.logistica.services;

import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import java.util.List;
import java.util.UUID;

public interface ICamionesService {

  CamionResponseDTO crear(CamionRequestDTO request);

  List<CamionResponseDTO> consultarTodos();

  /** Todos los camiones, también los DESHABILITADO (HB-23). */
  List<CamionResponseDTO> consultarIncluyendoDeshabilitados();

  CamionResponseDTO consultarPorId(UUID id);

  CamionResponseDTO cambiarEstado(UUID id, CambioEstadoCamionRequestDTO request);

  void darDeBaja(UUID id);
}
