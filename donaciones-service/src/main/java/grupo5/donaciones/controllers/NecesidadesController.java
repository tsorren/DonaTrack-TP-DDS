package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.necesidades.ActualizarNecesidadDTO;
import grupo5.donaciones.dto.necesidades.CrearNecesidadDTO;
import grupo5.donaciones.dto.necesidades.NecesidadDTO;
import grupo5.donaciones.routes.DonacionesRoutes;
import grupo5.donaciones.services.NecesidadesService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(DonacionesRoutes.NECESIDADES)
public class NecesidadesController {

  private final NecesidadesService necesidadesService;

  public NecesidadesController(NecesidadesService necesidadesService) {
    this.necesidadesService = necesidadesService;
  }

  @GetMapping
  public List<NecesidadDTO> buscarTodas() {
    return necesidadesService.buscarTodas();
  }

  @GetMapping("/{id}")
  public NecesidadDTO buscarPorId(@PathVariable Long id) {
    return necesidadesService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NecesidadDTO crear(@RequestBody CrearNecesidadDTO dto) {
    return necesidadesService.crear(dto);
  }

  @PutMapping("/{id}")
  public NecesidadDTO actualizar(@PathVariable Long id, @RequestBody ActualizarNecesidadDTO dto) {
    return necesidadesService.actualizar(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    necesidadesService.eliminar(id);
  }
}
