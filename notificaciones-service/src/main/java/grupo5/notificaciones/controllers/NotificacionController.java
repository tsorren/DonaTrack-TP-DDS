package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificacionController implements INotificacionController {

  private final NotificacionService service;

  @Override
  @PostMapping({"/api/notificaciones/eventos", "/notificaciones"})
  public ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @Override
  @GetMapping("/api/notificaciones")
  public ResponseEntity<List<NotificacionDTO>> obtenerNotificaciones(
      @RequestParam(required = false) UUID personaId,
      @RequestParam(required = false) String estado) {
    EstadoNotificacion estadoEnum = null;
    if (estado != null && !estado.isBlank()) {
      try {
        estadoEnum = EstadoNotificacion.valueOf(estado.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
      }
    }
    return ResponseEntity.ok(service.obtenerNotificaciones(personaId, estadoEnum));
  }

  @Override
  @GetMapping("/api/notificaciones/{id}")
  public ResponseEntity<NotificacionDTO> obtenerPorId(@PathVariable UUID id) {
    return service
        .obtenerPorId(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  @GetMapping({"/notificaciones/persona/{personaId}", "/api/notificaciones/persona/{personaId}"})
  public ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(@PathVariable UUID personaId) {
    return ResponseEntity.ok(service.obtenerPorPersona(personaId));
  }
}
