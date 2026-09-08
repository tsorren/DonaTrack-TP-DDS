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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificacionController implements INotificacionController {

  private final NotificacionService service;

  @Override
  public ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @Override
  public ResponseEntity<List<NotificacionDTO>> obtenerNotificaciones(
      @RequestParam(required = false) UUID personaId,
      @RequestParam(required = false) String estado) {

    if (personaId != null) {
      return ResponseEntity.ok(service.obtenerPorPersona(personaId));
    }

    // Como comportamiento por defecto devolvemos lista vacía si no hay filtros.
    return ResponseEntity.ok(List.of());
  }

  @Override
  public ResponseEntity<NotificacionDTO> obtenerPorId(@PathVariable UUID id) {
    // TODO: A futuro se debería llamar a service.obtenerPorId(id)
    return ResponseEntity.notFound().build();
  }
}
