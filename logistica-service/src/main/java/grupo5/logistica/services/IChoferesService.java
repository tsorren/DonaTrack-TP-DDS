package grupo5.logistica.services;

import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import java.util.List;
import java.util.UUID;

public interface IChoferesService {

  ChoferResponseDTO crear(ChoferRequestDTO request);

  List<ChoferResponseDTO> consultarTodos();

  ChoferResponseDTO consultarPorId(UUID id);

  ChoferResponseDTO cambiarEstado(UUID id, CambioEstadoChoferRequestDTO request);

  void darDeBaja(UUID id);
}
