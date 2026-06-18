package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {
  private final NotificacionService service;

  @PostMapping
  public ResponseEntity<Void> procesarEvento(@RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    return ResponseEntity.ok().build();
  }
}
