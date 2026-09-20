package grupo5.logistica.controllers;

import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface IEntregasController {
  ResponseEntity<EntregaResponseDTO> crear(@Valid @RequestBody CrearEntregaRequestDTO dto);

  ResponseEntity<List<EntregaResponseDTO>> listar();

  ResponseEntity<EntregaResponseDTO> obtenerPorId(@PathVariable("id") UUID id);

  ResponseEntity<EntregaResponseDTO> adjuntarFotoRecepcion(
      @PathVariable("id") UUID id, @Valid @RequestBody AdjuntarFotoRecepcionRequestDTO dto);

  ResponseEntity<List<CambioEstadoEntregaResponseDTO>> obtenerHistorial(
      @PathVariable("id") UUID id);

  // endpoint unificado restful
  ResponseEntity<EntregaResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @Valid @RequestBody CambioEstadoEntregaRequestDTO dto);
}
