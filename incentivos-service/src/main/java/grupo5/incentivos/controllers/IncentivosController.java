package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.*;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.services.IncentivosService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
public class IncentivosController {

  private final IncentivosService incentivosService;

  public IncentivosController(IncentivosService incentivosService) {
    this.incentivosService = incentivosService;
  }

  @PostMapping("/donaciones")
  public ResponseEntity<Void> procesarDonacion(@RequestBody NuevaDonacionRequest request) {
    EventoDonacion evento =
        EventoDonacion.builder()
            .categorias(request.categorias())
            .cantidadBienes(request.cantidadBienes())
            .fecha(request.fecha())
            .build();

    incentivosService.procesarDonacion(request.donanteId(), request.nombreDonante(), evento);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/donaciones/exitosa")
  public ResponseEntity<Void> procesarDonacionExitosa(@RequestBody DonacionExitosaRequest request) {
    incentivosService.procesarDonacionExitosa(request.donanteId(), request.organizacionId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/donantes/{donanteId}")
  public ResponseEntity<DonanteRegistradoDTO> registrarDonante(
      @PathVariable Long donanteId, @RequestBody String nombreDonante) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(incentivosService.registrarDonante(donanteId, nombreDonante));
  }

  @DeleteMapping("/donantes/{donanteId}")
  public ResponseEntity<Void> darDeBaja(@PathVariable Long donanteId) {
    incentivosService.darDeBaja(donanteId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/donantes/{donanteId}/metricas")
  public ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable Long donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerMetricas(donanteId));
  }

  @GetMapping("/donantes/{donanteId}/misiones")
  public ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable Long donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerMisiones(donanteId));
  }

  @GetMapping("/donantes/{donanteId}/insignias")
  public ResponseEntity<List<InsigniaDTO>> obtenerInsignias(@PathVariable Long donanteId) {
    return ResponseEntity.ok(incentivosService.obtenerInsignias(donanteId));
  }

  @PatchMapping("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  public ResponseEntity<Void> configurarVisibilidad(
      @PathVariable Long donanteId,
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
