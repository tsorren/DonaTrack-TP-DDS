package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {
  private final NotificacionService service;

  @PostMapping
  public ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    // 202 Accepted (Oleada 9, RF-09): el endpoint no crea "un" recurso identificable de punta a
    // punta desde la óptica del llamador — recibe un evento de dominio y, según el tipo, puede
    // generar 0..N Notificacion (ej. EntregaFallida genera notificación al admin y al donante),
    // sin un único id/Location que devolver. 201 Created no encaja con esa cardinalidad N.
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @GetMapping("/persona/{personaId}")
  public ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(@PathVariable UUID personaId) {
    return ResponseEntity.ok(service.obtenerPorPersona(personaId));
  }
}
