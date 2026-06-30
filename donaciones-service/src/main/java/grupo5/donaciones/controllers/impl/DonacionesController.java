package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IDonacionesController;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.services.IDonacionesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${donatrack.routes.donaciones.donaciones-base}")
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
}
