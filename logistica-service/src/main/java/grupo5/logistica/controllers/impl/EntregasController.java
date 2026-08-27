package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.IEntregasController;
import grupo5.logistica.dto.entregas.AdjuntarFotoRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaRequestDTO;
import grupo5.logistica.dto.entregas.CambioEstadoEntregaResponseDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.EntregaResponseDTO;
import grupo5.logistica.services.IEntregasService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/entregas")
public class EntregasController implements IEntregasController {
  private final IEntregasService entregasService;

  public EntregasController(IEntregasService entregasService) {
    this.entregasService = entregasService;
  }

  @Override
  @PostMapping
  public ResponseEntity<EntregaResponseDTO> crear(@Valid @RequestBody CrearEntregaRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(entregasService.crear(dto));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<EntregaResponseDTO>> listar() {
    return ResponseEntity.ok(entregasService.listar());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<EntregaResponseDTO> obtenerPorId(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(entregasService.obtenerPorId(id));
  }

  @Override
  @PatchMapping("/{id}/estado")
  public ResponseEntity<EntregaResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @Valid @RequestBody CambioEstadoEntregaRequestDTO request) {
    return ResponseEntity.ok(entregasService.cambiarEstado(id, request));
  }

  @Override
  @PatchMapping("/{id}/fotos")
  public ResponseEntity<EntregaResponseDTO> adjuntarFotoRecepcion(
      @PathVariable("id") UUID id, @Valid @RequestBody AdjuntarFotoRecepcionRequestDTO dto) {
    return ResponseEntity.ok(entregasService.adjuntarFotoRecepcion(id, dto));
  }

  @Override
  @GetMapping("/{id}/historial")
  public ResponseEntity<List<CambioEstadoEntregaResponseDTO>> obtenerHistorial(
      @PathVariable("id") UUID id) {
    return ResponseEntity.ok(entregasService.obtenerHistorial(id));
  }
}
