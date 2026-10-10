package grupo5.logistica.services;

import grupo5.logistica.dto.rutas.*;
import java.util.List;
import java.util.UUID;

public interface IRutasService {
  List<RutaResponseDTO> listar();

  RutaResponseDTO obtenerPorId(UUID id);

  RutaConEntregasResponseDTO obtenerConEntregas(UUID id);

  RutaResponseDTO agregarEntrega(UUID id, AgregarEntregaRutaRequestDTO dto);

  RutaResponseDTO cambiarEstado(UUID id, CambioEstadoRutaRequestDTO dto);

  List<RutaResponseDTO> listarPorCamion(UUID camionId);
}
