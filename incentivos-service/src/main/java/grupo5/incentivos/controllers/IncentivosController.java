package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.*;
import grupo5.incentivos.services.IIncentivosService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incentivos")
public class IncentivosController implements IIncentivosController {

  private final IIncentivosService incentivosService;

  public IncentivosController(IIncentivosService incentivosService) {
    this.incentivosService = incentivosService;
  }

  @PostMapping("/donaciones")
  public ResponseEntity<Void> procesarDonacion(@RequestBody NuevaDonacionRequest request) {

    incentivosService.procesarDonacion(request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/donaciones/exitosa")
  public ResponseEntity<Void> procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request) {
    incentivosService.procesarDonacionExitosa(request);
    return ResponseEntity.ok().build();
  }

  @Override
  @PostMapping("/donantes/{donanteId}")
  public ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable UUID donanteId, @RequestBody RegistrarDonanteRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(incentivosService.registrarDonante(request));
  }

  @DeleteMapping("/donantes/{donanteId}")
  public ResponseEntity<Void> darDeBaja(@PathVariable UUID donanteId) {
    incentivosService.darDeBaja(donanteId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/donantes/{donanteId}")
  public ResponseEntity<Void> modificarDonante(
      @PathVariable UUID donanteId, @RequestBody ModificarDonanteRequest request) {
    incentivosService.modificarDonante(donanteId, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/donantes/{donanteId}/metricas")
  public ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerMetricas(donanteId));
  }

  @GetMapping("/donantes/{donanteId}/misiones")
  public ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerMisiones(donanteId));
  }

  @GetMapping("/donantes/{donanteId}/insignias")
  public ResponseEntity<List<InsigniaDTO>> obtenerInsignias(@PathVariable UUID donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerInsignias(donanteId));
  }

  @PutMapping("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  public ResponseEntity<Void> configurarVisibilidad(
      @PathVariable UUID donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible) {
    incentivosService.configurarVisibilidadInsignia(donanteId, nombreInsignia, visible);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/admin/resumen")
  public ResponseEntity<ResumenSistemaDTO> obtenerResumenSistema() {
    return ResponseEntity.ok(incentivosService.obtenerResumenSistema());
  }
}
