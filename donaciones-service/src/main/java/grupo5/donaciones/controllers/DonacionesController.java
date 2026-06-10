package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donaciones.ActualizarDonacionDTO;
import grupo5.donaciones.dto.donaciones.CrearDonacionDTO;
import grupo5.donaciones.dto.donaciones.DonacionDTO;
import grupo5.donaciones.routes.DonacionesRoutes;
import grupo5.donaciones.services.DonacionesService;
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
@RequestMapping(DonacionesRoutes.DONACIONES)
public class DonacionesController {

  private final DonacionesService donacionesService;

  public DonacionesController(DonacionesService donacionesService) {
    this.donacionesService = donacionesService;
  }

  @GetMapping
  public List<DonacionDTO> buscarTodas() {
    return donacionesService.buscarTodas();
  }

  @GetMapping("/{id}")
  public DonacionDTO buscarPorId(@PathVariable Long id) {
    return donacionesService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DonacionDTO crear(@RequestBody CrearDonacionDTO dto) {
    return donacionesService.crear(dto);
  }

  @PutMapping("/{id}")
  public DonacionDTO actualizar(@PathVariable Long id, @RequestBody ActualizarDonacionDTO dto) {
    return donacionesService.actualizar(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    donacionesService.eliminar(id);
  }
}
