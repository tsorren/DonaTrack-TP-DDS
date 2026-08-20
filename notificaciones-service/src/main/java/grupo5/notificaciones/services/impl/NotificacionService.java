package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
  private final INotificacionRepository repository;
  private final IPersonaRepository personaRepository;
  private final NotificacionSender sender;
  private final EventoMapper mapper;

  public NotificacionService(
      INotificacionRepository repository,
      IPersonaRepository personaRepository,
      NotificacionSender sender,
      EventoMapper mapper) {
    this.repository = repository;
    this.personaRepository = personaRepository;
    this.sender = sender;
    this.mapper = mapper;
  }

  public void procesar(EventoNotificableDTO dto) {
    EventoNotificable evento = mapper.toEntity(dto);

    List<Notificacion> notificaciones = evento.generarNotificaciones();
    repository.saveAll(notificaciones);

    for (Notificacion notificacion : notificaciones) {
      Persona persona = personaRepository.findById(notificacion.getPersonaId()).orElse(null);
      notificacion.notificar(persona, sender);
      repository.save(notificacion);
    }
  }

  public List<NotificacionDTO> obtenerPorPersona(UUID personaId) {
    return repository.findByPersonaId(personaId).stream()
        .map(
            n ->
                new NotificacionDTO(
                    n.getId(),
                    n.getMensaje(),
                    n.getEstadoNotificacion().name(),
                    n.getFechaCreacion()))
        .toList();
  }
}
