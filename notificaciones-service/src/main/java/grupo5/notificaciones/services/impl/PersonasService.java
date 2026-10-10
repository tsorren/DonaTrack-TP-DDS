package grupo5.notificaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.exceptions.PersonaYaAnonimizadaException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.Anonimizable;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.IPersonasService;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PersonasService implements IPersonasService {

  private final IPersonaRepository repository;
  private final INotificacionRepository notificacionRepository;
  private final PersonaMapper mapper;

  public PersonasService(
      IPersonaRepository repository,
      INotificacionRepository notificacionRepository,
      PersonaMapper mapper) {
    this.repository = repository;
    this.notificacionRepository = notificacionRepository;
    this.mapper = mapper;
  }

  @Override
  public void sincronizar(PersonaReplicaDTO dto) {
    if (dto != null && dto.id() != null) {
      repository
          .findById(dto.id())
          .ifPresent(
              existente -> {
                if (Anonimizable.VALOR_STRING.equals(existente.getDenominacion())) {
                  throw new PersonaYaAnonimizadaException(dto.id());
                }
              });
    }
    Persona persona = mapper.toEntity(dto);
    repository.save(persona);
  }

  @Override
  public void anonimizar(UUID id) {
    Persona persona =
        repository
            .findById(id)
            .orElseThrow(() -> new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO));
    persona.anonimizar();
    repository.save(persona);

    // Coordinate the anonimizacion of related notifications
    java.util.List<Notificacion> notificaciones = notificacionRepository.findByPersonaId(id);
    for (Notificacion notificacion : notificaciones) {
      notificacion.anonimizar();
      notificacionRepository.save(notificacion);
    }
  }

  @Override
  public PersonaReplicaDTO obtenerPersona(UUID id) {
    Persona persona =
        repository
            .findById(id)
            .orElseThrow(() -> new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO));
    return mapper.toReplicaDTO(persona);
  }
}
