package grupo5.logistica.services;

import grupo5.logistica.dto.rutas.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRutasService {
  List<RutaResponseDTO> listar();

  RutaResponseDTO obtenerPorId(UUID id);

  RutaConEntregasResponseDTO obtenerConEntregas(UUID id);

  RutaResponseDTO agregarEntrega(UUID id, AgregarEntregaRutaRequestDTO dto);

  RutaResponseDTO cambiarEstado(UUID id, CambioEstadoRutaRequestDTO dto);

  List<RutaResponseDTO> listarPorCamion(UUID camionId);

  /**
   * Organizaciones (donantes y beneficiarias) con una entrega en la ruta, para la regla de
   * pertenencia; vacío si la ruta no existe o no lleva entregas.
   */
  Set<UUID> organizacionesDe(UUID id);
}
