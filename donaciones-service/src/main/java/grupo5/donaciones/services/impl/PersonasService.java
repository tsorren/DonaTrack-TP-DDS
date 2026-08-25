package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.config.AdminConstantes;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PersonasService implements IPersonasService {

  private final IPersonasRepository repository;
  private final PersonaMapper mapper;
  private final NotificacionesAsyncService notificacionesAsyncService;

  public PersonasService(
      IPersonasRepository repository,
      PersonaMapper mapper,
      NotificacionesAsyncService notificacionesAsyncService) {
    this.repository = repository;
    this.mapper = mapper;
    this.notificacionesAsyncService = notificacionesAsyncService;
  }

  @Override
  public PersonaOutputDTO crearPersona(PersonaInputDTO input) {
    Persona persona = mapper.toEntity(input);

    if (persona instanceof Juridica juridica) {
      juridica.getRepresentantes().forEach(repository::save);
    }

    Persona guardada = repository.save(persona);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.sincronizarPersona(mapper.toReplicaDTO(guardada));

    return mapper.toOutputDTO(guardada);
  }

  @Override
  public List<PersonaOutputDTO> consultarPersonas(TipoPersona tipo) {
    List<Persona> personas = repository.findAll();
    if (tipo != null) {
      personas = personas.stream().filter(p -> p.getTipoPersona() == tipo).toList();
    }
    return personas.stream().map(mapper::toOutputDTO).toList();
  }

  @Override
  public PersonaOutputDTO actualizarPersona(UUID id, PersonaInputDTO input) {
    Persona persona =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    mapper.updateEntity(persona, input);

    if (persona instanceof Juridica juridica) { // TODO: sacar instanceof si se puede
      juridica.getRepresentantes().forEach(repository::save);
    }

    Persona guardada = repository.save(persona);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.sincronizarPersona(mapper.toReplicaDTO(guardada));

    return mapper.toOutputDTO(guardada);
  }

  @Override
  public void eliminarPersona(UUID id) {
    Persona persona =
        repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));

    persona.anonimizar();
    repository.save(persona);

    // Sincronizar asincrónicamente con el servicio de notificaciones
    notificacionesAsyncService.anonimizarPersona(id);
  }

  @Override
  public UUID obtenerIdPersonaAdministradora() {
    return repository
        .findByDocumento(AdminConstantes.DOCUMENTO_ADMIN)
        .map(Persona::getId)
        .orElse(null);
  }
}
