package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IEntidadBeneficiariaController;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entidades")
public class EntidadBeneficiariaController implements IEntidadBeneficiariaController {

  private final IEntidadBeneficiariaService service;

  public EntidadBeneficiariaController(IEntidadBeneficiariaService service) {
    this.service = service;
  }

  @Override
  @PostMapping
  public ResponseEntity<EntidadBeneficiariaOutputDTO> crearEntidad(
      @Valid @RequestBody EntidadBeneficiariaInputDTO entidad) {
    EntidadBeneficiariaOutputDTO creada = service.crearEntidad(entidad);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<EntidadBeneficiariaOutputDTO> obtenerEntidad(@PathVariable UUID id) {
    return ResponseEntity.ok(service.obtenerEntidad(id));
  }

  @Override
  @GetMapping
  public ResponseEntity<List<EntidadBeneficiariaOutputDTO>> obtenerTodas() {
    return ResponseEntity.ok(service.obtenerTodas());
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<EntidadBeneficiariaOutputDTO> actualizarEntidad(
      @PathVariable UUID id, @Valid @RequestBody EntidadBeneficiariaInputDTO entidad) {
    return ResponseEntity.ok(service.actualizarEntidad(id, entidad));
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarEntidad(@PathVariable UUID id) {
    service.eliminarEntidad(id);
    return ResponseEntity.noContent().build();
  }
}
