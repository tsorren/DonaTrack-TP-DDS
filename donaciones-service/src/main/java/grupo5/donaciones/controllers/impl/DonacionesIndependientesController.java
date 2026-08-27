package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonacionesIndependientesController;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.services.IDonacionesIndependientesService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donaciones-independientes")
public class DonacionesIndependientesController implements IDonacionesIndependientesController {

  private final IDonacionesIndependientesService service;

  public DonacionesIndependientesController(IDonacionesIndependientesService service) {
    this.service = service;
  }

  @Override
  @PatchMapping("/{id}/estado")
  public ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      @PathVariable UUID id,
      @Valid @RequestBody CambioEstadoDonacionIndependienteRequestDTO request,
      @RequestHeader("X-Actor") String actor) {
    return ResponseEntity.ok(service.cambiarEstado(id, request, actor));
  }
}
