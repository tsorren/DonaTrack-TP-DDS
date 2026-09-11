package grupo5.logistica.controllers;

import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ICamionesController {

  ResponseEntity<CamionResponseDTO> crear(@Valid CamionRequestDTO request);

  ResponseEntity<List<CamionResponseDTO>> listar();

  ResponseEntity<CamionResponseDTO> consultarPorId(UUID id);

  ResponseEntity<CamionResponseDTO> cambiarEstado(
      UUID id, @Valid CambioEstadoCamionRequestDTO request);

  ResponseEntity<Void> darDeBaja(UUID id);
}
