package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.INecesidadesController;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.services.INecesidadesService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/necesidades")
public class NecesidadesController implements INecesidadesController {

  private final INecesidadesService necesidadesService;

  public NecesidadesController(INecesidadesService necesidadesService) {
    this.necesidadesService = necesidadesService;
  }

  @Override
  @PostMapping
  public ResponseEntity<NecesidadDTO> crearNecesidad(@Valid @RequestBody NecesidadDTO dto) {
    NecesidadDTO creada = necesidadesService.guardar(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<NecesidadDTO>> listarNecesidades(
      @RequestParam(required = false) UUID entidadId, @RequestParam(required = false) String tipo) {
    List<NecesidadDTO> lista = necesidadesService.listarConFiltros(entidadId, tipo);
    return ResponseEntity.ok(lista);
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<NecesidadDTO> obtenerNecesidad(@PathVariable UUID id) {
    return ResponseEntity.ok(necesidadesService.obtenerPorId(id));
  }
}
