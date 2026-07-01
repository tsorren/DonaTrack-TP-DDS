package grupo5.logistica.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.logistica.dto.eventos.EventoLogisticoResponseDTO;
import grupo5.logistica.dto.eventos.MarcarEventoProcesadoRequestDTO;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import grupo5.logistica.models.repositories.IEventosLogisticosRepository;
import grupo5.logistica.services.IEventosLogisticosService;
import grupo5.logistica.services.mappers.EventoLogisticoMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EventosLogisticosService implements IEventosLogisticosService {
  private final IEventosLogisticosRepository eventosRepository;
  private final EventoLogisticoMapper eventoMapper;

  public EventosLogisticosService(
      IEventosLogisticosRepository eventosRepository, EventoLogisticoMapper eventoMapper) {
    this.eventosRepository = eventosRepository;
    this.eventoMapper = eventoMapper;
  }

  @Override
  public List<EventoLogisticoResponseDTO> listarPendientes() {
    return eventosRepository.findPendientes().stream().map(eventoMapper::toResponseDTO).toList();
  }

  @Override
  public EventoLogisticoResponseDTO marcarProcesado(UUID id, MarcarEventoProcesadoRequestDTO dto) {
    EventoLogistico evento = buscarEvento(id);
    evento.marcarProcesado();
    return eventoMapper.toResponseDTO(eventosRepository.save(evento));
  }

  @Override
  public EventoLogisticoResponseDTO obtenerPorId(UUID id) {
    return eventoMapper.toResponseDTO(buscarEvento(id));
  }

  private EventoLogistico buscarEvento(UUID id) {
    return eventosRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
  }
}
