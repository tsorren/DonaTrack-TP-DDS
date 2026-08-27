package grupo5.logistica.controllers;

import grupo5.logistica.dto.rutas.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface IRutasController {
  ResponseEntity<List<RutaResponseDTO>> listar(
      @RequestParam(value = "camionId", required = false) UUID camionId);

  ResponseEntity<RutaResponseDTO> obtenerPorId(@PathVariable("id") UUID id);

  ResponseEntity<RutaConEntregasResponseDTO> obtenerConEntregas(@PathVariable("id") UUID id);

  ResponseEntity<RutaResponseDTO> agregarEntrega(
      @PathVariable("id") UUID id, @RequestBody AgregarEntregaRutaRequestDTO dto);

  ResponseEntity<RutaResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @RequestBody CambioEstadoRutaRequestDTO dto);
}
