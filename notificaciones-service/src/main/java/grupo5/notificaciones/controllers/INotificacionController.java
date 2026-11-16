package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.ConteoNoLeidasDTO;
import grupo5.notificaciones.dto.FiltroBandeja;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notificaciones", description = "API para gestión de notificaciones y eventos")
public interface INotificacionController {

  @Operation(
      summary = "Procesar un evento de dominio de forma asincrónica",
      description = "Recibe un evento y genera las notificaciones correspondientes.")
  @ApiResponse(responseCode = "202", description = "Evento aceptado para procesamiento")
  @PostMapping("/api/notificaciones/eventos")
  ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto);

  /**
   * Alias legado de {@link #procesarEvento}. Se retira cuando los clientes Feign migren a AMQP.
   *
   * @deprecated usar {@code POST /api/notificaciones/eventos} o los eventos AMQP (DTI-13).
   */
  @Deprecated(since = "ENTREGA_4", forRemoval = true)
  @Operation(summary = "Procesar un evento de dominio (alias legado)", deprecated = true)
  @ApiResponse(responseCode = "202", description = "Evento aceptado para procesamiento")
  @PostMapping("/notificaciones")
  ResponseEntity<Void> procesarEventoLegacy(@Valid @RequestBody EventoNotificableDTO dto);

  @Operation(
      summary = "Obtener notificaciones por persona y/o estado",
      description = "Devuelve una colección filtrada de notificaciones.")
  @ApiResponse(responseCode = "200", description = "Lista de notificaciones devuelta")
  @ApiResponse(responseCode = "400", description = "Estado inválido")
  @GetMapping("/api/notificaciones")
  ResponseEntity<List<NotificacionDTO>> obtenerNotificaciones(
      @RequestParam(required = false) UUID personaId,
      @RequestParam(required = false) String estado);

  @Operation(summary = "Obtener una notificación específica por su ID")
  @ApiResponse(responseCode = "200", description = "Notificación encontrada")
  @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
  @GetMapping("/api/notificaciones/{id}")
  ResponseEntity<NotificacionDTO> obtenerPorId(@PathVariable UUID id);

  @Operation(
      summary = "Una página de la bandeja de notificaciones de una persona",
      description =
          "Filtra por lectura (leida), texto (q, sin mayúsculas ni tildes), tipo y excluirTipo"
              + " (repetibles; excluirTipo deja entrar las que no tienen tipo); orden recientes"
              + " (por defecto) o antiguas, desempatado por id. Sin page ni size, la página 0 de"
              + " 20; el total va en X-Total-Count.")
  @ApiResponse(responseCode = "200", description = "Página de notificaciones")
  @ApiResponse(responseCode = "400", description = "Parámetro fuera de rango (ERR-CSR-003)")
  @GetMapping("/api/notificaciones/persona/{personaId}")
  ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(
      @PathVariable UUID personaId, @ParameterObject FiltroBandeja filtro);

  @Operation(
      summary = "Una página de la bandeja de una persona (alias legado, solo QA)",
      deprecated = true)
  @ApiResponse(responseCode = "200", description = "Página de notificaciones")
  @ApiResponse(responseCode = "400", description = "Parámetro fuera de rango (ERR-CSR-003)")
  @GetMapping("/notificaciones/persona/{personaId}")
  ResponseEntity<List<NotificacionDTO>> obtenerPorPersonaLegacy(
      @PathVariable UUID personaId, @ParameterObject FiltroBandeja filtro);

  @Operation(
      summary = "Obtener el conteo de notificaciones no leídas de una persona (para badge UI)")
  @ApiResponse(responseCode = "200", description = "Conteo devuelto exitosamente")
  @GetMapping("/api/notificaciones/persona/{personaId}/no-leidas/conteo")
  ResponseEntity<ConteoNoLeidasDTO> contarNoLeidas(@PathVariable UUID personaId);

  @Operation(summary = "Marcar una notificación como leída o no leída")
  @ApiResponse(responseCode = "200", description = "Estado de lectura actualizado")
  @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
  @PatchMapping("/api/notificaciones/{id}/leida")
  ResponseEntity<NotificacionDTO> marcarLeida(
      @PathVariable UUID id, @RequestParam(required = false, defaultValue = "true") boolean leida);

  @Operation(summary = "Marcar todas las notificaciones de una persona como leídas")
  @ApiResponse(responseCode = "204", description = "Todas las notificaciones marcadas como leídas")
  @PatchMapping("/api/notificaciones/persona/{personaId}/marcar-todas-leidas")
  ResponseEntity<Void> marcarTodasComoLeidas(@PathVariable UUID personaId);
}
