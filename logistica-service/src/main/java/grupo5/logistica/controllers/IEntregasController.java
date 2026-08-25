package grupo5.logistica.controllers;

import grupo5.logistica.dto.entregas.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IEntregasController {
  ResponseEntity<EntregaResponseDTO> crear(@RequestBody CrearEntregaRequestDTO dto);

  ResponseEntity<List<EntregaResponseDTO>> listar();

  ResponseEntity<EntregaResponseDTO> obtenerPorId(@PathVariable("id") UUID id);

  ResponseEntity<EntregaResponseDTO> adjuntarFotoRecepcion(
      @PathVariable("id") UUID id, @RequestBody AdjuntarFotoRecepcionRequestDTO dto);

  ResponseEntity<List<CambioEstadoEntregaResponseDTO>> obtenerHistorial(
      @PathVariable("id") UUID id);

  // endpoint unificado restful
  ResponseEntity<EntregaResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @RequestBody CambioEstadoEntregaRequestDTO dto);
}
