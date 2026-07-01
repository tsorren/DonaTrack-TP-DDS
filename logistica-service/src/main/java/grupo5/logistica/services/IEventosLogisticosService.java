package grupo5.logistica.services;

import grupo5.logistica.dto.eventos.EventoLogisticoResponseDTO;
import grupo5.logistica.dto.eventos.MarcarEventoProcesadoRequestDTO;
import java.util.List;
import java.util.UUID;

public interface IEventosLogisticosService {
  List<EventoLogisticoResponseDTO> listarPendientes();

  EventoLogisticoResponseDTO marcarProcesado(UUID id, MarcarEventoProcesadoRequestDTO dto);

  EventoLogisticoResponseDTO obtenerPorId(UUID id);
}
