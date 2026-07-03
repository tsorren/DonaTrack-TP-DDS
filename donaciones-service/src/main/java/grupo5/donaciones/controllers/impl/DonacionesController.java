package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonacionesController;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.services.IDonacionesService;
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

  public DonacionesController(IDonacionesService service) {
    this.service = service;
  }

  @Override
  @PostMapping
  public ResponseEntity<DonacionOutputDTO> cargarDonacion(@RequestBody DonacionInputDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.cargarDonacion(dto));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<DonacionOutputDTO>> listarDonaciones() {
    return ResponseEntity.ok(service.listarDonaciones());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<DonacionOutputDTO> obtenerDonacion(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerDonacion(id));
  }
}
