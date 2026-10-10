package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notificaciones", description = "API para gestión de notificaciones y eventos")
public interface INotificacionController {

  @Operation(
      summary = "Procesar un evento de dominio de forma asincrónica",
      description = "Recibe un evento y genera las notificaciones correspondientes.")
  @ApiResponse(responseCode = "202", description = "Evento aceptado para procesamiento")
  @PostMapping({"/api/notificaciones/eventos", "/notificaciones"})
  ResponseEntity<Void> procesarEvento(@Valid @RequestBody EventoNotificableDTO dto);

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

  @Operation(summary = "Obtener historial de notificaciones de una persona (legacy/alias)")
  @ApiResponse(responseCode = "200", description = "Historial de notificaciones devuelto")
  @GetMapping({"/notificaciones/persona/{personaId}", "/api/notificaciones/persona/{personaId}"})
  ResponseEntity<List<NotificacionDTO>> obtenerPorPersona(@PathVariable UUID personaId);
}
