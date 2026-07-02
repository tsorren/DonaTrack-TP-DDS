package grupo5.logistica.controllers.impl;

import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.services.ICamionesService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/camiones")
public class CamionesController {
  private final ICamionesService camionesService;

  public CamionesController(ICamionesService camionesService) {
    this.camionesService = camionesService;
  }

  @PostMapping
  public ResponseEntity<CamionResponseDTO> crear(@RequestBody CamionRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(camionesService.crear(dto));
  }

  @GetMapping
  public ResponseEntity<List<CamionResponseDTO>> listar() {
    return ResponseEntity.ok(camionesService.listar());
  }

  @GetMapping("/{id}")
  public ResponseEntity<CamionResponseDTO> obtenerPorId(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(camionesService.obtenerPorId(id));
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<CamionResponseDTO> cambiarEstado(
      @PathVariable("id") UUID id, @RequestBody CambioEstadoCamionRequestDTO dto) {
    return ResponseEntity.ok(camionesService.cambiarEstado(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable("id") UUID id) {
    camionesService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
