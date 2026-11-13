package grupo5.notificaciones.controllers;

import static grupo5.notificaciones.seguridad.Acceso.ADMIN;
import static grupo5.notificaciones.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.notificaciones.seguridad.Acceso.MARCAR_SU_NOTIFICACION;
import static grupo5.notificaciones.seguridad.Acceso.VER_SUS_NOTIFICACIONES;

import grupo5.notificaciones.dto.ConteoNoLeidasDTO;
import grupo5.notificaciones.dto.FiltroBandeja;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequiredArgsConstructor
public class NotificacionController implements INotificacionController {

  static final String HEADER_TOTAL = "X-Total-Count";

  private final NotificacionService service;

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping("/api/notificaciones/eventos")
  public ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto) {
    service.procesar(dto);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  /**
   * @deprecated usar {@code POST /api/notificaciones/eventos} o los eventos AMQP (DTI-13).
   */
  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @Deprecated(since = "ENTREGA_4", forRemoval = true)
  @PostMapping("/notificaciones")
  public ResponseEntity<Void> procesarEventoLegacy(@Valid @RequestBody EventoNotificableDTO dto) {
    return procesarEvento(dto);
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

  /**
   * Una página de la bandeja (R11-14): el cuerpo es el tramo pedido y {@code X-Total-Count},
   * cuántas cumplen los filtros. Sin {@code page} ni {@code size}, la página 0 de 20.
   */
  @Override
  @PreAuthorize(VER_SUS_NOTIFICACIONES)
  @GetMapping("/api/notificaciones/persona/{personaId}")
  public ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(
      @PathVariable UUID personaId, FiltroBandeja filtro) {
    NotificacionService.PaginaNotificaciones pagina = service.bandeja(personaId, filtro);
    return ResponseEntity.ok()
        .header(HEADER_TOTAL, String.valueOf(pagina.total()))
        .body(pagina.notificaciones());
  }

  /** Alias legado de la ruta anterior, solo para QA: lo usa el ADMIN (matriz de permisos, §8). */
  @Override
  @GetMapping("/notificaciones/persona/{personaId}")
  public ResponseEntity<List<NotificacionDTO>> obtenerPorPersonaLegacy(
      @PathVariable UUID personaId, FiltroBandeja filtro) {
    return obtenerPorPersona(personaId, filtro);
  }

  @Override
  @PreAuthorize(VER_SUS_NOTIFICACIONES)
  @GetMapping("/api/notificaciones/persona/{personaId}/no-leidas/conteo")
  public ResponseEntity<ConteoNoLeidasDTO> contarNoLeidas(@PathVariable UUID personaId) {
    long count = service.contarNoLeidas(personaId);
    return ResponseEntity.ok(new ConteoNoLeidasDTO(count));
  }

  @Override
  @PreAuthorize(MARCAR_SU_NOTIFICACION)
  @PatchMapping("/api/notificaciones/{id}/leida")
  public ResponseEntity<NotificacionDTO> marcarLeida(
      @PathVariable UUID id, @RequestParam(required = false, defaultValue = "true") boolean leida) {
    Optional<NotificacionDTO> resultado =
        leida ? service.marcarComoLeida(id) : service.marcarComoNoLeida(id);
    return resultado.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize(VER_SUS_NOTIFICACIONES)
  @PatchMapping("/api/notificaciones/persona/{personaId}/marcar-todas-leidas")
  public ResponseEntity<Void> marcarTodasComoLeidas(@PathVariable UUID personaId) {
    service.marcarTodasComoLeidas(personaId);
    return ResponseEntity.noContent().build();
  }
}
