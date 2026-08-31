package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.notificaciones.infrastructure.persistencia.entities.PersonaEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.PersonaPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataPersonaRepository;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class PersonaRepositoryJpaAdapter implements IPersonaRepository {
  private final SpringDataPersonaRepository springDataRepo;
  private final PersonaPersistenciaMapper mapper;

  public PersonaRepositoryJpaAdapter(
      SpringDataPersonaRepository springDataRepo, PersonaPersistenciaMapper mapper) {
    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  public Persona save(Persona aggregate) {
    PersonaEntity entity = mapper.toEntity(aggregate);
    PersonaEntity saved = springDataRepo.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Persona> saveAll(List<Persona> aggregates) {
    List<PersonaEntity> entities = aggregates.stream().map(mapper::toEntity).toList();
    return springDataRepo.saveAll(entities).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Persona> findAll() {
    return springDataRepo.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<Persona> findById(UUID id) {
    return springDataRepo.findById(id).map(mapper::toDomain);
  }

  @Override
  public void delete(Persona aggregate) {
    springDataRepo.deleteById(aggregate.getId());
  }

  @Override
  public boolean existsById(UUID id) {
    return springDataRepo.existsById(id);
  }

  @Override
  public long count() {
    return springDataRepo.count();
  }

  @Override
  public void deleteAll() {
    springDataRepo.deleteAll();
  }
}
