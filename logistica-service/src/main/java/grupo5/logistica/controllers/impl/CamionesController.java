package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.ICamionesController;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.services.ICamionesService;
import jakarta.validation.Valid;
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
public class CamionesController implements ICamionesController {

  private final ICamionesService camionesService;

  public CamionesController(ICamionesService camionesService) {
    this.camionesService = camionesService;
  }

  @Override
  @PostMapping
  public ResponseEntity<CamionResponseDTO> crear(@Valid @RequestBody CamionRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(camionesService.crear(request));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<CamionResponseDTO>> listar() {
    return ResponseEntity.ok(camionesService.consultarTodos());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<CamionResponseDTO> consultarPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(camionesService.consultarPorId(id));
  }

  @Override
  @PatchMapping("/{id}/estado")
  public ResponseEntity<CamionResponseDTO> cambiarEstado(
      @PathVariable UUID id, @Valid @RequestBody CambioEstadoCamionRequestDTO request) {
    return ResponseEntity.ok(camionesService.cambiarEstado(id, request));
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
    camionesService.darDeBaja(id);
    return ResponseEntity.noContent().build();
  }
}
