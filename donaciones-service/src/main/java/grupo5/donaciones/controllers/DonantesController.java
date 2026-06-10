package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donantes.ActualizarDonanteRequest;
import grupo5.donaciones.dto.donantes.CrearDonanteRequest;
import grupo5.donaciones.dto.donantes.DonanteDTO;
import grupo5.donaciones.routes.DonacionesRoutes;
import grupo5.donaciones.services.DonantesService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DonacionesRoutes.DONANTES)
public class DonantesController {

  private final DonantesService donantesService;

  public DonantesController(DonantesService donantesService) {
    this.donantesService = donantesService;
  }

  @GetMapping
  public List<DonanteDTO> buscarTodos() {
    return donantesService.buscarTodos();
  }

  @GetMapping("/{id}")
  public DonanteDTO buscarPorId(@PathVariable Long id) {
    return donantesService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DonanteDTO crear(@RequestBody CrearDonanteRequest request) {
    return donantesService.crear(request);
  }

  @PutMapping("/{id}")
  public DonanteDTO actualizar(
      @PathVariable Long id, @RequestBody ActualizarDonanteRequest request) {
    return donantesService.actualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    donantesService.eliminar(id);
  }
}
