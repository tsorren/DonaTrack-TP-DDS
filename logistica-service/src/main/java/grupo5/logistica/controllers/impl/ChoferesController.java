package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.IChoferesController;
import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.services.IChoferesService;
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
@RequestMapping("/api/choferes")
public class ChoferesController implements IChoferesController {

  private final IChoferesService choferesService;

  public ChoferesController(IChoferesService choferesService) {
    this.choferesService = choferesService;
  }

  @Override
  @PostMapping
  public ResponseEntity<ChoferResponseDTO> crear(@Valid @RequestBody ChoferRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(choferesService.crear(request));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<ChoferResponseDTO>> listar() {
    return ResponseEntity.ok(choferesService.consultarTodos());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<ChoferResponseDTO> consultarPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(choferesService.consultarPorId(id));
  }

  @Override
  @PatchMapping("/{id}/estado")
  public ResponseEntity<ChoferResponseDTO> cambiarEstado(
      @PathVariable UUID id, @Valid @RequestBody CambioEstadoChoferRequestDTO request) {
    return ResponseEntity.ok(choferesService.cambiarEstado(id, request));
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
    choferesService.darDeBaja(id);
    return ResponseEntity.noContent().build();
  }
}
