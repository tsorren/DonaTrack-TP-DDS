package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonacionesController;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.services.IDonacionesService;
import grupo5.donaciones.services.impl.ContadoresPublicosService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donaciones")
public class DonacionesController implements IDonacionesController {

  private final IDonacionesService service;
  private final ContadoresPublicosService contadoresPublicosService;

  public DonacionesController(
      IDonacionesService service, ContadoresPublicosService contadoresPublicosService) {
    this.service = service;
    this.contadoresPublicosService = contadoresPublicosService;
  }

  @Override
  @PostMapping
  public ResponseEntity<DonacionOutputDTO> cargarDonacion(
      @Valid @RequestBody DonacionInputDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.cargarDonacion(dto));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonacionOutputDTO>> listarDonaciones() {
    return ResponseEntity.ok(service.listarDonaciones());
  }

  /** Público (RF-01, HB-02): sin token cuando el gateway aplique la allowlist. */
  @Override
  @GetMapping("/contadores")
  public ResponseEntity<ContadoresPublicosDTO> obtenerContadoresPublicos() {
    return ResponseEntity.ok(contadoresPublicosService.calcular());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<DonacionOutputDTO> obtenerDonacion(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerDonacion(id));
  }
}
