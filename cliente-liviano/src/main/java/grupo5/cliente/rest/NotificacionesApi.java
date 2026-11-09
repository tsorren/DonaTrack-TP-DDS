package grupo5.cliente.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;

/**
 * Lo que el cliente usa de Notificaciones (notificaciones-service, NotificacionController). Los
 * records toman solo los campos que muestra la pantalla; el resto del JSON se ignora.
 */
interface NotificacionesApi {

  @GetExchange("/api/notificaciones/persona/{personaId}")
  List<Notificacion> porPersona(
      @PathVariable UUID personaId, @RequestParam(required = false) Boolean leida);

  @GetExchange("/api/notificaciones/persona/{personaId}/no-leidas/conteo")
  Conteo conteoNoLeidas(@PathVariable UUID personaId);

  @PatchExchange("/api/notificaciones/{id}/leida")
  Notificacion marcarLeida(@PathVariable UUID id, @RequestParam boolean leida);

  @PatchExchange("/api/notificaciones/persona/{personaId}/marcar-todas-leidas")
  void marcarTodasLeidas(@PathVariable UUID personaId);

  record Notificacion(
      UUID id,
      String mensaje,
      String estado,
      LocalDateTime fechaCreacion,
      boolean leida,
      LocalDateTime fechaLectura) {}

  record Conteo(long noLeidas) {}
}
