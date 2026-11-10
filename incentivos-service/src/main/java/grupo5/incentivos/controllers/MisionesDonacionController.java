package grupo5.incentivos.controllers;

import static grupo5.incentivos.seguridad.Acceso.ADMIN;
import static grupo5.incentivos.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.incentivos.seguridad.Acceso.VER_SUS_INCENTIVOS;

import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.services.IMisionesDonacionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/incentivos")
@Validated
public class MisionesDonacionController implements IMisionesDonacionController {

  private final IMisionesDonacionService misionesDonacionService;

  public MisionesDonacionController(IMisionesDonacionService misionesDonacionService) {
    this.misionesDonacionService = misionesDonacionService;
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/donaciones")
  public ResponseEntity<Void> procesarDonacion(@Valid @RequestBody NuevaDonacionRequest request) {
    misionesDonacionService.procesarDonacion(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/donaciones/exitosa")
  public ResponseEntity<Void> procesarDonacionExitosa(
      @Valid @RequestBody DonacionExitosaRequest request) {
    misionesDonacionService.procesarDonacionExitosa(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(VER_SUS_INCENTIVOS)
  @GetMapping("/donantes/{donanteId}/misiones")
  public ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(misionesDonacionService.obtenerMisiones(donanteId));
  }
}
