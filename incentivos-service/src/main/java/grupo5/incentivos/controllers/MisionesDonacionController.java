package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.services.IMisionesDonacionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
public class MisionesDonacionController implements IMisionesDonacionController {

  private final IMisionesDonacionService misionesDonacionService;

  public MisionesDonacionController(IMisionesDonacionService misionesDonacionService) {
    this.misionesDonacionService = misionesDonacionService;
  }

  @Override
  @PostMapping("/donaciones")
  public ResponseEntity<Void> procesarDonacion(@RequestBody NuevaDonacionRequest request) {
    misionesDonacionService.procesarDonacion(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @PostMapping("/donaciones/exitosa")
  public ResponseEntity<Void> procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request) {
    misionesDonacionService.procesarDonacionExitosa(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @GetMapping("/donantes/{donanteId}/misiones")
  public ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(misionesDonacionService.obtenerMisiones(donanteId));
  }
}
