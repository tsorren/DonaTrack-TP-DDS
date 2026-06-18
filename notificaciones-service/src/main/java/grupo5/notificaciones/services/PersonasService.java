package grupo5.notificaciones.services;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonasRepository;
import grupo5.notificaciones.services.mappers.PersonaMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PersonasService implements IPersonasService {

  private final IPersonasRepository repository;
  private final PersonaMapper mapper;

  public PersonasService(IPersonasRepository repository, PersonaMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void sincronizar(PersonaReplicaDTO dto) {
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
  }
}
