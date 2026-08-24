package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.IRutasController;
import grupo5.logistica.dto.rutas.AgregarEntregaRutaRequestDTO;
import grupo5.logistica.dto.rutas.IniciarRutaRequestDTO;
import grupo5.logistica.dto.rutas.RutaConEntregasResponseDTO;
import grupo5.logistica.dto.rutas.RutaResponseDTO;
import grupo5.logistica.services.IRutasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rutas")
public class RutasController implements IRutasController {
  private final IRutasService rutasService;

  public RutasController(IRutasService rutasService) {
    this.rutasService = rutasService;
  }

  @Override
  @GetMapping
  public ResponseEntity<List<RutaResponseDTO>> listar(
      @RequestParam(value = "camionId", required = false) UUID camionId) {
    if (camionId != null) {
      return ResponseEntity.ok(rutasService.listarPorCamion(camionId));
    }
    return ResponseEntity.ok(rutasService.listar());
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<RutaResponseDTO> obtenerPorId(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(rutasService.obtenerPorId(id));
  }

  @Override
  @GetMapping("/{id}/entregas")
  public ResponseEntity<RutaConEntregasResponseDTO> obtenerConEntregas(
      @PathVariable("id") UUID id) {
    return ResponseEntity.ok(rutasService.obtenerConEntregas(id));
  }

  @Override
  @PostMapping("/{id}/entregas")
  public ResponseEntity<RutaResponseDTO> agregarEntrega(
      @PathVariable("id") UUID id, @RequestBody AgregarEntregaRutaRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(rutasService.agregarEntrega(id, dto));
  }

  @Override
  @PatchMapping("/{id}/inicio")
  public ResponseEntity<RutaResponseDTO> iniciar(
      @PathVariable("id") UUID id, @RequestBody IniciarRutaRequestDTO dto) {
    return ResponseEntity.ok(rutasService.iniciar(id, dto));
  }

  @Override
  @PatchMapping("/{id}/finalizacion")
  public ResponseEntity<RutaResponseDTO> completar(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(rutasService.completar(id));
  }
}
