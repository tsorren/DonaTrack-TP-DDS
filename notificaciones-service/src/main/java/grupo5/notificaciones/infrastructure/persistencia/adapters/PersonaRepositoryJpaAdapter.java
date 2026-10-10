package grupo5.notificaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.notificaciones.infrastructure.persistencia.entities.PersonaEntity;
import grupo5.notificaciones.infrastructure.persistencia.mappers.PersonaPersistenciaMapper;
import grupo5.notificaciones.infrastructure.persistencia.repositories.SpringDataPersonaRepository;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PersonaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Persona, PersonaEntity, SpringDataPersonaRepository>
    implements IPersonaRepository {

  private final SpringDataPersonaRepository springDataRepo;
  private final PersonaPersistenciaMapper mapper;

  public PersonaRepositoryJpaAdapter(
      SpringDataPersonaRepository springDataRepo, PersonaPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  public Persona save(Persona domainEntity) {
    PersonaEntity existing = this.springDataRepo.findById(domainEntity.getId()).orElse(null);
    PersonaEntity entityToSave = this.mapper.toEntity(domainEntity, existing);

    // Guardar en la DB y mapear de vuelta a dominio para respetar el contrato
    PersonaEntity savedEntity = this.springDataRepo.save(entityToSave);
    return this.mapper.toDomain(savedEntity);
  }

  @Override
  public List<Persona> saveAll(List<Persona> aggregates) {
    if (aggregates == null) return List.of();
    return aggregates.stream().map(this::save).toList();
  }
}
