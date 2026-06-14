package grupo5.donaciones.controllers;

import grupo5.donaciones.services.IDonacionesIndependientesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/donaciones-independientes")
public class DonacionesIndependientesController {

  private final IDonacionesIndependientesService service;

  public DonacionesIndependientesController(IDonacionesIndependientesService service) {
    this.service = service;
  }

  @PostMapping("/{id}/asignar")
  public ResponseEntity<Void> asignar(
      @PathVariable Long id,
      @RequestParam Long necesidadId,
      @RequestHeader("X-Actor") String actor) {
    service.asignar(id, necesidadId, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/vencer")
  public ResponseEntity<Void> vencer(
      @PathVariable Long id,
      @RequestHeader(value = "X-Actor", defaultValue = "SISTEMA") String actor) {
    service.vencer(id, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/planificar-ruta")
  public ResponseEntity<Void> planificarRuta(
      @PathVariable Long id,
      @RequestHeader("X-Actor") String actor) {
    service.planificarRuta(id, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/iniciar-recorrido")
  public ResponseEntity<Void> iniciarRecorrido(
      @PathVariable Long id,
      @RequestHeader("X-Actor") String actor) {
    service.iniciarRecorrido(id, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/confirmar-entrega")
  public ResponseEntity<Void> confirmarEntrega(
      @PathVariable Long id,
      @RequestHeader("X-Actor") String actor) {
    service.confirmarEntrega(id, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/registrar-falla")
  public ResponseEntity<Void> registrarFalla(
      @PathVariable Long id,
      @RequestParam String justificacion,
      @RequestHeader("X-Actor") String actor) {
    service.registrarFalla(id, justificacion, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/retornar")
  public ResponseEntity<Void> retornar(
      @PathVariable Long id,
      @RequestHeader("X-Actor") String actor) {
    service.retornar(id, actor);
    return ResponseEntity.noContent().build();
  }
}
