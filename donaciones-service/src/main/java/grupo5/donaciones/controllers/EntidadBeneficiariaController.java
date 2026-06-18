package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
      @RequestBody EntidadBeneficiariaInputDTO entidad) {

    EntidadBeneficiariaOutputDTO creada = service.crearEntidad(entidad);

    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<EntidadBeneficiariaOutputDTO> obtenerEntidad(@PathVariable UUID id) {

    return ResponseEntity.ok(service.obtenerEntidad(id));
  }
}
