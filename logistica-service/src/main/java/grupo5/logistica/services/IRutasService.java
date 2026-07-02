package grupo5.logistica.services;

import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.IniciarRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import java.util.List;
import java.util.UUID;

public interface IRutasService {
  List<RutaResponseDTO> listar();

  RutaResponseDTO obtenerPorId(UUID id);

  RutaConEntregasResponseDTO obtenerConEntregas(UUID id);

  RutaResponseDTO agregarEntrega(UUID id, AgregarEntregaRutaRequestDTO dto);

  RutaResponseDTO iniciar(UUID id, IniciarRutaRequestDTO dto);

  RutaResponseDTO completar(UUID id);

  List<RutaResponseDTO> listarPorCamion(UUID camionId);
}
