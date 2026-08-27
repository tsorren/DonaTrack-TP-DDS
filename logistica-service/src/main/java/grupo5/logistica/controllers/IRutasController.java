package grupo5.logistica.controllers;

import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.CambioEstadoRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
