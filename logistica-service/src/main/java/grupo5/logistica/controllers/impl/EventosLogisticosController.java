package grupo5.logistica.controllers.impl;

import grupo5.logistica.dto.eventos.EventoLogisticoResponseDTO;
import grupo5.logistica.dto.eventos.MarcarEventoProcesadoRequestDTO;
import grupo5.logistica.services.IEventosLogisticosService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logistica/eventos")
public class EventosLogisticosController {
  private final IEventosLogisticosService eventosService;

  public EventosLogisticosController(IEventosLogisticosService eventosService) {
    this.eventosService = eventosService;
  }

  @GetMapping("/pendientes")
  public ResponseEntity<List<EventoLogisticoResponseDTO>> listarPendientes() {
    return ResponseEntity.ok(eventosService.listarPendientes());
  }

  @GetMapping("/{id}")
  public ResponseEntity<EventoLogisticoResponseDTO> obtenerPorId(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(eventosService.obtenerPorId(id));
  }

  @PatchMapping("/{id}/procesado")
  public ResponseEntity<EventoLogisticoResponseDTO> marcarProcesado(
      @PathVariable("id") UUID id, @RequestBody MarcarEventoProcesadoRequestDTO dto) {
    return ResponseEntity.ok(eventosService.marcarProcesado(id, dto));
  }
}
