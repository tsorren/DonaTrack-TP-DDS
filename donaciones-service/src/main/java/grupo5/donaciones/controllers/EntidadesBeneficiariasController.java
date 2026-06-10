package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.entidades.ActualizarEntidadBeneficiariaDTO;
import grupo5.donaciones.dto.entidades.CrearEntidadBeneficiariaDTO;
import grupo5.donaciones.dto.entidades.EntidadBeneficiariaDTO;
import grupo5.donaciones.routes.DonacionesRoutes;
import grupo5.donaciones.services.EntidadesBeneficiariasService;
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
@RequestMapping(DonacionesRoutes.ENTIDADES_BENEFICIARIAS)
public class EntidadesBeneficiariasController {

  private final EntidadesBeneficiariasService entidadesBeneficiariasService;

  public EntidadesBeneficiariasController(
      EntidadesBeneficiariasService entidadesBeneficiariasService) {
    this.entidadesBeneficiariasService = entidadesBeneficiariasService;
  }

  @GetMapping
  public List<EntidadBeneficiariaDTO> buscarTodas() {
    return entidadesBeneficiariasService.buscarTodas();
  }

  @GetMapping("/{id}")
  public EntidadBeneficiariaDTO buscarPorId(@PathVariable Long id) {
    return entidadesBeneficiariasService.buscarPorId(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EntidadBeneficiariaDTO crear(@RequestBody CrearEntidadBeneficiariaDTO dto) {
    return entidadesBeneficiariasService.crear(dto);
  }

  @PutMapping("/{id}")
  public EntidadBeneficiariaDTO actualizar(
      @PathVariable Long id, @RequestBody ActualizarEntidadBeneficiariaDTO dto) {
    return entidadesBeneficiariasService.actualizar(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    entidadesBeneficiariasService.eliminar(id);
  }
}
