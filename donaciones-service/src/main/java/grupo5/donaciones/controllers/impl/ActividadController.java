package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;

import grupo5.donaciones.controllers.IActividadController;
import grupo5.donaciones.dto.actividad.ActividadDTO;
import grupo5.donaciones.dto.actividad.AnotacionActividadDTO;
import grupo5.donaciones.dto.actividad.FiltroActividad;
import grupo5.donaciones.services.IActividadService;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El registro de actividad de administración (R11-26). Solo ADMIN. El listado va de a una página,
 * con los filtros en la base y el total en X-Total-Count; la anotación es solo para lo que pasa
 * fuera de Donaciones, y el autor sale del token.
 */
@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/actividad")
public class ActividadController implements IActividadController {

  private final IActividadService service;

  public ActividadController(IActividadService service) {
    this.service = service;
  }

  @Override
  @GetMapping
  public ResponseEntity<List<ActividadDTO>> listar(
      FiltroActividad filtro, ParametrosListado listado) {
    return Paginas.conTotal(service.listar(filtro, listado));
  }

  @Override
  @PostMapping
  public ResponseEntity<ActividadDTO> anotar(@RequestBody AnotacionActividadDTO anotacion) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.anotar(anotacion));
  }
}
