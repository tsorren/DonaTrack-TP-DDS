package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.NotificacionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${donatrack.routes.notificaciones.notificaciones-base}")
@RequiredArgsConstructor
public class NotificacionController {
  private final NotificacionService service;

  @PostMapping
  public ResponseEntity<Void> procesarEvento(@RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    return ResponseEntity.ok().build();
  }

  @GetMapping("${donatrack.routes.notificaciones.notificaciones-persona-id}")
  public ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(@PathVariable UUID personaId) {
    return ResponseEntity.ok(service.obtenerPorPersona(personaId));
  }
}
