package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.INecesidadesController;
import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.services.INecesidadesService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${donatrack.routes.donaciones.necesidades-base}")
public class NecesidadesController implements INecesidadesController {

  private final INecesidadesService necesidadesService;

  // Inyección de dependencias por constructor
  public NecesidadesController(INecesidadesService necesidadesService) {
    this.necesidadesService = necesidadesService;
  }

  // ENDPOINTS

  // POST: crear necesidad
  @Override
  @PostMapping
  public ResponseEntity<NecesidadDTO> crearNecesidad(@RequestBody NecesidadDTO dto) {
    NecesidadDTO creada = necesidadesService.guardar(dto);
    // Retorna HTTP 201 Created junto con el objeto persistido en memoria
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  // GET: mostrar todas las necesidades
  @Override
  @GetMapping
  public ResponseEntity<List<NecesidadDTO>> listarNecesidades(
      @RequestParam(required = false) UUID entidadId, @RequestParam(required = false) String tipo) {
    List<NecesidadDTO> lista = necesidadesService.listarConFiltros(entidadId, tipo);
    return ResponseEntity.ok(lista);
  }
}
