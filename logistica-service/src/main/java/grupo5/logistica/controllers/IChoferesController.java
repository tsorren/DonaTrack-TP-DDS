package grupo5.logistica.controllers;

import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IChoferesController {

  ResponseEntity<ChoferResponseDTO> crear(@Valid ChoferRequestDTO request);

  ResponseEntity<List<ChoferResponseDTO>> listar();

  ResponseEntity<ChoferResponseDTO> consultarPorId(UUID id);

  ResponseEntity<ChoferResponseDTO> cambiarEstado(
      UUID id, @Valid CambioEstadoChoferRequestDTO request);

  ResponseEntity<Void> darDeBaja(UUID id);
}
